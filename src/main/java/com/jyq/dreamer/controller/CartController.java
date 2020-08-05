package com.jyq.dreamer.controller;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.jyq.dreamer.entity.GoodVO;
import com.jyq.dreamer.entity.PageInfo;
import jodd.util.BinarySearch;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
     *
     * @param userId  用户id
     * @param goodsId 商品id
     * @param num     增加商品个数（减少为负数）
     * @param type    1：递增/递减 2：修改 -1：删除
     * @return
     */
    @RequestMapping("/updateCartGoods")
    public boolean updateGoods(Long userId, String goodsId, int num, int type) {
        if (BinarySearch.forArray(new Integer[]{1, -1, 2}).find(type) < 0) {
            return false;
        }
        String lockKey = Joiner.on(":").useForNull("null").join("lockKey", userId, goodsId);
        log.info("lockKey:{}", lockKey);
        RLock lock = redisson.getLock(lockKey);
        try {
            lock.lock();
            return type == 1 ? increCartGoodsTask(userId, goodsId, num) : type == 2 ? updateCartGoodsTask(userId, goodsId, num) : delCartGoodsTask(userId, goodsId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 递增/递减购物车
     *
     * @param userId
     * @param goodsId
     * @param num
     * @return
     */
    private boolean increCartGoodsTask(Long userId, String goodsId, int num) {
        try {
            if (num == 0) {
                return true;
            }
            String userKey = Joiner.on(":").useForNull("null").join("cart", userId);
            //判断购物车是否有此商品
            boolean isExist = stringRedisTemplate.opsForHash().hasKey(userKey, goodsId);
            //已有商品-增加/减少hincr
            if (isExist) {
                long res = stringRedisTemplate.opsForHash().increment(userKey, goodsId, num);
                log.info("increment结果：{}", res);
                //<=0移除
                if (res <= 0) {
                    res = stringRedisTemplate.opsForHash().delete(userKey, goodsId);
                    log.info("delete结果：{}", res);
                }
                log.info("用户：{},商品：{},增加结果：{}", userId, goodsId, stringRedisTemplate.opsForHash().get(userKey, goodsId));
                return true;
            }
            //不存在+数量<0 return
            if (num < 0) {
                log.info("不存在，且<0 直接返回");
                return true;
            }
            //不存在+num>0-hset
            log.info("不存在，put");
            stringRedisTemplate.opsForHash().put(userKey, goodsId, num + "");
            log.info("用户：{},商品：{},新增结果：{}", userId, goodsId, stringRedisTemplate.opsForHash().get(userKey, goodsId));
            return true;
        } catch (Exception e) {
            log.error("递增递减购物车数据异常：{}", e);
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 修改购物车
     *
     * @param userId
     * @param goodsId
     * @param num
     * @return
     */
    private boolean updateCartGoodsTask(Long userId, String goodsId, int num) {
        String userKey = Joiner.on(":").useForNull("null").join("cart", userId);
        try {
            if (num <= 0) {
                //删除
                long res = stringRedisTemplate.opsForHash().delete(userKey, goodsId);
                log.info("delete结果：{}", res);
                return true;
            }
            log.info("num>0，put");
            stringRedisTemplate.opsForHash().put(userKey, goodsId, num + "");
            log.info("用户：{},商品：{},修改结果：{}", userId, goodsId, stringRedisTemplate.opsForHash().get(userKey, goodsId));
            return true;
        } catch (Exception e) {
            log.error("修改购物车数据异常：{}", e);
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 删除购物车商品
     *
     * @param userId
     * @param goodsId
     * @return
     */
    private boolean delCartGoodsTask(Long userId, String goodsId) {
        String userKey = Joiner.on(":").useForNull("null").join("cart", userId);
        try {
            //删除
            long res = stringRedisTemplate.opsForHash().delete(userKey, goodsId);
            log.info("delete结果：{}", res);
            return true;
        } catch (Exception e) {
            log.error("删除购物车数据异常：{}", e);
            e.printStackTrace();
        }
        return false;
    }

    @RequestMapping("/getCartGoodsList")
    public PageInfo<GoodVO> getCartGoodsList(Long userId) {
        String userKey = Joiner.on(":").useForNull("null").join("cart", userId);
        Map<Object, Object> cartMap = stringRedisTemplate.opsForHash().entries(userKey);
        log.info("cartMap:{}", JSON.toJSON(cartMap));
        List<GoodVO> goodVOS = Lists.newArrayList();
        cartMap.keySet().stream().filter(Objects::nonNull).forEach(goodsId -> {
            //获取商品详情
            GoodVO goodVO = GoodVO.builder().goodsId(goodsId.toString()).introduction("introduction-" + goodsId).picLink("picLink-" + goodsId).price(BigDecimal.valueOf(Math.random() * 10000)).num(Long.parseLong(null == cartMap.get(goodsId) ? 0 + "" : cartMap.get(goodsId).toString())).build();
            goodVOS.add(goodVO);
        });
        PageInfo<GoodVO> resultPage=PageInfo.<GoodVO>builder().data(goodVOS).total(stringRedisTemplate.opsForHash().size(userKey)).build();
        log.info("返回购物车list：{}",JSON.toJSON(resultPage));
        return  resultPage;
    }
}
