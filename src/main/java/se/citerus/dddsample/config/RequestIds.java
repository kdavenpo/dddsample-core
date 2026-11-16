package se.citerus.dddsample.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Request IDs for the CS 8674 data-collection runs (DECISIONS 37). Each request gets an ID, or keeps
 * the one it arrives with, and calls to other services pass it on. The access log records the ID
 * and the header as received, so a call from another service can be told from a request from
 * outside. Kept in a configuration class, so the extractor leaves it out of the analyzed code.
 */
@Configuration
public class RequestIds {

    public static final String HEADER = "X-Request-Id";
    public static final String ATTRIBUTE = "requestId";

    @Bean
    public OncePerRequestFilter requestIdFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                String id = request.getHeader(HEADER);
                request.setAttribute(ATTRIBUTE, id != null ? id : UUID.randomUUID().toString());
                chain.doFilter(request, response);
            }
        };
    }

    /** Adds the current request's ID to an outgoing call; adds nothing outside a request. */
    public static ClientHttpRequestInterceptor passOn() {
        return (request, body, execution) -> {
            RequestAttributes current = RequestContextHolder.getRequestAttributes();
            Object id = current == null ? null : current.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            if (id != null) {
                request.getHeaders().set(HEADER, id.toString());
            }
            return execution.execute(request, body);
        };
    }
}
