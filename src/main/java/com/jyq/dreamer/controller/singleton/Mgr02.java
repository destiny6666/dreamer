package com.jyq.dreamer.controller.singleton;

/**
 * @ClassName: Mgr01
 * @description: 传统单例-懒汉式
 * 缺点：有很多业务处理的情况，影响效率
 * @author: jiayuqin
 * @create: 2020-12-25 17:11
 **/
public class Mgr02 {
    private static  Mgr02 INSTANCE;
    private Mgr02(){}
    public static synchronized Mgr02 getINSTANCE(){
        if(null==INSTANCE){
            INSTANCE=new Mgr02();
        }
        return INSTANCE;
    }
    public static void main(String[] args) {
        Mgr02 mgr01=getINSTANCE();
        Mgr02 mgr02=getINSTANCE();
        System.out.println(mgr01==mgr02);
    }

}
