package views.admin.questions;

import lombok.Builder;
import lombok.Data;

/** Data for a single Primary Applicant Info tag. */
@Data
@Builder
public final class PaiTagInfo {
  private final String fieldName;
  private final String displayName;
  private final String description;
  private final boolean enabled;
  private final boolean toggleHidden;
  private final boolean differentQuestionHasTag;
  private final String otherQuestionName;
  private final boolean isUniversal;

  public String getAlreadySetAlertText() {
    return String.format(
        "You cannot make this question a Primary applicant information question because the"
            + " question named \"%s\" is already set as a Primary applicant information"
            + " question.",
        otherQuestionName);
  }

  public String getNotUniversalAlreadySetAlertText() {
    return String.format(
        "You cannot make this question a Primary applicant information question because the"
            + " question is not a Universal question and because the question named \"%s\" is"
            + " already set as a Primary applicant information question.",
        otherQuestionName);
  }
}
