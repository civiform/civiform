package views.admin.programs;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public class ProgramTranslationPageViewModel implements BaseViewModel {
  private final String programName;
  private final String programAdminName;
  private final String localeTag;
  private final String currentLocaleDisplayName;
  private final ImmutableList<LocaleLink> localeLinks;
  private final ImmutableList<TranslationSection> sections;
  private final Optional<String> errorMessage;

  public String getFormActionUrl() {
    return routes.AdminProgramTranslationsController.update(programAdminName, localeTag).url();
  }

  @Data
  @Builder
  public static class LocaleLink {
    private final String programAdminName;
    private final String localeTag;
    private final String displayName;
    private final boolean selected;

    public String getUrl() {
      return routes.AdminProgramTranslationsController.edit(programAdminName, localeTag).url();
    }
  }

  @Data
  @Builder
  public static class TranslationSection {
    private final String legend;
    private final String editDefaultUrl;
    private final ImmutableList<TranslationField> fields;
    private final ImmutableList<HiddenField> hiddenFields;
  }

  @Data
  @Builder
  public static class TranslationField {
    private final String fieldName;
    private final String label;
    private final String value;
    private final String defaultText;
    private final boolean isTextArea;
    private final boolean required;
  }

  @Data
  @Builder
  public static class HiddenField {
    private final String name;
    private final String value;
  }
}
