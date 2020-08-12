package com.jyq.dreamer.common.aspect;

import com.google.common.base.Joiner;
import com.jyq.dreamer.common.annotation.RedisLockable;
import io.micrometer.core.instrument.util.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName: RedisLockInterceptor
 * @description: 分布式锁切面
 * @author: jiayuqin
 * @create: 2020-07-24 15:08
 **/
@Aspect
@Component
public class RedisLockInterceptor {
    @Autowired
    private RedissonClient redisson;
    private static final LocalVariableTableParameterNameDiscoverer DISCOVERER = new LocalVariableTableParameterNameDiscoverer();

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    @Pointcut("@annotation(com.jyq.dreamer.common.annotation.RedisLockable)")
    public void pointcut() {
    }

    @Around("pointcut()")
    public Object doAround(ProceedingJoinPoint point) throws Throwable {

        MethodSignature methodSignature = (MethodSignature) point.getSignature();
        Method targetMethod = AopUtils.getMostSpecificMethod(methodSignature.getMethod(), point.getTarget().getClass());
        String targetName = point.getTarget().getClass().getName();
        String methodName = point.getSignature().getName();
        Object[] arguments = point.getArgs();

        RedisLockable redisLock = targetMethod.getAnnotation(RedisLockable.class);
        long expire = redisLock.expiration();
        String redisKey = getLockKey(redisLock, targetMethod, targetName, methodName, arguments);
        RLock lock=redisson.getLock(redisKey);
        int retry = 0;
        while(retry<redisLock.retryCount()){
            boolean res=lock.tryLock(redisLock.retryWaitingTime(),expire,TimeUnit.SECONDS);
            if (!res) {
                TimeUnit.SECONDS.sleep(1);
                retry++;
                continue;
            }
            try {
                return point.proceed();
            } finally {
                lock.unlock();
            }
        }
        return point.proceed();
    }

    private String getLockKey(RedisLockable redisLock, Method targetMethod,
                              String targetName, String methodName, Object[] arguments) {
        String[] keys = redisLock.key();
        String prefix = redisLock.prefix();
        StringBuilder sb = new StringBuilder("lock.");
        if (StringUtils.isEmpty(prefix)) {
            sb.append(targetName).append(".").append(methodName);
        } else {
            sb.append(prefix);
        }
        if (keys != null) {
            String keyStr = Joiner.on(".").skipNulls().join(keys);
            EvaluationContext context = new StandardEvaluationContext(targetMethod);
            String[] parameterNames = DISCOVERER.getParameterNames(targetMethod);
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], arguments[i]);
            }
            Object key = PARSER.parseExpression(keyStr).getValue(context);
            sb.append("#").append(key);
        }
        return sb.toString();
    }

}
