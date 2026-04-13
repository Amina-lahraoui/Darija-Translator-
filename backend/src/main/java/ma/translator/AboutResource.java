package ma.translator;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("about")
public class AboutResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String about() {
        return "Darija Translator API is running.\n";
    }
}
