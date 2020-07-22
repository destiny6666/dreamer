package com.jyq.dreamer.controller;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName: RedisController
 * @description: redis使用test
 * @author: jiayuqin2
 * @create: 2020-07-21 16:25
 **/
@Slf4j
@RestController
@RequestMapping("/redis")
public class RedisController {
    @Value("${stock.retry.limit}")
    private int RETRY_LIMIT;
    @Value("${stock.lockKey.expire.time}")
    private int LOCK_KEY_EXPIRE;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    RedissonClient redisson;

    /**
     * 秒杀减库存
     *1、保证数据的准确性-防止超卖  单机：synchronized 分布式：redis加锁
     * 2、代码异常-锁无法释放  try-finally
     * 3、服务器重启-锁无妨释放  设置超时时间（原子操作）
     * 4、无法估计超时时间-导致线程交叉-形同无锁  uuid生成value，释放锁判断是否为当前线程
     * 5、线程交叉，未执行结束锁被超时释放，并被别的线程获取   设置定时任务 见：resetExpire，执行时间为每隔expire/3（当lockKey存在时，重置超时时间，保证线程执行结束）
     * @return
     */
    @RequestMapping("/stock")
    public Object decrStock(String sku) {
        System.out.println("value:"+RETRY_LIMIT);
        String lockKey = Joiner.on(":").useForNull("null").join("lockKey",sku);
        String stockKey = Joiner.on(":").useForNull("null").join("stock",sku);
        System.out.println("lockKey:"+lockKey);
        String clientId = UUID.randomUUID().toString();
        try {
            Boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, clientId, LOCK_KEY_EXPIRE, TimeUnit.SECONDS);
            if (!isLock) {
                System.out.println(clientId+"系统繁忙");
                return "wait";
            }
            System.out.println(clientId+"获得锁");
            return decrStockTask(stockKey);
        }
//        catch (InterruptedException e) {
//            e.printStackTrace();
//            return "error";
//        }
        finally {
            //保证异常时可以释放锁
            if (clientId.equals(stringRedisTemplate.opsForValue().get(lockKey))) {
                stringRedisTemplate.delete(lockKey);
            }
        }
    }
    /**
     * 秒杀减库存(重试)
     * 1、保证数据的准确性-防止超卖  单机：synchronized 分布式：redis加锁
     * 2、代码异常-锁无法释放  try-finally
     * 3、服务器重启-锁无妨释放  设置超时时间（原子操作）
     * 4、无法估计超时时间-导致线程交叉-形同无锁  uuid生成value，释放锁判断是否为当前线程
     * 5、线程交叉，未执行结束锁被超时释放，并被别的线程获取   设置定时任务，执行时间为每隔expire/3（当lockKey存在时，重置超时时间，保证线程执行结束）
     * 6：间歇性-重试
     * @return
     */
    @RequestMapping("/stockRetry")
    public Object decrStockRetry(String sku) {
        String result="ENOUGH";
        String lockKey = Joiner.on(":").useForNull("null").join("lockKey", sku);
        String stockKey = Joiner.on(":").useForNull("null").join("stock", sku);
        System.out.println("lockKey:" + lockKey);
        String clientId = UUID.randomUUID().toString();
        int retry = 0;
        //RETRY_LIMIT*sleep时间=最大并发量
        while (retry < RETRY_LIMIT) {
            try {
                Boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, clientId, 30, TimeUnit.SECONDS);
                if (!isLock) {
                    System.out.println(clientId + "系统繁忙");
                    TimeUnit.SECONDS.sleep(1);
                    retry++;
                    continue;
                }
                System.out.println(clientId + "获得锁");
                int currentStock = null == stringRedisTemplate.opsForValue().get(stockKey) ? 0 : Integer.parseInt(stringRedisTemplate.opsForValue().get(stockKey));
                if (currentStock > 0) {
//                    int remainStock = currentStock - 1;
                    stringRedisTemplate.opsForValue().decrement(stockKey, 1);
                    System.out.println("减库存成功，当前库存数：" + stringRedisTemplate.opsForValue().get(stockKey));
                    break;
                }
                result="NO-ENOUGH!";
                System.out.println("库存不足");
                break;
            } catch (Exception e) {
                e.printStackTrace();
                result="error";
            } finally {
                //保证异常时可以释放锁
                if (clientId.equals(stringRedisTemplate.opsForValue().get(lockKey))) {
                    stringRedisTemplate.delete(lockKey);
                }
            }
        }
        return result;
    }
    /**
     * 定时任务定时重置锁时间
     */
//    @Scheduled(cron = "${stock.lockKey.expire.resetCron}")
    public void resetExpire(){
        Set<String> lockKeys=stringRedisTemplate.keys("lockKey:*");
        log.info("存在当lockKeys：{}", JSON.toJSON(lockKeys));
        lockKeys.forEach(key->{
            log.info("锁存在，重置时间");
            stringRedisTemplate.expire(key,LOCK_KEY_EXPIRE,TimeUnit.SECONDS);
        });
    }
    @RequestMapping("/stockRedisson")
    public Object decrStockRedisson(String sku) {
        String lockKey = Joiner.on(":").useForNull("null").join("lockKey",sku);
        String stockKey = Joiner.on(":").useForNull("null").join("stock",sku);
        System.out.println("lockKey:"+lockKey);
        RLock lock=redisson.getLock(lockKey);
        try {
            lock.lock(LOCK_KEY_EXPIRE,TimeUnit.SECONDS);
            return decrStockTask(stockKey);
        } finally {
            lock.unlock();
        }
    }
    private String decrStockTask(String stockKey){
        int currentStock = null == stringRedisTemplate.opsForValue().get(stockKey) ? 0 : Integer.parseInt(stringRedisTemplate.opsForValue().get(stockKey));
        if (currentStock > 0) {
            int remainStock=currentStock-1;
            stringRedisTemplate.opsForValue().decrement(stockKey, 1);
//            stringRedisTemplate.opsForValue().set(stockKey, remainStock + "");
            System.out.println("减库存成功，当前库存数：" + remainStock);
            return "ENOUGH!";
        }
        System.out.println("库存不足");
        return "NO-ENOUGH!";
    }
}
