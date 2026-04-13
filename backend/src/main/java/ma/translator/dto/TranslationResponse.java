package ma.translator.dto;

public class TranslationResponse {
    private String sourceText;
    private String translatedText;
    private String model;

    public TranslationResponse() {
    }

    public TranslationResponse(String sourceText, String translatedText, String model) {
        this.sourceText = sourceText;
        this.translatedText = translatedText;
        this.model = model;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
