package services.program;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.net.URI;

@AutoValue
public abstract class PdfExportConfig {
  public abstract URI baseDocument();

  public abstract ImmutableMap<String, String> mappings();

  public static PdfExportConfig.Builder builder() {
    return new AutoValue_PdfExportConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract PdfExportConfig.Builder setBaseDocument(URI baseDocument);

    public abstract PdfExportConfig.Builder setMappings(ImmutableMap<String, String> mappings);

    public abstract PdfExportConfig build();
  }
}
