package com.jyq.dreamer.controller.singleton;

/**
 * @ClassName: Mgr01
 * @description: 传统单例-饿汉式
 * 类加载到内存后，就实例化一个单例，JVM保证线程安全
 * 缺点：不管用到与否，类装载过程都完成实例化
 * @author: jiayuqin
 * @create: 2020-12-25 17:11
 **/
public class Mgr01 {
    private static final Mgr01 INSTANCE=new Mgr01();
    private Mgr01(){}
    public static Mgr01 getINSTANCE(){
        return INSTANCE;
    }
    public static void main(String[] args) {
        Mgr01 mgr01=getINSTANCE();
        Mgr01 mgr02=getINSTANCE();
        System.out.println(mgr01==mgr02);
    }

}
