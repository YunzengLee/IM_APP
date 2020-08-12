package com.imooc.netty;

import com.imooc.SpringUtil;
import com.imooc.enums.MsgActionEnum;
import com.imooc.service.UserService;
import com.imooc.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于检测channel的心跳的handler类
 * 继承ChannelInboundHandlerAdapter类，从而不需要实现channelRead0方法
 */
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

		//判断evt是否属于IdleStateEvent，（用于触发用户事件，包含读空闲/写空闲/读写空闲）
		if(evt instanceof IdleStateEvent){ //IdleStateEvent是当channel空闲时触发的用户事件（user event）
			IdleStateEvent event = (IdleStateEvent) evt;//强制类型转换
			if (event.state() == IdleState.READER_IDLE){
				System.out.println("进入读空闲。。");
			}else if (event.state() == IdleState.WRITER_IDLE){
				System.out.println("进入写空闲。。");
			}else if (event.state() == IdleState.ALL_IDLE){
				System.out.println("channel关闭前，users的数量为："+ChatHandler.users.size());
				//说明用户进入了一个飞行模式之类的状态
				System.out.println("进入读写空闲。。");
				ctx.channel().close();//关闭无用的channel，以免资源浪费。这个关闭是一种异步的方式。

				System.out.println("channel关闭后，users的数量为："+ChatHandler.users.size());
			}
		}
		super.userEventTriggered(ctx, evt);
	}


	/**
	 *  注意，这个handler类本来是继承：SimpleChannelInboundHandler<TextWebSocketFrame>
	 *      但是继承这个类就必须实现下面这个channelRead方法，
	 *      此处我们直接让这个handler类去继承SimpleChannelInboundHandler的父类：ChannelInboundHandlerAdapter
	 *      这个父类中有个方法为userEventTriggered，是在用户事件发生时触发的。我们需要重写这个方法，
	 *      但不需要重写channelRead0这个方法
	 *      因为就直接去继承ChannelInboundHandlerAdapter类
	 *
	 */
	//@Override
	//protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
    //}
}
