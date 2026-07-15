package com.company.officialwebsite.modules.casecenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.officialwebsite.modules.casecenter.entity.CaseVersionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * CaseVersionMapper：标杆案例发布版本历史数据访问接口。
 */
@Mapper
public interface CaseVersionMapper extends BaseMapper<CaseVersionEntity> {
}
