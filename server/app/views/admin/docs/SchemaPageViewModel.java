package views.admin.docs;

import controllers.docs.routes;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public final class SchemaPageViewModel implements BaseViewModel {
  private final String programSlug;
  private final String stage;
  private final String openApiVersion;
  private final List<ProgramSlugOption> programSlugs;
  private final List<SelectOption> stageOptions;
  private final List<SelectOption> openApiVersionOptions;
  private final String apiUrl;

  public String getFormActionUrl() {
    return routes.OpenApiSchemaController.getSchemaUIRedirect(
            Optional.empty(), Optional.empty(), Optional.empty())
        .url();
  }

  public String getApiDocsTabUrl() {
    return controllers.docs.routes.ApiDocsController.index().url();
  }

  public String getSchemaTabUrl() {
    return routes.OpenApiSchemaController.getSchemaUI("", Optional.empty(), Optional.empty()).url();
  }

  @Data
  @Builder
  public static final class ProgramSlugOption {
    private final String slug;
    private final boolean selected;
  }

  @Data
  @Builder
  public static final class SelectOption {
    private final String label;
    private final String value;
    private final boolean selected;
  }
}
