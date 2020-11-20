package com.fitmap.gateway.filter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.style.ToStringCreator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class KeyIdAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<KeyIdAuthenticationGatewayFilterFactory.Config> {

    private static final int FILTER_ORDER = 100;

    private final FirebaseAuth firebaseAuth;

    public KeyIdAuthenticationGatewayFilterFactory(final FirebaseAuth firebaseAuth) {
        super(Config.class);
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public GatewayFilter apply(Config config) {

        return new OrderedGatewayFilter((exchange, chain) -> {

                var request = exchange.getRequest();
    
                if (isAnonymousUrl(request, config)) {
    
                    return chain.filter(exchange);
                }
    
                try {
    
                    var decodedToken = authenticateRequest(request);
    
                    var claims = decodedToken.getClaims();
                    List<String> roles = Objects.requireNonNullElse((List<String>) claims.get("roles"), Collections.emptyList());
    
                    var userId = decodedToken.getUid();
                    var userRoles = roles.stream().toArray(String[]::new);
    
                    var reqBuilder = request.mutate();
    
                    reqBuilder.header("USER_ID", userId);
                    reqBuilder.header("USER_ROLES", userRoles);
    
                    return chain.filter(exchange.mutate().request(reqBuilder.build()).build());
    
                } catch (Exception e) {
    
                    log.error(e);
    
                    var response = exchange.getResponse();
    
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    
                    return response.setComplete();
                }
    
            }, FILTER_ORDER);
    }

    private FirebaseToken authenticateRequest(ServerHttpRequest request) throws FirebaseAuthException {

        var headers = request.getHeaders();

        var authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);

        if (StringUtils.isBlank(authHeader) || !StringUtils.startsWithIgnoreCase(authHeader, "Bearer ")) {
            throw new  RuntimeException("without auth header");
        }

        var idToken = authHeader.replace("Bearer ", "");

        return firebaseAuth.verifyIdToken(idToken);
    }

    private boolean isAnonymousUrl(ServerHttpRequest request, Config config) {

        var path = request.getPath().value();

        if(CollectionUtils.isEmpty(config.getAnonymousUrlPatterns())) {
            return false;
        }

        return config.getAnonymousUrlPatterns().stream().anyMatch(path::matches);
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
