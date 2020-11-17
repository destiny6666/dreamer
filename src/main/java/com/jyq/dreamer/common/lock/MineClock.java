package com.jyq.dreamer.common.lock;


import sun.misc.Unsafe;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

/**
 * @ClassName: MineClock
 * @description: 自实现synchronized锁
 * @author: jiayuqin2
 * @create: 2020-11-17 16:22
 **/
public class MineClock {
    private static int state=0;
    private Thread holder;
    private ConcurrentLinkedQueue<Thread> queue= new ConcurrentLinkedQueue<>();

    public Thread getHolder() {
        return holder;
    }

    public void setHolder(Thread holder) {
        this.holder = holder;
    }

    public static int getState() {
        return state;
    }

    public static void setState(int state) {
        MineClock.state = state;
    }

    private  boolean tryAcquire(){
        Thread t=Thread.currentThread();
        if(0==getState()){
            unsafe.compareAndSwapInt(this,stateOffest,0,1);
            setHolder(t);
            return true;
        }
        queue.add(t);
        return false;
    }
    public   boolean lock(){
        Thread t=Thread.currentThread();
        //1、尝试获取锁 cas-compareAndSwap
        if(tryAcquire()){
            return true;
        }
        //2、没获取到等待
        for(;;){
            if(tryAcquire()){
                return true;
            }
            LockSupport.park(t);
        }
        //3、重试获取锁
    }
    public   boolean unlock(){
        Thread t=Thread.currentThread();
        if(t!=getHolder()){
            throw  new RuntimeException(String.format("非当前线程：%s 持有锁，不可释放",t.getName()));
        }
        unsafe.compareAndSwapInt(this,stateOffest,1,0);
        holder=null;
        LockSupport.unpark(queue.peek());
        return true;
    }
    private static final Unsafe unsafe=Unsafe.getUnsafe();
    private static  long stateOffest;
    static {
        try {
            stateOffest = unsafe.objectFieldOffset(MineClock.class.getField("state"));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
