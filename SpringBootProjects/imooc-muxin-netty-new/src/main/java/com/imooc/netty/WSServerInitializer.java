package com.imooc.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

public class WSServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipline = socketChannel.pipeline();

        //wbsocket基于http协议，，所以要有http编解码器
        pipline.addLast(new HttpServerCodec());

        //Chunked类添加了对写大数据流的支持
        pipline.addLast(new ChunkedWriteHandler());
        //对httpMessage进行聚合，聚合为FullHttpRequest或FullHttpResponse
        //几乎在netty中的编程都会用到此handler
        pipline.addLast(new HttpObjectAggregator(1024*64));//聚合器，参数是消息的最大长度

        //====================以上是用于支持http协议=======================

        //====================增加心跳支持start=======================

        //针对客户端，如果在1分钟时没有向服务器端发送读写心跳（ALL）则主动断开
        // 如果是读空闲或写空闲，不作处理
        // 在添加自定义的空闲状态检测之前，这里需要加上netty提供的IdleStateHandler
        pipline.addLast(new IdleStateHandler(40,40,60));
        //关于三个参数的解释：此处为了测试，先将allidle的时长设为6秒,也就是说，如果6秒内没有接收到客户端的心跳包，就进入读写空闲状态，就会触发HeartBeatHandler类中userEventTriggered函数

        // 自定义的空闲状态检测
        pipline.addLast(new HeartBeatHandler());
        //====================增加心跳支持end=======================


        //websocket服务器处理的协议，用于给客户端一个连接访问的路由：/ws
        /**
         * 本handler会帮助处理一些繁重复杂的事 包括握手动作（handshaking，包括close，ping，pong）
         * ping+pong=心跳
         * 对于websocket来讲，都是以frames进行传输的，不同的数据类型对应的frames也不同
         */
        pipline.addLast(new WebSocketServerProtocolHandler("/ws"));
        //参数是一个路径，一开始建立连接时会使用到，ws代表是websocket

        //添加自定义助手类
        pipline.addLast(new ChatHandler());


    }
}
