package com.popularpreferred.utils;

import org.apache.tomcat.jni.Local;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final long BEGIN_TIMESTAMP = 1704067200L;
    //序列号位数
    private static final int COUNT_BITS = 32;
    public long nextId(String keyPrefix){
        //时间戳
        LocalDateTime now = LocalDateTime.now();
        long stamp = now.toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;
        //序列号
        String format = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long increment = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + format);
        return stamp << COUNT_BITS | increment;
    }
}
