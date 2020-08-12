package com.imooc.netty;

import java.io.Serializable;

/**
 * 这个类是用于接收前端发送来的聊天信息的实体类
 */
public class DataContent implements Serializable {
    private static final long serialVersionUID = -2459444157511262346L;//实现一个序列化接口
    private Integer action; //动作类型，也就是消息类型，区别该消息是心跳，发送消息，签收消息，还是第一次websocket连接
    private ChatMsg chatMsg; //用户的聊天内容entity
    private String extand; //扩展参数

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Integer getAction() {
        return action;
    }

    public void setAction(Integer action) {
        this.action = action;
    }

    public ChatMsg getChatMsg() {
        return chatMsg;
    }

    public void setChatMsg(ChatMsg chatMsg) {
        this.chatMsg = chatMsg;
    }

    public String getExtand() {
        return extand;
    }

    public void setExtand(String extand) {
        this.extand = extand;
    }
}
