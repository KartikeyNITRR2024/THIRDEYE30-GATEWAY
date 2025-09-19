package com.thirdeye3.gateway.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class EurekaClientConfig {

    @Value("${thirdeye.api.key}")
    private String apiKey;

    @Bean
    public EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier() {
        return (SSLContext sslContext, HostnameVerifier hostnameVerifier) -> {
            ClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            RestTemplate restTemplate = new RestTemplate(factory);

            List<ClientHttpRequestInterceptor> interceptors =
                    new ArrayList<>(restTemplate.getInterceptors());

            interceptors.add((request, body, execution) -> {
                request.getHeaders().add("THIRDEYE-API-KEY", apiKey);
                return execution.execute(request, body);
            });

            restTemplate.setInterceptors(interceptors);

            return restTemplate.getRequestFactory();
        };
    }
}
