package views.applicant;

import views.questiontypes.ApplicantQuestionRendererFactory;

public interface ApplicantProgramBlockEditViewFactory {

  ApplicantProgramBlockEditView create(ApplicantQuestionRendererFactory rendererFactory);
}
