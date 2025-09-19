package com.thirdeye3.gateway.externalcontollers;

import com.thirdeye3.gateway.dtos.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class SelfClient {

    @Autowired
    private WebClient webClient;

    @Value("${self.url}")
    private String baseUrl;

    public Response<String> statusChecker(Integer id, String code) {
        return webClient.get()
                .uri(baseUrl + "/api/statuschecker/{id}/{code}", id, code)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Response<String>>() {})
                .block();  
    }
}

