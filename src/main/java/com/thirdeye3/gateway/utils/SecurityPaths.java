package com.thirdeye3.gateway.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecurityPaths {

    @Value("${thirdeye.open-api}")
    private String openApiPaths;

    @Value("${thirdeye.user-api}")
    private String userApiPaths;

    public List<String> getOpenApiPaths() {
        return Arrays.stream(openApiPaths.split(",")).map(String::trim).collect(Collectors.toList());
    }

    public List<String> getUserApiPaths() {
        return Arrays.stream(userApiPaths.split(",")).map(String::trim).collect(Collectors.toList());
    }
}
