package ma.translator;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import ma.translator.dto.TranslationRequest;
import ma.translator.dto.TranslationResponse;
import ma.translator.service.GroqTranslationService;

@RequestScoped
@Path("/translator")
public class TranslatorResource {

    @Inject
    private GroqTranslationService groqTranslationService;

    @POST
    @Path("/translate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TranslationResponse translate(TranslationRequest request) {
        String source = request != null ? request.getText() : null;
        var outcome = groqTranslationService.translateToDarija(source);
        return new TranslationResponse(source, outcome.translatedText(), outcome.modelUsed());
    }

    @GET
    @Path("/translate")
    @Produces(MediaType.APPLICATION_JSON)
    public TranslationResponse translateQuery(@QueryParam("text") String text) {
        var outcome = groqTranslationService.translateToDarija(text);
        return new TranslationResponse(text, outcome.translatedText(), outcome.modelUsed());
    }
}
