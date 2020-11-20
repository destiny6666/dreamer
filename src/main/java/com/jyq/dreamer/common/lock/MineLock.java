package com.jyq.dreamer.common.lock;


import sun.misc.Unsafe;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @ClassName: MineClock
 * @description: 自实现synchronized锁
 * @author: jiayuqin2
 * @create: 2020-11-17 16:22
 **/
public class MineLock {
    private volatile int state=0;
    /**
     * 当前持有锁的线程
     */
    private Thread holder;
    /**
     * 等待队列（公平锁）
     */
    private ConcurrentLinkedQueue<Thread> queue= new ConcurrentLinkedQueue<>();

    public Thread getHolder() {
        return holder;
    }

    public void setHolder(Thread holder) {
        this.holder = holder;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    private  boolean tryAcquire(){
        Thread t=Thread.currentThread();
        if(0==getState()){
           if((0==queue.size()||t==queue.peek())&&unsafe.compareAndSwapInt(this,stateOffest,0,1)){
               setHolder(t);
               return true;
           }

        }
        return false;
    }
    public  void lock(){
        Thread t=Thread.currentThread();
        //1、尝试获取锁 cas-compareAndSwap
        if(tryAcquire()){
            return;
        }
        queue.add(t);
        //2、没获取到自旋等待
        for(;;){
            if(t==queue.peek()&&tryAcquire()){
                queue.poll();
                return;
            }
            LockSupport.park(t);
        }
        //3、重试获取锁
    }
    public  void unlock(){
        Thread t=Thread.currentThread();
        if(t!=getHolder()){
            throw  new RuntimeException(String.format("非当前线程：%s 持有锁，不可释放",t.getName()));
        }
        if(unsafe.compareAndSwapInt(this,stateOffest,1,0)){
            setHolder(null);
            Thread head=queue.peek();
            if(null!=head){
                LockSupport.unpark(queue.peek());
            }
        }
    }
    private static final Unsafe unsafe=Unsafe.getUnsafe();
    private static  long stateOffest;
    static {
        try {
            stateOffest = unsafe.objectFieldOffset(MineLock.class.getField("state"));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

}
