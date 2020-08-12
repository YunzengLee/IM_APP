package com.imooc.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * 客户端发送请求，服务器返回 hello netty
 */
public class HelloServer {
    public static void main(String[] args) throws InterruptedException {
        //定义一对线程组
        //主线程组，用于接收客户端连接，但不做任何处理
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        //从线程组，老板线程组把任务丢给他处理。
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            //netty服务器的创建，serverBootstrap是启动类
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workGroup)//设置主从线程组
                    .channel(NioServerSocketChannel.class)//设置nio的双向通道
                    .childHandler(new HelloServerInitializer());//子处理器，用于处理workGroup

            //启动server并设置8088为启动端口号，启动方式为同步。
            ChannelFuture channelFuture = serverBootstrap.bind(8088).sync();//绑定端口，设置为同步的启动方式，也就是等待8088启动完毕

            //监听关闭的channel，设置为同步的方式。
            channelFuture.channel().closeFuture().sync();
        }finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
