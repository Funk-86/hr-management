package org.example.hrmanagement.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.example.hrmanagement.common.security.LoginUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                Long userId = getCurrentUserId();
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
                if (userId != null) {
                    this.strictInsertFill(metaObject, "createdBy", Long.class, userId);
                    this.strictInsertFill(metaObject, "updatedBy", Long.class, userId);
                }
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                Long userId = getCurrentUserId();
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);
                if (userId != null) {
                    this.strictUpdateFill(metaObject, "updatedBy", Long.class, userId);
                }
            }

            private Long getCurrentUserId() {
                try {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
                        return loginUser.getUserId();
                    }
                } catch (Exception ignored) {
                    // 非 Web 上下文（如定时任务、测试）无法获取用户
                }
                return null;
            }
        };
    }
}
