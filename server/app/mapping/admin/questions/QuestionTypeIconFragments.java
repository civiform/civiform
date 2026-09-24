package mapping.admin.questions;

import com.google.common.base.CaseFormat;
import services.question.types.QuestionType;

/**
 * Resolves the legacy icon fragment names in {@code LegacySvgFragments.html} for question types,
 * mirroring {@code Icons.getIconTypeFromQuestionType}.
 */
public final class QuestionTypeIconFragments {

  private QuestionTypeIconFragments() {}

  /** The legacy icon name for a question type: the Icons enum constant lowercased. */
  public static String questionTypeIconName(QuestionType type) {
    return switch (type) {
      case ADDRESS -> "address";
      case CHECKBOX -> "checkbox";
      case CURRENCY -> "currency";
      case DATE -> "date";
      case DROPDOWN -> "dropdown";
      case EMAIL -> "email";
      case FILEUPLOAD -> "fileupload";
      case ID -> "id";
      case MAP -> "map";
      case NAME -> "name";
      case NUMBER -> "number";
      case RADIO_BUTTON, YES_NO -> "radio_button";
      case ENUMERATOR -> "enumerator";
      case STATIC -> "annotation";
      case TEXT -> "text";
      case PHONE -> "phone";
      default -> "unknown";
    };
  }

  /** The question type's icon fragment name in LegacySvgFragments.html ("iconAddress", ...). */
  public static String questionTypeIconFragment(QuestionType type) {
    return "icon"
        + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, questionTypeIconName(type));
  }
}
