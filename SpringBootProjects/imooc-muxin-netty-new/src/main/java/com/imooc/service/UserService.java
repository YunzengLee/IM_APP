package com.imooc.service;

import com.imooc.netty.ChatMsg;
import com.imooc.pojo.Users;
import com.imooc.pojo.vo.FriendRequestVO;
import com.imooc.pojo.vo.MyFriendsVO;

import java.util.List;

public interface UserService {
    //查询用户名是否存在
    public boolean queryUsernameIsExist(String name);

    //查询用户是否存在
    public Users queryUserForLogin(String name, String psw);

    //用户注册，保存用户信息进入数据库
    public Users saveUser(Users user);

    //修改用户记录
    public Users updateUserInfo(Users user);

    //搜索朋友的前置条件
    public Integer preconditionSearchFriends(String myUserId, String friendUsername);

    //根据用户名查询用户对象
    public Users queryUserInfoByUsername(String username);

    //添加好友请求记录，保存到数据库
    public void sendFriendRequest(String myUserId, String friendUsername);

    //查询自己收到的添加好友请求
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId);

    //删除好友请求记录（用于不通过好友申请的时候）
    public void deleteFriendRequest(String sendUserId, String acceptUserId);


    //通过好友请求记录

    /**
     * 0.通过好友请求
     * 1.保存好友
     * 2.逆向保存好友
     * 3.删除好友请求记录
     * @param sendUserId
     * @param acceptUserId
     */
    public void passFriendRequest(String sendUserId, String acceptUserId);


    /**
     * 查询好友列表
     * @param userId
     * @return
     */
    public List<MyFriendsVO> queryMyFriends(String userId);

    /**
     * 保存聊天消息到数据库
     * @param chatMsg
     * @return
     */
    public String saveMsg(ChatMsg chatMsg);

    /**
     * 批量签收消息
     * @param msgIdList
     */
    public void updateMsgSigned(List<String> msgIdList);

    /**
     * 获取未签收的消息列表
     * @param acceptUserId
     * @return
     */
    public List<com.imooc.pojo.ChatMsg> getUnReadMsgList(String acceptUserId);
}
