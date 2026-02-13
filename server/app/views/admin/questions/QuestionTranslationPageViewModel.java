package views.admin.questions;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public class QuestionTranslationPageViewModel implements BaseViewModel {
  private final String questionName;
  private final long questionId;
  private final String localeTag;
  private final String currentLocaleDisplayName;
  private final String concurrencyToken;
  private final ImmutableList<LocaleLink> localeLinks;
  private final TranslationField questionTextField;
  private final Optional<TranslationField> helpTextField;
  private final String questionType;
  private final ImmutableList<TranslationField> typeSpecificFields;
  private final Optional<String> errorMessage;

  public String getFormActionUrl() {
    return routes.AdminQuestionTranslationsController.update(questionName, localeTag).url();
  }

  public String getEditQuestionUrl() {
    return routes.AdminQuestionController.edit(questionId).url();
  }

  @Data
  @Builder
  public static class LocaleLink {
    private final String questionName;
    private final String localeTag;
    private final String displayName;
    private final boolean selected;

    public String getUrl() {
      return routes.AdminQuestionTranslationsController.edit(questionName, localeTag).url();
    }
  }

  @Data
  @Builder
  public static class TranslationField {
    private final String fieldName;
    private final String label;
    private final String value;
    private final String defaultText;
    private final boolean isTextArea;
  }
}
