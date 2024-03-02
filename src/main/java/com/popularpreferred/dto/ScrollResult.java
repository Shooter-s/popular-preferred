package com.popularpreferred.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ClassName: ScrollResult
 * Package: com.popularpreferred.dto
 * Description:
 *
 * @Author:Shooter
 * @Create 2024/1/22 20:47
 * @Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScrollResult {

    private List<?> list;
    //最小分数
    private Long minTime;
    //偏移量
    private Integer offset;

}
