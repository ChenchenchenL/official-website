package com.company.officialwebsite.modules.pagebuilder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.officialwebsite.modules.pagebuilder.entity.EditorLockEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * EditorLockMapper：可视化编辑器独占锁数据访问接口。
 */
@Mapper
public interface EditorLockMapper extends BaseMapper<EditorLockEntity> {
}
