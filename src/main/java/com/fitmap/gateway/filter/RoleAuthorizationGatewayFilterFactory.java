package com.fitmap.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitmap.gateway.payload.response.ErrorResponse;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.style.ToStringCreator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;

@Log4j2
@Component
public class RoleAuthorizationGatewayFilterFactory extends AbstractGatewayFilterFactory<RoleAuthorizationGatewayFilterFactory.Config> {

    private static final int FILTER_ORDER = 200;

    private final ObjectMapper objMapper;

    public RoleAuthorizationGatewayFilterFactory(ObjectMapper objMapper) {
        super(Config.class);
        this.objMapper = objMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {

        return new OrderedGatewayFilter((exchange, chain) -> {

                var request = exchange.getRequest();

                try {

                    checkRequestAuthorization(request.getHeaders().getOrEmpty("User_roles"), config.getRoles());

                    return chain.filter(exchange);

                } catch (Exception e) {

                    log.error(e);

                    var responseStatus = HttpStatus.UNAUTHORIZED;

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

    private void checkRequestAuthorization(Collection<String> currentRoles, Collection<String> roles) {

        if(CollectionUtils.isEmpty(roles)) {

            return;
        }

        for (var role : roles) {

            if(currentRoles.contains(role)) {
                return;
            }
        }

        throw new RuntimeException("don't have enough privileges.");
    }

    @Getter
    @Setter
    public static class Config {

        private Collection<String> roles;

        @Override
        public String toString() {
            return new ToStringCreator(this).append("mandatory-roles", roles).toString();
        }
    }

}
