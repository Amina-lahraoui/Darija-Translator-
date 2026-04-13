package ma.translator;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import ma.translator.api.TranslationExceptionMapper;
import ma.translator.security.BasicAuthentificationFilter;
import ma.translator.security.CORSFilter;

import java.util.Set;

@ApplicationPath("/api")
public class TranslatorApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
                CORSFilter.class,
                BasicAuthentificationFilter.class,
                AboutResource.class,
                TranslatorResource.class,
                TranslationExceptionMapper.class
        );
    }
}
