package com.company.officialwebsite.infrastructure.cache;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** Portal 缓存失效补偿任务数据访问入口。 */
@Mapper
public interface PortalCacheInvalidationRetryMapper extends BaseMapper<PortalCacheInvalidationRetryEntity> {
}
