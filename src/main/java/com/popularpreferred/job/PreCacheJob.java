package com.popularpreferred.job;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.popularpreferred.dto.Result;
import com.popularpreferred.entity.ShopType;
import com.popularpreferred.service.IShopTypeService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

import static com.popularpreferred.utils.RedisConstants.CACHE_SHOPTYPE_KEY;

/**
 * ClassName: PreCacheJob
 * Package: com.popularpreferred.job
 * Description:
 *
 * @Author:Shooter
 * @Create 2024/3/3 23:13
 * @Version 1.0
 */
@Component
@Slf4j
public class PreCacheJob {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IShopTypeService shopTypeService;

    @Resource
    private RedissonClient redissonClient;

    @Scheduled(cron = "0 0 0 * * ? *")
    public void doCacheShopType() {
        RLock lock = redissonClient.getLock("popularpreferred:job:doCache:lock");
        try {
            if (lock.tryLock()) {
                // "cache:shoptype"
                String key = CACHE_SHOPTYPE_KEY;
                List<ShopType> shopTypes = shopTypeService.query().orderByAsc("sort").list();
                if (shopTypes.isEmpty()) {
                    log.error("店铺分类为空");
                    return;
                }
                String jsonStr = JSONUtil.toJsonStr(shopTypes);
                // 存入缓存
                try {
                    stringRedisTemplate.opsForValue().set(key, jsonStr);
                } catch (Exception e) {
                    log.error("redis set key error", e);
                }
            }
        } finally {
            lock.unlock();
        }
    }

}
