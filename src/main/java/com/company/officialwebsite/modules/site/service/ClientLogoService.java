package com.company.officialwebsite.modules.site.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.site.dto.ClientLogoCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.ClientLogoDeleteRequestDTO;
import com.company.officialwebsite.modules.site.dto.ClientLogoSortItemDTO;
import com.company.officialwebsite.modules.site.dto.ClientLogoUpdateRequestDTO;
import com.company.officialwebsite.modules.site.vo.AdminClientLogoVO;
import com.company.officialwebsite.modules.site.vo.PortalClientLogoVO;
import java.util.List;

/**
 * ClientLogoService：封装服务客户 Logo 墙的后台维护与前台读取能力。
 */
public interface ClientLogoService {

    PageResult<AdminClientLogoVO> getAdminClientLogos(int pageNo, int pageSize);

    Long createClientLogo(ClientLogoCreateRequestDTO requestDTO);

    void updateClientLogo(Long clientLogoId, ClientLogoUpdateRequestDTO requestDTO);

    void deleteClientLogo(Long clientLogoId, ClientLogoDeleteRequestDTO requestDTO);

    void batchSortClientLogos(List<ClientLogoSortItemDTO> requestDTO);

    List<PortalClientLogoVO> getPortalClientLogos();
}
