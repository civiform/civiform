package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.p;
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
import models.ApplicationStep;
import play.mvc.Http;
import play.twirl.api.Content;
import services.TranslationLocales;
import services.program.BlockDefinition;
import services.program.LocalizationUpdate;
import services.program.ProgramDefinition;
import services.settings.SettingsManifest;
import services.statuses.StatusDefinitions;
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
  private final SettingsManifest settingsManifest;

  @Inject
  public ProgramTranslationView(
      AdminLayoutFactory layoutFactory,
      TranslationLocales translationLocales,
      SettingsManifest settingsManifest) {
    super(translationLocales);
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.settingsManifest = settingsManifest;
  }

  public Content render(
      Http.Request request,
      Locale locale,
      ProgramDefinition program,
      StatusDefinitions activeStatusDefinitions,
      ProgramTranslationForm translationForm,
      Optional<ToastMessage> message) {
    String formAction =
        controllers.admin.routes.AdminProgramTranslationsController.update(
                program.adminName(), locale.toLanguageTag())
            .url();
    FormTag form =
        renderTranslationForm(
            request,
            locale,
            formAction,
            formFields(request, program, translationForm, activeStatusDefinitions));

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
      Http.Request request,
      ProgramDefinition program,
      ProgramTranslationForm translationForm,
      StatusDefinitions currentStatusDefinitions) {
    ImmutableList<BlockDefinition> blockDefinitions = program.blockDefinitions();
    ImmutableList<Long> blockIds =
        blockDefinitions.stream().map(block -> block.id()).collect(ImmutableList.toImmutableList());
    LocalizationUpdate updateData = translationForm.getUpdateData(blockIds);
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
                    getApplicantVisibleProgramDetailFields(program, updateData, request)));

    // Add Status Tracking messages.
    String programStatusesLink =
        controllers.admin.routes.AdminProgramStatusesController.index(program.id()).url();
    // Boolean indicates if eligibility message feature flag is on.
    Boolean isEligibilityMsgEnabled =
        settingsManifest.getCustomizedEligibilityMessageEnabled(request);

    Preconditions.checkState(
        updateData.statuses().size() == currentStatusDefinitions.getStatuses().size());
    for (int statusIdx = 0; statusIdx < updateData.statuses().size(); statusIdx++) {
      StatusDefinitions.Status configuredStatus =
          currentStatusDefinitions.getStatuses().get(statusIdx);
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

    // Add slim alert with warning that translations aren't visible yet.
    result.add(
        div()
            .withClasses("usa-alert", "usa-alert--info", "usa-alert--slim")
            .with(
                div()
                    .withClass("usa-alert__body")
                    .with(
                        p().withClass("usa-alert__text")
                            .withText(
                                "Translations entered below will be visible at a future launch"
                                    + " date."))));

    ImmutableList.Builder<DomContent> newProgramFieldsBuilder =
        ImmutableList.<DomContent>builder()
            .add(
                fieldWithDefaultLocaleTextHint(
                    FieldWithLabel.input()
                        .setFieldName(ProgramTranslationForm.SHORT_DESCRIPTION_FORM_NAME)
                        .setLabelText("Short program description")
                        .setValue(updateData.localizedShortDescription())
                        .setRequired(true)
                        .getInputTag(),
                    program.localizedShortDescription()));

    newProgramFieldsBuilder =
        addApplicationSteps(newProgramFieldsBuilder, program, updateData.applicationSteps());

    result.add(
        fieldSetForFields(
            legend()
                .with(
                    span("New program details fields"),
                    new LinkElement()
                        .setText("(edit default)")
                        .setHref(programDetailsLink)
                        .setStyles("ml-2")
                        .asAnchorText()),
            newProgramFieldsBuilder.build()));

    // Add fields for Screen names and descriptions
    ImmutableList<LocalizationUpdate.ScreenUpdate> screens = updateData.screens();
    for (int i = 0; i < screens.size(); i++) {
      LocalizationUpdate.ScreenUpdate screenUpdateData = screens.get(i);
      BlockDefinition block =
          blockDefinitions.stream()
              .filter(blockDefinition -> blockDefinition.id() == screenUpdateData.blockIdToUpdate())
              .findFirst()
              .get();
      ImmutableList.Builder<DomContent> fieldsBuilder =
          ImmutableList.<DomContent>builder()
              .add(
                  fieldWithDefaultLocaleTextHint(
                      FieldWithLabel.input()
                          .setFieldName(ProgramTranslationForm.localizedScreenName(block.id()))
                          .setLabelText("Screen name")
                          .setScreenReaderText("Screen name")
                          .setValue(screenUpdateData.localizedName())
                          .setRequired(true)
                          .getInputTag(),
                      block.localizedName()))
              .add(
                  fieldWithDefaultLocaleTextHint(
                      FieldWithLabel.input()
                          .setFieldName(
                              ProgramTranslationForm.localizedScreenDescription(block.id()))
                          .setLabelText("Screen description")
                          .setScreenReaderText("Screen description")
                          .setValue(screenUpdateData.localizedDescription())
                          .getInputTag(),
                      block.localizedDescription()));
      if (isEligibilityMsgEnabled && block.localizedEligibilityMessage().isPresent()) {
        fieldsBuilder.add(
            fieldWithDefaultLocaleTextHint(
                FieldWithLabel.input()
                    .setFieldName(ProgramTranslationForm.localizedEligibilityMessage(block.id()))
                    .setLabelText("Screen eligibility message")
                    .setScreenReaderText("Screen eligibility message")
                    .setValue(screenUpdateData.localizedEligibilityMessage())
                    .getInputTag(),
                block.localizedEligibilityMessage().get()));
      }
      String blockDetailsLink =
          controllers.admin.routes.AdminProgramBlocksController.edit(program.id(), block.id())
              .url();
      result.add(
          fieldSetForFields(
              legend()
                  .with(
                      span(String.format("Screen %d", i + 1)),
                      new LinkElement()
                          .setText("(edit default)")
                          .setHref(blockDetailsLink)
                          .setStyles("ml-2")
                          .asAnchorText()),
              fieldsBuilder.build()));
    }
    return result.build();
  }

  private ImmutableList<DomContent> getApplicantVisibleProgramDetailFields(
      ProgramDefinition program, LocalizationUpdate updateData, Http.Request request) {
    ImmutableList.Builder<DomContent> applicantVisibleDetails =
        ImmutableList.<DomContent>builder()
            .add(
                fieldWithDefaultLocaleTextHint(
                    FieldWithLabel.input()
                        .setFieldName(ProgramTranslationForm.DISPLAY_NAME_FORM_NAME)
                        .setLabelText("Program name")
                        .setValue(updateData.localizedDisplayName())
                        .setRequired(true)
                        .getInputTag(),
                    program.localizedName()));

    // If North Star is enabled and the program is a common intake program, do not show the optional
    // long description field
    boolean northStarIsOff = !settingsManifest.getNorthStarApplicantUi(request);
    boolean isNotCommonIntake = !program.isCommonIntakeForm();
    if (northStarIsOff || isNotCommonIntake) {
      applicantVisibleDetails.add(
          fieldWithDefaultLocaleTextHint(
              FieldWithLabel.input()
                  .setFieldName(ProgramTranslationForm.DISPLAY_DESCRIPTION_FORM_NAME)
                  .setLabelText("Program description")
                  .setValue(updateData.localizedDisplayDescription())
                  .getInputTag(),
              program.localizedDescription()));
    }

    applicantVisibleDetails.add(
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
    return applicantVisibleDetails.build();
  }

  private ImmutableList.Builder<DomContent> addApplicationSteps(
      ImmutableList.Builder<DomContent> newProgramFieldsBuilder,
      ProgramDefinition program,
      ImmutableList<LocalizationUpdate.ApplicationStepUpdate> updatedApplicationSteps) {
    // Get the application steps from the program data
    // if we have default text, add a box for translations
    ImmutableList<ApplicationStep> applicationSteps = program.applicationSteps();
    // TODO: Once we've fully transitioned to the new fields, we probably want each application step
    // to be it's own section
    for (int i = 0; i < applicationSteps.size(); i++) {
      ApplicationStep step = applicationSteps.get(i);
      LocalizationUpdate.ApplicationStepUpdate updatedStep = updatedApplicationSteps.get(i);
      if (!step.getTitle().getDefault().isEmpty()) {
        newProgramFieldsBuilder.add(
            fieldWithDefaultLocaleTextHint(
                FieldWithLabel.input()
                    .setFieldName(ProgramTranslationForm.localizedApplicationStepTitle(i))
                    .setLabelText(
                        String.format("Application step %s title", Integer.toString(i + 1)))
                    .setScreenReaderText(
                        String.format("Application step %s title", Integer.toString(i + 1)))
                    .setValue(updatedStep.localizedTitle())
                    .setRequired(true)
                    .getInputTag(),
                step.getTitle()));
        newProgramFieldsBuilder.add(
            fieldWithDefaultLocaleTextHint(
                FieldWithLabel.input()
                    .setFieldName(ProgramTranslationForm.localizedApplicationStepDescription(i))
                    .setLabelText(
                        String.format("Application step %s description", Integer.toString(i + 1)))
                    .setScreenReaderText(
                        String.format("Application step %s description", Integer.toString(i + 1)))
                    .setValue(updatedStep.localizedDescription())
                    .setRequired(true)
                    .getInputTag(),
                step.getDescription()));
      }
    }
    return newProgramFieldsBuilder;
  }
}
