package com.fitmap.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitmap.gateway.payload.response.ErrorResponse;
import com.google.firebase.auth.FirebaseAuth;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.style.ToStringCreator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;

@Log4j2
@Component
public class KeyIdAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<KeyIdAuthenticationGatewayFilterFactory.Config> {

    private static final int FILTER_ORDER = 100;

    private final FirebaseAuth firebaseAuth;
    private final ObjectMapper objMapper;

    public KeyIdAuthenticationGatewayFilterFactory(FirebaseAuth firebaseAuth, ObjectMapper objMapper) {
        super(Config.class);
        this.firebaseAuth = firebaseAuth;
        this.objMapper = objMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {

        return new OrderedGatewayFilter((exchange, chain) -> {

                var request = exchange.getRequest();

                if (isAnonymousUrl(request, config)) {

                    return chain.filter(exchange);
                }

                try {

                    var idToken = getIdToken(request);

                    var decodedToken = firebaseAuth.verifyIdToken(idToken);

                    var claims = decodedToken.getClaims();
                    List<String> roles = Objects.requireNonNullElse((List<String>) claims.get("roles"), Collections.emptyList());

                    var userId = decodedToken.getUid();
                    var userRoles = roles.stream().toArray(String[]::new);

                    var reqBuilder = request.mutate();

                    reqBuilder.header("User_id", userId);
                    reqBuilder.header("User_roles", userRoles);
                    reqBuilder.header("User_id_token", idToken);

                    return chain.filter(exchange.mutate().request(reqBuilder.build()).build());

                } catch (Exception e) {

                    log.error(e);

                    var responseStatus = HttpStatus.UNAUTHORIZED;

                    var response = exchange.getResponse();

                    response.setStatusCode(responseStatus);
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

                    try {

                        var body = ErrorResponse
                            .builder()
                            .timestamp(new Date())
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

    private String getIdToken(ServerHttpRequest request) {

        var headers = request.getHeaders();

        var authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);

        if (StringUtils.isBlank(authHeader) || !StringUtils.startsWithIgnoreCase(authHeader, "Bearer ")) {
            throw new  RuntimeException("without auth header");
        }

        return authHeader.replace("Bearer ", "");
    }

    private boolean isAnonymousUrl(ServerHttpRequest request, Config config) {

        var path = request.getPath().value();

        if(CollectionUtils.isEmpty(config.getAnonymousUrlPatterns())) {
            return false;
        }

        return config
            .getAnonymousUrlPatterns()
            .stream()
            .anyMatch(path::matches);
    }

    @Getter
    @Setter
    public static class Config {

        private Set<String> anonymousUrlPatterns;

        @Override
        public String toString() {
            return new ToStringCreator(this).append("anonymous-url-patterns", anonymousUrlPatterns).toString();
        }
    }

}
