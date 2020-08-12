package com.imooc.netty.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.time.LocalDateTime;

/**
 * 处理消息的handler
 * TextWebSocketFrame：在netty中用于为websocket专门处理文本的对象，frames是消息的载体
 */
public class ChartHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    //用于记录和管理所有客户端的channel
    private static ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);//记录和管理所有客户端的channel，都保存在一个组里

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        String content = msg.text();//获取客户端传输过来的字符串
        System.out.println("接受到的数据："+content);

        for(Channel channel:clients){
            channel.writeAndFlush(
                    new TextWebSocketFrame("[服务器在：]"
                            + LocalDateTime.now()
                            + "接收到消息为：" + content));
            //将数据写入缓冲区并刷到客户端，这个参数不能是字符串，而应当是TextWebSocketFrame，它是一个载体
        }

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
        clients.add(ctx.channel());

//        super.handlerAdded(ctx);
    }
    //用户离开客户端，关闭浏览器时触发，
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        //当触发handlerRemoved，ChannelGroup会自动移除对应客户端的channel
        //因此下面这句不必加上。
//        clients.remove(ctx.channel());
        System.out.println("客户端断开，channel对应的长id为："
                + ctx.channel().id().asLongText());//每个channel都有一个id，且有一个长id和一个短id。
        System.out.println("客户端断开，channel对应的短id为："
                + ctx.channel().id().asShortText());
    }
}
