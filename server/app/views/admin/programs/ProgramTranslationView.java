package views.admin.programs;

import static annotations.FeatureFlags.ApplicationStatusTrackingEnabled;
import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.admin.routes;
import forms.translation.ProgramTranslationForm;
import j2html.tags.DomContent;
import j2html.tags.specialized.FormTag;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;
import javax.inject.Inject;
import play.mvc.Http;
import play.twirl.api.Content;
import services.TranslationLocales;
import services.program.LocalizationUpdate;
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
  private final boolean statusTrackingEnabled;

  @Inject
  public ProgramTranslationView(
      AdminLayoutFactory layoutFactory,
      TranslationLocales translationLocales,
      @ApplicationStatusTrackingEnabled boolean statusTrackingEnabled) {
    super(translationLocales);
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.statusTrackingEnabled = statusTrackingEnabled;
  }

  public Content render(
      Http.Request request,
      Locale locale,
      ProgramDefinition program,
      ProgramTranslationForm translationForm,
      Optional<String> errors) {
    String formAction =
        controllers.admin.routes.AdminProgramTranslationsController.update(
                program.id(), locale.toLanguageTag())
            .url();
    FormTag form =
        renderTranslationForm(
            request, locale, formAction, formFields(program, locale, translationForm));

    String title = String.format("Manage program translations: %s", program.adminName());

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(renderHeader(title), renderLanguageLinks(program.id(), locale), form);

    errors.ifPresent(s -> htmlBundle.addToastMessages(ToastMessage.error(s).setDismissible(false)));

    return layout.renderCentered(htmlBundle);
  }

  @Override
  protected String languageLinkDestination(long programId, Locale locale) {
    return routes.AdminProgramTranslationsController.edit(programId, locale.toLanguageTag()).url();
  }

  private ImmutableList<DomContent> formFields(
      ProgramDefinition program, Locale locale, ProgramTranslationForm translationForm) {
    LocalizationUpdate updateData = translationForm.getUpdateData();
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
                                    .setFieldName(ProgramTranslationForm.DISPLAY_NAME_FORM_NAME)
                                    .setLabelText("Program name")
                                    .setValue(updateData.localizedDisplayName())
                                    .getInputTag(),
                                defaultLocaleTextHint(program.localizedName())),
                        div()
                            .with(
                                FieldWithLabel.input()
                                    .setFieldName(
                                        ProgramTranslationForm.DISPLAY_DESCRIPTION_FORM_NAME)
                                    .setLabelText("Program description")
                                    .setValue(updateData.localizedDisplayDescription())
                                    .getInputTag(),
                                defaultLocaleTextHint(program.localizedDescription())))));
    if (statusTrackingEnabled) {
      String programStatusesLink =
          controllers.admin.routes.AdminProgramStatusesController.index(program.id()).url();

      ImmutableMap<String, LocalizationUpdate.StatusUpdate> updateLookup =
          buildStatusUpdates(updateData, locale, program);
      for (int statusIdx = 0;
          statusIdx < program.statusDefinitions().getStatuses().size();
          statusIdx++) {
        StatusDefinitions.Status configuredStatus =
            program.statusDefinitions().getStatuses().get(statusIdx);
        LocalizationUpdate.StatusUpdate statusUpdateData =
            updateLookup.get(configuredStatus.statusText());
        ImmutableList.Builder<DomContent> fieldsBuilder =
            ImmutableList.<DomContent>builder()
                .add(
                    input()
                        .isHidden()
                        .withName(ProgramTranslationForm.configuredStatusFieldName(statusIdx))
                        .withValue(configuredStatus.statusText()),
                    div()
                        .with(
                            FieldWithLabel.input()
                                .setFieldName(ProgramTranslationForm.statusTextFieldName(statusIdx))
                                .setLabelText("Status name")
                                .setScreenReaderText("Status name")
                                .setValue(statusUpdateData.localizedStatusText())
                                .getInputTag(),
                            defaultLocaleTextHint(configuredStatus.localizedStatusText())));
        if (configuredStatus.localizedEmailBodyText().isPresent()) {
          fieldsBuilder.add(
              div()
                  .with(
                      FieldWithLabel.textArea()
                          .setFieldName(ProgramTranslationForm.statusEmailFieldName(statusIdx))
                          .setLabelText("Email content")
                          .setScreenReaderText("Email content")
                          .setValue(statusUpdateData.localizedEmailBody())
                          .setRows(OptionalLong.of(8))
                          .getTextareaTag(),
                      defaultLocaleTextHint(configuredStatus.localizedEmailBodyText().get())));
        }
        result.add(
            fieldSetForFields(
                legend()
                    .with(
                        span(
                            String.format("Application status: %s", configuredStatus.statusText())),
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

  private static ImmutableMap<String, LocalizationUpdate.StatusUpdate> buildStatusUpdates(
      LocalizationUpdate updateData, Locale locale, ProgramDefinition program) {
    // The form can be rendered in response to an error where the statuses in the form data are
    // out of sync with the statuses configured in the databawse (e.g. status removed in a
    // separate tab).
    ImmutableMap<String, LocalizationUpdate.StatusUpdate> statusTextToUpdatedContent =
        updateData.statuses().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    LocalizationUpdate.StatusUpdate::configuredStatusText, Function.identity()));

    ImmutableMap.Builder<String, LocalizationUpdate.StatusUpdate> resultBuilder =
        ImmutableMap.builder();
    program.statusDefinitions().getStatuses().stream()
        .forEach(
            s -> {
              if (statusTextToUpdatedContent.containsKey(s.statusText())) {
                resultBuilder.put(s.statusText(), statusTextToUpdatedContent.get(s.statusText()));
              } else {
                LocalizationUpdate.StatusUpdate.Builder updateBuilder =
                    LocalizationUpdate.StatusUpdate.builder()
                        .setConfiguredStatusText(s.statusText())
                        .setLocalizedStatusText(s.localizedStatusText().maybeGet(locale));
                if (s.localizedEmailBodyText().isPresent()) {
                  updateBuilder.setLocalizedEmailBody(
                      Optional.of(s.localizedEmailBodyText().get().maybeGet(locale).orElse("")));
                }
                resultBuilder.put(s.statusText(), updateBuilder.build());
              }
            });
    return resultBuilder.build();
  }
}
