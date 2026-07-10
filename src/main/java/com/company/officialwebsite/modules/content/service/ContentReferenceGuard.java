package com.company.officialwebsite.modules.content.service;

public interface ContentReferenceGuard {

    void assertNotReferenced(String contentType, Long contentId);
}
