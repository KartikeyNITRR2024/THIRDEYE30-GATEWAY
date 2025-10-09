package com.thirdeye3.gateway.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class Initiatier {
	
    private static final Logger logger = LoggerFactory.getLogger(Initiatier.class);
    
    @Value("${thirdeye.priority}")
    private Integer priority;
    
    @Value("${thirdeye.jwt.secret}")
    private String secretKey;

    @Value("${thirdeye.jwt.token.starter}")
    private String starter;

    
	@PostConstruct
    public void init() throws Exception{
        logger.info("Initializing Initiatier...");
        logger.info("Starter is {} and length is {}", starter, starter.length());
        logger.info("Secret key is {} and length is {}", secretKey, secretKey.length());
        logger.info("Initiatier initialized.");
    }

}


