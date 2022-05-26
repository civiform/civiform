package views.admin.questions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.FormTag;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Langs;
import play.mvc.Http;
import play.twirl.api.Content;
import services.LocalizedStrings;
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
import views.components.ToastMessage;

/** Renders a list of languages to select from, and a form for updating question information. */
public class QuestionTranslationView extends TranslationFormView {

  private final AdminLayout layout;

  @Inject
  public QuestionTranslationView(AdminLayoutFactory layoutFactory, Langs langs) {
    super(checkNotNull(langs));
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.QUESTIONS);
  }

  public Content render(Http.Request request, Locale locale, QuestionDefinition question) {
    return render(request, locale, question, Optional.empty());
  }

  public Content renderErrors(
      Http.Request request, Locale locale, QuestionDefinition invalidQuestion, String errors) {
    return render(request, locale, invalidQuestion, Optional.of(errors));
  }

  private Content render(
      Http.Request request, Locale locale, QuestionDefinition question, Optional<String> errors) {
    String formAction =
        controllers.admin.routes.AdminQuestionTranslationsController.update(
                question.getId(), locale.toLanguageTag())
            .url();

    // Add form fields for questions.
    ImmutableList.Builder<FieldWithLabel> inputFields = ImmutableList.builder();
    inputFields.addAll(
        questionTextFields(locale, question.getQuestionText(), question.getQuestionHelpText()));
    inputFields.addAll(getQuestionTypeSpecificFields(question, locale));

    FormTag form = renderTranslationForm(request, locale, formAction, inputFields.build());

    String title = "Manage Question Translations";

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(
                div(renderHeader(title)), renderLanguageLinks(question.getId(), locale), div(form));
    errors.ifPresent(s -> htmlBundle.addToastMessages(ToastMessage.error(s).setDismissible(false)));

    return layout.renderCentered(htmlBundle);
  }

  @Override
  protected String languageLinkDestination(long questionId, Locale locale) {
    return controllers.admin.routes.AdminQuestionTranslationsController.edit(
            questionId, locale.toLanguageTag())
        .url();
  }

  private ImmutableList<FieldWithLabel> getQuestionTypeSpecificFields(
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
      default:
        return ImmutableList.of();
    }
  }

  private ImmutableList<FieldWithLabel> questionTextFields(
      Locale locale, LocalizedStrings questionText, LocalizedStrings helpText) {
    ImmutableList.Builder<FieldWithLabel> fields = ImmutableList.builder();
    fields.add(
        FieldWithLabel.input()
            .setId("localize-question-text")
            .setFieldName("questionText")
            .setLabelText(questionText.getDefault())
            .setPlaceholderText("Question text")
            .setValue(questionText.maybeGet(locale)));

    // Help text is optional - only show if present.
    if (!helpText.isEmpty()) {
      fields.add(
          FieldWithLabel.input()
              .setId("localize-question-help-text")
              .setFieldName("questionHelpText")
              .setLabelText(helpText.getDefault())
              .setPlaceholderText("Question help text")
              .setValue(helpText.maybeGet(locale)));
    }

    return fields.build();
  }

  private ImmutableList<FieldWithLabel> multiOptionQuestionFields(
      ImmutableList<QuestionOption> options, Locale toUpdate) {
    return options.stream()
        .map(
            option ->
                FieldWithLabel.input()
                    .setFieldName("options[]")
                    .setLabelText(option.optionText().getDefault())
                    .setPlaceholderText("Answer option")
                    .setValue(option.optionText().translations().getOrDefault(toUpdate, "")))
        .collect(toImmutableList());
  }

  private ImmutableList<FieldWithLabel> enumeratorQuestionFields(
      LocalizedStrings entityType, Locale toUpdate) {
    return ImmutableList.of(
        FieldWithLabel.input()
            .setFieldName("entityType")
            .setLabelText(entityType.getDefault())
            .setPlaceholderText("What are we enumerating?")
            .setValue(entityType.maybeGet(toUpdate).orElse("")));
  }
}
