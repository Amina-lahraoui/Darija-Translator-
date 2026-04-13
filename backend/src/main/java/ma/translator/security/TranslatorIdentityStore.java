package ma.translator.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;

import java.util.Set;

@ApplicationScoped
public class TranslatorIdentityStore implements IdentityStore {

    @Override
    public CredentialValidationResult validate(Credential credential) {
        if (!(credential instanceof UsernamePasswordCredential up)) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }
        String expectedUser = firstNonBlank(
                System.getenv("TRANSLATOR_USER"),
                System.getProperty("translator.user"),
                "DarijaTranslator");
        String expectedPass = firstNonBlank(
                System.getenv("TRANSLATOR_PASSWORD"),
                System.getProperty("translator.password"),
                "Morocco");

        if (expectedUser.equals(up.getCaller()) && expectedPass.equals(up.getPasswordAsString())) {
            return new CredentialValidationResult(expectedUser, Set.of("user"));
        }
        return CredentialValidationResult.INVALID_RESULT;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }
}
