package views.applicant;

import views.questiontypes.ApplicantQuestionRendererFactory;

/**
 * Factory class for assisted injection of ApplicantQuestionRendererFactory.
 */
public interface ApplicantProgramBlockEditViewFactory {

  public ApplicantProgramBlockEditView create(ApplicantQuestionRendererFactory rendererFactory);
}
