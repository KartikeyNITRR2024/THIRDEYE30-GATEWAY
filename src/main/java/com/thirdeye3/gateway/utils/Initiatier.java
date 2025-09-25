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
    
	@PostConstruct
    public void init() throws Exception{
        logger.info("Initializing Initiatier...");
        logger.info("Initiatier initialized.");
    }

}


