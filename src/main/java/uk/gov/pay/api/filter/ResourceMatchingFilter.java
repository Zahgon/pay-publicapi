package uk.gov.pay.api.filter;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Produces;
import io.micronaut.web.router.Router;
import io.micronaut.web.router.UriRouteInfo;
import uk.gov.pay.api.exception.mapper.ContainerErrors;
import uk.gov.pay.api.utils.AcceptHeader;
import uk.gov.pay.api.utils.MediaTypes;
import uk.gov.pay.api.utils.QueryParameters;

import jakarta.inject.Provider;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Resolves each request against the service's resources the way the public API has always answered: unknown paths,
 * methods a path does not support, OPTIONS and HEAD requests, entities of an unsupported media type and unacceptable
 * {@code Accept} headers are answered here with the API's generic error entities; every other request reaches its
 * Micronaut route on the normalised path (matrix parameters and a trailing slash removed).
 */
public class ResourceMatchingFilter implements Filter {

    private static final Set<String> DISPATCHED_METHODS = Set.of("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH");

    private final Provider<Router> router;
    private volatile List<Resource> resources;

    public ResourceMatchingFilter(Provider<Router> router) {
        this.router = router;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String method = request.getMethod();

        if (!DISPATCHED_METHODS.contains(method)) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        request.setAttribute(QueryParameters.RAW_QUERY_ATTRIBUTE, request.getQueryString());
        String path = normalise(request.getRequestURI());
        List<Resource> matching = resources().stream().filter(resource -> resource.matches(path)).toList();
        if (matching.isEmpty()) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "HTTP 404 Not Found");
            return;
        }

        Set<String> methods = new HashSet<>();
        matching.forEach(resource -> methods.add(resource.method()));
        List<Resource> candidates = matching.stream().filter(resource -> resource.method().equals(method)).toList();

        if (candidates.isEmpty() && method.equals("HEAD") && methods.contains("GET")) {
            Resource get = matching.stream().filter(resource -> resource.method().equals("GET")).findFirst().orElseThrow();
            if (negotiate(request, response, get)) {
                dispatch(chain, new NormalisedRequest(request, "GET", path, get), new BodilessResponse(response), answerType(request));
            }
            return;
        }

        if (candidates.isEmpty() && method.equals("OPTIONS")) {
            answerOptions(request, response, methods);
            return;
        }

        if (candidates.isEmpty()) {
            Set<String> allowed = new HashSet<>(methods);
            allowed.add("OPTIONS");
            response.setHeader("Allow", String.join(",", allowed));
            writeError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "HTTP 405 Method Not Allowed");
            return;
        }

        if (negotiate(request, response, candidates.get(0))) {
            dispatch(chain, new NormalisedRequest(request, method, path, candidates.get(0)), response, answerType(request));
        }
    }

    /**
     * Runs the request through Micronaut, holding the answer back until it is complete: when translating an exception
     * failed, the container's 500 page is sent instead.
     */
    private static void dispatch(FilterChain chain, HttpServletRequest request, HttpServletResponse response,
                                 String jsonAnswerType) throws IOException, ServletException {
        HeldResponse held = new HeldResponse(request, response, jsonAnswerType);
        chain.doFilter(request, held);
        if (request.getAttribute(ContainerErrors.CONTAINER_ERROR_ATTRIBUTE) instanceof Integer status) {
            response.reset();
            response.sendError(status);
            return;
        }
        held.release();
    }

    /**
     * Checks the entity's media type against what the resource consumes and the {@code Accept} header against what it
     * produces, answering 415 or 406 when they do not fit.
     */
    private boolean negotiate(HttpServletRequest request, HttpServletResponse response, Resource resource) throws IOException {
        String declaredType = request.getHeader("Content-Type");
        if (Collections.list(request.getHeaders("Content-Type")).size() > 1
                || declaredType != null && !MediaTypes.isValid(declaredType)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
        if (!resource.consumesAnything()) {
            String contentType = request.getContentType();
            if (contentType == null || !resource.consumes(contentType)) {
                writeError(response, 415, "HTTP 415 Unsupported Media Type");
                return false;
            }
        }
        if (!resource.producesAnything()) {
            AcceptHeader accept = AcceptHeader.parse(Collections.list(request.getHeaders("Accept")));
            if (resource.produces().stream().noneMatch(accept::accepts)) {
                writeError(response, 406, "HTTP 406 Not Acceptable");
                return false;
            }
        }
        return true;
    }

    /**
     * How a JSON answer is labelled: as the client's {@code Accept} header names JSON, parameters included.
     */
    private static String answerType(HttpServletRequest request) {
        return AcceptHeader.parse(Collections.list(request.getHeaders("Accept"))).responseType(MediaType.APPLICATION_JSON_TYPE);
    }

    /**
     * The automatic answer to OPTIONS: the allowed methods, listed in the entity when plain text is acceptable.
     */
    private void answerOptions(HttpServletRequest request, HttpServletResponse response, Set<String> methods) throws IOException {
        Set<String> allowed = new HashSet<>(methods);
        allowed.add("OPTIONS");
        if (methods.contains("GET")) {
            allowed.add("HEAD");
        }
        AcceptHeader accept = AcceptHeader.parse(Collections.list(request.getHeaders("Accept")));
        response.setStatus(HttpServletResponse.SC_OK);
        MediaType preferred = accept.preferred();
        if (preferred == null || accept.prefersPlainText()) {
            response.setContentType("text/plain");
            response.setHeader("Allow", String.join(",", allowed));
            byte[] entity = String.join(", ", allowed).getBytes(StandardCharsets.UTF_8);
            response.setContentLength(entity.length);
            response.getOutputStream().write(entity);
        } else {
            response.setHeader("Allow", String.join(",", allowed));
            response.setContentType(preferred.toString());
            response.setContentLength(0);
        }
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        byte[] entity = ("{\"code\":" + status + ",\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        response.setStatus(status);
        response.setContentType("application/json");
        response.setContentLength(entity.length);
        response.getOutputStream().write(entity);
    }

    /**
     * The path resources are matched against: matrix parameters dropped from every segment and a trailing slash
     * removed.
     */
    static String normalise(String requestUri) {
        StringBuilder path = new StringBuilder();
        for (String segment : requestUri.split("/", -1)) {
            int matrix = segment.indexOf(';');
            if (!path.isEmpty() || !segment.isEmpty()) {
                path.append('/');
            }
            path.append(matrix < 0 ? segment : segment.substring(0, matrix));
        }
        String normalised = path.toString().replaceAll("/+$", "");
        return normalised.isEmpty() ? "/" : normalised;
    }

    private List<Resource> resources() {
        if (resources == null) {
            List<Resource> found = new ArrayList<>();
            router.get().uriRoutes()
                    .filter(route -> !route.getHttpMethodName().equals("HEAD"))
                    .forEach(route -> found.add(Resource.of(route)));
            resources = List.copyOf(found);
        }
        return resources;
    }

    private record Resource(String method, Pattern template, List<MediaType> consumesTypes, List<MediaType> produces) {

        static Resource of(UriRouteInfo<?, ?> route) {
            String template = route.getUriMatchTemplate().toPathString();
            String regex = Pattern.quote(template).replaceAll("\\{[^}]+}", "\\\\E[^/]+\\\\Q");
            return new Resource(route.getHttpMethodName(), Pattern.compile(regex + "/?"),
                    declared(route, Consumes.class), declared(route, Produces.class));
        }

        /**
         * The media types a resource method declares it consumes or produces; none declared means any.
         */
        private static List<MediaType> declared(UriRouteInfo<?, ?> route, Class<? extends Annotation> annotation) {
            return Arrays.stream(route.getAnnotationMetadata().stringValues(annotation))
                    .map(MediaType::of)
                    .toList();
        }

        boolean matches(String path) {
            return template.matcher(path).matches();
        }

        boolean consumesAnything() {
            return consumesTypes.isEmpty() || consumesTypes.contains(MediaType.ALL_TYPE);
        }

        boolean producesAnything() {
            return produces.isEmpty() || produces.contains(MediaType.ALL_TYPE);
        }

        boolean consumes(String contentType) {
            try {
                MediaType type = MediaType.of(contentType);
                return consumesTypes.stream().anyMatch(consumed -> consumed.matches(type));
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    /**
     * The request as Micronaut sees it: the matched method and normalised path ({@code +} escaped, as Micronaut's
     * router does not take it literally), no {@code Accept} header (negotiation is done) and, for resources that
     * consume JSON, a plain JSON content type (the entity is read from its bytes whatever charset was named).
     */
    private static class NormalisedRequest extends HttpServletRequestWrapper {

        private final String method;
        private final String path;
        private final boolean consumesJson;

        NormalisedRequest(HttpServletRequest request, String method, String path, Resource resource) {
            super(request);
            this.method = method;
            this.path = path.replace("+", "%2B");
            this.consumesJson = !resource.consumesAnything();
        }

        @Override
        public String getMethod() {
            return method;
        }

        @Override
        public String getRequestURI() {
            return path;
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer();
            url.append(getScheme()).append("://").append(getServerName()).append(':').append(getServerPort()).append(path);
            return url;
        }

        @Override
        public String getContentType() {
            return consumesJson ? "application/json" : super.getContentType();
        }

        /**
         * Query parameters are decoded by the service itself from the raw query (see {@link QueryParameters}); Micronaut
         * is not handed a query it might refuse.
         */
        @Override
        public String getQueryString() {
            return null;
        }

        @Override
        public String getHeader(String name) {
            if (name.equalsIgnoreCase("Accept")) {
                return null;
            }
            if (name.equalsIgnoreCase("Content-Type")) {
                return getContentType();
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (name.equalsIgnoreCase("Accept")) {
                return Collections.emptyEnumeration();
            }
            if (name.equalsIgnoreCase("Content-Type")) {
                String contentType = getContentType();
                return contentType == null ? Collections.emptyEnumeration() : Collections.enumeration(List.of(contentType));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new ArrayList<>(Collections.list(super.getHeaderNames()));
            names.removeIf(name -> name.equalsIgnoreCase("Accept"));
            return Collections.enumeration(names);
        }
    }

    /**
     * Holds an answer's entity until the request is done, so that it can still be replaced by an error page.
     */
    private static class HeldResponse extends HttpServletResponseWrapper {

        private final ByteArrayOutputStream entity = new ByteArrayOutputStream();
        private final ServletOutputStream stream = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                throw new UnsupportedOperationException("answers are written synchronously");
            }

            @Override
            public void write(int b) {
                entity.write(b);
            }

            @Override
            public void write(byte[] bytes, int offset, int length) {
                entity.write(bytes, offset, length);
            }
        };
        private final HttpServletRequest request;
        private final String jsonAnswerType;
        private PrintWriter writer;

        HeldResponse(HttpServletRequest request, HttpServletResponse response, String jsonAnswerType) {
            super(response);
            this.request = request;
            this.jsonAnswerType = jsonAnswerType;
        }

        @Override
        public void setContentType(String type) {
            super.setContentType(relabel(type));
        }

        @Override
        public void setHeader(String name, String value) {
            super.setHeader(name, name.equalsIgnoreCase("Content-Type") ? relabel(value) : value);
        }

        @Override
        public void addHeader(String name, String value) {
            super.addHeader(name, name.equalsIgnoreCase("Content-Type") ? relabel(value) : value);
        }

        private String relabel(String type) {
            boolean errorEntity = request.getAttribute(ContainerErrors.ERROR_ENTITY_ATTRIBUTE) != null;
            return type != null && !errorEntity && type.equalsIgnoreCase("application/json") ? jsonAnswerType : type;
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return stream;
        }

        @Override
        public PrintWriter getWriter() {
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8));
            }
            return writer;
        }

        @Override
        public void flushBuffer() {
            // held until the request is done
        }

        void release() throws IOException {
            if (writer != null) {
                writer.flush();
            }
            if (entity.size() > 0) {
                getResponse().getOutputStream().write(entity.toByteArray());
            }
        }
    }

    /**
     * Keeps the status and headers of a GET answer for a HEAD request and drops its entity.
     */
    private static class BodilessResponse extends HttpServletResponseWrapper {

        private final ServletOutputStream discarded = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                throw new UnsupportedOperationException("HEAD answers are written synchronously");
            }

            @Override
            public void write(int b) {
                // a HEAD answer has no entity
            }
        };

        BodilessResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return discarded;
        }

        @Override
        public PrintWriter getWriter() {
            return new PrintWriter(Writer.nullWriter());
        }

        @Override
        public void setContentLength(int length) {
            // the length of the entity a GET would carry is not known to a HEAD answer
        }

        @Override
        public void setContentLengthLong(long length) {
            // the length of the entity a GET would carry is not known to a HEAD answer
        }
    }
}
