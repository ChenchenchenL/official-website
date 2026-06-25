package com.company.officialwebsite.modules.product.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.product.service.ProductService;
import com.company.officialwebsite.modules.product.vo.PortalProductVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalProductController：前台产品矩阵公开接口。
 */
@RestController
@RequestMapping("/portal/api/products")
public class PortalProductController {

    private final ProductService productService;

    public PortalProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<List<PortalProductVO>> getPortalProducts() {
        return ApiResponse.success(productService.getPortalProducts());
    }
}
