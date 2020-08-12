package com.imooc.enums;

public enum SearchFriendsStatusEnum {
    SUCCESS(0,"OK"),
    USER_NOT_EXIST(1,"无此用户..."),
    NOT_YOURSELF(2,"不能添加你自己..."),
    ALREADY_FRIENDS(3,"该用户已经是你的好友...");
    public final Integer status;
    public final String msg;
    SearchFriendsStatusEnum(Integer status, String msg){
        this.status = status;
        this.msg = msg;
    }

    public static String getMsgByKey(Integer status) {
        if (status==0){
            return SUCCESS.msg;
        }
        if (status==1){
            return USER_NOT_EXIST.msg;
        }
        if (status==2){
            return NOT_YOURSELF.msg;
        }
        if (status==3){
            return ALREADY_FRIENDS.msg;
        }
        return "";
    }
}
