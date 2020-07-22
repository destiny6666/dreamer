package com.jyq.dreamer.controller;

import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName: CartController
 * @description: 电商购物车模拟
 * @author: jiayuqin2
 * @create: 2020-07-22 17:47
 **/
@RestController
@RequestMapping("/cart")
@Slf4j
public class CartController {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedissonClient redisson;
    @RequestMapping("/updateGoods")
    public boolean updateGoods(Long userId,String goodsId,int num){
        String lockKey = Joiner.on(":").useForNull("null").join("lockKey",userId,goodsId);
        System.out.println("lockKey:"+lockKey);
        RLock lock=redisson.getLock(lockKey);
        try {
            lock.lock();
            return updateGoodsTask(userId,goodsId,num);
        } finally {
            lock.unlock();
        }
    }
    private boolean updateGoodsTask(Long userId,String goodsId,int num){
        try {
            String userKey= Joiner.on(":").useForNull("null").join("cart",userId);
            //判断购物车是否有此商品
            boolean isExist=stringRedisTemplate.opsForHash().hasKey(userKey,goodsId);
            //已有商品-增加/减少hincr
            if(isExist){
                long res=stringRedisTemplate.opsForHash().increment(userKey,goodsId,num);
                log.info("increment结果：{}",res);
                //<=0移除
                if(res<=0){
                    res=stringRedisTemplate.opsForHash().delete(userKey,goodsId);
                    log.info("delete结果：{}",res);
                }
                log.info("用户：{},商品：{},增加结果：{}",userId,goodsId,stringRedisTemplate.opsForHash().get(userKey,goodsId));
                return true;
            }
            //不存在 数量<=0 return
            if(num<=0){
                log.info("不存在，且<=0 直接返回");
                return true;
            }
            //没有-hset
            log.info("不存在，set");
            stringRedisTemplate.opsForHash().put(userKey,goodsId,num+"");
            log.info("用户：{},商品：{},新增结果：{}",userId,goodsId,stringRedisTemplate.opsForHash().get(userKey,goodsId));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
