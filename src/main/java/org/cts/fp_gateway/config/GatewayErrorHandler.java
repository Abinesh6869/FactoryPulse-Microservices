package org.cts.fp_gateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@Order(-2)
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = resolveStatus(ex);
        String message = resolveMessage(ex, status);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("path", exchange.getRequest().getURI().getPath());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = ("{\"status\":" + status.value() + "}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (isConnectionFailure(ex)) return HttpStatus.SERVICE_UNAVAILABLE;
        if (ex instanceof org.springframework.web.server.ResponseStatusException rse) {
            return HttpStatus.resolve(rse.getStatusCode().value()) != null
                    ? HttpStatus.resolve(rse.getStatusCode().value())
                    : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(Throwable ex, HttpStatus status) {
        if (status == HttpStatus.SERVICE_UNAVAILABLE)
            return "Service is temporarily unavailable. Please try again later.";
        return ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred.";
    }

    private boolean isConnectionFailure(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConnectException) return true;
            String name = current.getClass().getName();
            if (name.contains("ConnectException") || name.contains("ConnectTimeout")) return true;
            current = current.getCause();
        }
        return false;
    }
}
