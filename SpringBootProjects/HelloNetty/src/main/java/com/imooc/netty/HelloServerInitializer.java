package com.imooc.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * 初始化器，channel注册后会执行里面相应的初始化方法
 */
public class HelloServerInitializer extends ChannelInitializer<SocketChannel> {

    //重写
    protected void initChannel(SocketChannel channel) throws Exception {

        //每一个channel由多个handler（助手类）共同组成该channel对应的管道（pipline）
        // 即一个channel对应一个pipline，一个pipline有多个handler
        //通过SocketChannel获取对应 的管道
        ChannelPipeline pipline = channel.pipeline();

        //通过管道添加handler助手类，可以理解为拦截器
        //HttpServerCodec是netty自己提供的助手类
        //当请求到服务器，我们需要做编解码，响应到客户端做编码
        pipline.addLast("HttpServerCodec",new HttpServerCodec());

        //添加自定义助手类，返回“hello netty~”字符串
        pipline.addLast("customHandler",new CustomHandler());
    }
}
