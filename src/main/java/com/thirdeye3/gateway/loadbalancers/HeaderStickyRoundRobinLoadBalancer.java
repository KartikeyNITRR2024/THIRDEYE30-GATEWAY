package com.thirdeye3.gateway.loadbalancers;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HeaderStickyRoundRobinLoadBalancer implements ReactorServiceInstanceLoadBalancer {

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
            return new org.springframework.cloud.client.loadbalancer.EmptyResponse();
        }

        String headerValue = null;
        if (request.getContext() instanceof RequestDataContext context) {
            headerValue = context.getClientRequest().getHeaders().getFirst("webscrapper-unique-id");
        }

        if (headerValue != null && !headerValue.isBlank()) {
            int index = Math.abs(headerValue.hashCode()) % instances.size();
            return new DefaultResponse(instances.get(index));
        }
        
        int pos = Math.abs(this.position.incrementAndGet());
        return new DefaultResponse(instances.get(pos % instances.size()));
    }
}