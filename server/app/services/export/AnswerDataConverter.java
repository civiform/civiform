package services.export;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.CfJsonDocumentContext;
import services.Path;
import services.applicant.AnswerData;
import services.applicant.question.Question;
import services.question.types.QuestionType;

public final class AnswerDataConverter {
  private static final Logger logger = LoggerFactory.getLogger(AnswerDataConverter.class);

  private static final ImmutableSet<QuestionType> USE_APPLICATION_PATH_TYPES =
      ImmutableSet.of(
          QuestionType.NAME,
          QuestionType.ID,
          QuestionType.TEXT,
          QuestionType.EMAIL,
          QuestionType.ADDRESS,
          QuestionType.DROPDOWN,
          QuestionType.RADIO_BUTTON,
          QuestionType.FILEUPLOAD);

  public static void exportToJsonApplication(
      CfJsonDocumentContext jsonApplication, AnswerData answerData) {
    ImmutableMap<Path, ?> entries = getEntriesForAnswerData(answerData);

    for (Map.Entry<Path, ?> entry : entries.entrySet()) {
      Path path = entry.getKey();

      if (USE_APPLICATION_PATH_TYPES.contains(answerData.questionDefinition().getQuestionType())) {
        path = path.asApplicationPath();
      }

      Object value = entry.getValue();
      if (value instanceof String) {
        jsonApplication.putString(path, (String) value);
      } else if (value instanceof Long) {
        jsonApplication.putLong(path, (Long) value);
      } else if (value instanceof Double) {
        jsonApplication.putDouble(path, (Double) value);
      } else if (instanceOfNonEmptyImmutableListOfString(value)) {
        @SuppressWarnings("unchecked")
        ImmutableList<String> list = (ImmutableList<String>) value;
        jsonApplication.putArray(path, list);
      }
    }
  }

  private static ImmutableMap<Path, ?> getEntriesForAnswerData(AnswerData answerData) {
    Question question;

    switch (answerData.questionDefinition().getQuestionType()) {
      case ENUMERATOR:
      case STATIC:
        // Enumerator and static content questions are not included in API response. See
        // EnumeratorQuestion.getJsonEntries and StaticContentQuestion.getJsonEntries.
        return ImmutableMap.of();
      case CHECKBOX:
        question = answerData.applicantQuestion().createMultiSelectQuestion();
        break;
      case CURRENCY:
        question = answerData.applicantQuestion().createCurrencyQuestion();
        break;
      case NUMBER:
        question = answerData.applicantQuestion().createNumberQuestion();
        break;
      case DATE:
        question = answerData.applicantQuestion().createDateQuestion();
        break;
      case PHONE:
        question = answerData.applicantQuestion().createPhoneQuestion();
        break;
      case NAME:
        question = answerData.applicantQuestion().createNameQuestion();
        break;
      case ID:
        question = answerData.applicantQuestion().createIdQuestion();
        break;
      case TEXT:
        question = answerData.applicantQuestion().createTextQuestion();
        break;
      case EMAIL:
        question = answerData.applicantQuestion().createEmailQuestion();
        break;
      case ADDRESS:
        question = answerData.applicantQuestion().createAddressQuestion();
        break;
      case DROPDOWN:
      case RADIO_BUTTON:
        question = answerData.applicantQuestion().createSingleSelectQuestion();
        break;
      case FILEUPLOAD:
        question = answerData.applicantQuestion().createFileUploadQuestion();
        break;
      default:
        logger
            .atError()
            .log("Unknown QuestionType {}", answerData.questionDefinition().getQuestionType());
        return ImmutableMap.of();
    }

    return question.getJsonEntries();
  }

  // Returns true if value is a non-empty ImmutableList<String>. This is the best
  // we can do given Java type erasure.
  private static boolean instanceOfNonEmptyImmutableListOfString(Object value) {
    if (!(value instanceof ImmutableList<?>)) {
      return false;
    }

    ImmutableList<?> list = (ImmutableList<?>) value;
    return !list.isEmpty() && list.get(0) instanceof String;
  }
}
