package views.admin.docs;

import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public final class ApiDocsPageViewModel implements BaseViewModel {
  private final String selectedProgramSlug;
  private final boolean activeVersion;
  private final List<ProgramSlugOption> programSlugs;
  private final boolean programFound;
  private final List<QuestionDoc> questions;
  private final String jsonPreview;
  private final String apiDocsLink;

  public String getApiDocsTabUrl() {
    return controllers.docs.routes.ApiDocsController.index().url();
  }

  public String getSchemaTabUrl() {
    return controllers.docs.routes.OpenApiSchemaController.getSchemaUI(
            "", Optional.empty(), Optional.empty())
        .url();
  }

  @Data
  @Builder
  public static final class ProgramSlugOption {
    private final String slug;
    private final boolean selected;
  }

  @Data
  @Builder
  public static final class QuestionDoc {
    private final String name;
    private final String questionNameKey;
    private final String type;
    private final String questionText;
    private final boolean multiOption;
    private final List<String> currentOptions;
    private final List<String> allHistoricOptions;
  }
}
