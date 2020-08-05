package com.jyq.dreamer.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @ClassName: PageInfo
 * @description: 分页封装类
 * @author: jiayuqin2
 * @create: 2020-08-03 14:05
 **/
@Data
@Builder
public class PageInfo<T> {
    private List<T> data;
    private Integer page;
    private Integer pageSize;
    private Long total;
}
