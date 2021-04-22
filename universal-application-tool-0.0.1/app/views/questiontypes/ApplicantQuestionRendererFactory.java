package views.questiontypes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.LifecycleStage;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

public class ApplicantQuestionRendererFactory {

  public ApplicantQuestionRenderer getSampleRenderer(QuestionType questionType)
      throws UnsupportedQuestionTypeException {
    QuestionDefinition questionDefinition = questionDefinitionSample(questionType);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(questionDefinition, new ApplicantData());
    return getRenderer(applicantQuestion);
  }

  public ApplicantQuestionRenderer getRenderer(ApplicantQuestion question) {
    switch (question.getType()) {
      case ADDRESS:
        return new AddressQuestionRenderer(question);
      case CHECKBOX:
        return new CheckboxQuestionRenderer(question);
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
      case REPEATER:
        return new RepeaterQuestionRenderer(question);
      case TEXT:
        return new TextQuestionRenderer(question);
      default:
        throw new UnsupportedOperationException(
            "Unrecognized question type: " + question.getType());
    }
  }

  private static QuestionDefinition questionDefinitionSample(QuestionType questionType) throws UnsupportedQuestionTypeException {
    QuestionDefinitionBuilder builder =
        new QuestionDefinitionBuilder()
            .setName("")
            .setDescription("")
            .setPath(Path.create("sample.question.path"))
            .setQuestionText(ImmutableMap.of(Locale.US, "Sample question text"))
            .setQuestionHelpText(ImmutableMap.of(Locale.US, "Sample question help text"))
            .setLifecycleStage(LifecycleStage.ACTIVE)
            .setQuestionType(questionType);

    if (questionType.isMultiOptionType()) {
      builder.setQuestionOptions(
          ImmutableList.of(
              QuestionOption.create(1L, ImmutableMap.of(Locale.US, "Sample question option"))));
    }

    return builder.build();
  }
}
