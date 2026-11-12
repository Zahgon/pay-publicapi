package uk.gov.pay.api.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Logs the start and end of each API call and keeps its {@code X-Request-Id} in the MDC, from where the requests the
 * call makes to connector, ledger and public auth take it. As before, an exception escaping the call is logged here
 * rather than handed to the container.
 */
public class LoggingFilter implements Filter {

    public static final String MDC_REQUEST_ID_KEY = "x_request_id";

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        long start = System.nanoTime();
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;

        String requestURL = httpRequest.getRequestURI();
        String requestMethod = httpRequest.getMethod();
        String requestId = httpRequest.getHeader("X-Request-Id");

        if (requestId == null) {
            MDC.remove(MDC_REQUEST_ID_KEY);
        } else {
            MDC.put(MDC_REQUEST_ID_KEY, requestId);
        }

        logger.info("{} to {} began", requestMethod, requestURL);
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception | Error exception) {
            logger.error("Exception - {}", exception.getMessage(), exception);
        } finally {
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            logger.info("{} to {} ended - total time {}ms", requestMethod, requestURL, elapsed);
        }
    }
}
