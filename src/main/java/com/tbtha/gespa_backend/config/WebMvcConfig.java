package com.tbtha.gespa_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        MediaType jsonUtf8 = new MediaType("application", "json", StandardCharsets.UTF_8);

        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                List<MediaType> supported = new ArrayList<>(jacksonConverter.getSupportedMediaTypes());

                if (!supported.contains(MediaType.APPLICATION_JSON)) {
                    supported.add(MediaType.APPLICATION_JSON);
                }
                if (!supported.contains(jsonUtf8)) {
                    supported.add(jsonUtf8);
                }

                jacksonConverter.setSupportedMediaTypes(supported);
            }
        }
    }
}
