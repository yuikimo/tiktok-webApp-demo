package com.example.tiktok.authority;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthorityVerify {

    Boolean authorityVerify(HttpServletRequest request, String[] permissions);
}

