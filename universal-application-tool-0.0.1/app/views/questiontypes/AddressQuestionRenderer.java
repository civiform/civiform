package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import j2html.tags.Tag;
import services.applicant.ApplicantQuestion;
import views.BaseHtmlView;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;
import views.style.Styles;
import views.style.StyleUtils;

public class AddressQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  private final static String SECONDARY_ADDRESS_STYLES = 
    StyleUtils.joinStyles(Styles.MY_0, Styles.P_2, Styles.PB_1);

  public AddressQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public Tag render() {
    ApplicantQuestion.AddressQuestion addressQuestion = question.getAddressQuestion();

    return div()
        .withId(question.getPath().path())
        .withClasses(Styles.MX_AUTO, Styles.PX_16)
        .with(
            div()
                .withClasses(ReferenceClasses.APPLICANT_QUESTION_TEXT)
                .withText(question.getQuestionText()),
            div()
                .withClasses(
                    ReferenceClasses.APPLICANT_QUESTION_HELP_TEXT,
                    Styles.TEXT_BASE,
                    Styles.FONT_THIN,
                    Styles.MB_2)
                .withText(question.getQuestionHelpText()),
            div()
                .withClasses("rounded bg-opacity-50 bg-gray-100 pt-2 pb-4")
                .with(
                    /** First line of address entry: Street */
                    FieldWithLabel.input()
                        .setFieldName(addressQuestion.getStreetPath().path())
                        .setLabelText("Street address")
                        .setFloatLabel(true)
                        .setValue(addressQuestion.getStreetValue().orElse(""))
                        .getContainer().withClasses("my-2 p-2 pb-0"),
                    /** Second line of address entry: City, State, Zip */
                    div()
                        .withClasses(Styles.FLEX, Styles.FLEX_ROW)
                        .with(
                            FieldWithLabel.input()
                                .setFieldName(addressQuestion.getCityPath().path())
                                .setLabelText("City")
                                .setFloatLabel(true)
                                .setValue(addressQuestion.getCityValue().orElse(""))
                                .getContainer().withClasses(SECONDARY_ADDRESS_STYLES, Styles.W_1_2),
                            FieldWithLabel.input()
                                .setFieldName(addressQuestion.getStatePath().path())
                                .setLabelText("State")
                                .setFloatLabel(true)
                                .setValue(addressQuestion.getStateValue().orElse(""))
                                .getContainer().withClasses(SECONDARY_ADDRESS_STYLES, Styles.W_1_3),
                            FieldWithLabel.input()
                                .setFieldName(addressQuestion.getZipPath().path())
                                .setLabelText("Zip")
                                .setFloatLabel(true)
                                .setValue(addressQuestion.getZipValue().orElse(""))
                                .getContainer().withClasses(SECONDARY_ADDRESS_STYLES, Styles.W_1_6)
                                )));
    // TODO: Need to add field errors back.
  }
}
