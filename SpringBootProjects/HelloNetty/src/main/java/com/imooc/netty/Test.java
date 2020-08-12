package com.imooc.netty;

public class Test {
    public static void main(String[] args) {

    }
}
class futher{
    private String name = "fu";

    public String getName() {
        return name;
    }
    public void setName(String name){
        this.name = name;
    }
}

class son extends futher{
    public void test(){
        super.setName("");
        System.out.println();
    }
}
