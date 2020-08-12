package com.imooc;

import com.imooc.netty.WSServer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class NettyBooter implements ApplicationListener<ContextRefreshedEvent> {
    //实现监听接口，泛型是一个事件类型，因为是需要对上下文做判断，所以使用上下文事件

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if(contextRefreshedEvent.getApplicationContext().getParent() == null){
            //getApplicationContext()是获取上下文
            try {
                WSServer.getInstance().start(); //启动WSServer
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
