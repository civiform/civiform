package views.admin.questions;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import j2html.tags.DomContent;
import j2html.tags.specialized.FormTag;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import play.mvc.Http;
import play.twirl.api.Content;
import services.LocalizedStrings;
import services.TranslationLocales;
import services.question.QuestionOption;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.admin.TranslationFormView;
import views.components.FieldWithLabel;
import views.components.LinkElement;
import views.components.ToastMessage;

/** Renders a list of languages to select from, and a form for updating question information. */
public final class QuestionTranslationView extends TranslationFormView {

  private final AdminLayout layout;

  @Inject
  public QuestionTranslationView(
      AdminLayoutFactory layoutFactory, TranslationLocales translationLocales) {
    super(translationLocales);
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.QUESTIONS);
  }

  public Content render(Http.Request request, Locale locale, QuestionDefinition question) {
    return render(request, locale, question, Optional.empty());
  }

  public Content renderErrors(
      Http.Request request,
      Locale locale,
      QuestionDefinition invalidQuestion,
      ToastMessage errors) {
    return render(request, locale, invalidQuestion, Optional.of(errors));
  }

  private Content render(
      Http.Request request,
      Locale locale,
      QuestionDefinition question,
      Optional<ToastMessage> message) {
    String formAction =
        controllers.admin.routes.AdminQuestionTranslationsController.update(
                question.getName(), locale.toLanguageTag())
            .url();

    // Add form fields for questions.
    ImmutableList.Builder<DomContent> inputFieldsBuilder =
        ImmutableList.<DomContent>builder()
            .add(
                questionTextFields(
                    question, locale, question.getQuestionText(), question.getQuestionHelpText()));
    Optional<DomContent> questionTypeSpecificContent =
        getQuestionTypeSpecificContent(question, locale);
    if (questionTypeSpecificContent.isPresent()) {
      inputFieldsBuilder.add(questionTypeSpecificContent.get());
    }

    FormTag form = renderTranslationForm(request, locale, formAction, inputFieldsBuilder.build());

    String title = String.format("Manage question translations: %s", question.getName());

    HtmlBundle htmlBundle =
        layout
            .getBundle(request)
            .setTitle(title)
            .addMainContent(
                renderHeader(title), renderLanguageLinks(question.getName(), locale), form);
    message.ifPresent(htmlBundle::addToastMessages);

    return layout.renderCentered(htmlBundle);
  }

  @Override
  protected String languageLinkDestination(String questionName, Locale locale) {
    return controllers.admin.routes.AdminQuestionTranslationsController.edit(
            questionName, locale.toLanguageTag())
        .url();
  }

  private Optional<DomContent> getQuestionTypeSpecificContent(
      QuestionDefinition question, Locale toUpdate) {
    switch (question.getQuestionType()) {
      case CHECKBOX: // fallthrough intended
      case DROPDOWN: // fallthrough intended
      case RADIO_BUTTON:
        MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) question;
        return multiOptionQuestionFields(multiOption.getOptions(), toUpdate);
      case ENUMERATOR:
        EnumeratorQuestionDefinition enumerator = (EnumeratorQuestionDefinition) question;
        return enumeratorQuestionFields(enumerator.getEntityType(), toUpdate);
      case ADDRESS: // fallthrough intended
      case CURRENCY: // fallthrough intended
      case FILEUPLOAD: // fallthrough intended
      case NAME: // fallthrough intended
      case NUMBER: // fallthrough intended
      case TEXT: // fallthrough intended
      case PHONE: // fallthrough intended
      default:
        return Optional.empty();
    }
  }

  private DomContent questionTextFields(
      QuestionDefinition questionDefinition,
      Locale locale,
      LocalizedStrings questionText,
      LocalizedStrings helpText) {
    ImmutableList.Builder<DomContent> fields = ImmutableList.builder();
    fields.add(
        fieldWithDefaultLocaleTextHint(
            FieldWithLabel.textArea()
                .setFieldName("questionText")
                .setLabelText("Question text")
                .setValue(questionText.maybeGet(locale))
                .setMarkdownSupported(true)
                .setMarkdownText("Markdown is supported, ")
                .setMarkdownLinkText("see how it works")
                .getTextareaTag(),
            questionText));

    // Help text is optional - only show if present.
    if (!helpText.isEmpty()) {
      fields.add(
          fieldWithDefaultLocaleTextHint(
              FieldWithLabel.textArea()
                  .setFieldName("questionHelpText")
                  .setLabelText("Question help text")
                  .setValue(helpText.maybeGet(locale))
                  .setMarkdownSupported(true)
                  .setMarkdownText("Markdown is supported, ")
                  .setMarkdownLinkText("see how it works")
                  .getTextareaTag(),
              helpText));
    }

    return fieldSetForFields(
        legend()
            .with(
                span("Applicant-visible question details"),
                new LinkElement()
                    .setText("(edit default)")
                    .setHref(
                        controllers.admin.routes.AdminQuestionController.edit(
                                questionDefinition.getId())
                            .url())
                    .setStyles("ml-2")
                    .asAnchorText()),
        fields.build());
  }

  private Optional<DomContent> multiOptionQuestionFields(
      ImmutableList<QuestionOption> options, Locale toUpdate) {
    if (options.isEmpty()) {
      return Optional.empty();
    }
    ImmutableList.Builder<DomContent> optionFieldsBuilder = ImmutableList.builder();
    for (int optionIdx = 0; optionIdx < options.size(); optionIdx++) {
      QuestionOption option = options.get(optionIdx);
      optionFieldsBuilder.add(
          fieldWithDefaultLocaleTextHint(
              FieldWithLabel.input()
                  .setFieldName("options[]")
                  .setLabelText(String.format("Answer option #%d", optionIdx + 1))
                  .setValue(option.optionText().maybeGet(toUpdate).orElse(""))
                  .getInputTag(),
              option.optionText()));
    }

    return Optional.of(fieldSetForFields(legend("Answer options"), optionFieldsBuilder.build()));
  }

  private Optional<DomContent> enumeratorQuestionFields(
      LocalizedStrings entityType, Locale toUpdate) {
    return Optional.of(
        fieldWithDefaultLocaleTextHint(
            FieldWithLabel.textArea()
                .setFieldName("entityType")
                .setLabelText("What is being enumerated")
                .setValue(entityType.maybeGet(toUpdate).orElse(""))
                .setMarkdownSupported(true)
                .setMarkdownText("Markdown is supported, ")
                .setMarkdownLinkText("see how it works")
                .getTextareaTag(),
            entityType));
  }
}
