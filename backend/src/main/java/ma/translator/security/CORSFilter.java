package ma.translator.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;



/**
 *This CORS "Cross-Origin Resource Sharing" allows browser client to access API gateways via a different origin/port.
 * If this filter is not used, CORS will block the fetch from {@code http://localhost:19006} to {@code :8080}

 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION - 100)
public class CORSFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static boolean isAllowedOrigin(String or) {
        if (or == null || or.isBlank()) {
            return false;
        }
        return or.startsWith("http://localhost:")
                || or.startsWith("http://127.0.0.1:")
                || or.startsWith("http://192.168.")
                || or.startsWith("http://10.");
    }

    @Override
    public void filter(ContainerRequestContext RContext) {
        if (!HttpMethod.OPTIONS.equalsIgnoreCase(RContext.getMethod())) {
            return;
        }
        String or = RContext.getHeaderString("Origin");
        if (!isAllowedOrigin(or)) {
            return;
        }
        RContext.abortWith(Response.noContent()
                .header("Access-Control-Allow-Origin", or)
                .header("Access-Control-Allow-Credentials", "true")
                .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                .header("Access-Control-Allow-Headers", "authorization, content-type")
                .header("Access-Control-Max-Age", "86400")
                .build());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String origin = requestContext.getHeaderString("Origin");
        if (origin != null && isAllowedOrigin(origin)) {
            responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", origin);
            responseContext.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
            responseContext.getHeaders().add("Vary", "Origin");
        }
    }
}
