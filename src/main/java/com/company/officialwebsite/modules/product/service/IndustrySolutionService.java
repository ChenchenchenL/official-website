package com.company.officialwebsite.modules.product.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.product.dto.IndustrySolutionBatchSortDTO;
import com.company.officialwebsite.modules.product.dto.IndustrySolutionCreateDTO;
import com.company.officialwebsite.modules.product.dto.IndustrySolutionDeleteDTO;
import com.company.officialwebsite.modules.product.dto.IndustrySolutionUpdateDTO;
import com.company.officialwebsite.modules.product.vo.AdminIndustrySolutionVO;
import com.company.officialwebsite.modules.product.vo.PortalIndustrySolutionVO;
import java.util.List;

/**
 * IndustrySolutionService：行业解决方案管理业务接口。
 */
public interface IndustrySolutionService {

    /**
     * 分页查询后台行业解决方案列表。
     */
    PageResult<AdminIndustrySolutionVO> getAdminIndustrySolutionList(int pageNo, int pageSize);

    /**
     * 新增行业解决方案，返回创建后的后台列表。
     */
    List<AdminIndustrySolutionVO> createIndustrySolution(IndustrySolutionCreateDTO createDTO);

    /**
     * 编辑行业解决方案，返回更新后的后台列表。
     */
    List<AdminIndustrySolutionVO> updateIndustrySolution(Long id, IndustrySolutionUpdateDTO updateDTO);

    /**
     * 删除行业解决方案，返回删除后的后台列表。
     */
    List<AdminIndustrySolutionVO> deleteIndustrySolution(Long id, IndustrySolutionDeleteDTO deleteDTO);

    /**
     * 批量重排行业解决方案，返回更新后的后台列表。
     */
    List<AdminIndustrySolutionVO> batchSortIndustrySolutions(IndustrySolutionBatchSortDTO sortDTO);

    /**
     * 获取前台可见行业解决方案列表，带 Portal 缓存。
     */
    List<PortalIndustrySolutionVO> getPortalIndustrySolutions();
}
