package com.imooc.mapper;

import com.imooc.pojo.ChatMsg;
import com.imooc.utils.MyMapper;
//一般需要在这种Mapper接口上（也就是DAO接口）加个@Reposity注解，此处没有加应该是MyMapper的作用？
public interface ChatMsgMapper extends MyMapper<ChatMsg> {
}