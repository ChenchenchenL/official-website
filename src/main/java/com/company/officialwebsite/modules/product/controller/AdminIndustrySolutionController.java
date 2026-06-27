package com.company.officialwebsite.modules.product.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.product.dto.IndustrySolutionBatchSortDTO;
import com.company.officialwebsite.modules.product.dto.IndustrySolutionCreateDTO;
import com.company.officialwebsite.modules.product.dto.IndustrySolutionDeleteDTO;
import com.company.officialwebsite.modules.product.dto.IndustrySolutionUpdateDTO;
import com.company.officialwebsite.modules.product.service.IndustrySolutionService;
import com.company.officialwebsite.modules.product.vo.AdminIndustrySolutionVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminIndustrySolutionController：后台行业解决方案管理接口。
 */
@RestController
@RequestMapping("/admin/api/industry-solutions")
public class AdminIndustrySolutionController {

    private final IndustrySolutionService industrySolutionService;

    public AdminIndustrySolutionController(IndustrySolutionService industrySolutionService) {
        this.industrySolutionService = industrySolutionService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<AdminIndustrySolutionVO>> getIndustrySolutionList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(industrySolutionService.getAdminIndustrySolutionList(pageNo, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminIndustrySolutionVO>> createIndustrySolution(
            @Valid @RequestBody IndustrySolutionCreateDTO createDTO) {
        return ApiResponse.success(industrySolutionService.createIndustrySolution(createDTO));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminIndustrySolutionVO>> updateIndustrySolution(
            @PathVariable Long id,
            @Valid @RequestBody IndustrySolutionUpdateDTO updateDTO) {
        return ApiResponse.success(industrySolutionService.updateIndustrySolution(id, updateDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminIndustrySolutionVO>> deleteIndustrySolution(
            @PathVariable Long id,
            @Valid @RequestBody IndustrySolutionDeleteDTO deleteDTO) {
        return ApiResponse.success(industrySolutionService.deleteIndustrySolution(id, deleteDTO));
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminIndustrySolutionVO>> batchSortIndustrySolutions(
            @Valid @RequestBody IndustrySolutionBatchSortDTO sortDTO) {
        return ApiResponse.success(industrySolutionService.batchSortIndustrySolutions(sortDTO));
    }
}
