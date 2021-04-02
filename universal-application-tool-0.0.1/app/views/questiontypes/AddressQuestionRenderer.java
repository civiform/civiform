package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.AddressQuestion;
import views.BaseHtmlView;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

public class AddressQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  private static final String SECONDARY_ADDRESS_STYLES =
      StyleUtils.joinStyles(Styles.MY_0, Styles.P_2, Styles.PB_1);

  public AddressQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public Tag render() {
    AddressQuestion addressQuestion = question.getAddressQuestion();

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
                .withClasses(
                    Styles.ROUNDED,
                    Styles.BG_OPACITY_50,
                    Styles.BG_GRAY_100,
                    Styles.PT_2,
                    Styles.PB_4)
                .with(
                    /** First line of address entry: Street */
                    FieldWithLabel.input()
                        .setFieldName(addressQuestion.getStreetPath().path())
                        .setLabelText("Street address")
                        .setPlaceholderText("Enter your street address")
                        .setFloatLabel(true)
                        .setValue(addressQuestion.getStreetValue().orElse(""))
                        .getContainer()
                        .withClasses(Styles.MY_2, Styles.P_2, Styles.PB_0),
                    /** Second line of address entry: City, State, Zip */
                    div()
                        .withClasses(Styles.FLEX, Styles.FLEX_ROW)
                        .with(
                            FieldWithLabel.input()
                                .setFieldName(addressQuestion.getCityPath().path())
                                .setLabelText("City")
                                .setPlaceholderText("City")
                                .setFloatLabel(true)
                                .setValue(addressQuestion.getCityValue().orElse(""))
                                .getContainer()
                                .withClasses(SECONDARY_ADDRESS_STYLES, Styles.W_1_2),
                            FieldWithLabel.input()
                                .setFieldName(addressQuestion.getStatePath().path())
                                .setLabelText("State")
                                .setPlaceholderText("State")
                                .setFloatLabel(true)
                                .setValue(addressQuestion.getStateValue().orElse(""))
                                .getContainer()
                                .withClasses(SECONDARY_ADDRESS_STYLES, Styles.W_1_3),
                            FieldWithLabel.input()
                                .setFieldName(addressQuestion.getZipPath().path())
                                .setLabelText("Zip")
                                .setPlaceholderText("Zip")
                                .setFloatLabel(true)
                                .setValue(addressQuestion.getZipValue().orElse(""))
                                .getContainer()
                                .withClasses(SECONDARY_ADDRESS_STYLES, Styles.W_1_6)),
                    fieldErrors(addressQuestion.getQuestionErrors())
                        .withClasses(
                            Styles.ML_2, Styles.TEXT_XS, Styles.TEXT_RED_600, Styles.FONT_BOLD)));
  }
}
