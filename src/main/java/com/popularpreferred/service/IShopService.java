package com.popularpreferred.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.popularpreferred.dto.Result;
import com.popularpreferred.entity.Shop;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {


    Result queryById(Long id);

    Result updateShop(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);

}
