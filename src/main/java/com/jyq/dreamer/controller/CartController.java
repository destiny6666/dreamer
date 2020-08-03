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

    /**
     * 更新购物车商品信息
     * @param userId 用户id
     * @param goodsId 商品id
     * @param num 增加商品个数（减少为负数）
     * @param isIncre 是否为增加/减少  true：递增/递减 false:修改
     * @return
     */
    @RequestMapping("/updateCartGoods")
    public boolean updateGoods(Long userId,String goodsId,int num,boolean isIncre){
        String lockKey = Joiner.on(":").useForNull("null").join("lockKey",userId,goodsId);
        log.info("lockKey:{}",lockKey);
        RLock lock=redisson.getLock(lockKey);
        try {
            lock.lock();
            return isIncre?increCartGoodsTask(userId,goodsId,num):updateCartGoodsTask(userId,goodsId,num);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 递增/递减购物车
     * @param userId
     * @param goodsId
     * @param num
     * @return
     */
    private boolean increCartGoodsTask(Long userId,String goodsId,int num){
        try {
            if(num==0){
                return true;
            }
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
            //不存在+数量<0 return
            if(num<0){
                log.info("不存在，且<0 直接返回");
                return true;
            }
            //不存在+num>0-hset
            log.info("不存在，put");
            stringRedisTemplate.opsForHash().put(userKey,goodsId,num+"");
            log.info("用户：{},商品：{},新增结果：{}",userId,goodsId,stringRedisTemplate.opsForHash().get(userKey,goodsId));
            return true;
        } catch (Exception e) {
            log.error("递增递减购物车数据异常：{}",e);
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 修改购物车
     * @param userId
     * @param goodsId
     * @param num
     * @return
     */
    private boolean updateCartGoodsTask(Long userId,String goodsId,int num){
        String userKey= Joiner.on(":").useForNull("null").join("cart",userId);
        try {
            if(num<=0){
                //删除
                long res=stringRedisTemplate.opsForHash().delete(userKey,goodsId);
                log.info("delete结果：{}",res);
                return true;
            }
            log.info("num>0，put");
            stringRedisTemplate.opsForHash().put(userKey,goodsId,num+"");
            log.info("用户：{},商品：{},修改结果：{}",userId,goodsId,stringRedisTemplate.opsForHash().get(userKey,goodsId));
            return true;
        } catch (Exception e) {
            log.error("修改购物车数据异常：{}",e);
            e.printStackTrace();
        }
        return false;
    }
}
