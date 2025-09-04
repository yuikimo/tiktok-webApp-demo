package com.example.tiktok.authority;

import jakarta.servlet.http.HttpServletRequest;

public class DefaultAuthorityVerify implements AuthorityVerify{

    @Override
    public Boolean authorityVerify(HttpServletRequest request, String[] permissions) {
        return true;
    }
}
