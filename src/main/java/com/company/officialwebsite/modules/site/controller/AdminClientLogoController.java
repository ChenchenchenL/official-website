package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.site.dto.ClientLogoCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.ClientLogoDeleteRequestDTO;
import com.company.officialwebsite.modules.site.dto.ClientLogoSortItemDTO;
import com.company.officialwebsite.modules.site.dto.ClientLogoUpdateRequestDTO;
import com.company.officialwebsite.modules.site.service.ClientLogoService;
import com.company.officialwebsite.modules.site.vo.AdminClientLogoVO;
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
 * AdminClientLogoController：提供后台服务客户 Logo 墙管理接口。
 */
@RestController
@RequestMapping("/admin/api/site/client-logos")
public class AdminClientLogoController {

    private final ClientLogoService clientLogoService;

    public AdminClientLogoController(ClientLogoService clientLogoService) {
        this.clientLogoService = clientLogoService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<AdminClientLogoVO>> getClientLogoList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(clientLogoService.getAdminClientLogos(pageNo, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Long> createClientLogo(@Valid @RequestBody ClientLogoCreateRequestDTO requestDTO) {
        return ApiResponse.success(clientLogoService.createClientLogo(requestDTO));
    }

    @PutMapping("/{clientLogoId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateClientLogo(
            @PathVariable Long clientLogoId,
            @Valid @RequestBody ClientLogoUpdateRequestDTO requestDTO) {
        clientLogoService.updateClientLogo(clientLogoId, requestDTO);
        return ApiResponse.success();
    }

    @DeleteMapping("/{clientLogoId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteClientLogo(
            @PathVariable Long clientLogoId,
            @RequestParam("version") Integer version) {
        ClientLogoDeleteRequestDTO requestDTO = new ClientLogoDeleteRequestDTO();
        requestDTO.setVersion(version);
        clientLogoService.deleteClientLogo(clientLogoId, requestDTO);
        return ApiResponse.success();
    }

    @PutMapping("/batch-sort")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> batchSortClientLogos(@Valid @RequestBody List<@Valid ClientLogoSortItemDTO> requestDTO) {
        clientLogoService.batchSortClientLogos(requestDTO);
        return ApiResponse.success();
    }
}
