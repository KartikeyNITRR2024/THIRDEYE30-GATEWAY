package com.thirdeye3.gateway.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thirdeye3.gateway.dtos.Response;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ApiKeyFilter extends AbstractGatewayFilterFactory<ApiKeyFilter.Config> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiKeyFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst("TELEGRAMBOT-API-KEY");

            if (apiKey == null || !apiKey.equals(config.getValidApiKey())) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

                Response<String> response = new Response<>(
                        false,
                        HttpStatus.UNAUTHORIZED.value(),
                        "Invalid Request",
                        null
                );

                try {
                    byte[] bytes = objectMapper.writeValueAsBytes(response);
                    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                    return exchange.getResponse().writeWith(Mono.just(buffer));
                } catch (JsonProcessingException e) {
                    return exchange.getResponse().setComplete();
                }
            }

            return chain.filter(exchange);
        };
    }

    public static class Config {
        private String validApiKey;

        public String getValidApiKey() {
            return validApiKey;
        }

        public void setValidApiKey(String validApiKey) {
            this.validApiKey = validApiKey;
        }
    }
}
