package com.fitmap.gateway.filter;

import java.io.IOException;
import java.util.Collections;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenProvider;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class ServiceToServiceAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<ServiceToServiceAuthenticationGatewayFilterFactory.Config> {

    private static final int FILTER_ORDER = 300;

    private final GoogleCredentials googleCredentials;

    public ServiceToServiceAuthenticationGatewayFilterFactory(final GoogleCredentials googleCredentials) {
        super(Config.class);
        this.googleCredentials = googleCredentials;
    }

    @Override
    public GatewayFilter apply(Config config) {

        return new OrderedGatewayFilter((exchange, chain) -> {

            try {

                var serviceIdToken = createServiceIdToken(exchange);

                var request = exchange.getRequest();

                var reqBuilder = request.mutate();

                reqBuilder.header(HttpHeaders.AUTHORIZATION, serviceIdToken);

                return chain.filter(exchange.mutate().request(reqBuilder.build()).build());

            } catch (Exception e) {

                log.error(e);

                var response = exchange.getResponse();

                response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

                return response.setComplete();
            }

        }, FILTER_ORDER);
    }

    private String createServiceIdToken(ServerWebExchange exchange) throws IOException {

        String serviceUrl = ((Route) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)).getUri().toString();
        serviceUrl = serviceUrl.replace(":443", "");

        String token = ((IdTokenProvider) googleCredentials).idTokenWithAudience(serviceUrl, Collections.emptyList()).getTokenValue();

        return "Bearer " + token;
    }

    public static class Config { /** */ }

}
