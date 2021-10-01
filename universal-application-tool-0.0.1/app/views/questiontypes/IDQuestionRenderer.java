package views.questiontypes;

import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.IDQuestion;
import views.components.FieldWithLabel;

/** Renders an id question. */
public class IDQuestionRenderer extends ApplicantQuestionRenderer {

    public IDQuestionRenderer(ApplicantQuestion question) {
        super(question);
    }

    @Override
    public String getReferenceClass() {
        return "cf-question-id";
    }

    @Override
    public Tag render(ApplicantQuestionRendererParams params) {
        IDQuestion idQuestion = question.createIDQuestion();

        Tag questionFormContent =
                FieldWithLabel.input()
                        .setFieldName(idQuestion.getIDPath().toString())
                        .setValue(idQuestion.getIDValue().orElse(""))
                        .setFieldErrors(params.messages(), idQuestion.getQuestionErrors())
                        .setScreenReaderText(question.getQuestionText())
                        .getContainer();

        return renderInternal(params.messages(), questionFormContent, false);
    }
}
