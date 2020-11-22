package com.fitmap.gateway.filter;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitmap.gateway.payload.response.ErrorResponse;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenProvider;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;

@Log4j2
@Component
public class ServiceToServiceAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<ServiceToServiceAuthenticationGatewayFilterFactory.Config> {

    private static final int FILTER_ORDER = 300;

    private final GoogleCredentials googleCredentials;
    private final ObjectMapper objMapper;

    public ServiceToServiceAuthenticationGatewayFilterFactory(GoogleCredentials googleCredentials, ObjectMapper objMapper) {
        super(Config.class);
        this.googleCredentials = googleCredentials;
        this.objMapper = objMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {

        return new OrderedGatewayFilter((exchange, chain) -> {

            var request = exchange.getRequest();

            try {

                var serviceIdToken = createServiceIdToken(exchange);

                var reqBuilder = request.mutate();

                reqBuilder.header(HttpHeaders.AUTHORIZATION, serviceIdToken);

                return chain.filter(exchange.mutate().request(reqBuilder.build()).build());

            } catch (Exception e) {

                log.error(e);

                var responseStatus = HttpStatus.INTERNAL_SERVER_ERROR;

                var response = exchange.getResponse();

                response.setStatusCode(responseStatus);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

                try {

                    var body = ErrorResponse
                        .builder()
                        .timestamp(ZonedDateTime.now())
                        .status(responseStatus.value())
                        .statusError(responseStatus.getReasonPhrase())
                        .message(e.getMessage())
                        .path(request.getPath().value())
                        .build();

                        var bf = response.bufferFactory();

                        var db = bf.wrap(objMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8));

                        return response.writeWith(Flux.just(db));

                } catch (Exception ex) {
                    log.error(ex);
                }

                return response.setComplete();

            }

        }, FILTER_ORDER);
    }

    private String createServiceIdToken(ServerWebExchange exchange) throws IOException {

        var baseUri = ((Route) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)).getUri().toString();
        var path = ((URI) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR)).getPath();

        String serviceUrl = baseUri + path;
        serviceUrl = serviceUrl.replace(":443", "");

        String token = ((IdTokenProvider) googleCredentials).idTokenWithAudience(serviceUrl, Collections.emptyList()).getTokenValue();

        return "Bearer " + token;
    }

    public static class Config { /** */ }

}
