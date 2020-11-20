package com.fitmap.gateway.filter;

import java.util.Collection;
import java.util.Set;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.style.ToStringCreator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class RoleAuthorizationGatewayFilterFactory extends AbstractGatewayFilterFactory<RoleAuthorizationGatewayFilterFactory.Config> {

    private static final int FILTER_ORDER = 200;

    public RoleAuthorizationGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {

        return new OrderedGatewayFilter((exchange, chain) -> {

                try {

                    var request = exchange.getRequest();

                    checkRequestAuthorization(request.getHeaders().getOrEmpty("USER_ROLES"), config.getRolesGroups());
    
                    return chain.filter(exchange);
    
                } catch (Exception e) {
    
                    log.error(e);
    
                    var response = exchange.getResponse();
    
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    
                    return response.setComplete();
                }
    
            }, FILTER_ORDER);
    }

    private void checkRequestAuthorization(Collection<String> currentRoles, Collection<Set<String>> mandatoryRolesGroups) {

        if(CollectionUtils.isEmpty(mandatoryRolesGroups)) {

            return;
        }

        for (var group : mandatoryRolesGroups) {

            if(group.containsAll(currentRoles)) {
                return;
            }
        }

        throw new RuntimeException("don't have enough privileges.");
    }

    @Getter
    @Setter
    public static class Config {

        private Set<Set<String>> rolesGroups;

        @Override
        public String toString() {
            return new ToStringCreator(this).append("roles-groups", rolesGroups).toString();
        }
    }

}
