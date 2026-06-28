package com.company.officialwebsite.modules.site.service;

import com.company.officialwebsite.modules.site.dto.PromiseContentUpdateRequestDTO;
import com.company.officialwebsite.modules.site.vo.AdminPromiseContentVO;

/**
 * PromiseContentService：封装"我们的承诺"主体宣导文案的后台维护能力。
 */
public interface PromiseContentService {

    AdminPromiseContentVO getAdminPromiseContent();

    void updatePromiseContent(PromiseContentUpdateRequestDTO requestDTO);
}
