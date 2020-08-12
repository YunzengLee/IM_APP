package com.imooc.controller;

import com.imooc.enums.OperatorFriendRequestTypeEnum;
import com.imooc.enums.SearchFriendsStatusEnum;
import com.imooc.pojo.Users;
import com.imooc.pojo.bo.UsersBO;
import com.imooc.pojo.vo.MyFriendsVO;
import com.imooc.pojo.vo.UsersVO;
import com.imooc.service.UserService;
import com.imooc.utils.FastDFSClient;
import com.imooc.utils.FileUtils;
import com.imooc.utils.IMoocJSONResult;
import com.imooc.utils.MD5Utils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("u")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FastDFSClient fastDFSClient;


    @PostMapping("/registOrLogin")//注册或登录
    public IMoocJSONResult registOrLogin(@RequestBody Users user) throws Exception {
        //传递进来的Users类是前端传来的一个实例，前端根据用户在登录页上输入的信息，创建了一个实例，
        // 后端（也就是此函数）需要根据这个实例内容访问数据库，
        // 如果是注册的话（当用户不存在于数据库时），就需要完善这个实例的其他内容（前端用户只填写账号和密码），并保存进数据库
        //0. 判断用户名和密码不为空
        if (StringUtils.isBlank(user.getUsername())
                || StringUtils.isBlank(user.getPassword())) {
            return IMoocJSONResult.errorMsg("用户名或密码不能为空");
        }
        //数据库交互
        //1. 判断用户名是否存在于数据库，存在则登录，否则注册
        boolean usernameIsExist = userService.queryUsernameIsExist(user.getUsername());

        Users userResult = null;
        if (usernameIsExist) {
            //1.1 登录
            //查询用户名和密码是否匹配
            userResult = userService.queryUserForLogin(user.getUsername(),
                    MD5Utils.getMD5Str(user.getPassword()));

            if (userResult == null) {
                return IMoocJSONResult.errorMsg("用户名或密码不正确");
            }
        } else {
            //1.2 注册

            //设置昵称
            user.setNickname(user.getUsername());

            //设置头像
            user.setFaceImage("");
            user.setFaceImageBig("");

            //设置密码, 密码使用MD5加密
            user.setPassword(MD5Utils.getMD5Str(user.getPassword()));

            userResult = userService.saveUser(user); //此时该用户已经存入数据库，并且返回的这个userResult包含该用户的所有信息


        }

        UsersVO userVO = new UsersVO();
        BeanUtils.copyProperties(userResult, userVO);//一个对象拷贝属性值给后一个对象
        return IMoocJSONResult.ok(userVO); //返回到前端
        /**
         *      IMoocJSONResult.ok(Object data)是一个实例，具有以下属性：
         *          this.status = 200;
         *         this.msg = "OK";
         *         this.data = data;
         *         此处直接传data=userResult的话，前端实际上不需要如此完善的信息
         *         ，因此要创建一个新的类， 该类包含前端需要的信息且没有多余信息，实例化后作为data传给前端 这个类就是VO
         */
//
    }

    @PostMapping("/uploadFaceBase64")
    public IMoocJSONResult uploadFaceBase64(@RequestBody UsersBO userBO) throws Exception {

        //获取base64的字符串，转为文件对象，并上传至fastDFS
//        System.out.println("0");
        String base64Data = userBO.getFaceData();
//        System.out.println("1");
        String userFacePath = "D:\\" + userBO.getUserId() + "userface64.png";//临时文件存储,别放在c盘里，权限不够会报错
//        System.out.println("2");
        System.out.println(userFacePath);
        FileUtils.base64ToFile(userFacePath, base64Data);//转为文件类型
//        System.out.println("3");
        MultipartFile faceFile = FileUtils.fileToMultipart(userFacePath);//转为MultiPart类型
//        System.out.println("4");

        String url = fastDFSClient.uploadBase64(faceFile); //上传，返回一个path，是fastdfs返回的当前文件的存储地址
        System.out.println(url);
        //此时文件存了两份，一个是大图，一个是缩略图，返回的是大图地址，“xxx/123.png”,
        // 对应的小图地址就是“xxx/123_80x80.png”

        //获取缩略图的url
        String arr[] = url.split("\\.");
        String thump = "_80x80.";
        String thumpImgUrl = arr[0] + thump + arr[1]; //拼接小图的路径

        //更新用户头像
        Users user = new Users();
        user.setId(userBO.getUserId());
        user.setFaceImage(thumpImgUrl);
        user.setFaceImageBig(url);

        //更新到数据库
        Users res = userService.updateUserInfo(user); //这个方法返回一个更新后重新查询到的user

        return IMoocJSONResult.ok(res);//返回json给前端，前端需要做刷新，把登录的信息覆盖一下

    }


    @PostMapping("/setNickname")
    public IMoocJSONResult serNickname(@RequestBody UsersBO userBO) throws Exception {

        Users user = new Users();
        user.setId(userBO.getUserId());
        user.setNickname(userBO.getNickname());
        Users res = userService.updateUserInfo(user);
        return IMoocJSONResult.ok(res);
    }

    /**
     * 搜索好友接口，根据账号做匹配查询而不是模糊查询。
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/search")
    public IMoocJSONResult searchUser(String myUserId, String friendUsername) throws Exception {
        System.out.println("start...");
        //0.判断传入的参数不为空
        if (StringUtils.isBlank(myUserId)
                || StringUtils.isBlank(friendUsername)) {
            return IMoocJSONResult.errorMsg("输入的用户名不能为空");
        }

        //前置条件-1，用户不存在 返回【无此用户】
        //前置条件-2，搜索的是自己 返回【不能添加自己】
        //前置条件-3，已经是好友 返回【该用户已经是你的好友】
        Integer status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            Users user = userService.queryUserInfoByUsername(friendUsername);
            UsersVO usersVO = new UsersVO();
            BeanUtils.copyProperties(user, usersVO);
            return IMoocJSONResult.ok(usersVO);
        } else {
            System.out.println("输入的用户名不符合要求");
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return IMoocJSONResult.errorMsg(errorMsg);
        }
    }

    /**
     * 发送添加好友的请求
     *
     * @param myUserId
     * @param friendUsername
     * @return
     * @throws Exception
     */
    @PostMapping("/addFriendRequest")
    public IMoocJSONResult addFriendRequest(String myUserId, String friendUsername) throws Exception {
        //0.判断传入的参数不为空
        if (StringUtils.isBlank(myUserId)
                || StringUtils.isBlank(friendUsername)) {
            return IMoocJSONResult.errorMsg("输入的用户名不能为空");
        }
        System.out.println("start...");

        //前置条件-1，用户不存在 返回【无此用户】
        //前置条件-2，搜索的是自己 返回【不能添加自己】
        //前置条件-3，已经是好友 返回【该用户已经是你的好友】
        Integer status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            userService.sendFriendRequest(myUserId, friendUsername);

        } else {
            System.out.println("输入的用户名不符合要求");
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return IMoocJSONResult.errorMsg(errorMsg);
        }
        return IMoocJSONResult.ok();
    }


    /**
     * 查询用户收到的好友请求，用于展示在前端的通讯录页面上显示好友申请
     * 这个功能是客户端每次刷新页面时都会主动发起这样一个请求来查询好友申请，应该还是需要websocket主动推送比较好。
     *
     * @param userId
     * @return
     * @throws Exception
     */
    @PostMapping("/queryFriendRequests")
    public IMoocJSONResult queryFriendRequests(String userId) throws Exception {

        //0.判断传入的参数不为空
        if (StringUtils.isBlank(userId)) {
            return IMoocJSONResult.errorMsg("");
        }
        //1.查询用户接收到的好友申请
        return IMoocJSONResult.ok(userService.queryFriendRequestList(userId));
    }

    /**
     * 接收方 通过或忽略好友请求
     */
    @PostMapping("/operFriendRequest")
    public IMoocJSONResult operFriendRequest(String acceptUserId, String sendUserId, Integer operType) throws Exception {

        //0.判断传入的参数不为空
        if (StringUtils.isBlank(acceptUserId) || StringUtils.isBlank(sendUserId) || operType == null) {
            return IMoocJSONResult.errorMsg("");
        }

        //1.如果操作类型没有对应的枚举值，则跑出空错误信息
        if (StringUtils.isBlank(OperatorFriendRequestTypeEnum.getMsgByType(operType))) {
            return IMoocJSONResult.errorMsg("");
        }

        if (operType == OperatorFriendRequestTypeEnum.IGNORE.type) {
            //2.如果忽略，则删除数据库中的好友请求记录
            userService.deleteFriendRequest(sendUserId, acceptUserId);
        } else if (operType == OperatorFriendRequestTypeEnum.PASS.type) {
            //3.如果通过，则互相增加好友记录到数据库对应的表
            //然后删除好友请求的数据库记录
            userService.passFriendRequest(sendUserId, acceptUserId);

        }

        //4.数据库查询好友列表
        List<MyFriendsVO> myFriends = userService.queryMyFriends(acceptUserId);
        return IMoocJSONResult.ok(myFriends);

    }

    /**
     * 根据用户id获取该用户的好友列表
     * @param userId
     * @return
     * @throws Exception
     */
    @PostMapping("/myFriends")
    public IMoocJSONResult myFriends(String userId) throws Exception {

        //0.参数不为空
        if (StringUtils.isBlank(userId)) {
            return IMoocJSONResult.errorMsg("");
        }

        //数据库查询好友列表
        List<MyFriendsVO> myFriends = userService.queryMyFriends(userId);

        return IMoocJSONResult.ok(myFriends);
    }

    /**
     * 用户向后端发请求，获取数据库中未读的消息，未读是指用户手机设备没有接收到的消息、未签收的消息
     * 当朋友发送了一条消息之后，按照我们的业务，如果该用户在线（websocket连接建立），那么会立刻推送到该用户的手机设备上，
     * 否则：好像还没写这一块的处理业务代码
     * 当用户上线后，就需要向后端发起请求，读取所有未签收的消息
     * @param acceptUserId
     * @return
     * @throws Exception
     */
    @PostMapping("/getUnReadMsgList")
    public IMoocJSONResult getUnReadMsgList(String acceptUserId) throws Exception {
        //0.参数不为空
        if (StringUtils.isBlank(acceptUserId)) {
            return IMoocJSONResult.errorMsg("");
        }
        //查询列表
        List<com.imooc.pojo.ChatMsg> unReadMsgList = userService.getUnReadMsgList(acceptUserId);

        return IMoocJSONResult.ok(unReadMsgList);
    }
}