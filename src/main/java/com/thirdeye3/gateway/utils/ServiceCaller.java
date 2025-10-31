package com.thirdeye3.gateway.utils;

import com.thirdeye3.gateway.configs.ServiceConfig;
import com.thirdeye3.gateway.externalcontollers.ServiceClient;
import com.thirdeye3.gateway.dtos.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Component
public class ServiceCaller {

    private static final Logger logger = LoggerFactory.getLogger(ServiceCaller.class);

    @Autowired
    private ServiceConfig serviceConfig;

    @Autowired
    private ServiceClient serviceClient;
    
    @Value("${thirdeye.call.services}")
    private Integer callServices;

    public void callAllServices() {
    	
    	if(callServices.intValue() == 0)
    	{
    		return;
    	}
    	
        List<String> urls = serviceConfig.getUrls();

        if (urls == null || urls.isEmpty()) {
            logger.warn("⚠️ No URLs configured in ServiceConfig.");
            return;
        }

        List<CompletableFuture<Response<String>>> futures = new ArrayList<>();

        for (String url : urls) {
            futures.add(serviceClient.statusChecker(url));
        }

        CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    futures.forEach(future -> {
                        try {
                            Response<String> response = future.get();
                            if (response == null) {
                            	logger.warn("⚠️ Received null or empty response from one of the APIs.");
                            }
                        } catch (Exception e) {
                            logger.error("❌ Exception while processing API response: {}", e.getMessage(), e);
                        }
                    });

                    logger.info("📘 Finished processing all async API responses.");
                })
                .exceptionally(ex -> {
                    logger.error("💥 Error completing async API calls: {}", ex.getMessage(), ex);
                    return null;
                });
    }
}
