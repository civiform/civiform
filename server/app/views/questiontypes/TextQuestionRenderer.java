package views.questiontypes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.OptionTag;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.TextQuestion;
import views.components.FieldWithLabel;
import views.style.BaseStyles;

import static j2html.TagCreator.*;

/** Renders a text question. */
public class TextQuestionRenderer extends ApplicantSingleQuestionRenderer {

  public TextQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-text";
  }

  @Override
  protected DivTag renderInputTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ImmutableList<String> ariaDescribedByIds) {
    TextQuestion textQuestion = question.createTextQuestion();

   /* FieldWithLabel textField =
      FieldWithLabel.input()
        .setFieldName(textQuestion.getTextPath().toString())
        .setValue(textQuestion.getTextValue().orElse(""))
        .setFieldErrors(
          params.messages(),
          validationErrors.getOrDefault(textQuestion.getTextPath(), ImmutableSet.of()))
        .setAriaDescribedByIds(ariaDescribedByIds)
        .setScreenReaderText(question.getQuestionTextForScreenReader());*/
    OptionTag optionTagUS = option("US").withValue("US");
    OptionTag optionTagCA = option("CA").withValue("CA");
    //OptionTag optionTagIN = option("IN").withValue("IN");


  /**  DivTag phoneField =
      new DivTag().with(select()
        .withId("cf-country-selector")
        .with(optionTagCA)
        .with(optionTagUS))
        .with(textField.getInputTag());*/
   /* OptionTag optionTagUS = option("US").withValue("US");
    OptionTag optionTagCA = option("CA").withValue("CA");
    OptionTag optionTagIN = option("IN").withValue("IN");
*/
    DivTag phoneInput = new DivTag().with(select()
                                    .withId("cf-country-selector")
                                    .with(optionTagCA)
                                    .with(optionTagUS))
                                  .with(input()
                                  .withId(getReferenceClass())
                                  .withType("text")
                                  .withValue(textQuestion.getTextValue().orElse("2066150808")));
    return phoneInput;//return textField.getInputTag();//phoneInput;
  }
}
