package com.popularpreferred.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.popularpreferred.dto.Result;
import com.popularpreferred.entity.Shop;
import com.popularpreferred.mapper.ShopMapper;
import com.popularpreferred.service.IShopService;
import com.popularpreferred.utils.CacheClient;
import com.popularpreferred.utils.RedisData;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.popularpreferred.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;
    @Override
    public Result queryById(Long id) {

        Shop shop = /*cacheClient.queryLogicalExpire(CACHE_SHOP_KEY,LOCK_SHOP_KEY,
                id,Shop.class,this::getById,20L,TimeUnit.SECONDS);*/
        cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,this::getById,20L,TimeUnit.SECONDS);
        if (shop == null){
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    //缓存击穿(互斥锁)
    private Result queryWithMutex(Long id) {
        String key = CACHE_SHOP_KEY + id;
        String jsonObj = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(jsonObj)) {
            Shop shop = JSONUtil.toBean(jsonObj, Shop.class);
            return Result.ok(shop);
        }
        //缓存穿透
        if (jsonObj != null) {
            return Result.fail("店铺不存在！");
        }
        //缓存击穿
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            //尝试获取锁
            if (!isLock) {
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //获取互斥锁成功后，doubleCheck，防止二次重建
            jsonObj = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(jsonObj)) {
                return Result.ok(JSONUtil.toBean(jsonObj, Shop.class));
            }
            shop = getById(id);
            //模拟重建时间久
            Thread.sleep(200);
            if (shop == null) {
                //防止缓存穿透，redis存空值
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return Result.fail("店铺不存在！");
            }
            //将缓存做入redis中
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //释放锁
            unlock(lockKey);
        }

        return Result.ok(shop);
    }


    //获取锁
    private boolean tryLock(String key) {
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(aBoolean);
    }

    //释放锁
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    //热点预热，存入redis
    public void saveShopToRedis(Long id, Long expireSeconds) throws InterruptedException {
        Shop shop = getById(id);
        Thread.sleep(100);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    @Override
    @Transactional // 保证更新数据库和删除缓存的事务一致性
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        //先更新数据库
        updateById(shop);
        //再删缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        //判断是否需要根据坐标查询
        if (x == null || y == null) {
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, DEFAULT_BATCH_SIZE));
            return Result.ok(page.getRecords());
        }
        //分页起始点
        int from = (current - 1) * DEFAULT_BATCH_SIZE;
        int end = current * DEFAULT_BATCH_SIZE;

        String key = "shop:geo:" + typeId;
        //GEOSEARCH key BYLONLAT x y BYRADIUS 5000 WITHDISTANCE
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(key,
                GeoReference.fromCoordinate(x, y),
                new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end));
        if (results == null){
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();
        //没有下一页了，结束
        if (content.size() <= from){
            return Result.ok(Collections.emptyList());
        }
        //截取from~end部分信息，收集全部店铺id和对应的距离
        List<String> ids = new ArrayList<>(content.size());
        Map<String,Distance> distanceMap = new HashMap<>(content.size());
        content.stream().skip(from).forEach(result->{
            String shopIdStr = result.getContent().getName();
            ids.add(shopIdStr);
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr,distance);
        });
        String idStr = StrUtil.join(",",ids);
        //根据店铺id获得全部shop然后补全距离
        List<Shop> shops = query().in("id", ids).last("order by field(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        return Result.ok(shops);
    }
}
