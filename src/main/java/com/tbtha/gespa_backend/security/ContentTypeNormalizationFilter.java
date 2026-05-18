package com.tbtha.gespa_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
public class ContentTypeNormalizationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String contentType = request.getContentType();
        if (contentType == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String normalized = normalizeJsonContentType(contentType);
        if (normalized == null) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequestWrapper wrapped = new HttpServletRequestWrapper(request) {
            @Override
            public String getContentType() {
                return normalized;
            }

            @Override
            public String getHeader(String name) {
                if ("Content-Type".equalsIgnoreCase(name)) {
                    return normalized;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if ("Content-Type".equalsIgnoreCase(name)) {
                    return Collections.enumeration(List.of(normalized));
                }
                return super.getHeaders(name);
            }
        };

        filterChain.doFilter(wrapped, response);
    }

    private String normalizeJsonContentType(String contentType) {
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            if (!MediaType.APPLICATION_JSON.includes(mediaType)) {
                return null;
            }
            return MediaType.APPLICATION_JSON_VALUE;
        } catch (Exception ex) {
            return null;
        }
    }
}
