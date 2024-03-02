package com.popularpreferred.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.popularpreferred.dto.Result;
import com.popularpreferred.entity.Follow;

/**
 * ClassName: IFollowService
 * Package: com.popularpreferred.service
 * Description:
 *
 * @Author:Shooter
 * @Create 2024/1/22 16:48
 * @Version 1.0
 */
public interface IFollowService extends IService<Follow> {
    Result follow(Long followUserId, Boolean isFollow);

    Result isFollow(Long followUserId);

    Result followCommons(Long followUserId);
}
