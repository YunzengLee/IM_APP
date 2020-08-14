package com.imooc.netty;

import com.imooc.SpringUtil;
import com.imooc.enums.MsgActionEnum;
import com.imooc.service.UserService;
import com.imooc.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 处理消息的handler
 * TextWebSocketFrame：在netty中用于为websocket专门处理文本的对象，frames是消息的载体
 */
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

	//用于记录和管理所有客户端的channel
	public static ChannelGroup users = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);//记录和管理所有客户端的channel，都保存在一个组里

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {

		String content = msg.text();//获取客户端传输过来的字符串
		System.out.println("接受到的数据："+content);
		Channel currentChannel = ctx.channel();
		//接下来需要处理字符串，转化为一些实体类，在当前包下写一些实体类： DataContent  ChatMsg

		//1. 获取客户端发来的消息
		DataContent dataContent = JsonUtils.jsonToPojo(content,DataContent.class);//把字符串转换为一个类
		Integer action = dataContent.getAction();


		//2.判断消息类型，根据不同的类型处理不同的业务
		if(action == MsgActionEnum.CONNECT.type){
			//2.1 当websocket第一次open的时候，初始化channel，把用户的channel和userid关联起来
			String senderId =  dataContent.getChatMsg().getSenderId();
			UserChannelRelationship.put(senderId,currentChannel); //就是把用户id和channel丢到一个HashMap里去

			//测试
//			for (Channel c:users){
//				System.out.println(c.id().asLongText());
//			}
			System.out.println("前端建立了websocket连接");
//			UserChannelRelationship.output();

		}else if(action == MsgActionEnum.CHAT.type) {
			//2.2 聊天类型的消息，把聊天记录保存到数据库，同时标记消息的签收状态（未签收）。一般聊天消息放到数据库中需要加密
			ChatMsg chatMsg = dataContent.getChatMsg();
			String msgText =  chatMsg.getMsg();
			String receiverId = chatMsg.getReceiverId();
			String senderId = chatMsg.getSenderId();

			//保存到数据库并设为未签收
//			此时需要调用service的方法进行数据库保存，但是在handler中无法注入service，此时需要依托spring
			//SpringUtil类就是用来干这事的
			UserService userService = (UserService) SpringUtil.getBean("userServiceImpl");//如果不去定义service的名字，spring会把类名第一个字母小写后作为名字
			//此时需要在UserService中定义一个将聊天消息保存到数据库的方法
			String msgId = userService.saveMsg(chatMsg);//保存到数据库并返回msg的id，这个id是在保存到数据库时生成的
			chatMsg.setMsgId(msgId);//将返回的id赋值给chatMsg

			DataContent dataContentMsg = new DataContent();
			dataContentMsg.setChatMsg(chatMsg);

			//此处应该是一个发送的操作,将chatMsg发送给接受者，接收者根据其中的msgId进行签收。
			Channel receiverChannel = UserChannelRelationship.get(receiverId);//找到接收者与本服务器的channel，把消息发到该channel上
			if(receiverChannel == null){
				//channel为空代表用户离线，应当推送消息（使用第三方工具：JPUSh，个推，小米推送）
                //但是本项目的处理是，用户上线时自动去数据库查询一次未接收的消息，然后拉取到本地


			}else{
				//当receiverChannel不为空，从ChannelGroup中查找对应的channel是否存在
				Channel findChannel = users.find(receiverChannel.id());
				if(findChannel!=null){
					//用户在线
					receiverChannel.writeAndFlush(
							new TextWebSocketFrame(
									JsonUtils.objectToJson(dataContentMsg)));
							//将chatMsg转为json字符串后发送
				}else{
					//用户离线 推送消息。
					//与上面同理，不主动推送，等用户上线时由用户自己去数据库读取未接收的消息。

				}
			}


		}else if(action == MsgActionEnum.SIGNED.type) {
			//2.3 签收消息类型，针对具体的消息进行签收，实际就是修改数据库中消息的签收状态
				//签收并不是指接收消息的用户已经查看了消息，而是指接收方的手机收到了消息，类似于人签收了快递，但是打没打开是自己的事
			UserService userService = (UserService) SpringUtil.getBean("userServiceImpl");//如果不去定义service的名字，spring会把类名第一个字母小写后作为名字
			//扩展字段在signed类型的消息中代表需要去签收的消息的id
			String msgIdStr = dataContent.getExtand();//dataContent的extend里存放的是消息id，如果有多条消息就用用逗号间隔。
			String msgIds[] = msgIdStr.split(",");
			List<String> msgIdList = new ArrayList<>();
			for(String mid:msgIds){
				if(StringUtils.isNotBlank(mid)){
					msgIdList.add(mid);
				}
			}
			System.out.println(msgIdList.toString());//

			//批量签收msgIdList中的msg
			if(msgIdList!=null && !msgIdList.isEmpty() && msgIdList.size()>0){
				//签收
				userService.updateMsgSigned(msgIdList);
			}



		}else if(action == MsgActionEnum.KEEPALIVE.type) {

			//2.4 心跳类型的消息
			System.out.println("收到来自channel为【"+currentChannel+"】的心跳包"); //这个发送的数据仅仅是为了发送而发送，只要客户端收到了客户端的消息，就会刷新读写空闲时间，避免进入读写空闲

		}







//		for(Channel channel: users){
//			channel.writeAndFlush(
//					new TextWebSocketFrame("[服务器在：]"
//							+ LocalDateTime.now()
//							+ "接收到消息为：" + content));
//			//将数据写入缓冲区并刷到客户端，这个参数不能是字符串，而应当是TextWebSocketFrame，它是一个载体
//		}

		//下面这个方法和上面的for循环一致
//        clients.writeAndFlush(
//                new TextWebSocketFrame("[服务器在：]"
//                + LocalDateTime.now()
//                + "接收到消息为：" + content));


	}

	/**
	 * 当客户端打开连接，获取客户端的channel并放到channelGroup中进行管理
	 * @param ctx
	 * @throws Exception
	 */
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		//客户端和服务器端连接之后二者就有了一个双向通道channel，添加到channelGrounp里
		users.add(ctx.channel());

//        super.handlerAdded(ctx);
	}

	//用户离开客户端，关闭浏览器时触发，
	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		//当触发handlerRemoved，ChannelGroup会自动移除对应客户端的channel
		//因此下面这句不必加上。
//        users.remove(ctx.channel());

		String channelId = ctx.channel().id().asShortText();
		System.out.println("客户端被移除，channelId为："+channelId);

	}

	/**
	 * 重写方法
	 * @param ctx
	 * @param cause
	 * @throws Exception
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		//发生异常则关闭连接（也就是关闭channel），随后从ChannelGroup中移出
		ctx.channel().close();
		users.remove(ctx.channel());
//		super.exceptionCaught(ctx, cause);
	}
}
