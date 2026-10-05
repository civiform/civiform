package mapping.admin.programs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import forms.translation.ProgramTranslationForm;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import models.ApplicationStep;
import services.LocalizedStrings;
import services.RandomStringUtils;
import services.program.BlockDefinition;
import services.program.LocalizationUpdate;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.statuses.StatusDefinitions;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramTranslationPageViewModel;
import views.admin.programs.ProgramTranslationPageViewModel.ApplicationStepSection;
import views.admin.programs.ProgramTranslationPageViewModel.LanguageLink;
import views.admin.programs.ProgramTranslationPageViewModel.ScreenSection;
import views.admin.programs.ProgramTranslationPageViewModel.StatusSection;
import views.admin.programs.ProgramTranslationPageViewModel.TranslationField;
import views.components.TextFormatter;

/** Maps data to the ProgramTranslationPageViewModel for the program translations page. */
public final class ProgramTranslationPageMapper {

  /**
   * Maps a program and the locale being translated to the view model.
   *
   * @param program the program being translated
   * @param activeStatusDefinitions the program's configured application statuses
   * @param updateData the translation values to pre-populate the form with, either the program's
   *     saved translations or the values the admin just submitted
   * @param localeToEdit the locale whose translations are being edited
   * @param translatableLocales every locale the deployment supports translations for, used to build
   *     the language picker
   * @param errorMessage a save failure or flash error, rendered as an error toast
   */
  public ProgramTranslationPageViewModel map(
      ProgramDefinition program,
      StatusDefinitions activeStatusDefinitions,
      LocalizationUpdate updateData,
      Locale localeToEdit,
      ImmutableList<Locale> translatableLocales,
      Optional<String> errorMessage) {
    Optional<String> errorToastMessage = errorMessage.map(message -> "Error: " + message);
    String editProgramDetailsUrl =
        controllers.admin.routes.AdminProgramController.edit(
                program.id(), ProgramEditStatus.EDIT.name())
            .url();

    ProgramTranslationPageViewModel.ProgramTranslationPageViewModelBuilder builder =
        ProgramTranslationPageViewModel.builder()
            .title(
                String.format(
                    "Manage program translations: %s", program.localizedName().getDefault()))
            .formActionUrl(
                controllers.admin.routes.AdminProgramTranslationsController.update(
                        program.adminName(), localeToEdit.toLanguageTag())
                    .url())
            .displayLanguage(getDisplayLanguage(localeToEdit))
            .languageLinks(
                buildLanguageLinks(program.adminName(), localeToEdit, translatableLocales))
            .editProgramDetailsUrl(editProgramDetailsUrl)
            .editStatusesUrl(
                controllers.admin.routes.AdminProgramStatusesController.index(program.id()).url())
            .applicationSteps(buildApplicationSteps(program, updateData.applicationSteps()))
            .statuses(buildStatuses(activeStatusDefinitions, updateData.statuses()))
            .screens(buildScreens(program, updateData.screens()))
            .errorMessage(errorToastMessage)
            .errorToastId(errorToastMessage.isPresent() ? UUID.randomUUID().toString() : null);

    addProgramDetailFields(builder, program, updateData);

    return builder.build();
  }

  private void addProgramDetailFields(
      ProgramTranslationPageViewModel.ProgramTranslationPageViewModelBuilder builder,
      ProgramDefinition program,
      LocalizationUpdate updateData) {
    ProgramType programType = program.programType();

    builder.displayNameField(
        buildField(
            ProgramTranslationForm.DISPLAY_NAME_FORM_NAME,
            updateData.localizedDisplayName(),
            /* required= */ true,
            program.localizedName()));

    // Only DEFAULT programs have a long description.
    boolean showDisplayDescription = programType.equals(ProgramType.DEFAULT);
    builder.showDisplayDescription(showDisplayDescription);
    if (showDisplayDescription) {
      builder.displayDescriptionField(
          buildField(
              ProgramTranslationForm.DISPLAY_DESCRIPTION_FORM_NAME,
              updateData.localizedDisplayDescription(),
              /* required= */ false,
              program.localizedDescription()));
    }

    // External programs don't have a confirmation message.
    boolean showConfirmationMessage = !programType.equals(ProgramType.EXTERNAL);
    builder.showConfirmationMessage(showConfirmationMessage);
    if (showConfirmationMessage) {
      builder.confirmationMessageField(
          buildField(
              ProgramTranslationForm.CUSTOM_CONFIRMATION_MESSAGE_FORM_NAME,
              updateData.localizedConfirmationMessage(),
              /* required= */ false,
              program.localizedConfirmationMessage()));
    }

    // Only add the summary image description input if a summary image description is set.
    boolean showImageDescription = program.localizedSummaryImageDescription().isPresent();
    builder.showImageDescription(showImageDescription);
    if (showImageDescription) {
      builder.imageDescriptionField(
          buildField(
              ProgramTranslationForm.IMAGE_DESCRIPTION_FORM_NAME,
              updateData.localizedSummaryImageDescription().orElse(""),
              /* required= */ false,
              program.localizedSummaryImageDescription().get()));
    }

    builder.shortDescriptionField(
        buildField(
            ProgramTranslationForm.SHORT_DESCRIPTION_FORM_NAME,
            updateData.localizedShortDescription(),
            /* required= */ true,
            program.localizedShortDescription()));
  }

  private ImmutableList<LanguageLink> buildLanguageLinks(
      String programName, Locale currentlySelected, ImmutableList<Locale> translatableLocales) {
    return translatableLocales.stream()
        .map(
            locale ->
                LanguageLink.builder()
                    .href(
                        controllers.admin.routes.AdminProgramTranslationsController.edit(
                                programName, locale.toLanguageTag())
                            .url())
                    .displayLanguage(getDisplayLanguage(locale))
                    .selected(locale.equals(currentlySelected))
                    .build())
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * One section per application step. Steps without a default-locale title are skipped, as the
   * legacy view did; the step number still counts them so the numbering matches the program.
   */
  private ImmutableList<ApplicationStepSection> buildApplicationSteps(
      ProgramDefinition program,
      ImmutableList<LocalizationUpdate.ApplicationStepUpdate> updatedApplicationSteps) {
    ImmutableList<ApplicationStep> applicationSteps = program.applicationSteps();
    ImmutableList.Builder<ApplicationStepSection> sections = ImmutableList.builder();

    for (int i = 0; i < applicationSteps.size(); i++) {
      ApplicationStep step = applicationSteps.get(i);
      LocalizationUpdate.ApplicationStepUpdate updatedStep = updatedApplicationSteps.get(i);
      if (step.getTitle().getDefault().isEmpty()) {
        continue;
      }

      sections.add(
          ApplicationStepSection.builder()
              .stepNumber(i + 1)
              .titleField(
                  buildField(
                      ProgramTranslationForm.localizedApplicationStepTitle(i),
                      updatedStep.localizedTitle(),
                      /* required= */ true,
                      step.getTitle()))
              .descriptionField(
                  buildField(
                      ProgramTranslationForm.localizedApplicationStepDescription(i),
                      updatedStep.localizedDescription(),
                      /* required= */ true,
                      step.getDescription()))
              .build());
    }

    return sections.build();
  }

  /**
   * One section per configured status. The fields of a status share a common index in their names,
   * which is how {@link ProgramTranslationForm} groups them back together on submit.
   */
  private ImmutableList<StatusSection> buildStatuses(
      StatusDefinitions currentStatusDefinitions,
      ImmutableList<LocalizationUpdate.StatusUpdate> statusUpdates) {
    ImmutableList<StatusDefinitions.Status> configuredStatuses =
        currentStatusDefinitions.getStatuses();
    Preconditions.checkState(statusUpdates.size() == configuredStatuses.size());
    ImmutableList.Builder<StatusSection> sections = ImmutableList.builder();

    for (int statusIdx = 0; statusIdx < statusUpdates.size(); statusIdx++) {
      StatusDefinitions.Status configuredStatus = configuredStatuses.get(statusIdx);
      LocalizationUpdate.StatusUpdate statusUpdateData = statusUpdates.get(statusIdx);
      Optional<LocalizedStrings> emailBody = configuredStatus.localizedEmailBodyText();

      StatusSection.StatusSectionBuilder section =
          StatusSection.builder()
              .statusKeyFieldName(ProgramTranslationForm.statusKeyToUpdateFieldName(statusIdx))
              .statusText(configuredStatus.statusText())
              .statusNameField(
                  buildField(
                      ProgramTranslationForm.localizedStatusFieldName(statusIdx),
                      statusUpdateData.localizedStatusText().orElse(""),
                      /* required= */ false,
                      configuredStatus.localizedStatusText()))
              .showEmailBody(emailBody.isPresent());
      if (emailBody.isPresent()) {
        section.emailBodyField(
            buildField(
                ProgramTranslationForm.localizedEmailFieldName(statusIdx),
                statusUpdateData.localizedEmailBody().orElse(""),
                /* required= */ false,
                emailBody.get()));
      }

      sections.add(section.build());
    }

    return sections.build();
  }

  /** One section per screen. External programs have no screens, so they get none. */
  private ImmutableList<ScreenSection> buildScreens(
      ProgramDefinition program, ImmutableList<LocalizationUpdate.ScreenUpdate> screenUpdates) {
    if (program.programType().equals(ProgramType.EXTERNAL)) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<ScreenSection> sections = ImmutableList.builder();

    for (int i = 0; i < screenUpdates.size(); i++) {
      LocalizationUpdate.ScreenUpdate screenUpdateData = screenUpdates.get(i);
      BlockDefinition block =
          program.blockDefinitions().stream()
              .filter(blockDefinition -> blockDefinition.id() == screenUpdateData.blockIdToUpdate())
              .findFirst()
              .get();
      Optional<LocalizedStrings> eligibilityMessage = block.localizedEligibilityMessage();

      ScreenSection.ScreenSectionBuilder section =
          ScreenSection.builder()
              .screenNumber(i + 1)
              .editUrl(
                  controllers.admin.routes.AdminProgramBlocksController.edit(
                          program.id(), block.id())
                      .url())
              .nameField(
                  buildField(
                      ProgramTranslationForm.localizedScreenName(block.id()),
                      screenUpdateData.localizedName(),
                      /* required= */ true,
                      block.localizedName()))
              .descriptionField(
                  buildField(
                      ProgramTranslationForm.localizedScreenDescription(block.id()),
                      screenUpdateData.localizedDescription(),
                      /* required= */ false,
                      block.localizedDescription()))
              .showEligibilityMessage(eligibilityMessage.isPresent());
      if (eligibilityMessage.isPresent()) {
        section.eligibilityMessageField(
            buildField(
                ProgramTranslationForm.localizedEligibilityMessage(block.id()),
                screenUpdateData.localizedEligibilityMessage().orElse(""),
                /* required= */ false,
                eligibilityMessage.get()));
      }

      sections.add(section.build());
    }

    return sections.build();
  }

  private TranslationField buildField(
      String name, String value, boolean required, LocalizedStrings defaultText) {
    return TranslationField.builder()
        .id(RandomStringUtils.randomAlphabetic(8))
        .name(name)
        .value(value)
        .required(required)
        .defaultTextHtml(
            TextFormatter.formatTextToSanitizedHTML(
                defaultText.getDefault(),
                /* preserveEmptyLines= */ false,
                /* addRequiredIndicator= */ false,
                /* ariaLabelForNewTabs= */ "opens in a new tab"))
        .build();
  }

  /**
   * Returns the English display text for a locale. Mirrors {@code
   * TranslationFormView#getDisplayLanguage}, which the legacy j2html view uses.
   */
  private static String getDisplayLanguage(Locale locale) {
    return locale.equals(Locale.TRADITIONAL_CHINESE)
        ? "Traditional Chinese"
        : locale.getDisplayLanguage(LocalizedStrings.DEFAULT_LOCALE);
  }
}
