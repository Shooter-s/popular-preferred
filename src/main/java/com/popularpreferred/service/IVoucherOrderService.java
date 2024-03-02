package com.popularpreferred.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.popularpreferred.dto.Result;
import com.popularpreferred.entity.VoucherOrder;

/**
 * ClassName: IVoucherOrderService
 * Package: com.popularpreferred.service
 * Description:
 *
 * @Author:Shooter
 * @Create 2024/1/19 17:36
 * @Version 1.0
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {
    Result seckillVoucher(Long voucherId);

    void createVoucherOrder(VoucherOrder voucherOrder);
}
