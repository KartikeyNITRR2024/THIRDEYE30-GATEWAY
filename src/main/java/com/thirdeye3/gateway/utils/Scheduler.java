package com.thirdeye3.gateway.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.thirdeye3.gateway.dtos.Response;
import com.thirdeye3.gateway.externalcontollers.SelfClient;
@Component
public class Scheduler {
	
    @Autowired
    SelfClient selfClient;
	
	private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);
    
    @Value("${thirdeye.uniqueId}")
    private Integer uniqueId;

    @Value("${thirdeye.uniqueCode}")
    private String uniqueCode;
	
	@Scheduled(fixedRate = 30000)
    public void checkStatusTask() {
        Response<String> response = selfClient.statusChecker(uniqueId, uniqueCode);
        logger.info("Status check response is {}", response.getResponse());
    }

}



