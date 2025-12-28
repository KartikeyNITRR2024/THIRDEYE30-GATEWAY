package com.thirdeye3.gateway.loadbalancers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class HeaderStickyRoundRobinLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private static final Logger log = LoggerFactory.getLogger(HeaderStickyRoundRobinLoadBalancer.class);
    
    private final String serviceId;
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    private final AtomicInteger position = new AtomicInteger(0);

    public HeaderStickyRoundRobinLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId) {
        this.serviceId = serviceId;
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);
        return supplier.get(request).next().map(instances -> processInstanceResponse(instances, request));
    }

    private Response<ServiceInstance> processInstanceResponse(List<ServiceInstance> instances, Request request) {
        if (instances.isEmpty()) {
            log.warn("No instances available for service: {}", serviceId);
            return new org.springframework.cloud.client.loadbalancer.EmptyResponse();
        }

        List<ServiceInstance> sortedInstances = instances.stream()
                .sorted(Comparator.comparing(ServiceInstance::getInstanceId))
                .collect(Collectors.toList());

        String headerValue = null;
        if (request.getContext() instanceof RequestDataContext context) {
            headerValue = context.getClientRequest().getHeaders().getFirst("webscrapper-unique-id");
        }

        if (headerValue != null && !headerValue.isBlank()) {
            int index = Math.abs(headerValue.hashCode()) % sortedInstances.size();
            ServiceInstance chosen = sortedInstances.get(index);
            
            log.info("STICKY ROUTE: [webscrapper-unique-id: {}] -> Instance: {} (Port: {}) [Total Instances: {}]", 
                     headerValue, chosen.getInstanceId(), chosen.getPort(), sortedInstances.size());
            
            return new DefaultResponse(chosen);
        }
        
        int pos = Math.abs(this.position.incrementAndGet());
        ServiceInstance chosen = sortedInstances.get(pos % sortedInstances.size());
        
        log.info("ROUND-ROBIN ROUTE: No header -> Instance: {} (Port: {})", 
                 chosen.getInstanceId(), chosen.getPort());
                 
        return new DefaultResponse(chosen);
    }
}