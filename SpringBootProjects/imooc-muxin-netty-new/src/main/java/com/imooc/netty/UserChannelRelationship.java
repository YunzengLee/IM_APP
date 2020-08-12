package com.imooc.netty;

import io.netty.channel.Channel;

import java.util.HashMap;

/**
 * 用户id和channel的关联关系处理
 */
public class UserChannelRelationship {

    private static HashMap<String, Channel> manager = new HashMap<>();

    public static void put(String senderId, Channel channel){
        manager.put(senderId,channel);
    }
    public static Channel get(String senderId){
        return manager.get(senderId);
    }

    //测试用的函数
    public static void output(){
        for (HashMap.Entry<String,Channel> entry: manager.entrySet()){
            System.out.println("UserId"+entry.getKey()+",Channel id:"+entry.getValue().id().asLongText());
        }

    }
}
