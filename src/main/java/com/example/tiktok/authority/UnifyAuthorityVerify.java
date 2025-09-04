package com.example.tiktok.authority;

import com.example.tiktok.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component(value = "postMappingAuthorityVerify")
public class UnifyAuthorityVerify extends DefaultAuthorityVerify{
    @Override
    public Boolean authorityVerify (HttpServletRequest request, String[] permissions) {
        // 获取当前用户权限
        Long uId = JwtUtils.getUserId(request);
        for (String permission : permissions) {
            if (!AuthorityUtils.verify(uId,permission)) {
                return false;
            }
        }
        return true;
    }
}
