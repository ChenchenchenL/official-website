package com.company.officialwebsite.modules.product.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.product.dto.ProductCreateDTO;
import com.company.officialwebsite.modules.product.dto.ProductSortItemDTO;
import com.company.officialwebsite.modules.product.dto.ProductUpdateDTO;
import com.company.officialwebsite.modules.product.vo.PortalProductVO;
import com.company.officialwebsite.modules.product.vo.ProductVO;
import java.util.List;

/**
 * ProductService：产品矩阵与产品信息管理服务接口。
 */
public interface ProductService {

    /**
     * 分页查询产品列表（管理端）。
     */
    PageResult<ProductVO> getProductList(int pageNo, int pageSize);

    /**
     * 新增产品。
     */
    Long createProduct(ProductCreateDTO createDTO);

    /**
     * 编辑产品。
     */
    void updateProduct(Long id, ProductUpdateDTO updateDTO);

    /**
     * 删除产品。
     */
    void deleteProduct(Long id, Integer version);

    /**
     * 批量更新产品排序。
     */
    void batchSort(List<ProductSortItemDTO> sortItems);

    /**
     * 获取前台展示的已上架产品矩阵列表（Portal端，带缓存）。
     */
    List<PortalProductVO> getPortalProducts();
}
