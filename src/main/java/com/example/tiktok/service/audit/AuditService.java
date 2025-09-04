package com.example.tiktok.service.audit;

/**
 * 用于处理审核
 */
public interface AuditService<T,R> {

    /**
     *  审核规范
     * @param task
     * @return
     */
    R audit(T task);
}
