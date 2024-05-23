package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
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
import play.mvc.Http;
import play.twirl.api.Content;
import services.TranslationLocales;
import services.program.BlockDefinition;
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

/** Renders a list of languages to select from, and a form for updating program information. */
public final class ProgramTranslationView extends TranslationFormView {
  private final AdminLayout layout;

  @Inject
  public ProgramTranslationView(
      AdminLayoutFactory layoutFactory, TranslationLocales translationLocales) {
    super(translationLocales);
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
  }

  public Content render(
      Http.Request request,
      Locale locale,
      ProgramDefinition program,
      ProgramTranslationForm translationForm,
      Optional<ToastMessage> message) {
    String formAction =
        controllers.admin.routes.AdminProgramTranslationsController.update(
                program.adminName(), locale.toLanguageTag())
            .url();
    FormTag form =
        renderTranslationForm(request, locale, formAction, formFields(program, translationForm));

    String title =
        String.format("Manage program translations: %s", program.localizedName().getDefault());

    String subtitle = "Enter text in the language(s) you want to translate to";
    HtmlBundle htmlBundle =
        layout
            .getBundle(request)
            .setTitle(title)
            .addMainContent(
                renderHeader(title, "mt-3"),
                renderSubHeader(subtitle),
                renderLanguageLinks(program.adminName(), locale),
                form);

    message.ifPresent(htmlBundle::addToastMessages);

    return layout.renderCentered(htmlBundle);
  }

  @Override
  protected String languageLinkDestination(String programName, Locale locale) {
    return routes.AdminProgramTranslationsController.edit(programName, locale.toLanguageTag())
        .url();
  }

  private ImmutableList<DomContent> formFields(
      ProgramDefinition program, ProgramTranslationForm translationForm) {
    LocalizationUpdate updateData = translationForm.getUpdateData();
    String programDetailsLink =
        controllers.admin.routes.AdminProgramController.edit(
                program.id(), ProgramEditStatus.EDIT.name())
            .url();
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
                                .setStyles("ml-2")
                                .asAnchorText()),
                    getApplicantVisibleProgramDetailFields(program, updateData)));

    // Add Status Tracking messages.
    String programStatusesLink =
        controllers.admin.routes.AdminProgramStatusesController.index(program.id()).url();

    Preconditions.checkState(
        updateData.statuses().size() == program.statusDefinitions().getStatuses().size());
    for (int statusIdx = 0; statusIdx < updateData.statuses().size(); statusIdx++) {
      StatusDefinitions.Status configuredStatus =
          program.statusDefinitions().getStatuses().get(statusIdx);
      LocalizationUpdate.StatusUpdate statusUpdateData = updateData.statuses().get(statusIdx);
      // Note: While displayed as siblings, fields are logically grouped together by sharing a
      // common index in their field names. These are dynamically generated via helper methods
      // within ProgramTranslationForm (e.g. statusKeyToUpdateFieldName).
      ImmutableList.Builder<DomContent> fieldsBuilder =
          ImmutableList.<DomContent>builder()
              .add(
                  // This input serves as the key indicating which status to update translations
                  // for and isn't configurable.
                  input()
                      .isHidden()
                      .withName(ProgramTranslationForm.statusKeyToUpdateFieldName(statusIdx))
                      .withValue(configuredStatus.statusText()),
                  fieldWithDefaultLocaleTextHint(
                      FieldWithLabel.input()
                          .setFieldName(ProgramTranslationForm.localizedStatusFieldName(statusIdx))
                          .setLabelText("Status name")
                          .setScreenReaderText("Status name")
                          .setValue(statusUpdateData.localizedStatusText())
                          .getInputTag(),
                      configuredStatus.localizedStatusText()));
      if (configuredStatus.localizedEmailBodyText().isPresent()) {
        fieldsBuilder.add(
            fieldWithDefaultLocaleTextHint(
                FieldWithLabel.textArea()
                    .setFieldName(ProgramTranslationForm.localizedEmailFieldName(statusIdx))
                    .setLabelText("Email content")
                    .setScreenReaderText("Email content")
                    .setValue(statusUpdateData.localizedEmailBody())
                    .setRows(OptionalLong.of(8))
                    .getTextareaTag(),
                configuredStatus.localizedEmailBodyText().get()));
      }
      result.add(
          fieldSetForFields(
              legend()
                  .with(
                      span(String.format("Application status: %s", configuredStatus.statusText())),
                      new LinkElement()
                          .setText("(edit default)")
                          .setHref(programStatusesLink)
                          .setStyles("ml-2")
                          .asAnchorText()),
              fieldsBuilder.build()));
    }
    return result.build();
  }

  private ImmutableList<DomContent> getApplicantVisibleProgramDetailFields(
      ProgramDefinition program, LocalizationUpdate updateData) {
    ImmutableList.Builder<DomContent> applicantVisibleDetails =
        ImmutableList.<DomContent>builder()
            .add(
                fieldWithDefaultLocaleTextHint(
                    FieldWithLabel.input()
                        .setFieldName(ProgramTranslationForm.DISPLAY_NAME_FORM_NAME)
                        .setLabelText("Program name")
                        .setValue(updateData.localizedDisplayName())
                        .getInputTag(),
                    program.localizedName()),
                fieldWithDefaultLocaleTextHint(
                    FieldWithLabel.input()
                        .setFieldName(ProgramTranslationForm.DISPLAY_DESCRIPTION_FORM_NAME)
                        .setLabelText("Program description")
                        .setValue(updateData.localizedDisplayDescription())
                        .getInputTag(),
                    program.localizedDescription()),
                fieldWithDefaultLocaleTextHint(
                    FieldWithLabel.input()
                        .setFieldName(ProgramTranslationForm.CUSTOM_CONFIRMATION_MESSAGE_FORM_NAME)
                        .setLabelText("Custom confirmation screen message")
                        .setValue(updateData.localizedConfirmationMessage())
                        .getInputTag(),
                    program.localizedConfirmationMessage()));

    // Only add the summary image description input to the page if a summary image description is
    // actually set.
    if (program.localizedSummaryImageDescription().isPresent()) {
      applicantVisibleDetails.add(
          fieldWithDefaultLocaleTextHint(
              FieldWithLabel.input()
                  .setFieldName(ProgramTranslationForm.IMAGE_DESCRIPTION_FORM_NAME)
                  .setLabelText("Program image description")
                  .setValue(updateData.localizedSummaryImageDescription())
                  .getInputTag(),
              program.localizedSummaryImageDescription().get()));
    }

    for (BlockDefinition block : program.blockDefinitions()) {
      // Add for block title

      // Add for block description
    }
    return applicantVisibleDetails.build();
  }
}
