package com.imooc.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.stereotype.Component;


@Component
public class WSServer {


    //改造成单例模式
    private static class SingletionWSServer{
        static final WSServer instance = new WSServer();
    }
    public static WSServer getInstance(){
        return SingletionWSServer.instance;
    }


    private EventLoopGroup mainGroup;
    private EventLoopGroup subGroup;
    private ServerBootstrap server;
    private ChannelFuture future;

    public WSServer(){
        //定义一对线程组
        mainGroup = new NioEventLoopGroup();
        subGroup = new NioEventLoopGroup();
        server = new ServerBootstrap();
        server.group(mainGroup,subGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new WSServerInitializer());

    }
    public void start(){
        //不需要同步，因为不是在main方法里面，是在springboot里面
        this.future = server.bind(8088);
        System.err.println("netty websocket server 启动完毕...");
    }
    //不需要关闭，因为依托给了springboot容器

}
