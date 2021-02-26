package services.program;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PdfExportConfig {
    public abstract String baseDocument();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract PdfExportConfig.Builder setBaseDocument(String baseDocument);
        public abstract PdfExportConfig build();
    }
}
