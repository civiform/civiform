package views.questiontypes;

import services.applicant.ApplicantData;
import services.applicant.ApplicantQuestion;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

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
      case DROPDOWN:
        {
          return new DropdownQuestionRenderer(question);
        }

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
