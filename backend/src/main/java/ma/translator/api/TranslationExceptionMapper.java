package ma.translator.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class TranslationExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException wae) {
            return wae.getResponse();
        }

        Throwable t = exception;
        for (int i = 0; i < 12 && t != null; i++) {
            if (t instanceof IllegalArgumentException ia) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(singleError(ia.getMessage()))
                        .build();
            }
            if (t instanceof IllegalStateException is) {
                return Response.status(Response.Status.BAD_GATEWAY)
                        .entity(singleError(is.getMessage()))
                        .build();
            }
            t = t.getCause();
        }

        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", exception.getMessage() != null ? exception.getMessage() : "Unexpected error");
        body.put("type", exception.getClass().getSimpleName());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(body)
                .build();
    }

    private static Map<String, String> singleError(String msg) {
        return Map.of("error", msg != null ? msg : "error");
    }
}
