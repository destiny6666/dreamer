package com.jyq.dreamer.common.annotation;

public @interface RedisLockable {
    String prefix() default "";

    String[] key() default "";

    long expiration() default 60;

    int retryCount() default -1; // 锁等待重试次数，-1未不限制

    int retryWaitingTime() default 0; // 锁等待重试间隔时间，默认0,不等待
}
