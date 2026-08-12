package com.dawn.common.infra.web;

import com.dawn.common.core.constant.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api/v1", HandlerTypePredicate.forAnnotation(RestController.class));
    }

    @Bean("internalRestClient")
    public RestClient internalRestClient() {
        HttpClient httpClient = HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(Constants.INTERNAL_CONNECT_TIMEOUT_SECONDS))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(Constants.INTERNAL_READ_TIMEOUT_SECONDS));
        return RestClient
                .builder()
                .requestFactory(factory)
                .defaultHeader("Content-Type", "application/json")
//                .requestInterceptor((request, body, execution) -> {
//                    log.info("[RestClient] {} {}", request.getMethod(), request.getURI());
//                    return execution.execute(request, body);
//                })
                .build();
    }

    @Bean("externalRestClientBuilder")
    public RestClient.Builder externalRestClientBuilder() {
        HttpClient httpClient = HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(Constants.EXTERNAL_CONNECT_TIMEOUT_SECONDS))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(Constants.EXTERNAL_READ_TIMEOUT_SECONDS));
        return RestClient
                .builder()
                .requestFactory(factory)
                .defaultHeader("Content-Type", "application/json");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("redirect:/swagger-ui/index.html");
    }
}
