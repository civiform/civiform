package mapping.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import forms.ProgramForm;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import models.ApplicationStep;
import models.CategoryModel;
import models.DisplayMode;
import models.ProgramNotificationPreference;
import models.TrustedIntermediaryGroupModel;
import org.junit.Before;
import org.junit.Test;
import repository.AccountRepository;
import repository.CategoryRepository;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramFormPageViewModel;

public final class ProgramFormPageMapperTest {

  private static final String BASE_URL = "https://civiform.example.com";
  private static final String DEFAULT_CONFIRMATION_MESSAGE =
      "Your application was successfully saved.";

  private ProgramFormPageMapper mapper;
  private CategoryRepository categoryRepository;
  private AccountRepository accountRepository;

  @Before
  public void setup() {
    mapper = new ProgramFormPageMapper();

    categoryRepository = mock(CategoryRepository.class);
    when(categoryRepository.listCategories()).thenReturn(ImmutableList.of());

    accountRepository = mock(AccountRepository.class);
    when(accountRepository.listTrustedIntermediaryGroups()).thenReturn(ImmutableList.of());
  }

  private ProgramFormPageViewModel mapNew(ProgramForm form) {
    return mapper.mapNew(
        form,
        categoryRepository,
        accountRepository,
        DEFAULT_CONFIRMATION_MESSAGE,
        /* preScreenerFormDisplayName= */ Optional.empty(),
        /* errorMessage= */ Optional.empty());
  }

  private ProgramFormPageViewModel mapEdit(
      ProgramDefinition program, ProgramEditStatus status, Optional<ProgramForm> form) {
    return mapper.mapEdit(
        program,
        status,
        form,
        BASE_URL,
        categoryRepository,
        accountRepository,
        DEFAULT_CONFIRMATION_MESSAGE,
        /* preScreenerFormDisplayName= */ Optional.empty(),
        /* errorMessage= */ Optional.empty());
  }

  /** A program definition builder with the fields the mapper reads populated. */
  private ProgramDefinition.Builder programBuilder() {
    return ProgramDefinition.builder()
        .setId(11L)
        .setAdminName("food-benefits")
        .setAdminDescription("admin note")
        .setLocalizedName(LocalizedStrings.withDefaultValue("Food benefits"))
        .setLocalizedDescription(LocalizedStrings.withDefaultValue("A longer description"))
        .setLocalizedShortDescription(LocalizedStrings.withDefaultValue("Get food"))
        .setLocalizedConfirmationMessage(LocalizedStrings.withDefaultValue("Thanks!"))
        .setExternalLink("https://example.com")
        .setDisplayMode(DisplayMode.PUBLIC)
        .setBlockDefinitions(ImmutableList.of())
        .setProgramType(ProgramType.DEFAULT)
        .setEligibilityIsGating(true)
        .setLoginOnly(false)
        .setAcls(new ProgramAcls())
        .setCategories(ImmutableList.of())
        .setApplicationSteps(ImmutableList.of())
        .setBridgeDefinitions(ImmutableMap.of());
  }

  private ProgramDefinition mockProgram() {
    return programBuilder().build();
  }

  @Test
  public void mapNew_setsTitleAndNewMode() {
    ProgramFormPageViewModel result = mapNew(new ProgramForm());

    assertThat(result.isEditMode()).isFalse();
    assertThat(result.getTitle()).isEqualTo("New program information");
  }

  @Test
  public void mapNew_formActionIsCreateUrl() {
    ProgramFormPageViewModel result = mapNew(new ProgramForm());

    assertThat(result.getFormActionUrl()).isEqualTo("/admin/programs");
  }

  @Test
  public void mapNew_slugIsEditableAndSubmitContinues() {
    ProgramFormPageViewModel result = mapNew(new ProgramForm());

    assertThat(result.isSlugEditable()).isTrue();
    assertThat(result.isSaveAndContinue()).isTrue();
  }

  @Test
  public void map_setsFormFieldValues() {
    ProgramForm form = new ProgramForm();
    form.setAdminName("food-benefits");
    form.setLocalizedDisplayName("Food benefits");
    form.setLocalizedShortDescription("Get food");
    form.setAdminDescription("admin note");
    form.setLocalizedDisplayDescription("A longer description");
    form.setLocalizedConfirmationMessage("Thanks!");
    form.setExternalLink("https://example.com");
    form.setDisplayMode("PUBLIC");

    ProgramFormPageViewModel result = mapNew(form);

    assertThat(result.getAdminName()).isEqualTo("food-benefits");
    assertThat(result.getDisplayName()).isEqualTo("Food benefits");
    assertThat(result.getShortDescription()).isEqualTo("Get food");
    assertThat(result.getAdminDescription()).isEqualTo("admin note");
    assertThat(result.getDisplayDescription()).isEqualTo("A longer description");
    assertThat(result.getConfirmationMessage()).isEqualTo("Thanks!");
    assertThat(result.getExternalLink()).isEqualTo("https://example.com");
    assertThat(result.getDisplayMode()).isEqualTo("PUBLIC");
    assertThat(result.getDefaultConfirmationMessage()).isEqualTo(DEFAULT_CONFIRMATION_MESSAGE);
  }

  @Test
  public void map_defaultProgram_enablesEligibilityAndDisablesExternalLink() {
    ProgramForm form = new ProgramForm();
    form.setEligibilityIsGating(true);

    ProgramFormPageViewModel result = mapNew(form);

    assertThat(result.getProgramTypeValue()).isEqualTo("default");
    assertThat(result.isDisableProgramEligibility()).isFalse();
    assertThat(result.isEligibilityGatingChecked()).isTrue();
    assertThat(result.isEligibilityNotGatingChecked()).isFalse();
    assertThat(result.isDisableExternalLink()).isTrue();
    assertThat(result.isExternalProgram()).isFalse();
  }

  @Test
  public void map_preScreenerForm_disablesEligibilityStepsAndCategories() {
    ProgramForm form = new ProgramForm();
    form.setProgramTypeValue("common_intake_form");

    ProgramFormPageViewModel result = mapNew(form);

    assertThat(result.isDisableProgramEligibility()).isTrue();
    // Eligibility radios are both unchecked when the fieldset is disabled.
    assertThat(result.isEligibilityGatingChecked()).isFalse();
    assertThat(result.isEligibilityNotGatingChecked()).isFalse();
    assertThat(result.isDisableLongDescription()).isTrue();
    assertThat(result.isDisableExternalLink()).isTrue();
    assertThat(result.isDisableApplicationSteps()).isTrue();
    assertThat(result.isCategoriesDisabled()).isTrue();
    assertThat(result.isDisableEmailNotifications()).isFalse();
  }

  @Test
  public void map_externalProgram_disablesEmailLoginOnlyAndConfirmation() {
    ProgramForm form = new ProgramForm();
    form.setProgramTypeValue("external");

    ProgramFormPageViewModel result = mapNew(form);

    assertThat(result.isExternalProgram()).isTrue();
    assertThat(result.isDisableExternalLink()).isFalse();
    assertThat(result.isDisableEmailNotifications()).isTrue();
    assertThat(result.isDisableLoginOnly()).isTrue();
    assertThat(result.isDisableConfirmationMessage()).isTrue();
    assertThat(result.isDisableProgramEligibility()).isTrue();
  }

  @Test
  public void map_setsCategoryOptions() {
    CategoryModel health = mock(CategoryModel.class);
    when(health.getId()).thenReturn(1L);
    when(health.getDefaultName()).thenReturn("Health");
    CategoryModel housing = mock(CategoryModel.class);
    when(housing.getId()).thenReturn(2L);
    when(housing.getDefaultName()).thenReturn("Housing");
    when(categoryRepository.listCategories()).thenReturn(ImmutableList.of(health, housing));

    ProgramForm form = new ProgramForm();
    form.setCategories(List.of(2L));

    ProgramFormPageViewModel result = mapNew(form);

    assertThat(result.getCategoryOptions()).hasSize(2);
    assertThat(result.getCategoryOptions().get(0).getName()).isEqualTo("Health");
    assertThat(result.getCategoryOptions().get(0).getValue()).isEqualTo("1");
    assertThat(result.getCategoryOptions().get(0).isChecked()).isFalse();
    assertThat(result.getCategoryOptions().get(1).isChecked()).isTrue();
  }

  @Test
  public void map_preScreenerForm_unchecksSelectedCategories() {
    CategoryModel health = mock(CategoryModel.class);
    when(health.getId()).thenReturn(1L);
    when(health.getDefaultName()).thenReturn("Health");
    when(categoryRepository.listCategories()).thenReturn(ImmutableList.of(health));

    ProgramForm form = new ProgramForm();
    form.setProgramTypeValue("common_intake_form");
    form.setCategories(List.of(1L));

    ProgramFormPageViewModel result = mapNew(form);

    assertThat(result.getCategoryOptions().get(0).isChecked()).isFalse();
  }

  @Test
  public void map_setsTiGroupsAndVisibility() {
    TrustedIntermediaryGroupModel tiGroup =
        new TrustedIntermediaryGroupModel("Helping Hands", "description");
    tiGroup.id = 5L;
    when(accountRepository.listTrustedIntermediaryGroups()).thenReturn(ImmutableList.of(tiGroup));

    ProgramForm form = new ProgramForm();
    form.setDisplayMode("SELECT_TI");
    form.setTiGroups(List.of(5L));

    ProgramFormPageViewModel result = mapNew(form);

    assertThat(result.isTiListVisible()).isTrue();
    assertThat(result.getTiGroups()).hasSize(1);
    assertThat(result.getTiGroups().get(0).getValue()).isEqualTo("5");
    assertThat(result.getTiGroups().get(0).getName()).isEqualTo("Helping Hands");
    assertThat(result.getTiGroups().get(0).isChecked()).isTrue();
  }

  @Test
  public void map_tiListHiddenForOtherDisplayModes() {
    ProgramForm form = new ProgramForm();
    form.setDisplayMode("PUBLIC");

    ProgramFormPageViewModel result = mapNew(form);

    assertThat(result.isTiListVisible()).isFalse();
  }

  @Test
  public void map_buildsFiveApplicationStepsWithOnlyFirstRequired() {
    ProgramForm form = new ProgramForm();
    form.setApplicationSteps(List.of(Map.of("title", "Apply", "description", "Fill out the form")));

    ProgramFormPageViewModel result = mapNew(form);

    assertThat(result.getApplicationSteps()).hasSize(5);
    assertThat(result.getApplicationSteps().get(0).getTitle()).isEqualTo("Apply");
    assertThat(result.getApplicationSteps().get(0).getDescription()).isEqualTo("Fill out the form");
    assertThat(result.getApplicationSteps().get(0).isRequired()).isTrue();
    assertThat(result.getApplicationSteps().get(1).getTitle()).isEmpty();
    assertThat(result.getApplicationSteps().get(1).isRequired()).isFalse();
  }

  @Test
  public void map_applicationStepsNotRequiredWhenDisabled() {
    ProgramForm form = new ProgramForm();
    form.setProgramTypeValue("external");
    form.setApplicationSteps(List.of(Map.of("title", "Apply", "description", "Form")));

    ProgramFormPageViewModel result = mapNew(form);

    assertThat(result.getApplicationSteps().get(0).isRequired()).isFalse();
  }

  @Test
  public void map_setsEmailNotificationChecked() {
    ProgramForm form = new ProgramForm();
    form.setNotificationPreferences(List.of("EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS"));

    ProgramFormPageViewModel result = mapNew(form);

    assertThat(result.isEmailNotificationChecked()).isTrue();
  }

  @Test
  public void map_externalProgram_unchecksEmailNotification() {
    ProgramForm form = new ProgramForm();
    form.setProgramTypeValue("external");
    form.setNotificationPreferences(List.of("EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS"));

    ProgramFormPageViewModel result = mapNew(form);

    assertThat(result.isEmailNotificationChecked()).isFalse();
  }

  @Test
  public void map_prefixesErrorMessageAndGeneratesToastId() {
    ProgramFormPageViewModel result =
        mapper.mapNew(
            new ProgramForm(),
            categoryRepository,
            accountRepository,
            DEFAULT_CONFIRMATION_MESSAGE,
            /* preScreenerFormDisplayName= */ Optional.empty(),
            Optional.of("A program name is required"));

    assertThat(result.getErrorMessage()).hasValue("Error: A program name is required");
    assertThat(result.getErrorToastId()).isNotNull();
  }

  @Test
  public void map_noErrorMessage_hasNoToastId() {
    ProgramFormPageViewModel result = mapNew(new ProgramForm());

    assertThat(result.getErrorMessage()).isEmpty();
    assertThat(result.getErrorToastId()).isNull();
  }

  @Test
  public void map_setsPreScreenerFormDisplayName() {
    ProgramFormPageViewModel result =
        mapper.mapNew(
            new ProgramForm(),
            categoryRepository,
            accountRepository,
            DEFAULT_CONFIRMATION_MESSAGE,
            Optional.of("Benefits pre-screener"),
            /* errorMessage= */ Optional.empty());

    assertThat(result.getPreScreenerFormDisplayName()).isEqualTo("Benefits pre-screener");
  }

  @Test
  public void mapEdit_setsTitleFromStoredProgramName() {
    ProgramDefinition program = mockProgram();

    ProgramFormPageViewModel result =
        mapEdit(program, ProgramEditStatus.EDIT, Optional.of(new ProgramForm()));

    assertThat(result.isEditMode()).isTrue();
    assertThat(result.getTitle()).isEqualTo("Edit program: Food benefits");
  }

  @Test
  public void mapEdit_formActionIsUpdateUrlWithStatus() {
    ProgramDefinition program = mockProgram();

    ProgramFormPageViewModel result =
        mapEdit(program, ProgramEditStatus.CREATION_EDIT, Optional.of(new ProgramForm()));

    assertThat(result.getFormActionUrl()).isEqualTo("/admin/programs/11/update/CREATION_EDIT");
  }

  @Test
  public void mapEdit_editStatus_showsProgramUrlInsteadOfSlugField() {
    ProgramDefinition program = mockProgram();
    ProgramForm form = new ProgramForm();
    form.setAdminName("Food Benefits");

    ProgramFormPageViewModel result = mapEdit(program, ProgramEditStatus.EDIT, Optional.of(form));

    assertThat(result.isSlugEditable()).isFalse();
    assertThat(result.isSaveAndContinue()).isFalse();
    assertThat(result.getSlugFieldText()).isEqualTo(BASE_URL + "/programs/food-benefits");
  }

  @Test
  public void mapEdit_externalProgram_showsProgramIdInsteadOfUrl() {
    ProgramDefinition program = mockProgram();
    ProgramForm form = new ProgramForm();
    form.setAdminName("external-program");
    form.setProgramTypeValue("external");

    ProgramFormPageViewModel result = mapEdit(program, ProgramEditStatus.EDIT, Optional.of(form));

    assertThat(result.getSlugFieldText()).isEqualTo("external-program");
  }

  @Test
  public void mapEdit_creationStatus_keepsSlugEditable() {
    ProgramDefinition program = mockProgram();

    ProgramFormPageViewModel result =
        mapEdit(program, ProgramEditStatus.CREATION, Optional.of(new ProgramForm()));

    assertThat(result.isSlugEditable()).isTrue();
    assertThat(result.isSaveAndContinue()).isTrue();
    assertThat(result.getSlugFieldText()).isNull();
  }

  @Test
  public void mapEdit_editStatus_disablesExternalProgramTypeForDefaultProgram() {
    ProgramDefinition program = mockProgram();

    ProgramFormPageViewModel result =
        mapEdit(program, ProgramEditStatus.EDIT, Optional.of(new ProgramForm()));

    assertThat(result.isDefaultProgramFieldDisabled()).isFalse();
    assertThat(result.isPreScreenerFieldDisabled()).isFalse();
    assertThat(result.isExternalProgramFieldDisabled()).isTrue();
  }

  @Test
  public void mapEdit_editStatus_disablesOtherProgramTypesForExternalProgram() {
    ProgramDefinition program = mockProgram();
    ProgramForm form = new ProgramForm();
    form.setProgramTypeValue("external");

    ProgramFormPageViewModel result = mapEdit(program, ProgramEditStatus.EDIT, Optional.of(form));

    assertThat(result.isDefaultProgramFieldDisabled()).isTrue();
    assertThat(result.isPreScreenerFieldDisabled()).isTrue();
    assertThat(result.isExternalProgramFieldDisabled()).isFalse();
  }

  @Test
  public void mapEdit_creationEditStatus_keepsProgramTypeFieldsEnabled() {
    ProgramDefinition program = mockProgram();

    ProgramFormPageViewModel result =
        mapEdit(program, ProgramEditStatus.CREATION_EDIT, Optional.of(new ProgramForm()));

    assertThat(result.isDefaultProgramFieldDisabled()).isFalse();
    assertThat(result.isPreScreenerFieldDisabled()).isFalse();
    assertThat(result.isExternalProgramFieldDisabled()).isFalse();
  }

  @Test
  public void mapEdit_showsManageQuestionsLinkForDefaultProgram() {
    ProgramDefinition program = mockProgram();

    ProgramFormPageViewModel result =
        mapEdit(program, ProgramEditStatus.EDIT, Optional.of(new ProgramForm()));

    assertThat(result.isShowManageQuestionsLink()).isTrue();
    assertThat(result.getManageQuestionsUrl()).isEqualTo("/admin/programs/11/blocks");
  }

  @Test
  public void mapEdit_hidesManageQuestionsLinkForExternalProgram() {
    ProgramDefinition program = programBuilder().setProgramType(ProgramType.EXTERNAL).build();

    ProgramFormPageViewModel result =
        mapEdit(program, ProgramEditStatus.EDIT, Optional.of(new ProgramForm()));

    assertThat(result.isShowManageQuestionsLink()).isFalse();
  }

  @Test
  public void mapEdit_noForm_buildsFieldValuesFromProgramDefinition() {
    ProgramDefinition program =
        programBuilder()
            .setDisplayMode(DisplayMode.SELECT_TI)
            .setNotificationPreferences(
                ImmutableList.of(ProgramNotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS))
            .setEligibilityIsGating(false)
            .setLoginOnly(true)
            .setAcls(new ProgramAcls(ImmutableSet.of(5L)))
            .setApplicationSteps(
                ImmutableList.of(new ApplicationStep("Apply", "Fill out the form")))
            .build();

    ProgramFormPageViewModel result = mapEdit(program, ProgramEditStatus.EDIT, Optional.empty());

    assertThat(result.getAdminName()).isEqualTo("food-benefits");
    assertThat(result.getAdminDescription()).isEqualTo("admin note");
    assertThat(result.getDisplayName()).isEqualTo("Food benefits");
    assertThat(result.getDisplayDescription()).isEqualTo("A longer description");
    assertThat(result.getShortDescription()).isEqualTo("Get food");
    assertThat(result.getConfirmationMessage()).isEqualTo("Thanks!");
    assertThat(result.getExternalLink()).isEqualTo("https://example.com");
    assertThat(result.getDisplayMode()).isEqualTo("SELECT_TI");
    assertThat(result.isTiListVisible()).isTrue();
    assertThat(result.isEmailNotificationChecked()).isTrue();
    assertThat(result.isEligibilityGatingChecked()).isFalse();
    assertThat(result.isEligibilityNotGatingChecked()).isTrue();
    assertThat(result.isLoginOnlyChecked()).isTrue();
    assertThat(result.getApplicationSteps().get(0).getTitle()).isEqualTo("Apply");
  }
}
