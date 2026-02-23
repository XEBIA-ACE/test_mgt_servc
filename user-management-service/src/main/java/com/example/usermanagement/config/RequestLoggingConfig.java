package com.example.usermanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class RequestLoggingConfig {

    /**
     * Logs incoming HTTP requests at DEBUG level.
     * Excludes headers to prevent accidental logging of Authorization tokens.
     */
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeClientInfo(true);
        filter.setIncludeQueryString(true);
        filter.setIncludeHeaders(false);
        filter.setIncludePayload(false);
        filter.setMaxPayloadLength(1000);
        filter.setBeforeMessagePrefix("[REQUEST] ");
        filter.setAfterMessagePrefix("[RESPONSE] ");
        return filter;
    }
}
