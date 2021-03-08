package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.text;

import j2html.tags.Tag;
import services.applicant.ApplicantQuestion;
import views.BaseHtmlView;

public class AddressQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public AddressQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public Tag render() {
    ApplicantQuestion.AddressQuestion addressQuestion = question.getAddressQuestion();

    return div(
        text(question.getQuestionText()),
        label(
            input()
                .withType("text")
                .withCondValue(
                    addressQuestion.hasStreetValue(), addressQuestion.getStreetValue().orElse(""))
                .withName(addressQuestion.getStreetPath().path())
                .withPlaceholder("Street address"),
            fieldErrors(addressQuestion.getStreetErrors())),
        label(
            input()
                .withType("text")
                .withCondValue(
                    addressQuestion.hasCityValue(), addressQuestion.getCityValue().orElse(""))
                .withName(addressQuestion.getCityPath().path())
                .withPlaceholder("City"),
            fieldErrors(addressQuestion.getCityErrors())),
        label(
            input()
                .withType("text")
                .withCondValue(
                    addressQuestion.hasStateValue(), addressQuestion.getStateValue().orElse(""))
                .withName(addressQuestion.getStatePath().path())
                .withPlaceholder("State"),
            fieldErrors(addressQuestion.getStateErrors())),
        label(
            input()
                .withType("text")
                .withCondValue(
                    addressQuestion.hasZipValue(), addressQuestion.getZipValue().orElse(""))
                .withName(addressQuestion.getZipPath().path())
                .withPlaceholder("Zip code"),
            fieldErrors(addressQuestion.getZipErrors())));
  }
}
