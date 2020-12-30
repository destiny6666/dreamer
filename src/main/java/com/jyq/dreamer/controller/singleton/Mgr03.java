package com.jyq.dreamer.controller.singleton;

/**
 * @ClassName: Mgr03
 * @description: -懒汉式 DCL (double check clock)
 * @author: jiayuqin
 * @create: 2020-12-25 17:11
 **/
public class Mgr03 {
    //volatile 创建过程中访问，避免产生指令重排，返回半初始化对象
    private static volatile Mgr03 INSTANCE;
    private Mgr03(){}
    public static  Mgr03 getINSTANCE(){
        if(null==INSTANCE){
            synchronized (Mgr03.class){
                if(null==INSTANCE){
                    INSTANCE=new Mgr03();
                }
            }
        }
        return INSTANCE;
    }
    public static void main(String[] args) {
        Mgr03 mgr01=getINSTANCE();
        Mgr03 mgr02=getINSTANCE();
        System.out.println(mgr01==mgr02);
    }
 
}
