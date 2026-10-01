package mapping.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import models.ApplicationStep;
import models.DisplayMode;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import services.program.LocalizationUpdate;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.statuses.StatusDefinitions;
import views.admin.programs.ProgramTranslationPageViewModel;
import views.admin.programs.ProgramTranslationPageViewModel.ApplicationStepSection;
import views.admin.programs.ProgramTranslationPageViewModel.ScreenSection;
import views.admin.programs.ProgramTranslationPageViewModel.StatusSection;
import views.admin.programs.ProgramTranslationPageViewModel.TranslationField;

public final class ProgramTranslationPageMapperTest {

  private static final Locale SPANISH = Locale.forLanguageTag("es-US");
  private static final Locale KOREAN = Locale.forLanguageTag("ko");
  private static final ImmutableList<Locale> TRANSLATABLE_LOCALES =
      ImmutableList.of(SPANISH, KOREAN);

  private ProgramTranslationPageMapper mapper;

  @Before
  public void setup() {
    mapper = new ProgramTranslationPageMapper();
  }

  private static ProgramDefinition.Builder programBuilder() {
    return ProgramDefinition.builder()
        .setId(7L)
        .setAdminName("my-program")
        .setAdminDescription("admin description")
        .setLocalizedName(LocalizedStrings.of(Locale.US, "My Program"))
        .setLocalizedDescription(LocalizedStrings.of(Locale.US, "A **long** description"))
        .setLocalizedShortDescription(LocalizedStrings.of(Locale.US, "Short description"))
        .setLocalizedConfirmationMessage(LocalizedStrings.of(Locale.US, "Thanks"))
        .setExternalLink("")
        .setDisplayMode(DisplayMode.PUBLIC)
        .setProgramType(ProgramType.DEFAULT)
        .setEligibilityIsGating(true)
        .setLoginOnly(false)
        .setAcls(new ProgramAcls())
        .setCategories(ImmutableList.of())
        .setApplicationSteps(ImmutableList.of())
        .setBridgeDefinitions(ImmutableMap.of());
  }

  private static LocalizationUpdate.Builder updateBuilder() {
    return LocalizationUpdate.builder()
        .setLocalizedDisplayName("Mi Programa")
        .setLocalizedDisplayDescription("Descripcion larga")
        .setLocalizedShortDescription("Descripcion corta")
        .setLocalizedConfirmationMessage("Gracias")
        .setApplicationSteps(ImmutableList.of())
        .setStatuses(ImmutableList.of())
        .setScreens(ImmutableList.of());
  }

  private static BlockDefinition.Builder blockBuilder(long id) {
    return BlockDefinition.builder()
        .setId(id)
        .setName("Screen " + id)
        .setDescription("Screen description " + id)
        .setLocalizedName(LocalizedStrings.of(Locale.US, "Screen " + id))
        .setLocalizedDescription(LocalizedStrings.of(Locale.US, "Screen description " + id));
  }

  private static LocalizationUpdate.ScreenUpdate screenUpdate(long blockId) {
    return LocalizationUpdate.ScreenUpdate.builder()
        .setBlockIdToUpdate(blockId)
        .setLocalizedName("Pantalla " + blockId)
        .setLocalizedDescription("Descripcion de pantalla " + blockId)
        .setLocalizedEligibilityMessage("Elegibilidad " + blockId)
        .build();
  }

  private static StatusDefinitions.Status status(String statusText, Optional<String> emailBody) {
    return StatusDefinitions.Status.builder()
        .setStatusText(statusText)
        .setLocalizedStatusText(LocalizedStrings.of(Locale.US, statusText))
        .setLocalizedEmailBodyText(emailBody.map(body -> LocalizedStrings.of(Locale.US, body)))
        .build();
  }

  private static LocalizationUpdate.StatusUpdate statusUpdate(
      String statusText, Optional<String> localizedStatusText, Optional<String> localizedEmail) {
    return LocalizationUpdate.StatusUpdate.builder()
        .setStatusKeyToUpdate(statusText)
        .setLocalizedStatusText(localizedStatusText)
        .setLocalizedEmailBody(localizedEmail)
        .build();
  }

  private ProgramTranslationPageViewModel map(
      ProgramDefinition program, StatusDefinitions statuses, LocalizationUpdate updateData) {
    return mapper.map(
        program,
        statuses,
        updateData,
        SPANISH,
        TRANSLATABLE_LOCALES,
        /* errorMessage= */ Optional.empty());
  }

  private ProgramTranslationPageViewModel map(ProgramDefinition program) {
    return map(program, new StatusDefinitions(), updateBuilder().build());
  }

  @Test
  public void map_setsTitleFromDefaultLocaleName() {
    ProgramTranslationPageViewModel result = map(programBuilder().build());

    assertThat(result.getTitle()).isEqualTo("Manage program translations: My Program");
  }

  @Test
  public void map_setsFormActionUrl() {
    ProgramTranslationPageViewModel result = map(programBuilder().build());

    assertThat(result.getFormActionUrl())
        .isEqualTo("/admin/programs/my-program/translations/es-US");
  }

  @Test
  public void map_setsDisplayLanguage() {
    ProgramTranslationPageViewModel result = map(programBuilder().build());

    assertThat(result.getDisplayLanguage()).isEqualTo("Spanish");
  }

  @Test
  public void map_setsOneLanguageLinkPerLocaleAndMarksTheCurrentOne() {
    ProgramTranslationPageViewModel result = map(programBuilder().build());

    assertThat(result.getLanguageLinks()).hasSize(2);
    assertThat(result.getLanguageLinks().get(0).getHref())
        .isEqualTo("/admin/programs/my-program/translations/es-US/edit");
    assertThat(result.getLanguageLinks().get(0).getDisplayLanguage()).isEqualTo("Spanish");
    assertThat(result.getLanguageLinks().get(0).isSelected()).isTrue();
    assertThat(result.getLanguageLinks().get(1).getHref())
        .isEqualTo("/admin/programs/my-program/translations/ko/edit");
    assertThat(result.getLanguageLinks().get(1).getDisplayLanguage()).isEqualTo("Korean");
    assertThat(result.getLanguageLinks().get(1).isSelected()).isFalse();
  }

  @Test
  public void map_setsEditDefaultUrls() {
    ProgramTranslationPageViewModel result = map(programBuilder().build());

    assertThat(result.getEditProgramDetailsUrl()).isEqualTo("/admin/programs/7/edit/EDIT");
    assertThat(result.getEditStatusesUrl()).isEqualTo("/admin/programs/7/statuses");
  }

  @Test
  public void map_setsDisplayNameFieldFromUpdateData() {
    ProgramTranslationPageViewModel result = map(programBuilder().build());

    TranslationField field = result.getDisplayNameField();
    assertThat(field.getId()).isNotEmpty();
    assertThat(field.getName()).isEqualTo("displayName");
    assertThat(field.getValue()).isEqualTo("Mi Programa");
    assertThat(field.isRequired()).isTrue();
    assertThat(field.getDefaultTextHtml()).contains("My Program");
  }

  @Test
  public void map_setsDefaultTextAsRenderedMarkdown() {
    ProgramTranslationPageViewModel result = map(programBuilder().build());

    assertThat(result.getDisplayDescriptionField().getDefaultTextHtml())
        .contains("<strong>long</strong>");
  }

  @Test
  public void map_setsShortDescriptionFieldRequired() {
    ProgramTranslationPageViewModel result = map(programBuilder().build());

    TranslationField field = result.getShortDescriptionField();
    assertThat(field.getName()).isEqualTo("shortDescription");
    assertThat(field.getValue()).isEqualTo("Descripcion corta");
    assertThat(field.isRequired()).isTrue();
  }

  @Test
  public void map_defaultProgram_showsDescriptionAndConfirmationMessage() {
    ProgramTranslationPageViewModel result = map(programBuilder().build());

    assertThat(result.isShowDisplayDescription()).isTrue();
    assertThat(result.getDisplayDescriptionField().getName()).isEqualTo("displayDescription");
    assertThat(result.getDisplayDescriptionField().getValue()).isEqualTo("Descripcion larga");
    assertThat(result.getDisplayDescriptionField().isRequired()).isFalse();
    assertThat(result.isShowConfirmationMessage()).isTrue();
    assertThat(result.getConfirmationMessageField().getName()).isEqualTo("confirmationMessage");
    assertThat(result.getConfirmationMessageField().getValue()).isEqualTo("Gracias");
  }

  @Test
  public void map_preScreenerProgram_hidesDescriptionOnly() {
    ProgramTranslationPageViewModel result =
        map(programBuilder().setProgramType(ProgramType.PRE_SCREENER_FORM).build());

    assertThat(result.isShowDisplayDescription()).isFalse();
    assertThat(result.getDisplayDescriptionField()).isNull();
    assertThat(result.isShowConfirmationMessage()).isTrue();
  }

  @Test
  public void map_externalProgram_hidesDescriptionConfirmationMessageAndScreens() {
    ProgramDefinition program =
        programBuilder()
            .setProgramType(ProgramType.EXTERNAL)
            .addBlockDefinition(blockBuilder(1L).build())
            .build();
    LocalizationUpdate updateData =
        updateBuilder().setScreens(ImmutableList.of(screenUpdate(1L))).build();

    ProgramTranslationPageViewModel result = map(program, new StatusDefinitions(), updateData);

    assertThat(result.isShowDisplayDescription()).isFalse();
    assertThat(result.isShowConfirmationMessage()).isFalse();
    assertThat(result.getConfirmationMessageField()).isNull();
    assertThat(result.getScreens()).isEmpty();
  }

  @Test
  public void map_withoutImageDescription_hidesImageDescriptionField() {
    ProgramTranslationPageViewModel result = map(programBuilder().build());

    assertThat(result.isShowImageDescription()).isFalse();
    assertThat(result.getImageDescriptionField()).isNull();
  }

  @Test
  public void map_withImageDescription_setsImageDescriptionField() {
    ProgramDefinition program =
        programBuilder()
            .setLocalizedSummaryImageDescription(
                Optional.of(LocalizedStrings.of(Locale.US, "A picture")))
            .build();
    LocalizationUpdate updateData =
        updateBuilder().setLocalizedSummaryImageDescription("Una foto").build();

    ProgramTranslationPageViewModel result = map(program, new StatusDefinitions(), updateData);

    assertThat(result.isShowImageDescription()).isTrue();
    assertThat(result.getImageDescriptionField().getName()).isEqualTo("imageDescription");
    assertThat(result.getImageDescriptionField().getValue()).isEqualTo("Una foto");
    assertThat(result.getImageDescriptionField().getDefaultTextHtml()).contains("A picture");
  }

  @Test
  public void map_applicationSteps_numbersSectionsAndSkipsStepsWithoutTitle() {
    ProgramDefinition program =
        programBuilder()
            .setApplicationSteps(
                ImmutableList.of(
                    new ApplicationStep("Apply", "Fill out the form"),
                    new ApplicationStep("", "No title"),
                    new ApplicationStep("Wait", "We review it")))
            .build();
    LocalizationUpdate updateData =
        updateBuilder()
            .setApplicationSteps(
                ImmutableList.of(
                    LocalizationUpdate.ApplicationStepUpdate.builder()
                        .setLocalizedTitle("Aplicar")
                        .setLocalizedDescription("Llene el formulario")
                        .build(),
                    LocalizationUpdate.ApplicationStepUpdate.builder()
                        .setLocalizedTitle("")
                        .setLocalizedDescription("Sin titulo")
                        .build(),
                    LocalizationUpdate.ApplicationStepUpdate.builder()
                        .setLocalizedTitle("Esperar")
                        .setLocalizedDescription("Lo revisamos")
                        .build()))
            .build();

    ProgramTranslationPageViewModel result = map(program, new StatusDefinitions(), updateData);

    assertThat(result.getApplicationSteps()).hasSize(2);
    ApplicationStepSection first = result.getApplicationSteps().get(0);
    assertThat(first.getStepNumber()).isEqualTo(1);
    assertThat(first.getTitleField().getName()).isEqualTo("application-step-title-0");
    assertThat(first.getTitleField().getValue()).isEqualTo("Aplicar");
    assertThat(first.getTitleField().isRequired()).isTrue();
    assertThat(first.getTitleField().getDefaultTextHtml()).contains("Apply");
    assertThat(first.getDescriptionField().getName()).isEqualTo("application-step-description-0");
    assertThat(first.getDescriptionField().getValue()).isEqualTo("Llene el formulario");
    assertThat(first.getDescriptionField().isRequired()).isTrue();
    // The skipped step still counts towards the numbering and field indexes.
    ApplicationStepSection second = result.getApplicationSteps().get(1);
    assertThat(second.getStepNumber()).isEqualTo(3);
    assertThat(second.getTitleField().getName()).isEqualTo("application-step-title-2");
    assertThat(second.getTitleField().getValue()).isEqualTo("Esperar");
  }

  @Test
  public void map_statuses_setsKeyFieldAndStatusNameField() {
    StatusDefinitions statuses =
        new StatusDefinitions(
            ImmutableList.of(
                status("Approved", Optional.empty()), status("Rejected", Optional.empty())));
    LocalizationUpdate updateData =
        updateBuilder()
            .setStatuses(
                ImmutableList.of(
                    statusUpdate("Approved", Optional.of("Aprobado"), Optional.empty()),
                    statusUpdate("Rejected", Optional.empty(), Optional.empty())))
            .build();

    ProgramTranslationPageViewModel result = map(programBuilder().build(), statuses, updateData);

    assertThat(result.getStatuses()).hasSize(2);
    StatusSection first = result.getStatuses().get(0);
    assertThat(first.getStatusKeyFieldName()).isEqualTo("status-key-to-update-0");
    assertThat(first.getStatusText()).isEqualTo("Approved");
    assertThat(first.getStatusNameField().getName()).isEqualTo("localized-status-0");
    assertThat(first.getStatusNameField().getValue()).isEqualTo("Aprobado");
    assertThat(first.getStatusNameField().isRequired()).isFalse();
    assertThat(first.getStatusNameField().getDefaultTextHtml()).contains("Approved");
    StatusSection second = result.getStatuses().get(1);
    assertThat(second.getStatusKeyFieldName()).isEqualTo("status-key-to-update-1");
    assertThat(second.getStatusNameField().getName()).isEqualTo("localized-status-1");
    assertThat(second.getStatusNameField().getValue()).isEmpty();
  }

  @Test
  public void map_statusWithoutEmailBody_hidesEmailField() {
    StatusDefinitions statuses =
        new StatusDefinitions(ImmutableList.of(status("Approved", Optional.empty())));
    LocalizationUpdate updateData =
        updateBuilder()
            .setStatuses(
                ImmutableList.of(statusUpdate("Approved", Optional.empty(), Optional.empty())))
            .build();

    ProgramTranslationPageViewModel result = map(programBuilder().build(), statuses, updateData);

    assertThat(result.getStatuses().get(0).isShowEmailBody()).isFalse();
    assertThat(result.getStatuses().get(0).getEmailBodyField()).isNull();
  }

  @Test
  public void map_statusWithEmailBody_setsEmailField() {
    StatusDefinitions statuses =
        new StatusDefinitions(
            ImmutableList.of(status("Approved", Optional.of("You were approved"))));
    LocalizationUpdate updateData =
        updateBuilder()
            .setStatuses(
                ImmutableList.of(
                    statusUpdate("Approved", Optional.of("Aprobado"), Optional.of("Fue aprobado"))))
            .build();

    ProgramTranslationPageViewModel result = map(programBuilder().build(), statuses, updateData);

    StatusSection section = result.getStatuses().get(0);
    assertThat(section.isShowEmailBody()).isTrue();
    assertThat(section.getEmailBodyField().getName()).isEqualTo("localized-email-0");
    assertThat(section.getEmailBodyField().getValue()).isEqualTo("Fue aprobado");
    assertThat(section.getEmailBodyField().isRequired()).isFalse();
    assertThat(section.getEmailBodyField().getDefaultTextHtml()).contains("You were approved");
  }

  @Test
  public void map_statusCountMismatch_throws() {
    StatusDefinitions statuses =
        new StatusDefinitions(ImmutableList.of(status("Approved", Optional.empty())));
    LocalizationUpdate updateData = updateBuilder().setStatuses(ImmutableList.of()).build();

    assertThatThrownBy(() -> map(programBuilder().build(), statuses, updateData))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void map_screens_setsFieldsAndEditUrlPerScreen() {
    ProgramDefinition program =
        programBuilder()
            .addBlockDefinition(blockBuilder(10L).build())
            .addBlockDefinition(blockBuilder(20L).build())
            .build();
    LocalizationUpdate updateData =
        updateBuilder().setScreens(ImmutableList.of(screenUpdate(10L), screenUpdate(20L))).build();

    ProgramTranslationPageViewModel result = map(program, new StatusDefinitions(), updateData);

    assertThat(result.getScreens()).hasSize(2);
    ScreenSection first = result.getScreens().get(0);
    assertThat(first.getScreenNumber()).isEqualTo(1);
    assertThat(first.getEditUrl()).isEqualTo("/admin/programs/7/blocks/10/edit");
    assertThat(first.getNameField().getName()).isEqualTo("screen-name-10");
    assertThat(first.getNameField().getValue()).isEqualTo("Pantalla 10");
    assertThat(first.getNameField().isRequired()).isTrue();
    assertThat(first.getNameField().getDefaultTextHtml()).contains("Screen 10");
    assertThat(first.getDescriptionField().getName()).isEqualTo("screen-description-10");
    assertThat(first.getDescriptionField().getValue()).isEqualTo("Descripcion de pantalla 10");
    assertThat(first.getDescriptionField().isRequired()).isFalse();
    ScreenSection second = result.getScreens().get(1);
    assertThat(second.getScreenNumber()).isEqualTo(2);
    assertThat(second.getEditUrl()).isEqualTo("/admin/programs/7/blocks/20/edit");
    assertThat(second.getNameField().getName()).isEqualTo("screen-name-20");
  }

  @Test
  public void map_screenWithoutEligibilityMessage_hidesEligibilityField() {
    ProgramDefinition program =
        programBuilder().addBlockDefinition(blockBuilder(10L).build()).build();
    LocalizationUpdate updateData =
        updateBuilder().setScreens(ImmutableList.of(screenUpdate(10L))).build();

    ProgramTranslationPageViewModel result = map(program, new StatusDefinitions(), updateData);

    assertThat(result.getScreens().get(0).isShowEligibilityMessage()).isFalse();
    assertThat(result.getScreens().get(0).getEligibilityMessageField()).isNull();
  }

  @Test
  public void map_screenWithEligibilityMessage_setsEligibilityField() {
    ProgramDefinition program =
        programBuilder()
            .addBlockDefinition(
                blockBuilder(10L)
                    .setLocalizedEligibilityMessage(
                        Optional.of(LocalizedStrings.of(Locale.US, "You may be eligible")))
                    .build())
            .build();
    LocalizationUpdate updateData =
        updateBuilder().setScreens(ImmutableList.of(screenUpdate(10L))).build();

    ProgramTranslationPageViewModel result = map(program, new StatusDefinitions(), updateData);

    ScreenSection section = result.getScreens().get(0);
    assertThat(section.isShowEligibilityMessage()).isTrue();
    assertThat(section.getEligibilityMessageField().getName())
        .isEqualTo("custom-eligibility-message-10");
    assertThat(section.getEligibilityMessageField().getValue()).isEqualTo("Elegibilidad 10");
    assertThat(section.getEligibilityMessageField().isRequired()).isFalse();
    assertThat(section.getEligibilityMessageField().getDefaultTextHtml())
        .contains("You may be eligible");
  }

  @Test
  public void map_withErrorMessage_prefixesErrorAndSetsToastId() {
    ProgramTranslationPageViewModel result =
        mapper.map(
            programBuilder().build(),
            new StatusDefinitions(),
            updateBuilder().build(),
            SPANISH,
            TRANSLATABLE_LOCALES,
            Optional.of("something broke"));

    assertThat(result.getErrorMessage()).hasValue("Error: something broke");
    assertThat(result.getErrorToastId()).isNotEmpty();
  }

  @Test
  public void map_withoutErrorMessage_hasNoToastId() {
    ProgramTranslationPageViewModel result = map(programBuilder().build());

    assertThat(result.getErrorMessage()).isEmpty();
    assertThat(result.getErrorToastId()).isNull();
  }
}
