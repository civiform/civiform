package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import j2html.tags.specialized.DivTag;
import java.util.Locale;
import play.i18n.Messages;
import services.MessageKey;
import services.applicant.question.ApplicantQuestion;
import views.components.TextFormatter;
import views.style.ReferenceClasses;

/** This renders the question text as formatted text. */
public class StaticContentQuestionRenderer implements ApplicantQuestionRenderer {
  private final ApplicantQuestion question;
  private final Messages messages;

  public StaticContentQuestionRenderer(ApplicantQuestion question, Messages messages) {
    this.question = checkNotNull(question);
    this.messages = checkNotNull(messages);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-static";
  }

  @Override
  public DivTag render(ApplicantQuestionRendererParams params) {
    DivTag questionTextDiv =
        div()
            .withClasses(ReferenceClasses.APPLICANT_QUESTION_TEXT, "mb-2", "font-bold", "text-xl")
            .with(
                TextFormatter.formatTextWithAriaLabel(
                    question.getQuestionText(),
                    /* preserveEmptyLines= */ true,
                    /* addRequiredIndicator= */ false,
                    messages
                        .at(MessageKey.LINK_OPENS_NEW_TAB_SR.getKeyName())
                        .toLowerCase(Locale.ROOT)));
    return div()
        .withId(question.getContextualizedPath().toString())
        .withClasses("mx-auto", "mb-8", this.getReferenceClass())
        .with(questionTextDiv);
  }
}
