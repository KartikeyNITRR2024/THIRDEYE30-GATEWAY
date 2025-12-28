package com.thirdeye3.gateway.configs;

import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.context.annotation.Configuration;

@Configuration
@LoadBalancerClient(name = "THIRDEYE30-STOCKVIEWER", configuration = StockViewerLoadBalancerConfig.class)
public class GatewayClientConfig {
}