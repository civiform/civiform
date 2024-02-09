package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableList;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import java.util.Locale;
import java.util.Optional;
import play.i18n.Messages;
import services.MessageKey;
import services.applicant.question.ApplicantQuestion;
import views.components.TextFormatter;
import views.style.ReferenceClasses;

/** This renders the question text as formatted text. */
public class StaticContentQuestionRenderer implements ApplicantQuestionRenderer {
  private final ApplicantQuestion question;
  private final Optional<Messages> maybeMessages;

  public StaticContentQuestionRenderer(
      ApplicantQuestion question, Optional<Messages> maybeMessages) {
    this.question = checkNotNull(question);
    this.maybeMessages = checkNotNull(maybeMessages);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-static";
  }

  @Override
  public DivTag render(ApplicantQuestionRendererParams params) {

    ImmutableList<DomContent> formattedText;
    // Static questions that are shown to the applicant will have the messages object passed in
    // Previews of static questions that are shown to admin will not
    if (maybeMessages.isPresent()) {
      formattedText =
          TextFormatter.formatTextWithAriaLabel(
              question.getQuestionText(),
              /* preserveEmptyLines= */ true,
              /* addRequiredIndicator= */ false,
              maybeMessages
                  .get()
                  .at(MessageKey.LINK_OPENS_NEW_TAB_SR.getKeyName())
                  .toLowerCase(Locale.ROOT));
    } else {
      formattedText =
          TextFormatter.formatText(
              question.getQuestionText(),
              /* preserveEmptyLines= */ true,
              /* addRequiredIndicator= */ false);
    }

    DivTag questionTextDiv =
        div()
            .withClasses(ReferenceClasses.APPLICANT_QUESTION_TEXT, "mb-2", "font-bold", "text-xl")
            .with(formattedText);
    return div()
        .withId(question.getContextualizedPath().toString())
        .withClasses("mx-auto", "mb-8", this.getReferenceClass())
        .with(questionTextDiv);
  }
}
