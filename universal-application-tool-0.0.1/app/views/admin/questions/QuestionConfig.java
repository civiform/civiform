package views.admin.questions;

import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.label;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import java.util.AbstractMap.SimpleEntry;
import services.question.QuestionType;
import views.components.FieldWithLabel;
import views.components.SelectWithLabel;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

public class QuestionConfig {
  private String id = "";
  private String headerText = "Question Settings";
  private ContainerTag content = div();

  private static final String HEADER_CLASSES =
      StyleUtils.joinStyles(
          Styles.BG_TRANSPARENT,
          Styles.TEXT_GRAY_600,
          Styles.BLOCK,
          Styles.FONT_BOLD,
          Styles.TEXT_SM,
          Styles._MT_1,
          Styles.PB_0,
          Styles.MB_0,
          Styles.MX_2,
          Styles.UPPERCASE);

  private static final String INNER_DIV_CLASSES =
      StyleUtils.joinStyles(
          Styles.BORDER, Styles.BG_GRAY_100,
          Styles.PT_4, Styles.M_4);

  private static final String OUTER_DIV_CLASSES =
      StyleUtils.joinStyles(Styles.W_FULL, Styles.PT_0, Styles._MT_4);

  public QuestionConfig() {}

  public QuestionConfig setId(String id) {
    this.id = id;
    return this;
  }

  public QuestionConfig setHeaderText(String headerText) {
    this.headerText = headerText;
    return this;
  }

  public static ContainerTag buildQuestionConfig(QuestionType type) {
    QuestionConfig config = new QuestionConfig();
    switch (type) {
      case ADDRESS:
        return config.setId("address-question-config").addAddressQuestionConfig().getContainer();
      case DROPDOWN:
      case RADIO_BUTTON:
        return config
            .setId("single-select-question-config")
            .addMultiOptionQuestionConfig()
            .getContainer();
      case TEXT:
        return config.setId("text-question-config").addTextQuestionConfig().getContainer();
      case NUMBER:
        return config.setId("number-question-config").addNumberQuestionConfig().getContainer();
      case REPEATER: // fallthrough intended
      case NAME: // fallthrough intended - no options
      default:
        return div();
    }
  }

  private QuestionConfig addAddressQuestionConfig() {
    content.with(
        new SelectWithLabel()
            .setId("address-question-default-state-select")
            .setFieldName("defaultState")
            .setLabelText("Default State")
            .setOptions(stateOptions())
            .setValue("-")
            .getContainer(),
        FieldWithLabel.checkbox()
            .setId("address-question-allow-po-box-checkbox")
            .setFieldName("poBox")
            .setLabelText("Allow post office boxes")
            .getContainer(),
        FieldWithLabel.checkbox()
            .setId("address-question-include-none-checkbox")
            .setFieldName("noAddress")
            .setLabelText("Include \"No address\" option")
            .getContainer());
    return this;
  }

  private QuestionConfig addTextQuestionConfig() {
    content.with(
        FieldWithLabel.number()
            .setId("text-question-min-length-input")
            .setFieldName("minLength")
            .setLabelText("Min length")
            .getContainer(),
        FieldWithLabel.number()
            .setId("text-question-max-length-input")
            .setFieldName("maxLength")
            .setLabelText("Maximum length")
            .getContainer());
    return this;
  }

  private QuestionConfig addMultiOptionQuestionConfig() {
    content.with(
        button("Add answer option")
            .withType("button")
            .withId("add-new-option")
            .withClasses(Styles.M_2));
    return this;
  }

  private QuestionConfig addNumberQuestionConfig() {
    content.with(
        FieldWithLabel.number()
            .setId("number-question-min-value-input")
            .setFieldName("min")
            .setLabelText("Minimum value")
            .getContainer(),
        FieldWithLabel.number()
            .setId("number-question-max-value-input")
            .setFieldName("max")
            .setLabelText("Maximum value")
            .getContainer());
    return this;
  }

  public ContainerTag getContainer() {
    return div()
        .withCondId(!Strings.isNullOrEmpty(this.id), this.id)
        .withClasses(ReferenceClasses.QUESTION_CONFIG)
        .with(headerLabel(this.headerText))
        .with(
            div()
                .withClasses(OUTER_DIV_CLASSES)
                .with(content.withId("question-settings").withClasses(INNER_DIV_CLASSES)));
  }

  private static ContainerTag headerLabel(String text) {
    return label().withClasses(HEADER_CLASSES).withText(text);
  }

  /**
   * I don't feel like hard-coding a list of states here, so this will do until we can think up a
   * better approach.
   */
  private static ImmutableList<SimpleEntry<String, String>> stateOptions() {
    return ImmutableList.of(
        new SimpleEntry<String, String>("-- Leave blank --", "-"),
        new SimpleEntry<String, String>("Washington", "WA"));
  }
}
