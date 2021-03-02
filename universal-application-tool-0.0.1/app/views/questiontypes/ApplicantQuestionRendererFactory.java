package views.questiontypes;

import services.applicant.ApplicantQuestion;

public class ApplicantQuestionRendererFactory {

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
