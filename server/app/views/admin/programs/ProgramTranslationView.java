package views.admin.programs;

import static annotations.FeatureFlags.ApplicationStatusTrackingEnabled;
import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import forms.translation.ProgramTranslationForm;
import j2html.tags.DomContent;
import j2html.tags.specialized.FormTag;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import javax.inject.Inject;
import javax.inject.Provider;
import play.mvc.Http;
import play.twirl.api.Content;
import services.TranslationLocales;
import services.program.ProgramDefinition;
import services.program.StatusDefinitions;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.admin.TranslationFormView;
import views.components.FieldWithLabel;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.Styles;

/** Renders a list of languages to select from, and a form for updating program information. */
public final class ProgramTranslationView extends TranslationFormView {
  private final AdminLayout layout;
  private final Provider<Boolean> statusTrackingEnabled;

  @Inject
  public ProgramTranslationView(
      AdminLayoutFactory layoutFactory,
      TranslationLocales translationLocales,
      @ApplicationStatusTrackingEnabled Provider<Boolean> statusTrackingEnabled) {
    super(translationLocales);
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.statusTrackingEnabled = statusTrackingEnabled;
  }

  public Content render(
      Http.Request request,
      Locale locale,
      ProgramDefinition program,
      Optional<ProgramTranslationForm> maybeTranslationForm,
      Optional<ToastMessage> message) {
    String formAction =
        controllers.admin.routes.AdminProgramTranslationsController.update(
                program.id(), locale.toLanguageTag())
            .url();
    FormTag form =
        renderTranslationForm(
            request, locale, formAction, formFields(program, locale, maybeTranslationForm));

    String title = String.format("Manage program translations: %s", program.adminName());

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(renderHeader(title), renderLanguageLinks(program.id(), locale), form);

    message.ifPresent(htmlBundle::addToastMessages);

    return layout.renderCentered(htmlBundle);
  }

  @Override
  protected String languageLinkDestination(long programId, Locale locale) {
    return routes.AdminProgramTranslationsController.edit(programId, locale.toLanguageTag()).url();
  }

  private ImmutableList<DomContent> formFields(
      ProgramDefinition program,
      Locale locale,
      Optional<ProgramTranslationForm> maybeTranslationForm) {
    ProgramTranslationForm translationForm =
        maybeTranslationForm.orElse(ProgramTranslationForm.fromProgram(program, locale));
    String programDetailsLink =
        controllers.admin.routes.AdminProgramController.edit(program.id()).url();
    ImmutableList.Builder<DomContent> result =
        ImmutableList.<DomContent>builder()
            .add(
                fieldSetForFields(
                    legend()
                        .with(
                            span("Applicant-visible program details"),
                            new LinkElement()
                                .setText("(edit default)")
                                .setHref(programDetailsLink)
                                .setStyles(Styles.ML_2)
                                .asAnchorText()),
                    ImmutableList.of(
                        div()
                            .with(
                                FieldWithLabel.input()
                                    .setFieldName("displayName")
                                    .setLabelText("Program name")
                                    .setValue(translationForm.getDisplayName())
                                    .getInputTag(),
                                defaultLocaleTextHint(program.localizedName())),
                        div()
                            .with(
                                FieldWithLabel.input()
                                    .setFieldName("displayDescription")
                                    .setLabelText("Program description")
                                    .setValue(translationForm.getDisplayDescription())
                                    .getInputTag(),
                                defaultLocaleTextHint(program.localizedDescription())))));
    if (statusTrackingEnabled.get()) {
      String programStatusesLink =
          controllers.admin.routes.AdminProgramStatusesController.index(program.id()).url();

      for (StatusDefinitions.Status s : program.statusDefinitions().getStatuses()) {
        ImmutableList.Builder<DomContent> fieldsBuilder =
            ImmutableList.<DomContent>builder()
                .add(
                    div()
                        .with(
                            FieldWithLabel.input()
                                .setLabelText("Status name")
                                .setScreenReaderText("Status name")
                                .setValue(s.localizedStatusText().maybeGet(locale))
                                .getInputTag(),
                            defaultLocaleTextHint(s.localizedStatusText())));
        if (s.localizedEmailBodyText().isPresent()) {
          fieldsBuilder.add(
              div()
                  .with(
                      FieldWithLabel.textArea()
                          .setLabelText("Email content")
                          .setScreenReaderText("Email content")
                          .setValue(
                              s.localizedEmailBodyText()
                                  .map(localizedEmail -> localizedEmail.maybeGet(locale))
                                  .orElse(Optional.empty()))
                          .setRows(OptionalLong.of(8))
                          .getTextareaTag(),
                      defaultLocaleTextHint(s.localizedEmailBodyText().get())));
        }
        result.add(
            fieldSetForFields(
                legend()
                    .with(
                        span(String.format("Application status: %s", s.statusText())),
                        new LinkElement()
                            .setText("(edit default)")
                            .setHref(programStatusesLink)
                            .setStyles(Styles.ML_2)
                            .asAnchorText()),
                fieldsBuilder.build()));
      }
    }
    return result.build();
  }
}
