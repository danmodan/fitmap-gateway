package com.fitmap.gateway.payload.response;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(value = Include.NON_EMPTY)
@JsonNaming(value = SnakeCaseStrategy.class)
public class ErrorResponse {

    private ZonedDateTime timestamp;
    private int status;
    private String statusError;
    private Set<Map<String, Object>> errors;
    private String message;
    private String path;

}
