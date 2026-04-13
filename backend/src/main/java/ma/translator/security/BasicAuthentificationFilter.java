package ma.translator.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.security.enterprise.credential.Password;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class BasicAuthentificationFilter implements ContainerRequestFilter {

    @Inject
    private TranslatorIdentityStore identityStore;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }
        String path = requestContext.getUriInfo().getRequestUri().getPath();
        if (path != null && path.contains("/api/about")) {
            return;
        }

        String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.regionMatches(true, 0, "Basic ", 0, 6)) {
            unauthorized(requestContext);
            return;
        }

        String decoded;
        try {
            byte[] bytes = Base64.getDecoder().decode(auth.substring(6).trim());
            decoded = new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            unauthorized(requestContext);
            return;
        }

        int colon = decoded.indexOf(':');
        if (colon < 0) {
            unauthorized(requestContext);
            return;
        }

        String user = decoded.substring(0, colon);
        String pass = decoded.substring(colon + 1);
        UsernamePasswordCredential credential = new UsernamePasswordCredential(user, new Password(pass.toCharArray()));

        CredentialValidationResult result = identityStore.validate(credential);
        if (result.getStatus() != CredentialValidationResult.Status.VALID) {
            unauthorized(requestContext);
        }
    }

    private static void unauthorized(ContainerRequestContext ctx) {
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"TranslatorRealm\"")
                .build());
    }
}
