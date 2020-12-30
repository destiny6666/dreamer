package com.jyq.dreamer.controller;

import org.redisson.api.RRingBuffer;

import java.util.concurrent.CountDownLatch;

/**
 * @ClassName: jucController
 * @description: java.util.concurrent并发
 * @author: jiayuqin
 * @create: 2020-12-25 15:29
 **/
public class jucController {
    private static /**volatile**/ boolean flag=true;
    public static void m(){
        System.out.println("m---start");
        while (flag){
            //println 中使用synchronized 可保障可见性，所以线程也是可以停止的
            System.out.println("====running");
        }
        System.out.println("m---end");
    }

//    public static void main(String[] args) throws InterruptedException {
//        new Thread(jucController::m,"M").start();
//        Thread.sleep(1000);
//        flag=false;
//    }
    private static long count=1000000L;
    private static class T{
        //long 为8个字节  7*8+8=一个缓存行，不需要去同步别的线程，故可减少运行时间
        private long p1,p2,p3,p4,p5,p6,p7;
        private long x=0L;
        private long p9,p10,p11,p12,p13,p14,p15;
    }
    private static T[] arr=new T[2];
    static {
        arr[0]=new T();
        arr[1]=new T();
    }

//    public static void main(String[] args) throws InterruptedException {
//        CountDownLatch countDownLatch=new CountDownLatch(2);
//        Thread t1=new Thread(()->{
//            for(int i=0;i<count;i++){
//                arr[0].x=i;
//            }
//            countDownLatch.countDown();
//        });
//        Thread t2=new Thread(()->{
//            for(int i=0;i<count;i++){
//                arr[0].x=i;
//            }
//            countDownLatch.countDown();
//        });
//        final long start=System.nanoTime();
//        t1.start();
//        t2.start();
//        countDownLatch.await();
//        System.out.println((System.nanoTime()-start)/100000);
//    }
    private static int x=0,y=0,a=0,b=0;

    public static void main(String[] args) throws InterruptedException {
        x=0;y=0;a=0;b=0;
        for(int i=0;i<Integer.MAX_VALUE;i++){
            CountDownLatch countDownLatch=new CountDownLatch(2);
            Thread t1=new Thread(()->{
                a=1;
                x=b;
                countDownLatch.countDown();
            });
            Thread t2=new Thread(()->{
                b=1;
                y=a;
                countDownLatch.countDown();
            });
            t1.start();
            t2.start();
            countDownLatch.await();
            String res="第"+i+"次";
            if(x==0&&y==0){
                System.out.println(res);
                break;
            }
        }

    }
}
