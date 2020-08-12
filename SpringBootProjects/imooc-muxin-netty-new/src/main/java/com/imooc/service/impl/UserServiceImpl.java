package com.imooc.service.impl;

import com.imooc.enums.MsgActionEnum;
import com.imooc.enums.MsgSignFlagEnum;
import com.imooc.enums.SearchFriendsStatusEnum;
import com.imooc.mapper.*;
import com.imooc.netty.ChatMsg;
import com.imooc.netty.DataContent;
import com.imooc.netty.UserChannelRelationship;
import com.imooc.pojo.FriendsRequest;
import com.imooc.pojo.MyFriends;
import com.imooc.pojo.Users;
import com.imooc.pojo.vo.FriendRequestVO;
import com.imooc.pojo.vo.MyFriendsVO;
import com.imooc.service.UserService;
import com.imooc.utils.FastDFSClient;
import com.imooc.utils.FileUtils;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.QRCodeUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UsersMapper usersMapper;
    //这只是个普通的接口，没有加任何注解，只是继承了MyMapper这个接口，为什么可以@Autowired？是不是与mybatis整合有关，还是继承了MyMapper的原题？

    @Autowired
    private Sid sid;  //sid是个@component

    @Autowired
    private QRCodeUtils qrCodeUtils;  //也是个@Component

    @Autowired
    private FastDFSClient fastDFSClient; //@Component

    //判断某个用户是否存在于数据库
    @Transactional(propagation = Propagation.SUPPORTS)//事务,参数代表事务的级别
    @Override
    public boolean queryUsernameIsExist(String username) {

        Users user = new Users();
        user.setUsername(username);
        Users result = usersMapper.selectOne(user);//去数据库查询

        return result != null ? true:false;
    }

    @Transactional(propagation = Propagation.SUPPORTS)//事务,参数代表事务的级别
    @Override
    public Users queryUserForLogin(String name, String psw) {

        //使用逆向工具和工具类
        Example userExample = new Example(Users.class);

        Example.Criteria criteria = userExample.createCriteria();
        criteria.andEqualTo("username",name);//第一个参数是Users类的属性名，第二个参数是前端传来的值，
                                                        // 该函数是做一个匹配
        criteria.andEqualTo("password",psw);

        Users result = usersMapper.selectOneByExample(userExample);
        return result;
    }
    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users saveUser(Users user) {

        //使用id生成工具为用户生成唯一id
        String userId = sid.nextShort();

        //为每个用户生成唯一的二维码

        String qrCodePath = "D://user"+userId+"qrcode.png";
        //muxin_qrcode:[username]
        qrCodeUtils.createQRCode(qrCodePath,"muxin_qrcode:"+user.getUsername());
        MultipartFile qrCodeFile = FileUtils.fileToMultipart(qrCodePath);

        String qrCodeUrl = "";
        try {
            qrCodeUrl = fastDFSClient.uploadQRCode(qrCodeFile);
        }catch (IOException e){
            e.printStackTrace();
        }
        user.setQrcode(qrCodeUrl);
        user.setId(userId);
        usersMapper.insert(user);  //将用户信息保存进数据库

        return user;
    }

    @Transactional(propagation = Propagation.REQUIRED)//事务,参数代表事务的级别
    @Override
    public Users updateUserInfo(Users user) {
        usersMapper.updateByPrimaryKeySelective(user);
        //selective函数意思是只更新user对象中有值的属性，如果属性值为空就不更新该属性，显然这个方法更安全。

        //更新完后做一次查询
        return queryUserById(user.getId());//这个查询结果要返回给前端
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    private Users queryUserById(String userId){
        return usersMapper.selectByPrimaryKey(userId);
    }


    @Autowired
    private MyFriendsMapper myFriendsMapper;


    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Integer preconditionSearchFriends(String myUserId, String friendUsername) {

        Users user = queryUserInfoByUsername(friendUsername);
        //前置条件-1，用户不存在 返回【无此用户】
        if (user == null){
            return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
        }
        //前置条件-2，搜索的是自己 返回【不能添加自己】
        if (user.getId().equals(myUserId)){
            return SearchFriendsStatusEnum.NOT_YOURSELF.status;
        }
        //前置条件-3，已经是好友 返回【该用户已经是你的好友】
        Example mfe = new Example(MyFriends.class);
        Example.Criteria mfc =  mfe.createCriteria();
        mfc.andEqualTo("myUserId",myUserId); //此处“myUserId”要和pojo里面的MyFriends类里定义的名字一致
        mfc.andEqualTo("myFriendUserId",user.getId()); // 设置查询条件，查找MyFriends表中是否有这样的一条记录存在
        MyFriends myFriendsRel = myFriendsMapper.selectOneByExample(mfe);
        if (myFriendsRel != null){
            return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
        }


        return SearchFriendsStatusEnum.SUCCESS.status;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserInfoByUsername(String username){
        Example ue = new Example(Users.class);
        Example.Criteria uc =  ue.createCriteria();
        uc.andEqualTo("username",username);
        return usersMapper.selectOneByExample(ue);

    }


    @Autowired
    private FriendsRequestMapper friendsRequestMapper;

    @Override
    public void sendFriendRequest(String myUserId, String friendUsername) {

        //判断是否已经存在于数据库
        // 根据用户名把朋友信息查询出来
        Users friend = queryUserInfoByUsername(friendUsername);

        //1.查询发送好友请求记录表
        Example fre = new Example(FriendsRequest.class);
        Example.Criteria frc = fre.createCriteria();
        frc.andEqualTo("sendUserId",myUserId);
        frc.andEqualTo("acceptUserId",friend.getId());
        FriendsRequest friendsRequest = friendsRequestMapper.selectOneByExample(fre);
        if (friendsRequest==null){
            //2.如果还不是你的好友，并且好友记录没有添加，则新增好友请求记录
            String requestId = sid.nextShort();
            FriendsRequest request = new FriendsRequest();
            request.setId(requestId);
            request.setSendUserId(myUserId);
            request.setAcceptUserId(friend.getId());
            request.setRequestDateTime(new Date());
            friendsRequestMapper.insert(request); //保存到数据库
        }



    }


    @Autowired
    private UsersMapperCustom usersMapperCustom;

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId) {
        System.out.println("正在查询好友请求列表。。。");
        return usersMapperCustom.queryFriendRequestList(acceptUserId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void deleteFriendRequest(String sendUserId, String acceptUserId) {

        Example fre = new Example(FriendsRequest.class);
        Example.Criteria frc = fre.createCriteria();
        frc.andEqualTo("sendUserId",sendUserId);
        frc.andEqualTo("acceptUserId",acceptUserId);
        friendsRequestMapper.deleteByExample(fre); //通过mapper删除数据库对应的记录
    }


    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void passFriendRequest(String sendUserId, String acceptUserId) {

        //为二人各自添加一条好友
        saveFriends(sendUserId,acceptUserId);
        saveFriends(acceptUserId,sendUserId);
        deleteFriendRequest(sendUserId, acceptUserId);

        //使用websocket主动推送消息到请求发起者，更新他的通讯录列表
        // （因为对于请求的发起者来说，对方通过好友申请后，应该由我们服务器立刻告知一下，否则只能等到发起者下一次主动请求好友列表的时候才知道好友添加成功了）
        Channel sendChannel = UserChannelRelationship.get(sendUserId);//先获取申请发起者的channel
        if (sendChannel != null){ //如果获取的channel不为空，就推送
            DataContent dataContent = new DataContent();
            dataContent.setAction(MsgActionEnum.PULL_FRIEND.type);//将这个消息的类型设为 拉去好友信息的类型，通过websocket发送给申请发起者，让他重新拉去好友列表

            sendChannel.writeAndFlush(
                    new TextWebSocketFrame(
                            JsonUtils.objectToJson(dataContent)));//推送到对应的channel上
        }else{ //如果好友申请的接收方通过了申请，而此时好友申请的发起方此时不在线呢？？
            //待完成
        }


    }

    @Transactional(propagation = Propagation.REQUIRED)
    private void saveFriends(String sendUserId,String acceptUserId){
        MyFriends myFriends = new MyFriends();
        String recodrId = sid.nextShort();
        myFriends.setId(recodrId);
        myFriends.setMyUserId(sendUserId);
        myFriends.setMyFriendUserId(acceptUserId);
        myFriendsMapper.insert(myFriends); //插入数据库

    }


    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<MyFriendsVO> queryMyFriends(String userId) {
        List<MyFriendsVO> myFriends = usersMapperCustom.queryMyFriends(userId);
        return myFriends;
    }

    @Autowired
    private ChatMsgMapper chatMsgMapper;

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public String saveMsg(ChatMsg chatMsg) {
        com.imooc.pojo.ChatMsg msgDB = new com.imooc.pojo.ChatMsg();
        String msgId = sid.nextShort();
        msgDB.setId(msgId);
        msgDB.setAcceptUserId(chatMsg.getReceiverId());
        msgDB.setSendUserId(chatMsg.getSenderId());
        msgDB.setCreateTime(new Date());
        msgDB.setSignFlag(MsgSignFlagEnum.unsign.type);
        msgDB.setMsg(chatMsg.getMsg());

        chatMsgMapper.insert(msgDB);//插入数据库
        return msgId; //返回msg的id
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateMsgSigned(List<String> msgIdList) {
        //需要自定义写sql，生成的MyMapper是无法满足我们的需求
        usersMapperCustom.batchUpdateMsgSigned(msgIdList);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<com.imooc.pojo.ChatMsg> getUnReadMsgList(String acceptUserId) {

        Example chatExample= new Example(com.imooc.pojo.ChatMsg.class);
        Example.Criteria chatExampleCriteria = chatExample.createCriteria();
        chatExampleCriteria.andEqualTo("signFlag",0);
        chatExampleCriteria.andEqualTo("acceptUserId",acceptUserId);

        List<com.imooc.pojo.ChatMsg> result = chatMsgMapper.selectByExample(chatExample);

        return result;
    }
}
