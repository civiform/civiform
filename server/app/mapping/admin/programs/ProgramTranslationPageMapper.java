package mapping.admin.programs;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import forms.translation.ProgramTranslationForm;
import java.util.Locale;
import java.util.Optional;
import models.ApplicationStep;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.statuses.StatusDefinitions;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramTranslationPageViewModel;

/** Maps data to the ProgramTranslationPageViewModel for the program translation page. */
public final class ProgramTranslationPageMapper {

  public ProgramTranslationPageViewModel map(
      ProgramDefinition program,
      Locale locale,
      StatusDefinitions activeStatusDefinitions,
      ImmutableList<Locale> translatableLocales,
      Optional<String> errorMessage) {
    ImmutableList<ProgramTranslationPageViewModel.LocaleLink> localeLinks =
        translatableLocales.stream()
            .map(
                l ->
                    ProgramTranslationPageViewModel.LocaleLink.builder()
                        .programAdminName(program.adminName())
                        .localeTag(l.toLanguageTag())
                        .displayName(getDisplayLanguage(l))
                        .selected(l.equals(locale))
                        .build())
            .collect(ImmutableList.toImmutableList());

    ImmutableList.Builder<ProgramTranslationPageViewModel.TranslationSection> sections =
        ImmutableList.builder();

    // Program details section
    sections.add(buildProgramDetailsSection(program, locale));

    // Application steps sections
    ImmutableList<ApplicationStep> applicationSteps = program.applicationSteps();
    String programDetailsLink =
        routes.AdminProgramController.edit(program.id(), ProgramEditStatus.EDIT.name()).url();
    for (int i = 0; i < applicationSteps.size(); i++) {
      ApplicationStep step = applicationSteps.get(i);
      if (!step.getTitle().getDefault().isEmpty()) {
        sections.add(buildApplicationStepSection(step, locale, i, programDetailsLink));
      }
    }

    // Status sections
    String programStatusesLink = routes.AdminProgramStatusesController.index(program.id()).url();
    ImmutableList<StatusDefinitions.Status> statuses = activeStatusDefinitions.getStatuses();
    for (int i = 0; i < statuses.size(); i++) {
      sections.add(buildStatusSection(statuses.get(i), locale, i, programStatusesLink));
    }

    // Screen sections (not for external programs)
    if (!program.programType().equals(ProgramType.EXTERNAL)) {
      ImmutableList<BlockDefinition> blocks = program.blockDefinitions();
      for (int i = 0; i < blocks.size(); i++) {
        sections.add(buildScreenSection(blocks.get(i), locale, i, program.id()));
      }
    }

    return ProgramTranslationPageViewModel.builder()
        .programName(program.localizedName().getDefault())
        .programAdminName(program.adminName())
        .localeTag(locale.toLanguageTag())
        .currentLocaleDisplayName(getDisplayLanguage(locale))
        .localeLinks(localeLinks)
        .sections(sections.build())
        .errorMessage(errorMessage)
        .build();
  }

  private ProgramTranslationPageViewModel.TranslationSection buildProgramDetailsSection(
      ProgramDefinition program, Locale locale) {
    String editUrl =
        routes.AdminProgramController.edit(program.id(), ProgramEditStatus.EDIT.name()).url();
    ImmutableList.Builder<ProgramTranslationPageViewModel.TranslationField> fields =
        ImmutableList.builder();

    fields.add(
        translationField(
            ProgramTranslationForm.DISPLAY_NAME_FORM_NAME,
            "Program name",
            program.localizedName().maybeGet(locale).orElse(""),
            program.localizedName().getDefault(),
            false,
            true));

    if (program.programType().equals(ProgramType.DEFAULT)) {
      fields.add(
          translationField(
              ProgramTranslationForm.DISPLAY_DESCRIPTION_FORM_NAME,
              "Program description",
              program.localizedDescription().maybeGet(locale).orElse(""),
              program.localizedDescription().getDefault(),
              false,
              false));
    }

    if (!program.programType().equals(ProgramType.EXTERNAL)) {
      fields.add(
          translationField(
              ProgramTranslationForm.CUSTOM_CONFIRMATION_MESSAGE_FORM_NAME,
              "Custom confirmation screen message",
              program.localizedConfirmationMessage().maybeGet(locale).orElse(""),
              program.localizedConfirmationMessage().getDefault(),
              false,
              false));
    }

    if (program.localizedSummaryImageDescription().isPresent()
        && isNotBlank(program.localizedSummaryImageDescription().get().getDefault())) {

      fields.add(
          translationField(
              ProgramTranslationForm.IMAGE_DESCRIPTION_FORM_NAME,
              "Program image description",
              program.localizedSummaryImageDescription().get().maybeGet(locale).orElse(""),
              program.localizedSummaryImageDescription().get().getDefault(),
              false,
              false));
    }

    fields.add(
        translationField(
            ProgramTranslationForm.SHORT_DESCRIPTION_FORM_NAME,
            "Short program description",
            program.localizedShortDescription().maybeGet(locale).orElse(""),
            program.localizedShortDescription().getDefault(),
            false,
            true));

    return ProgramTranslationPageViewModel.TranslationSection.builder()
        .legend("Applicant-visible program details")
        .editDefaultUrl(editUrl)
        .fields(fields.build())
        .hiddenFields(ImmutableList.of())
        .build();
  }

  private ProgramTranslationPageViewModel.TranslationSection buildApplicationStepSection(
      ApplicationStep step, Locale locale, int index, String editUrl) {
    return ProgramTranslationPageViewModel.TranslationSection.builder()
        .legend(String.format("Application step %d", index + 1))
        .editDefaultUrl(editUrl)
        .fields(
            ImmutableList.of(
                translationField(
                    ProgramTranslationForm.localizedApplicationStepTitle(index),
                    "Title",
                    step.getTitle().maybeGet(locale).orElse(""),
                    step.getTitle().getDefault(),
                    false,
                    true),
                translationField(
                    ProgramTranslationForm.localizedApplicationStepDescription(index),
                    "Description",
                    step.getDescription().maybeGet(locale).orElse(""),
                    step.getDescription().getDefault(),
                    false,
                    true)))
        .hiddenFields(ImmutableList.of())
        .build();
  }

  private ProgramTranslationPageViewModel.TranslationSection buildStatusSection(
      StatusDefinitions.Status status, Locale locale, int index, String editUrl) {
    ImmutableList.Builder<ProgramTranslationPageViewModel.TranslationField> fields =
        ImmutableList.builder();
    fields.add(
        translationField(
            ProgramTranslationForm.localizedStatusFieldName(index),
            "Status name",
            status.localizedStatusText().maybeGet(locale).orElse(""),
            status.localizedStatusText().getDefault(),
            false,
            false));

    if (status.localizedEmailBodyText().isPresent()) {
      fields.add(
          translationField(
              ProgramTranslationForm.localizedEmailFieldName(index),
              "Email content",
              status.localizedEmailBodyText().get().maybeGet(locale).orElse(""),
              status.localizedEmailBodyText().get().getDefault(),
              true,
              false));
    }

    ImmutableList<ProgramTranslationPageViewModel.HiddenField> hiddenFields =
        ImmutableList.of(
            ProgramTranslationPageViewModel.HiddenField.builder()
                .name(ProgramTranslationForm.statusKeyToUpdateFieldName(index))
                .value(status.statusText())
                .build());

    return ProgramTranslationPageViewModel.TranslationSection.builder()
        .legend(String.format("Application status: %s", status.statusText()))
        .editDefaultUrl(editUrl)
        .fields(fields.build())
        .hiddenFields(hiddenFields)
        .build();
  }

  private ProgramTranslationPageViewModel.TranslationSection buildScreenSection(
      BlockDefinition block, Locale locale, int index, long programId) {
    String editUrl = routes.AdminProgramBlocksController.edit(programId, block.id()).url();
    ImmutableList.Builder<ProgramTranslationPageViewModel.TranslationField> fields =
        ImmutableList.builder();

    fields.add(
        translationField(
            ProgramTranslationForm.localizedScreenName(block.id()),
            "Screen name",
            block.localizedName().maybeGet(locale).orElse(""),
            block.localizedName().getDefault(),
            false,
            true));
    fields.add(
        translationField(
            ProgramTranslationForm.localizedScreenDescription(block.id()),
            "Screen description",
            block.localizedDescription().maybeGet(locale).orElse(""),
            block.localizedDescription().getDefault(),
            false,
            false));

    if (block.localizedEligibilityMessage().isPresent()) {
      fields.add(
          translationField(
              ProgramTranslationForm.localizedEligibilityMessage(block.id()),
              "Screen eligibility message",
              block.localizedEligibilityMessage().get().maybeGet(locale).orElse(""),
              block.localizedEligibilityMessage().get().getDefault(),
              false,
              false));
    }

    return ProgramTranslationPageViewModel.TranslationSection.builder()
        .legend(String.format("Screen %d", index + 1))
        .editDefaultUrl(editUrl)
        .fields(fields.build())
        .hiddenFields(ImmutableList.of())
        .build();
  }

  private static ProgramTranslationPageViewModel.TranslationField translationField(
      String fieldName,
      String label,
      String value,
      String defaultText,
      boolean isTextArea,
      boolean required) {
    return ProgramTranslationPageViewModel.TranslationField.builder()
        .fieldName(fieldName)
        .label(label)
        .value(value)
        .defaultText(defaultText)
        .isTextArea(isTextArea)
        .required(required)
        .build();
  }

  private static String getDisplayLanguage(Locale locale) {
    return locale.equals(Locale.TRADITIONAL_CHINESE)
        ? "Traditional Chinese"
        : locale.getDisplayLanguage(LocalizedStrings.DEFAULT_LOCALE);
  }
}
