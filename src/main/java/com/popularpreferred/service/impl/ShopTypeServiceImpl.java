package com.popularpreferred.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.popularpreferred.dto.Result;
import com.popularpreferred.entity.ShopType;
import com.popularpreferred.mapper.ShopTypeMapper;
import com.popularpreferred.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.popularpreferred.utils.RedisConstants.CACHE_SHOPTYPE_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String key = CACHE_SHOPTYPE_KEY;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)){
            List<ShopType> shopTypes = JSONUtil.toList(json, ShopType.class);
            return Result.ok(shopTypes);
        }
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        if (shopTypes.isEmpty()){
            return Result.fail("店铺分类查询有误");
        }
        String jsonStr = JSONUtil.toJsonStr(shopTypes);
        stringRedisTemplate.opsForValue().set(key, jsonStr);
        return Result.ok(shopTypes);


        //list写法
        /*List<String> stringList = new ArrayList<>();
        String shopTypeJson;
        while ((shopTypeJson=stringRedisTemplate.opsForList().rightPop("cache:shopTypeList"))!=null){
            stringList.add(shopTypeJson);
        }

        if (stringList.size()>0) {
            return Result.ok(stringList);
        }

        List<ShopType> typeList = this.query().orderByAsc("sort").list();

        if (typeList == null) {
            return Result.fail("商铺分类信息不存在");
        }
        //    从左边一个一个加进去
        for (ShopType shopType : typeList) {
            stringRedisTemplate.opsForList().leftPush("cache:shopTypeList",shopType.toString());
        }
        return Result.ok(typeList);*/
    }
}
