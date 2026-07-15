package com.company.officialwebsite.modules.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.officialwebsite.modules.product.entity.ProductVersionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * ProductVersionMapper：产品发布版本历史数据访问接口。
 */
@Mapper
public interface ProductVersionMapper extends BaseMapper<ProductVersionEntity> {
}
