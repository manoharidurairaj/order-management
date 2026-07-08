package com.ordermgmt.pipeline.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient simulatorsRestClient(
            @Value("${ordermgmt.simulators.base-url:http://localhost:8082}") String baseUrl,
            @Value("${ordermgmt.simulators.connect-timeout:PT2S}") Duration connectTimeout,
            @Value("${ordermgmt.simulators.read-timeout:PT6S}") Duration readTimeout) {

        // Bounding the read timeout above the simulator's max injected latency
        // (5s) is a deliberate substitute for Resilience4j's reactive
        // TimeLimiter here: the calls are synchronous, and wrapping a blocking
        // RestClient call in CompletableFuture just to satisfy an annotation
        // would add complexity without changing behavior.
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(connectTimeout)
                .withReadTimeout(readTimeout);
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
