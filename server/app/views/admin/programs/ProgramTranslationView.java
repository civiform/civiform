package views.admin.programs;

import static annotations.FeatureFlags.ApplicationStatusTrackingEnabled;
import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.span;

import com.google.common.base.Preconditions;
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
      ProgramTranslationForm translationForm,
      Optional<String> errors) {
    String formAction =
        controllers.admin.routes.AdminProgramTranslationsController.update(
                program.id(), locale.toLanguageTag())
            .url();
    FormTag form =
        renderTranslationForm(request, locale, formAction, formFields(program, translationForm));

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
      ProgramDefinition program, ProgramTranslationForm translationForm) {
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
    if (statusTrackingEnabled.get()) {
      String programStatusesLink =
          controllers.admin.routes.AdminProgramStatusesController.index(program.id()).url();

      Preconditions.checkState(
          updateData.statuses().size() == program.statusDefinitions().getStatuses().size());
      for (int statusIdx = 0; statusIdx < updateData.statuses().size(); statusIdx++) {
        StatusDefinitions.Status configuredStatus =
            program.statusDefinitions().getStatuses().get(statusIdx);
        LocalizationUpdate.StatusUpdate statusUpdateData = updateData.statuses().get(statusIdx);
        ImmutableList.Builder<DomContent> fieldsBuilder =
            ImmutableList.<DomContent>builder()
                .add(
                    // This input serves as the key indicating which status to update translations
                    // for and isn't configurable.
                    input()
                        .isHidden()
                        .withName(ProgramTranslationForm.statusKeyToUpdateFieldName(statusIdx))
                        .withValue(configuredStatus.statusText()),
                    div()
                        .with(
                            FieldWithLabel.input()
                                .setFieldName(
                                    ProgramTranslationForm.localizedStatusFieldName(statusIdx))
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
                          .setFieldName(ProgramTranslationForm.localizedEmailFieldName(statusIdx))
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
}
