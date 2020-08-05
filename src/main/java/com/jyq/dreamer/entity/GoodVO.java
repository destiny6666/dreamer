package com.jyq.dreamer.entity;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @ClassName: GoodVO
 * @description: 商品详情
 * @author: jiayuqin2
 * @create: 2020-08-03 11:37
 **/
@Data
@Builder
public class GoodVO {
    private String goodsId;
    private Long num;
    private String picLink;
    private String introduction;
    private BigDecimal price;
}
