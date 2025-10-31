package com.thirdeye3.gateway.externalcontollers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.thirdeye3.gateway.dtos.Response;

import java.util.concurrent.CompletableFuture;

@Component
public class ServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ServiceClient.class);

    @Autowired
    private WebClient webClient;
    
    @Value("${thirdeye.api.key}")
    private String apikey;

    @Async("asyncExecutor")
    public CompletableFuture<Response<String>> statusChecker(String baseUrl) {
        return webClient.get()
                .uri(baseUrl)
                .header("THIRDEYE-API-KEY", apikey)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Response<String>>() {})
                .toFuture()
                .thenApply(response -> {
                    return response;
                })
                .exceptionally(ex -> {
                    logger.error("❌ Error while calling API [{}]: {}", baseUrl, ex.getMessage(), ex);
                    return null;
                });
    }
}
