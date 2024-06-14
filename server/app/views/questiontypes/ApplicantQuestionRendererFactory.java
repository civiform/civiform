package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import play.i18n.Messages;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.program.ProgramQuestionDefinition;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import views.applicant.ApplicantFileUploadRenderer;

/** A helper class for constructing type-specific applicant question renderers. */
public final class ApplicantQuestionRendererFactory {

  private final ApplicantFileUploadRenderer applicantFileUploadRenderer;

  public ApplicantQuestionRendererFactory(ApplicantFileUploadRenderer applicantFileUploadRenderer) {
    this.applicantFileUploadRenderer = checkNotNull(applicantFileUploadRenderer);
  }

  public ApplicantQuestionRenderer getSampleRenderer(QuestionType questionType)
      throws UnsupportedQuestionTypeException {
    QuestionDefinition questionDefinition = questionDefinitionSample(questionType);
    System.out.println(
        "ssandbekkhaug J2HTML question text: " + questionDefinition.getQuestionText());
    ProgramQuestionDefinition pqd =
        ProgramQuestionDefinition.create(questionDefinition, Optional.empty());
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(pqd, new ApplicantData(), Optional.empty());
    return getRenderer(applicantQuestion, Optional.empty());
  }

  public ApplicantQuestionRenderer getRenderer(
      ApplicantQuestion question, Optional<Messages> maybeMessages) {
    switch (question.getType()) {
      case ADDRESS:
        return new AddressQuestionRenderer(question);
      case CHECKBOX:
        return new CheckboxQuestionRenderer(question);
      case CURRENCY:
        return new CurrencyQuestionRenderer(question);
      case DATE:
        return new DateQuestionRenderer(question);
      case DROPDOWN:
        return new DropdownQuestionRenderer(question);
      case EMAIL:
        return new EmailQuestionRenderer(question);
      case FILEUPLOAD:
        return new FileUploadQuestionRenderer(question, applicantFileUploadRenderer);
      case ID:
        return new IdQuestionRenderer(question);
      case NAME:
        return new NameQuestionRenderer(question);
      case NUMBER:
        return new NumberQuestionRenderer(question);
      case RADIO_BUTTON:
        return new RadioButtonQuestionRenderer(question);
      case ENUMERATOR:
        return new EnumeratorQuestionRenderer(question);
      case STATIC:
        return new StaticContentQuestionRenderer(question, maybeMessages);
      case TEXT:
        return new TextQuestionRenderer(question);
      case PHONE:
        return new PhoneQuestionRenderer(question);
      case NULL_QUESTION:
        throw new IllegalStateException(
            String.format(
                "Question type %s should not be rendered. Question ID: %s. Active program question"
                    + " definition is possibly pointing to an old question ID",
                question.getType(), question.getQuestionDefinition().getId()));
      default:
        throw new UnsupportedOperationException(
            "Unrecognized question type: " + question.getType());
    }
  }

  private static QuestionDefinition questionDefinitionSample(QuestionType questionType)
      throws UnsupportedQuestionTypeException {
    QuestionDefinitionBuilder builder =
        new QuestionDefinitionBuilder()
            .setId(1L)
            .setName("")
            .setDescription("")
            .setQuestionText(LocalizedStrings.of(Locale.US, "Sample question text"))
            .setQuestionType(questionType);

    if (questionType.isMultiOptionType()) {
      builder.setQuestionOptions(
          ImmutableList.of(
              QuestionOption.create(
                  1L,
                  1L,
                  "sample option admin name",
                  LocalizedStrings.of(Locale.US, "Sample question option"))));
    }

    if (questionType.equals(QuestionType.ENUMERATOR)) {
      builder.setEntityType(LocalizedStrings.withDefaultValue("Sample repeated entity type"));
    }

    return builder.build();
  }
}
