package views.questiontypes;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import services.LocalizedStrings;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

public class ApplicantQuestionRendererFactory {

  /** This is just used in creating samples, and they don't need real contextualized paths. */
  private static final Path FAKE_CONTEXTUALIZED_PATH = Path.create("fake");

  public ApplicantQuestionRenderer getSampleRenderer(QuestionType questionType)
      throws UnsupportedQuestionTypeException {
    QuestionDefinition questionDefinition = questionDefinitionSample(questionType);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(questionDefinition, new ApplicantData(), FAKE_CONTEXTUALIZED_PATH);
    return getRenderer(applicantQuestion);
  }

  public ApplicantQuestionRenderer getRenderer(ApplicantQuestion question) {
    switch (question.getType()) {
      case ADDRESS:
        return new AddressQuestionRenderer(question);
      case CHECKBOX:
        return new CheckboxQuestionRenderer(question);
      case DATE:
        return new DateQuestionRenderer(question);
      case DROPDOWN:
        return new DropdownQuestionRenderer(question);
      case FILEUPLOAD:
        return new FileUploadQuestionRenderer(question);
      case NAME:
        return new NameQuestionRenderer(question);
      case NUMBER:
        return new NumberQuestionRenderer(question);
      case RADIO_BUTTON:
        return new RadioButtonQuestionRenderer(question);
      case ENUMERATOR:
        return new EnumeratorQuestionRenderer(question);
      case TEXT:
        return new TextQuestionRenderer(question);
      default:
        throw new UnsupportedOperationException(
            "Unrecognized question type: " + question.getType());
    }
  }

  private static QuestionDefinition questionDefinitionSample(QuestionType questionType)
      throws UnsupportedQuestionTypeException {
    QuestionDefinitionBuilder builder =
        new QuestionDefinitionBuilder()
            .setName("")
            .setDescription("")
            .setQuestionText(LocalizedStrings.of(Locale.US, "Sample question text"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "Sample question help text"))
            .setQuestionType(questionType);

    if (questionType.isMultiOptionType()) {
      builder.setQuestionOptions(
          ImmutableList.of(
              QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "Sample question option"))));
    }

    if (questionType.equals(QuestionType.ENUMERATOR)) {
      builder.setEntityType(LocalizedStrings.withDefaultValue("Sample repeated entity type"));
    }

    return builder.build();
  }
}
