package views.questiontypes;

import services.applicant.ApplicantData;
import services.applicant.ApplicantQuestion;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;
import services.question.UnsupportedQuestionTypeException;

public class ApplicantQuestionRendererFactory {

  public ApplicantQuestionRenderer getSampleRenderer(QuestionType questionType)
      throws UnsupportedQuestionTypeException {
    QuestionDefinition questionDefinition = QuestionDefinitionBuilder.sample(questionType).build();
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(questionDefinition, new ApplicantData());
    return getRenderer(applicantQuestion);
  }

  public ApplicantQuestionRenderer getRenderer(ApplicantQuestion question) {
    switch (question.getType()) {
      case TEXT:
        {
          return new TextQuestionRenderer(question);
        }

      case NAME:
        {
          return new NameQuestionRenderer(question);
        }

      case NUMBER:
        {
          return new NumberQuestionRenderer(question);
        }

      case ADDRESS:
        {
          return new AddressQuestionRenderer(question);
        }

      default:
        throw new UnsupportedOperationException(
            "Unrecognized question type: " + question.getType());
    }
  }
}
