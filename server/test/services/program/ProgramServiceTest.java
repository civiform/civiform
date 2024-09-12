package services.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static services.LocalizedStrings.DEFAULT_LOCALE;
import static support.FakeRequestBuilder.fakeRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import controllers.BadRequestException;
import controllers.admin.ImageDescriptionNotRemovableException;
import forms.BlockForm;
import io.ebean.DB;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.AccountModel;
import models.CategoryModel;
import models.DisplayMode;
import models.ProgramModel;
import models.ProgramNotificationPreference;
import models.QuestionModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import play.cache.NamedCacheImpl;
import play.cache.SyncCacheApi;
import play.i18n.Lang;
import play.inject.BindingKey;
import play.mvc.Http.Request;
import repository.ApplicationStatusesRepository;
import repository.CategoryRepository;
import repository.ResetPostgres;
import services.CiviFormError;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.applicant.question.Scalar;
import services.program.predicate.AndNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.OrNode;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.TextQuestionDefinition;
import services.settings.SettingsManifest;
import support.ProgramBuilder;

@RunWith(JUnitParamsRunner.class)
public class ProgramServiceTest extends ResetPostgres {

  private QuestionDefinition addressQuestion;
  private QuestionDefinition secondaryAddressQuestion;
  private QuestionDefinition colorQuestion;
  private QuestionDefinition nameQuestion;
  private ProgramService ps;
  private SyncCacheApi programDefCache;
  private SettingsManifest mockSettingsManifest;
  private CategoryRepository categoryRepository;
  private final Request request = fakeRequest();
  private ApplicationStatusesRepository applicationStatusesRepo;

  @Before
  public void setProgramServiceImpl() {
    BindingKey<SyncCacheApi> programDefKey =
        new BindingKey<>(SyncCacheApi.class)
            .qualifiedWith(new NamedCacheImpl("full-program-definition"));
    programDefCache = instanceOf(programDefKey.asScala());
    ps = instanceOf(ProgramService.class);
    applicationStatusesRepo = instanceOf(ApplicationStatusesRepository.class);
  }

  @Before
  public void setUp() {
    addressQuestion = testQuestionBank.addressApplicantAddress().getQuestionDefinition();
    secondaryAddressQuestion =
        testQuestionBank.addressApplicantSecondaryAddress().getQuestionDefinition();
    colorQuestion = testQuestionBank.textApplicantFavoriteColor().getQuestionDefinition();
    nameQuestion = testQuestionBank.nameApplicantName().getQuestionDefinition();
    mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    when(mockSettingsManifest.getDisabledVisibilityConditionEnabled(request)).thenReturn(false);
    categoryRepository = instanceOf(CategoryRepository.class);
  }

  @Test
  public void syncQuestions_constructsAllQuestionDefinitions() {
    QuestionDefinition questionOne = nameQuestion;
    QuestionDefinition questionTwo = addressQuestion;
    QuestionDefinition questionThree = colorQuestion;

    ProgramBuilder.newDraftProgram("program1")
        .withBlock()
        .withRequiredQuestionDefinition(questionOne)
        .withRequiredQuestionDefinition(questionTwo)
        .withBlock()
        .withRequiredQuestionDefinition(questionThree)
        .buildDefinition();
    ProgramBuilder.newDraftProgram("program2")
        .withBlock()
        .withRequiredQuestionDefinition(questionTwo)
        .withBlock()
        .withRequiredQuestionDefinition(questionOne)
        .buildDefinition();

    ImmutableList<ProgramDefinition> programDefinitions =
        ps.getActiveAndDraftPrograms().getDraftPrograms();

    QuestionDefinition found = programDefinitions.get(0).getQuestionDefinition(0, 0);
    assertThat(found).isInstanceOf(NameQuestionDefinition.class);
    found = programDefinitions.get(0).getQuestionDefinition(0, 1);
    assertThat(found).isInstanceOf(AddressQuestionDefinition.class);
    found = programDefinitions.get(0).getQuestionDefinition(1, 0);
    assertThat(found).isInstanceOf(TextQuestionDefinition.class);
    found = programDefinitions.get(1).getQuestionDefinition(0, 0);
    assertThat(found).isInstanceOf(AddressQuestionDefinition.class);
    found = programDefinitions.get(1).getQuestionDefinition(1, 0);
    assertThat(found).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void getActiveAndDraftProgramsWithoutQuestionLoad_hasBasicProgramInfo() {
    QuestionDefinition questionOne = nameQuestion;
    QuestionDefinition questionTwo = addressQuestion;
    QuestionDefinition questionThree = colorQuestion;

    ProgramBuilder.newDraftProgram("program1")
        .withBlock()
        .withRequiredQuestionDefinition(questionOne)
        .withRequiredQuestionDefinition(questionTwo)
        .withBlock()
        .withRequiredQuestionDefinition(questionThree)
        .buildDefinition();
    ProgramBuilder.newActiveProgram("program2")
        .withBlock()
        .withRequiredQuestionDefinition(questionTwo)
        .withBlock()
        .withRequiredQuestionDefinition(questionOne)
        .buildDefinition();

    ImmutableList<ProgramDefinition> draftPrograms =
        ps.getActiveAndDraftProgramsWithoutQuestionLoad().getDraftPrograms();
    ImmutableList<ProgramDefinition> activePrograms =
        ps.getActiveAndDraftProgramsWithoutQuestionLoad().getActivePrograms();

    ProgramDefinition draftProgramDef = draftPrograms.get(0);
    assertThat(draftProgramDef.getBlockCount()).isEqualTo(2);
    assertThat(draftProgramDef.getQuestionCount()).isEqualTo(3);
    ProgramDefinition activeProgramDef = activePrograms.get(0);
    assertThat(activeProgramDef.getBlockCount()).isEqualTo(2);
    assertThat(activeProgramDef.getQuestionCount()).isEqualTo(2);
  }

  @Test
  public void getDisabledActiveAndDraftProgramsWithoutQuestionLoad_hasBasicProgramInfo() {
    when(mockSettingsManifest.getDisabledVisibilityConditionEnabled(request)).thenReturn(true);
    QuestionDefinition questionOne = nameQuestion;
    QuestionDefinition questionTwo = addressQuestion;
    QuestionDefinition questionThree = colorQuestion;

    ProgramBuilder.newDisabledDraftProgram("program1")
        .withBlock()
        .withRequiredQuestionDefinition(questionOne)
        .withRequiredQuestionDefinition(questionTwo)
        .withBlock()
        .withRequiredQuestionDefinition(questionThree)
        .buildDefinition();
    ProgramBuilder.newDisabledActiveProgram("program2")
        .withBlock()
        .withRequiredQuestionDefinition(questionTwo)
        .withBlock()
        .withRequiredQuestionDefinition(questionOne)
        .buildDefinition();

    ImmutableList<ProgramDefinition> draftPrograms =
        ps.getDisabledActiveAndDraftProgramsWithoutQuestionLoad().getDraftPrograms();
    ImmutableList<ProgramDefinition> activePrograms =
        ps.getDisabledActiveAndDraftProgramsWithoutQuestionLoad().getActivePrograms();

    ProgramDefinition activeProgramDef = activePrograms.get(0);
    assertThat(activeProgramDef.getBlockCount()).isEqualTo(2);
    assertThat(activeProgramDef.getQuestionCount()).isEqualTo(2);
    ProgramDefinition draftProgramDef = draftPrograms.get(0);
    assertThat(draftProgramDef.getBlockCount()).isEqualTo(2);
    assertThat(draftProgramDef.getQuestionCount()).isEqualTo(3);
  }

  @Test
  public void getInUseActiveAndDraftProgramsWithoutQuestionLoad_hasBasicProgramInfo() {
    QuestionDefinition questionOne = nameQuestion;
    QuestionDefinition questionTwo = addressQuestion;
    QuestionDefinition questionThree = colorQuestion;

    ProgramBuilder.newDraftProgram("program1")
        .withBlock()
        .withRequiredQuestionDefinition(questionOne)
        .withRequiredQuestionDefinition(questionTwo)
        .withBlock()
        .withRequiredQuestionDefinition(questionThree)
        .buildDefinition();
    ProgramBuilder.newActiveProgram("program2")
        .withBlock()
        .withRequiredQuestionDefinition(questionTwo)
        .withBlock()
        .withRequiredQuestionDefinition(questionOne)
        .buildDefinition();

    ImmutableList<ProgramDefinition> draftPrograms =
        ps.getInUseActiveAndDraftProgramsWithoutQuestionLoad().getDraftPrograms();
    ImmutableList<ProgramDefinition> activePrograms =
        ps.getInUseActiveAndDraftProgramsWithoutQuestionLoad().getActivePrograms();

    ProgramDefinition draftProgramDef = draftPrograms.get(0);
    assertThat(draftProgramDef.getBlockCount()).isEqualTo(2);
    assertThat(draftProgramDef.getQuestionCount()).isEqualTo(3);
    ProgramDefinition activeProgramDef = activePrograms.get(0);
    assertThat(activeProgramDef.getBlockCount()).isEqualTo(2);
    assertThat(activeProgramDef.getQuestionCount()).isEqualTo(2);
  }

  @Test
  public void createProgram_setsId() {
    assertThat(ps.getActiveAndDraftPrograms().getActivePrograms()).isEmpty();
    assertThat(ps.getActiveAndDraftPrograms().getDraftPrograms()).isEmpty();

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition(
            "test-program",
            "description",
            "name",
            "description",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.DEFAULT,
            ImmutableList.of(),
            /* categoryIds= */ ImmutableList.of());

    assertThat(result.hasResult()).isTrue();
    assertThat(result.getResult().id()).isNotNull();
  }

  @Test
  public void createProgram_setsNotificationPreferences() {
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition(
            "test-program",
            "description",
            "name",
            "description",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(
                ProgramNotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS.getValue()),
            /* eligibilityIsGating= */ true,
            ProgramType.DEFAULT,
            ImmutableList.of(),
            /* categoryIds= */ ImmutableList.of());

    assertThat(result.hasResult()).isTrue();
    assertThat(result.getResult().notificationPreferences())
        .containsExactlyInAnyOrder(
            ProgramNotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS);
  }

  @Test
  public void createProgram_hasEmptyBlock() {
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition(
            "test-program",
            "description",
            "name",
            "description",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.DEFAULT,
            ImmutableList.of(),
            /* categoryIds= */ ImmutableList.of());

    assertThat(result.hasResult()).isTrue();
    assertThat(result.getResult().blockDefinitions()).hasSize(1);
    BlockDefinition firstBlock = result.getResult().getBlockDefinitionByIndex(0).get();
    assertThat(firstBlock.id()).isEqualTo(1L);
    assertThat(firstBlock.name()).isEqualTo("Screen 1");
    assertThat(firstBlock.description()).isEqualTo("Screen 1 description");
  }

  @Test
  public void createProgram_returnsErrors() {
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition(
            "",
            "",
            "",
            "",
            "",
            "",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of("invalid notification preference"),
            /* eligibilityIsGating= */ true,
            ProgramType.DEFAULT,
            ImmutableList.of(),
            /* categoryIds= */ ImmutableList.of());

    assertThat(result.hasResult()).isFalse();
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors())
        .containsExactlyInAnyOrder(
            CiviFormError.of("A public display name for the program is required"),
            CiviFormError.of("A public description for the program is required"),
            CiviFormError.of("A program URL is required"),
            CiviFormError.of("One or more notification preferences are invalid"));
  }

  @Test
  public void createProgramWithoutDisplayMode_returnsError() {
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition(
            "test-program",
            "description",
            "name",
            "description",
            "confirm",
            "https://usa.gov",
            "",
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.DEFAULT,
            /* tiGroup */ ImmutableList.of(),
            /* categoryIds= */ ImmutableList.of());

    assertThat(result.hasResult()).isFalse();
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors())
        .containsExactly(CiviFormError.of("A program visibility option must be selected"));
  }

  @Test
  public void createProgram_protectsAgainstProgramNameCollisions() {
    ps.createProgramDefinition(
        "name",
        "description",
        "display name",
        "display description",
        "",
        "https://usa.gov",
        DisplayMode.PUBLIC.getValue(),
        ImmutableList.of(),
        /* eligibilityIsGating= */ true,
        ProgramType.DEFAULT,
        ImmutableList.of(),
        /* categoryIds= */ ImmutableList.of());

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition(
            "name",
            "description",
            "display name",
            "display description",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.DEFAULT,
            ImmutableList.of(),
            /* categoryIds= */ ImmutableList.of());

    assertThat(result.hasResult()).isFalse();
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors())
        .containsExactly(CiviFormError.of("A program URL of name already exists"));
  }

  @Test
  @Parameters({"name with spaces", "DiFfErEnT-cAsEs", "special-characters-$#@"})
  public void createProgram_requiresSlug(String adminName) {
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition(
            adminName,
            "description",
            "display name",
            "display description",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.DEFAULT,
            ImmutableList.of(),
            /* categoryIds= */ ImmutableList.of());

    assertThat(result.hasResult()).isFalse();
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors())
        .containsExactly(
            CiviFormError.of(
                "A program URL may only contain lowercase letters, numbers, and dashes"));
  }

  @Test
  public void createProgram_protectsAgainstProgramSlugCollisions() {
    // Two programs with names that are different but slugify to same value.
    // To simulate this state, we first create a program with a slugified name, then manually
    // update the Program entity in order to add a name value that slugifies to the same value.
    ProgramDefinition originalProgramDefinition =
        ps.createProgramDefinition(
                "name-one",
                "description",
                "display name",
                "display description",
                "",
                "https://usa.gov",
                DisplayMode.PUBLIC.getValue(),
                ImmutableList.of(),
                /* eligibilityIsGating= */ true,
                ProgramType.DEFAULT,
                ImmutableList.of(),
                /* categoryIds= */ ImmutableList.of())
            .getResult();
    // Program name here is missing the extra space
    // so that the names are different but the resulting
    // slug is the same.
    ProgramModel updatedProgram =
        originalProgramDefinition.toBuilder().setAdminName("name    one").build().toProgram();
    updatedProgram.update();
    assertThat(updatedProgram.getProgramDefinition().adminName()).isEqualTo("name    one");

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition(
            "name-one",
            "description",
            "display name",
            "display description",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.DEFAULT,
            ImmutableList.of(),
            /* categoryIds= */ ImmutableList.of());
    assertThat(result.hasResult()).isFalse();
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors())
        .containsExactly(CiviFormError.of("A program URL of name-one already exists"));
  }

  @Test
  public void createProgram_eligibilityIsGating_true() {
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition(
            "name-one",
            "description",
            "display name",
            "display description",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.COMMON_INTAKE_FORM,
            ImmutableList.of(),
            /* categoryIds= */ ImmutableList.of());

    assertThat(result.hasResult()).isTrue();
    assertThat(result.isError()).isFalse();
    assertThat(result.getResult().eligibilityIsGating()).isTrue();
  }

  @Test
  public void createProgram_eligibilityIsGating_false() {
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition(
            "name-one",
            "description",
            "display name",
            "display description",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ false,
            ProgramType.COMMON_INTAKE_FORM,
            ImmutableList.of(),
            /* categoryIds= */ ImmutableList.of());

    assertThat(result.hasResult()).isTrue();
    assertThat(result.isError()).isFalse();
    assertThat(result.getResult().eligibilityIsGating()).isFalse();
  }

  @Test
  public void createProgram_allowsSettingDefaultProgram() {
    ps.createProgramDefinition(
        "name-one",
        "description",
        "display name",
        "display description",
        "",
        "https://usa.gov",
        DisplayMode.PUBLIC.getValue(),
        ImmutableList.of(),
        /* eligibilityIsGating= */ true,
        ProgramType.COMMON_INTAKE_FORM,
        ImmutableList.of(),
        /* categoryIds= */ ImmutableList.of());
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition(
            "name-two",
            "description",
            "display name",
            "display description",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.DEFAULT,
            ImmutableList.of(),
            /* categoryIds= */ ImmutableList.of());

    assertThat(result.hasResult()).isTrue();
    assertThat(result.isError()).isFalse();
    assertThat(result.getResult().programType()).isEqualTo(ProgramType.DEFAULT);
  }

  @Test
  public void createProgram_clearsExistingCommonIntakeForm() {
    ps.createProgramDefinition(
        "name-one",
        "description",
        "display name",
        "display description",
        "",
        "https://usa.gov",
        DisplayMode.PUBLIC.getValue(),
        ImmutableList.of(),
        /* eligibilityIsGating= */ true,
        ProgramType.COMMON_INTAKE_FORM,
        ImmutableList.of(),
        /* categoryIds= */ ImmutableList.of());

    Optional<ProgramDefinition> commonIntakeForm = ps.getCommonIntakeForm();
    assertThat(commonIntakeForm).isPresent();
    assertThat(commonIntakeForm.get().adminName()).isEqualTo("name-one");

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.createProgramDefinition(
            "name-two",
            "description",
            "display name",
            "display description",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.COMMON_INTAKE_FORM,
            ImmutableList.of(),
            /* categoryIds= */ ImmutableList.of());
    assertThat(result.hasResult()).isTrue();
    assertThat(result.isError()).isFalse();
    assertThat(result.getResult().programType()).isEqualTo(ProgramType.COMMON_INTAKE_FORM);

    commonIntakeForm = ps.getCommonIntakeForm();
    assertThat(commonIntakeForm).isPresent();
    assertThat(commonIntakeForm.get().adminName()).isEqualTo("name-two");
    Optional<ProgramDefinition> oldCommonIntake =
        ps.getActiveAndDraftPrograms().getDraftProgramDefinition("name-one");
    assertThat(oldCommonIntake).isPresent();
    assertThat(oldCommonIntake.get().programType()).isEqualTo(ProgramType.DEFAULT);
  }

  @Test
  public void validateProgramDataForCreate_returnsErrors() {
    ImmutableSet<CiviFormError> result =
        ps.validateProgramDataForCreate(
            "",
            "",
            "",
            "",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of("invalid notification preference"),
            ImmutableList.of(),
            ImmutableList.of());

    assertThat(result)
        .containsExactlyInAnyOrder(
            CiviFormError.of("A public display name for the program is required"),
            CiviFormError.of("A public description for the program is required"),
            CiviFormError.of("A program URL is required"),
            CiviFormError.of("One or more notification preferences are invalid"));
  }

  @Test
  public void validateProgramDataForCreate_withInvalidCategory_returnsError() {
    ImmutableMap<Locale, String> categoryTranslations =
        ImmutableMap.of(
            Lang.forCode("en-US").toLocale(), "Health", Lang.forCode("es-US").toLocale(), "Salud");
    CategoryModel category = new CategoryModel(categoryTranslations);
    Long validCategoryId = categoryRepository.fetchOrSaveUniqueCategory(category).getId();
    ImmutableSet<CiviFormError> result =
        ps.validateProgramDataForCreate(
            "name",
            "display name",
            "display desc",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            ImmutableList.of(validCategoryId + 1), // This category doesn't exist in the database
            ImmutableList.of());

    assertThat(result)
        .containsExactlyInAnyOrder(CiviFormError.of("Only existing category ids are allowed"));
  }

  @Test
  public void validateProgramDataForCreate_withValidCategory_returnsNoErrors() {
    ImmutableMap<Locale, String> categoryTranslations =
        ImmutableMap.of(
            Lang.forCode("en-US").toLocale(), "Health", Lang.forCode("es-US").toLocale(), "Salud");
    CategoryModel category = new CategoryModel(categoryTranslations);
    Long validCategoryId = categoryRepository.fetchOrSaveUniqueCategory(category).getId();
    ImmutableSet<CiviFormError> result =
        ps.validateProgramDataForCreate(
            "name",
            "display name",
            "display desc",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            ImmutableList.of(validCategoryId),
            ImmutableList.of());

    assertThat(result).isEmpty();
  }

  @Test
  @Parameters({"name with spaces", "DiFfErEnT-cAsEs", "special-characters-$#@"})
  public void validateProgramDataForCreate_requiresSlug(String adminName) {
    ImmutableSet<CiviFormError> result =
        ps.validateProgramDataForCreate(
            adminName,
            "display name",
            "display desc",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            ImmutableList.of(),
            ImmutableList.of());

    assertThat(result)
        .containsExactly(
            CiviFormError.of(
                "A program URL may only contain lowercase letters, numbers, and dashes"));
  }

  @Test
  @Parameters({"123", "235", "56789"})
  public void validateProgramDataForCreate_requiresSlugBeingAlphanumerical_failsIfNoLetters(
      String adminName) {
    ImmutableSet<CiviFormError> result =
        ps.validateProgramDataForCreate(
            adminName,
            "display name",
            "display desc",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            ImmutableList.of(),
            ImmutableList.of());

    assertThat(result)
        .containsExactly(CiviFormError.of("A program URL must contain at least one letter"));
  }

  @Test
  @Parameters({"a7859", "b8419", "c-302"})
  public void validateProgramDataForCreate_requiresSlugBeingAlphanumerical_validIfOneLetter(
      String adminName) {
    ImmutableSet<CiviFormError> result =
        ps.validateProgramDataForCreate(
            adminName,
            "display name",
            "display desc",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            ImmutableList.of(),
            ImmutableList.of());

    assertThat(result).isEmpty();
  }

  @Test
  public void validateProgramDataForCreate_requiresTIListInSelectTiMode() {
    ImmutableSet<CiviFormError> result =
        ps.validateProgramDataForCreate(
            "program",
            "display name",
            "display desc",
            "https://usa.gov",
            DisplayMode.SELECT_TI.getValue(),
            ImmutableList.of(),
            ImmutableList.of(),
            ImmutableList.of());

    assertThat(result)
        .containsExactly(
            CiviFormError.of("One or more TI Org must be selected for program visibility"));
  }

  @Test
  public void validateProgramDataForUpdate_requiresTIListInSELECT_TIMode() {
    ImmutableSet<CiviFormError> result =
        ps.validateProgramDataForUpdate(
            "description",
            "display name",
            "https://usa.gov",
            DisplayMode.SELECT_TI.getValue(),
            ImmutableList.of(),
            ImmutableList.of(),
            ImmutableList.of());

    assertThat(result)
        .containsExactly(
            CiviFormError.of("One or more TI Org must be selected for program visibility"));
  }

  @Test
  public void validateProgramDataForCreate_protectsAgainstProgramSlugCollisions() {
    // Two programs with names that are different but slugify to same value.
    // To simulate this state, we first create a program with a slugified name, then manually
    // update the Program entity in order to add a name value that slugifies to the same value.
    ProgramDefinition originalProgramDefinition =
        ps.createProgramDefinition(
                "name-one",
                "description",
                "display name",
                "display description",
                "",
                "https://usa.gov",
                DisplayMode.PUBLIC.getValue(),
                ImmutableList.of(),
                /* eligibilityIsGating= */ true,
                ProgramType.DEFAULT,
                ImmutableList.of(),
                /* categoryIds= */ ImmutableList.of())
            .getResult();
    // Program name here is missing the extra space
    // so that the names are different but the resulting
    // slug is the same.
    ProgramModel updatedProgram =
        originalProgramDefinition.toBuilder().setAdminName("name    one").build().toProgram();
    updatedProgram.update();
    assertThat(updatedProgram.getProgramDefinition().adminName()).isEqualTo("name    one");

    ImmutableSet<CiviFormError> result =
        ps.validateProgramDataForCreate(
            "name-one",
            "display name",
            "display desc",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            ImmutableList.of(),
            ImmutableList.of());
    assertThat(result)
        .containsExactly(CiviFormError.of("A program URL of name-one already exists"));
  }

  @Test
  public void validateProgramDataForCreate_returnsNoErrorsForValidData() {
    ImmutableSet<CiviFormError> result =
        ps.validateProgramDataForCreate(
            "name-two",
            "display name",
            "display description",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(
                ProgramNotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS.getValue()),
            ImmutableList.of(),
            ImmutableList.of());

    assertThat(result).isEmpty();
  }

  @Test
  public void validateProgramDataForCreate_returnsNoErrorsInSELECT_TIMode() {
    List<Long> tiGroups = new ArrayList<>();
    tiGroups.add(1L);
    tiGroups.add(3L);
    ImmutableSet<CiviFormError> result =
        ps.validateProgramDataForCreate(
            "name-two",
            "display name",
            "display description",
            "https://usa.gov",
            DisplayMode.SELECT_TI.getValue(),
            ImmutableList.of(),
            ImmutableList.of(),
            ImmutableList.copyOf(tiGroups));

    assertThat(result).isEmpty();
  }

  @Test
  public void updateProgram_withNoProgram_throwsProgramNotFoundException() {
    assertThatThrownBy(
            () ->
                ps.updateProgramDefinition(
                    1L,
                    Locale.US,
                    "new description",
                    "name",
                    "description",
                    "",
                    "https://usa.gov",
                    DisplayMode.PUBLIC.getValue(),
                    ImmutableList.of(),
                    /* eligibilityIsGating= */ true,
                    ProgramType.DEFAULT,
                    ImmutableList.of(),
                    /* categories= */ ImmutableList.of()))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessage("Program not found for ID: 1");
  }

  @Test
  public void updateProgram_updatesProgram() throws Exception {
    ProgramDefinition originalProgram =
        ProgramBuilder.newDraftProgram("original", "original description").buildDefinition();
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateProgramDefinition(
            originalProgram.id(),
            Locale.US,
            "new description",
            "name",
            "description",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(
                ProgramNotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS.getValue()),
            /* eligibilityIsGating= */ true,
            ProgramType.DEFAULT,
            ImmutableList.of(),
            /* categories= */ ImmutableList.of());

    assertThat(result.hasResult()).isTrue();
    ProgramDefinition updatedProgram = result.getResult();

    ProgramDefinition found = ps.getFullProgramDefinition(updatedProgram.id());

    assertThat(ps.getActiveAndDraftPrograms().getDraftPrograms()).hasSize(1);
    assertThat(ps.getActiveAndDraftProgramsWithoutQuestionLoad().getDraftPrograms()).hasSize(1);
    assertThat(found.adminName()).isEqualTo(updatedProgram.adminName());
    assertThat(found.lastModifiedTime().isPresent()).isTrue();
    assertThat(found.notificationPreferences())
        .containsExactlyInAnyOrder(
            ProgramNotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS);
    assertThat(originalProgram.lastModifiedTime().isPresent()).isTrue();
    assertThat(found.lastModifiedTime().get().isAfter(originalProgram.lastModifiedTime().get()))
        .isTrue();
  }

  @Test
  public void updateProgram_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();
    ps.addQuestionsToBlock(program.id(), 1L, ImmutableList.of(question.getId()));

    ProgramDefinition found =
        ps.updateProgramDefinition(
                program.id(),
                Locale.US,
                "new description",
                "name",
                "description",
                "",
                "https://usa.gov",
                DisplayMode.PUBLIC.getValue(),
                ImmutableList.of(),
                /* eligibilityIsGating= */ true,
                ProgramType.DEFAULT,
                ImmutableList.of(),
                /* categories= */ ImmutableList.of())
            .getResult();

    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void updateProgram_returnsErrors() throws Exception {
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateProgramDefinition(
            program.id(),
            Locale.US,
            "",
            "",
            "",
            "",
            "",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.DEFAULT,
            ImmutableList.of(),
            /* categories= */ ImmutableList.of());

    assertThat(result.hasResult()).isFalse();
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors())
        .containsOnly(
            CiviFormError.of("A public display name for the program is required"),
            CiviFormError.of("A public description for the program is required"));
  }

  @Test
  public void updateProgram_clearsOldConfirmationScreenTranslations() throws Exception {
    ProgramDefinition originalProgram =
        ProgramBuilder.newDraftProgram("original", "original description").buildDefinition();
    ErrorAnd<ProgramDefinition, CiviFormError> resultOne =
        ps.updateProgramDefinition(
            originalProgram.id(),
            Locale.US,
            "new description",
            "name",
            "description",
            "custom confirmation screen message",
            "",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.DEFAULT,
            ImmutableList.of(),
            /* categories= */ ImmutableList.of());

    // check that the confirmation screen message saved
    LocalizedStrings expectedUsString =
        LocalizedStrings.create(ImmutableMap.of(Locale.US, "custom confirmation screen message"));
    ProgramDefinition firstProgramUpdate = resultOne.getResult();
    assertThat(firstProgramUpdate.localizedConfirmationMessage()).isEqualTo(expectedUsString);

    // update the confirmation screen with an translation and check that it saves correctly
    ErrorAnd<ProgramDefinition, CiviFormError> resultTwo =
        ps.updateProgramDefinition(
            firstProgramUpdate.id(),
            Locale.FRANCE,
            "new description",
            "name",
            "description",
            "french custom confirmation screen message",
            "",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.DEFAULT,
            ImmutableList.of(),
            /* categories= */ ImmutableList.of());
    ProgramDefinition secondProgramUpdate = resultTwo.getResult();
    assertThat(secondProgramUpdate.localizedConfirmationMessage())
        .isEqualTo(
            expectedUsString.updateTranslation(
                Locale.FRANCE, "french custom confirmation screen message"));

    // delete the english confirmation screen and check that all translations were cleared as well
    ErrorAnd<ProgramDefinition, CiviFormError> resultThree =
        ps.updateProgramDefinition(
            firstProgramUpdate.id(),
            Locale.US,
            "new description",
            "name",
            "description",
            "",
            "",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.DEFAULT,
            ImmutableList.of(),
            /* categories= */ ImmutableList.of());
    ProgramDefinition thirdProgramUpdate = resultThree.getResult();
    assertThat(thirdProgramUpdate.localizedConfirmationMessage())
        .isEqualTo(LocalizedStrings.create(ImmutableMap.of(Locale.US, "")));
  }

  @Test
  public void getProgramDefinition() throws Exception {
    ProgramDefinition programDefinition = ProgramBuilder.newDraftProgram().buildDefinition();
    ProgramDefinition found = ps.getFullProgramDefinition(programDefinition.id());

    assertThat(found.adminName()).isEqualTo(programDefinition.adminName());
  }

  @Test
  public void updateProgram_clearsExistingCommonIntakeForm() throws Exception {
    ps.createProgramDefinition(
        "name-one",
        "description",
        "display name",
        "display description",
        "",
        "https://usa.gov",
        DisplayMode.PUBLIC.getValue(),
        ImmutableList.of(),
        /* eligibilityIsGating= */ true,
        ProgramType.COMMON_INTAKE_FORM,
        ImmutableList.of(),
        /* categoryIds= */ ImmutableList.of());

    Optional<ProgramDefinition> commonIntakeForm = ps.getCommonIntakeForm();
    assertThat(commonIntakeForm).isPresent();
    assertThat(commonIntakeForm.get().adminName()).isEqualTo("name-one");

    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateProgramDefinition(
            program.id(),
            Locale.US,
            "a",
            "a",
            "a",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.COMMON_INTAKE_FORM,
            ImmutableList.of(),
            /* categories= */ ImmutableList.of());

    assertThat(result.hasResult()).isTrue();
    assertThat(result.isError()).isFalse();
    assertThat(result.getResult().programType()).isEqualTo(ProgramType.COMMON_INTAKE_FORM);
    commonIntakeForm = ps.getCommonIntakeForm();
    assertThat(commonIntakeForm).isPresent();
    assertThat(commonIntakeForm.get().adminName()).isEqualTo(program.adminName());
    Optional<ProgramDefinition> oldCommonIntake =
        ps.getActiveAndDraftPrograms().getDraftProgramDefinition("name-one");
    assertThat(oldCommonIntake).isPresent();
    assertThat(oldCommonIntake.get().programType()).isEqualTo(ProgramType.DEFAULT);
  }

  @Test
  public void updateProgram_allowsUpdatingCommonIntakeForm() throws Exception {
    ErrorAnd<ProgramDefinition, CiviFormError> commonIntakeForm =
        ps.createProgramDefinition(
            "name-one",
            "description",
            "display name",
            "display description",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.COMMON_INTAKE_FORM,
            ImmutableList.of(),
            /* categoryIds= */ ImmutableList.of());

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateProgramDefinition(
            commonIntakeForm.getResult().id(),
            Locale.US,
            "a",
            "a",
            "a",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.COMMON_INTAKE_FORM,
            ImmutableList.of(),
            /* categories= */ ImmutableList.of());

    assertThat(result.hasResult()).isTrue();
    assertThat(result.isError()).isFalse();
  }

  @Test
  public void updateProgram_allowsUpdatingCommonIntakeFormToDefaultProgram() throws Exception {
    ErrorAnd<ProgramDefinition, CiviFormError> commonIntakeForm =
        ps.createProgramDefinition(
            "name-one",
            "description",
            "display name",
            "display description",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.COMMON_INTAKE_FORM,
            ImmutableList.of(),
            /* categoryIds= */ ImmutableList.of());

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateProgramDefinition(
            commonIntakeForm.getResult().id(),
            Locale.US,
            "a",
            "a",
            "a",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.DEFAULT,
            ImmutableList.of(),
            /* categories= */ ImmutableList.of());

    assertThat(result.hasResult()).isTrue();
    assertThat(result.isError()).isFalse();
    assertThat(result.getResult().programType()).isEqualTo(ProgramType.DEFAULT);
  }

  @Test
  public void updateProgram_clearsEligibilityConditionsWhenSettingCommonIntakeForm()
      throws Exception {
    QuestionDefinition question = nameQuestion;
    EligibilityDefinition eligibility =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.create(
                            question.getId(),
                            Scalar.FIRST_NAME,
                            Operator.EQUAL_TO,
                            PredicateValue.of(""))),
                    PredicateAction.HIDE_BLOCK))
            .build();
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withEligibilityDefinition(eligibility)
            .withBlock()
            .withEligibilityDefinition(eligibility)
            .buildDefinition();

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateProgramDefinition(
            program.id(),
            Locale.US,
            "a",
            "a",
            "a",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.COMMON_INTAKE_FORM,
            ImmutableList.of(),
            /* categories= */ ImmutableList.of());

    assertThat(result.hasResult()).isTrue();
    assertThat(result.isError()).isFalse();
    assertThat(result.getResult().programType()).isEqualTo(ProgramType.COMMON_INTAKE_FORM);
    assertThat(result.getResult().getBlockCount()).isEqualTo(2);
    assertThat(result.getResult().getBlockDefinitionByIndex(0).get().eligibilityDefinition())
        .isNotPresent();
    assertThat(result.getResult().getBlockDefinitionByIndex(1).get().eligibilityDefinition())
        .isNotPresent();
  }

  @Test
  public void updateProgram_doesNotClearEligibilityConditionsForDefaultProgram() throws Exception {
    QuestionDefinition question = nameQuestion;
    EligibilityDefinition eligibility =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.create(
                            question.getId(),
                            Scalar.FIRST_NAME,
                            Operator.EQUAL_TO,
                            PredicateValue.of(""))),
                    PredicateAction.HIDE_BLOCK))
            .build();
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withEligibilityDefinition(eligibility)
            .buildDefinition();

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateProgramDefinition(
            program.id(),
            Locale.US,
            "a",
            "a",
            "a",
            "",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* eligibilityIsGating= */ true,
            ProgramType.DEFAULT,
            ImmutableList.of(),
            /* categories= */ ImmutableList.of());

    assertThat(result.hasResult()).isTrue();
    assertThat(result.isError()).isFalse();
    assertThat(result.getResult().programType()).isEqualTo(ProgramType.DEFAULT);
    assertThat(result.getResult().getBlockCount()).isEqualTo(1);
    assertThat(result.getResult().getLastBlockDefinition().eligibilityDefinition()).isPresent();
  }

  @Test
  public void getProgramDefinition_throwsWhenProgramNotFound() {
    assertThatThrownBy(() -> ps.getFullProgramDefinition(1L))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining("Program not found for ID");
  }

  @Test
  public void getProgramDefinition_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();
    ps.addQuestionsToBlock(program.id(), 1L, ImmutableList.of(question.getId()));

    ProgramDefinition found = ps.getFullProgramDefinition(program.id());

    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void getProgramDefinitionAsync_getsDraftProgram() {
    ProgramDefinition programDefinition = ProgramBuilder.newDraftProgram().buildDefinition();

    CompletionStage<ProgramDefinition> found =
        ps.getFullProgramDefinitionAsync(programDefinition.id());

    assertThat(found.toCompletableFuture().join().adminName())
        .isEqualTo(programDefinition.adminName());
    assertThat(found.toCompletableFuture().join().id()).isEqualTo(programDefinition.id());
  }

  @Test
  public void getProgramDefinitionAsync_getsActiveProgram() {
    ProgramDefinition programDefinition = ProgramBuilder.newActiveProgram().buildDefinition();

    CompletionStage<ProgramDefinition> found =
        ps.getFullProgramDefinitionAsync(programDefinition.id());
    assertThat(found.toCompletableFuture().join().id()).isEqualTo(programDefinition.id());
  }

  @Test
  public void getProgramDefinitionAsync_cannotFindRequestedProgram_throwsException() {
    ProgramDefinition programDefinition = ProgramBuilder.newActiveProgram().buildDefinition();

    CompletionStage<ProgramDefinition> found =
        ps.getFullProgramDefinitionAsync(programDefinition.id() + 1);

    assertThatThrownBy(() -> found.toCompletableFuture().join())
        .isInstanceOf(CompletionException.class)
        .hasRootCauseInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining("Program not found for ID");
  }

  @Test
  public void getProgramDefinitionAsync_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestionDefinition(question)
            .buildDefinition();

    ProgramDefinition found =
        ps.getFullProgramDefinitionAsync(program.id()).toCompletableFuture().join();

    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void getProgramDefinitionAsync_setsCacheWithQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestionDefinition(question)
            .buildDefinition();

    ProgramDefinition found =
        ps.getFullProgramDefinitionAsync(program.id()).toCompletableFuture().join();
    ProgramDefinition cachedProgram =
        (ProgramDefinition) programDefCache.get(String.valueOf(program.id())).get();

    QuestionDefinition foundQuestion =
        cachedProgram
            .blockDefinitions()
            .get(0)
            .programQuestionDefinitions()
            .get(0)
            .getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
    assertThat(found.getQuestionIdsInProgram()).isEqualTo(cachedProgram.getQuestionIdsInProgram());
  }

  @Test
  public void getProgramDefinitionAsync_doesNotSetCacheForDraft() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(question)
            .buildDefinition();

    ProgramDefinition found =
        ps.getFullProgramDefinitionAsync(program.id()).toCompletableFuture().join();
    Optional<ProgramDefinition> maybeCachedProgram =
        programDefCache.get(String.valueOf(program.id()));

    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
    assertThat(maybeCachedProgram).isEmpty();
  }

  @Test
  public void getActiveProgramDefinitionAsync_getsActiveProgram() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("Test Program").buildDefinition();

    CompletionStage<ProgramDefinition> found =
        ps.getActiveFullProgramDefinitionAsync(programDefinition.slug());

    assertThat(found.toCompletableFuture().join().id()).isEqualTo(programDefinition.id());
  }

  @Test
  public void getActiveProgramDefinitionAsync_cannotFindRequestedProgram_throwsException() {
    ProgramBuilder.newActiveProgram("Test Program").buildDefinition();

    CompletionStage<ProgramDefinition> found =
        ps.getActiveFullProgramDefinitionAsync("non-existent-program");

    assertThatThrownBy(() -> found.toCompletableFuture().join())
        .isInstanceOf(CompletionException.class)
        .hasRootCauseInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining("Program not found for slug: non-existent-program");
  }

  @Test
  public void getDraftProgramDefinitionAsync_getsDraftProgram() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram("Test Program").buildDefinition();

    ProgramDefinition found = ps.getDraftFullProgramDefinition(programDefinition.slug());

    assertThat(found.id()).isEqualTo(programDefinition.id());
  }

  @Test
  public void getDraftProgramDefinitionAsync_cannotFindRequestedProgram_throwsException() {
    ProgramBuilder.newDraftProgram("Test Program").buildDefinition();

    assertThatThrownBy(() -> ps.getDraftFullProgramDefinition("non-existent-program"))
        .isInstanceOf(ProgramDraftNotFoundException.class)
        .hasMessageContaining("Program draft not found for slug: non-existent-program");
  }

  @Test
  public void addBlockToProgram_noProgram_throwsProgramNotFoundException() {
    assertThatThrownBy(() -> ps.addBlockToProgram(1L))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessage("Program not found for ID: 1");
  }

  @Test
  public void addBlockToProgram_emptyBlock_returnsProgramDefinitionWithBlock() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram().withBlock("Screen 1").buildDefinition();
    ErrorAnd<ProgramBlockAdditionResult, CiviFormError> result =
        ps.addBlockToProgram(programDefinition.id());

    assertThat(result.isError()).isFalse();
    assertThat(result.hasResult()).isTrue();
    ProgramDefinition updatedProgramDefinition = result.getResult().program();
    assertThat(result.getResult().maybeAddedBlock()).isNotEmpty();
    BlockDefinition addedBlock = result.getResult().maybeAddedBlock().get();

    ProgramDefinition found = ps.getFullProgramDefinition(programDefinition.id());

    assertThat(found.blockDefinitions()).hasSize(2);
    assertThat(found.blockDefinitions())
        .containsExactlyElementsOf(updatedProgramDefinition.blockDefinitions());

    BlockDefinition emptyBlock = found.blockDefinitions().get(0);
    assertThat(emptyBlock.name()).isEqualTo("Screen 1");
    assertThat(emptyBlock.description()).isEqualTo("");
    assertThat(emptyBlock.programQuestionDefinitions()).hasSize(0);

    BlockDefinition newBlock = found.blockDefinitions().get(1);
    assertThat(newBlock.id()).isEqualTo(addedBlock.id());
    assertThat(newBlock.name()).isEqualTo("Screen 2");
    assertThat(newBlock.description()).isEqualTo("Screen 2 description");
    assertThat(newBlock.programQuestionDefinitions()).hasSize(0);
  }

  @Test
  public void addBlockToProgram_returnsProgramDefinitionWithBlock() throws Exception {
    ProgramDefinition programDefinition = ProgramBuilder.newDraftProgram().buildDefinition();
    long programId = programDefinition.id();

    ErrorAnd<ProgramBlockAdditionResult, CiviFormError> result =
        ps.addBlockToProgram(programDefinition.id());

    assertThat(result.isError()).isFalse();
    assertThat(result.hasResult()).isTrue();
    ProgramDefinition updatedProgramDefinition = result.getResult().program();
    assertThat(result.getResult().maybeAddedBlock()).isNotEmpty();
    BlockDefinition addedBlock = result.getResult().maybeAddedBlock().get();

    ProgramDefinition found = ps.getFullProgramDefinition(programId);

    assertThat(found.blockDefinitions()).hasSize(2);
    assertThat(found.blockDefinitions())
        .containsExactlyElementsOf(updatedProgramDefinition.blockDefinitions());
    assertThat(found.blockDefinitions().get(1).id()).isEqualTo(addedBlock.id());
  }

  @Test
  public void addRepeatedBlockToProgram() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank.enumeratorApplicantHouseholdMembers())
            .withRepeatedBlock()
            .withRequiredQuestion(testQuestionBank.enumeratorNestedApplicantHouseholdMemberJobs())
            .withBlock()
            .withRequiredQuestion(testQuestionBank.textApplicantFavoriteColor())
            .build();

    ErrorAnd<ProgramBlockAdditionResult, CiviFormError> result =
        ps.addRepeatedBlockToProgram(program.id, 1L);

    assertThat(result.isError()).isFalse();
    assertThat(result.hasResult()).isTrue();
    ProgramDefinition updatedProgramDefinition = result.getResult().program();
    assertThat(result.getResult().maybeAddedBlock()).isNotEmpty();
    BlockDefinition addedBlock = result.getResult().maybeAddedBlock().get();

    ProgramDefinition found = ps.getFullProgramDefinition(program.id);

    assertThat(found.blockDefinitions()).hasSize(4);
    assertThat(found.getBlockDefinitionByIndex(0).get().isEnumerator()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(0).get().isRepeated()).isFalse();
    assertThat(found.getBlockDefinitionByIndex(0).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.enumeratorApplicantHouseholdMembers().getQuestionDefinition());

    assertThat(found.getBlockDefinitionByIndex(1).get().isEnumerator()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(1).get().isRepeated()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(1).get().enumeratorId()).contains(1L);
    assertThat(found.getBlockDefinitionByIndex(1).get().getQuestionDefinition(0))
        .isEqualTo(
            testQuestionBank
                .enumeratorNestedApplicantHouseholdMemberJobs()
                .getQuestionDefinition());

    // The newly added block.
    assertThat(found.getBlockDefinitionByIndex(2).get().isRepeated()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(2).get().enumeratorId()).contains(1L);
    assertThat(found.getBlockDefinitionByIndex(2).get().getQuestionCount()).isEqualTo(0);
    assertThat(found.getBlockDefinitionByIndex(2).get().id()).isEqualTo(addedBlock.id());

    assertThat(found.getBlockDefinitionByIndex(3).get().isRepeated()).isFalse();
    assertThat(found.getBlockDefinitionByIndex(3).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.textApplicantFavoriteColor().getQuestionDefinition());

    assertThat(found.blockDefinitions())
        .containsExactlyElementsOf(updatedProgramDefinition.blockDefinitions());
  }

  @Test
  public void addRepeatedBlockToProgram_toEndOfBlockList() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank.textApplicantFavoriteColor())
            .withBlock()
            .withRequiredQuestion(testQuestionBank.enumeratorApplicantHouseholdMembers())
            .withRepeatedBlock()
            .withRequiredQuestion(testQuestionBank.enumeratorNestedApplicantHouseholdMemberJobs())
            .build();

    ErrorAnd<ProgramBlockAdditionResult, CiviFormError> result =
        ps.addRepeatedBlockToProgram(program.id, 2L);

    assertThat(result.isError()).isFalse();
    assertThat(result.hasResult()).isTrue();
    ProgramDefinition updatedProgramDefinition = result.getResult().program();
    assertThat(result.getResult().maybeAddedBlock()).isNotEmpty();
    BlockDefinition addedBlock = result.getResult().maybeAddedBlock().get();

    ProgramDefinition found = ps.getFullProgramDefinition(program.id);

    assertThat(found.blockDefinitions()).hasSize(4);
    assertThat(found.getBlockDefinitionByIndex(0).get().isEnumerator()).isFalse();
    assertThat(found.getBlockDefinitionByIndex(0).get().isRepeated()).isFalse();
    assertThat(found.getBlockDefinitionByIndex(0).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.textApplicantFavoriteColor().getQuestionDefinition());

    assertThat(found.getBlockDefinitionByIndex(1).get().isEnumerator()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(1).get().isRepeated()).isFalse();
    assertThat(found.getBlockDefinitionByIndex(1).get().getQuestionDefinition(0))
        .isEqualTo(testQuestionBank.enumeratorApplicantHouseholdMembers().getQuestionDefinition());

    assertThat(found.getBlockDefinitionByIndex(2).get().isEnumerator()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(2).get().isRepeated()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(2).get().enumeratorId()).contains(2L);
    assertThat(found.getBlockDefinitionByIndex(2).get().getQuestionDefinition(0))
        .isEqualTo(
            testQuestionBank
                .enumeratorNestedApplicantHouseholdMemberJobs()
                .getQuestionDefinition());

    // The newly added block.
    assertThat(found.getBlockDefinitionByIndex(3).get().isRepeated()).isTrue();
    assertThat(found.getBlockDefinitionByIndex(3).get().enumeratorId()).contains(2L);
    assertThat(found.getBlockDefinitionByIndex(3).get().getQuestionCount()).isEqualTo(0);
    assertThat(found.blockDefinitions())
        .containsExactlyElementsOf(updatedProgramDefinition.blockDefinitions());
    assertThat(found.getBlockDefinitionByIndex(3).get().id()).isEqualTo(addedBlock.id());
  }

  @Test
  public void addRepeatedBlockToProgram_invalidProgramId_throwsProgramNotFoundException() {
    assertThatThrownBy(() -> ps.addRepeatedBlockToProgram(1L, 1L))
        .isInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void
      addRepeatedBlockToProgram_invalidEnumeratorId_throwsProgramBlockDefinitionNotFoundException() {
    ProgramModel program = ProgramBuilder.newActiveProgram().build();

    assertThatThrownBy(() -> ps.addRepeatedBlockToProgram(program.id, 5L))
        .isInstanceOf(ProgramBlockDefinitionNotFoundException.class);
  }

  @Test
  public void updateBlock_noProgram_throwsProgramNotFoundException() {
    assertThatThrownBy(() -> ps.updateBlock(1L, 1L, new BlockForm("block", "description")))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessage("Program not found for ID: 1");
  }

  @Test
  public void updateBlock_invalidBlock_returnsErrors() throws Exception {
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateBlock(program.id(), 1L, new BlockForm());

    // Returns the unmodified program definition.
    assertThat(result.hasResult()).isTrue();
    assertThat(result.getResult().adminName()).isEqualTo(program.adminName());
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors())
        .containsOnly(
            CiviFormError.of("screen name cannot be blank"),
            CiviFormError.of("screen description cannot be blank"));
  }

  @Test
  public void updateBlock() throws Exception {
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();
    BlockForm blockForm = new BlockForm();
    blockForm.setName("new screen name");
    blockForm.setDescription("new description");

    ErrorAnd<ProgramDefinition, CiviFormError> result = ps.updateBlock(program.id(), 1L, blockForm);
    assertThat(result.isError()).isFalse();
    assertThat(result.hasResult()).isTrue();

    ProgramDefinition found = ps.getFullProgramDefinition(program.id());

    assertThat(found.blockDefinitions()).hasSize(1);
    assertThat(found.getBlockDefinition(1L).name()).isEqualTo("new screen name");
    assertThat(found.getBlockDefinition(1L).description()).isEqualTo("new description");
  }

  @Test
  public void setBlockQuestions_updatesBlock() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition programDefinition = ProgramBuilder.newDraftProgram().buildDefinition();
    Long programId = programDefinition.id();

    ps.setBlockQuestions(
        programId,
        1L,
        ImmutableList.of(ProgramQuestionDefinition.create(question, Optional.of(programId))));
    ProgramDefinition found = ps.getFullProgramDefinition(programId);

    assertThat(found.blockDefinitions()).hasSize(1);

    BlockDefinition foundBlock = found.blockDefinitions().get(0);
    assertThat(foundBlock.programQuestionDefinitions()).hasSize(1);

    ProgramQuestionDefinition foundPqd =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0);
    assertThat(foundPqd.id()).isEqualTo(question.getId());
    assertThat(foundPqd.getQuestionDefinition()).isInstanceOf(NameQuestionDefinition.class);
    assertThat(foundPqd.getQuestionDefinition().getName()).isEqualTo("applicant name");
  }

  @Test
  public void setBlockQuestions_withBogusBlockId_throwsProgramBlockDefinitionNotFoundException() {
    ProgramDefinition p =
        ps.createProgramDefinition(
                "name",
                "description",
                "name",
                "description",
                "",
                "https://usa.gov",
                DisplayMode.PUBLIC.getValue(),
                ImmutableList.of(),
                /* eligibilityIsGating= */ true,
                ProgramType.DEFAULT,
                ImmutableList.of(),
                /* categoryIds= */ ImmutableList.of())
            .getResult();
    assertThatThrownBy(() -> ps.setBlockQuestions(p.id(), 100L, ImmutableList.of()))
        .isInstanceOf(ProgramBlockDefinitionNotFoundException.class)
        .hasMessage(
            String.format(
                "Block not found in Program (ID %d) for block definition ID 100", p.id()));
  }

  @Test
  public void setBlockQuestions_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition programDefinition = ProgramBuilder.newDraftProgram().buildDefinition();
    Long programId = programDefinition.id();

    ProgramDefinition found =
        ps.setBlockQuestions(
            programId,
            1L,
            ImmutableList.of(ProgramQuestionDefinition.create(question, Optional.of(programId))));
    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void setBlockQuestions_overwritesPredicateQuestion_throwsException() {
    QuestionDefinition question = nameQuestion;
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    question.getId(), Scalar.FIRST_NAME, Operator.EQUAL_TO, PredicateValue.of(""))),
            PredicateAction.HIDE_BLOCK);
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(question)
            .withBlock()
            .withVisibilityPredicate(predicate)
            .buildDefinition();

    // Overwriting the question in the first block invalidates the predicate in the second block.
    assertThatExceptionOfType(IllegalPredicateOrderingException.class)
        .isThrownBy(
            () ->
                ps.setBlockQuestions(
                    program.id(),
                    1L,
                    ImmutableList.of(
                        ProgramQuestionDefinition.create(
                            addressQuestion, Optional.of(program.id())))))
        .withMessage("This action would invalidate a block condition");
  }

  @Test
  public void addQuestionsToBlock_withDuplicatedQuestions_throwsCantAddQuestionToBlockException() {
    QuestionDefinition questionA = nameQuestion;

    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(questionA)
            .build();

    assertThatThrownBy(
            () -> ps.addQuestionsToBlock(program.id, 1L, ImmutableList.of(questionA.getId())))
        .isInstanceOf(CantAddQuestionToBlockException.class)
        .hasMessage(
            String.format(
                "Can't add question to the block. Error: DUPLICATE. Program ID %d, block ID %d,"
                    + " question ID %d",
                program.id, 1L, questionA.getId()));
  }

  @Test
  public void addQuestionsToBlock_addsQuestionsToTheBlock() throws Exception {
    QuestionDefinition questionA = nameQuestion;
    QuestionDefinition questionB = addressQuestion;

    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(questionA)
            .buildDefinition();

    program = ps.addQuestionsToBlock(program.id(), 1L, ImmutableList.of(questionB.getId()));

    assertThat(program.hasQuestion(questionA)).isTrue();
    assertThat(program.hasQuestion(questionB)).isTrue();
  }

  @Test
  public void removeQuestionsFromBlock_withoutQuestion_throwsQuestionNotFoundException()
      throws Exception {
    QuestionDefinition questionA = nameQuestion;
    ProgramModel program = ProgramBuilder.newDraftProgram().withBlock().build();

    assertThatThrownBy(
            () -> ps.removeQuestionsFromBlock(program.id, 1L, ImmutableList.of(questionA.getId())))
        .isInstanceOf(QuestionNotFoundException.class)
        .hasMessage(
            String.format(
                "Question (ID %d) not found in Program (ID %d)", questionA.getId(), program.id));
  }

  @Test
  public void removeQuestionsFromBlock_removesQuestionsFromTheBlock() throws Exception {
    QuestionDefinition questionA = nameQuestion;
    QuestionDefinition questionB = addressQuestion;

    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(questionA)
            .withRequiredQuestionDefinition(questionB)
            .buildDefinition();

    program = ps.removeQuestionsFromBlock(program.id(), 1L, ImmutableList.of(questionB.getId()));

    assertThat(program.hasQuestion(questionA)).isTrue();
    assertThat(program.hasQuestion(questionB)).isFalse();
  }

  @Test
  public void removeQuestionsFromBlock_invalidatesPredicate_throwsException() {
    QuestionDefinition question = nameQuestion;
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    question.getId(), Scalar.FIRST_NAME, Operator.EQUAL_TO, PredicateValue.of(""))),
            PredicateAction.HIDE_BLOCK);
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(question)
            .withBlock()
            .withVisibilityPredicate(predicate)
            .buildDefinition();

    assertThatExceptionOfType(IllegalPredicateOrderingException.class)
        .isThrownBy(
            () -> ps.removeQuestionsFromBlock(program.id(), 1L, ImmutableList.of(question.getId())))
        .withMessage("This action would invalidate a block condition");
  }

  @Test
  public void setBlockPredicate_updatesBlock() throws Exception {
    QuestionModel question = testQuestionBank.addressApplicantAddress();
    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(question)
            .withBlock()
            .build();

    var cityPredicate =
        PredicateExpressionNode.create(
            LeafOperationExpressionNode.create(
                question.id, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("")));
    var statePredicate =
        PredicateExpressionNode.create(
            LeafOperationExpressionNode.create(
                question.id, Scalar.STATE, Operator.EQUAL_TO, PredicateValue.of("")));
    var zipPredicate =
        PredicateExpressionNode.create(
            LeafOperationExpressionNode.create(
                question.id, Scalar.ZIP, Operator.EQUAL_TO, PredicateValue.of("")));
    // Exercise all node types.
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                OrNode.create(
                    ImmutableList.of(
                        cityPredicate,
                        PredicateExpressionNode.create(
                            AndNode.create(ImmutableList.of(statePredicate, zipPredicate)))))),
            PredicateAction.HIDE_BLOCK);

    ps.setBlockVisibilityPredicate(program.id, 2L, Optional.of(predicate));

    ProgramDefinition found = ps.getFullProgramDefinition(program.id);

    // Verify serialization and deserialization.
    assertThat(found.blockDefinitions().get(1).visibilityPredicate()).hasValue(predicate);
  }

  @Test
  public void setBlockPredicate_withBogusBlockId_throwsProgramBlockDefinitionNotFoundException() {
    ProgramDefinition p = ProgramBuilder.newDraftProgram().buildDefinition();
    assertThatThrownBy(
            () ->
                ps.setBlockVisibilityPredicate(
                    p.id(),
                    100L,
                    Optional.of(
                        PredicateDefinition.create(
                            PredicateExpressionNode.create(
                                LeafOperationExpressionNode.create(
                                    1L, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of(""))),
                            PredicateAction.HIDE_BLOCK))))
        .isInstanceOf(ProgramBlockDefinitionNotFoundException.class)
        .hasMessage(
            String.format(
                "Block not found in Program (ID %d) for block definition ID 100", p.id()));
  }

  @Test
  public void setBlockPredicate_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(question)
            .withBlock()
            .buildDefinition();
    Long programId = programDefinition.id();

    ProgramDefinition found =
        ps.setBlockVisibilityPredicate(
            programId,
            2L,
            Optional.of(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.create(
                            question.getId(),
                            Scalar.CITY,
                            Operator.EQUAL_TO,
                            PredicateValue.of(""))),
                    PredicateAction.HIDE_BLOCK)));

    QuestionDefinition foundQuestion =
        found.blockDefinitions().get(0).programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(foundQuestion).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void setBlockPredicate_illegalPredicate_throwsException() {
    QuestionDefinition question = nameQuestion;
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    question.getId(), Scalar.FIRST_NAME, Operator.EQUAL_TO, PredicateValue.of(""))),
            PredicateAction.HIDE_BLOCK);
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(addressQuestion)
            .withBlock()
            .buildDefinition();

    // This predicate depends on a question that doesn't exist in a prior block.
    assertThatExceptionOfType(IllegalPredicateOrderingException.class)
        .isThrownBy(() -> ps.setBlockVisibilityPredicate(program.id(), 2L, Optional.of(predicate)))
        .withMessage("This action would invalidate a block condition");
  }

  @Test
  public void setBlockEligibilityDefinition_updatesBlock() throws Exception {
    QuestionModel question = testQuestionBank.addressApplicantAddress();
    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(question)
            .withBlock()
            .build();

    var cityPredicate =
        PredicateExpressionNode.create(
            LeafOperationExpressionNode.create(
                question.id, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("")));
    var statePredicate =
        PredicateExpressionNode.create(
            LeafOperationExpressionNode.create(
                question.id, Scalar.STATE, Operator.EQUAL_TO, PredicateValue.of("")));
    var zipPredicate =
        PredicateExpressionNode.create(
            LeafOperationExpressionNode.create(
                question.id, Scalar.ZIP, Operator.EQUAL_TO, PredicateValue.of("")));
    // Exercise all node types.
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                OrNode.create(
                    ImmutableList.of(
                        cityPredicate,
                        PredicateExpressionNode.create(
                            AndNode.create(ImmutableList.of(statePredicate, zipPredicate)))))),
            PredicateAction.HIDE_BLOCK);

    ps.setBlockEligibilityDefinition(
        program.id,
        2L,
        Optional.of(EligibilityDefinition.builder().setPredicate(predicate).build()));

    ProgramDefinition found = ps.getFullProgramDefinition(program.id);

    // Verify serialization and deserialization.
    assertThat(found.blockDefinitions().get(1).eligibilityDefinition()).isPresent();
    assertThat(found.blockDefinitions().get(1).eligibilityDefinition().get().predicate())
        .isEqualTo(predicate);
  }

  @Test
  public void setBlockEligibilityDefinition_throwsEligibilityNotValidForProgramTypeException()
      throws Exception {
    QuestionDefinition question = nameQuestion;
    EligibilityDefinition eligibility =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.create(
                            question.getId(),
                            Scalar.FIRST_NAME,
                            Operator.EQUAL_TO,
                            PredicateValue.of(""))),
                    PredicateAction.HIDE_BLOCK))
            .build();
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withProgramType(ProgramType.COMMON_INTAKE_FORM)
            .withBlock()
            .withRequiredQuestionDefinition(question)
            .buildDefinition();

    assertThatExceptionOfType(EligibilityNotValidForProgramTypeException.class)
        .isThrownBy(
            () -> ps.setBlockEligibilityDefinition(program.id(), 1L, Optional.of(eligibility)))
        .withMessage("Eligibility conditions cannot be set for ProgramType COMMON_INTAKE_FORM");
  }

  @Test
  public void removeBlockPredicate() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(addressQuestion)
            .withBlock()
            .build();

    // First set the predicate and assert its presence.
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    addressQuestion.getId(),
                    Scalar.CITY,
                    Operator.EQUAL_TO,
                    PredicateValue.of(""))),
            PredicateAction.HIDE_BLOCK);
    ps.setBlockVisibilityPredicate(program.id, 2L, Optional.of(predicate));

    ProgramDefinition foundWithPredicate = ps.getFullProgramDefinition(program.id);
    assertThat(foundWithPredicate.blockDefinitions().get(1).visibilityPredicate())
        .hasValue(predicate);

    // Then remove that predicate and assert its absence.
    ps.removeBlockPredicate(program.id, 2L);

    ProgramDefinition foundWithoutPredicate = ps.getFullProgramDefinition(program.id);
    assertThat(foundWithoutPredicate.blockDefinitions().get(1).visibilityPredicate()).isEmpty();
  }

  @Test
  public void setProgramQuestionDefinitionOptionality() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(question)
            .buildDefinition();
    Long programId = programDefinition.id();

    assertThat(
            programDefinition
                .getBlockDefinitionByIndex(0)
                .get()
                .programQuestionDefinitions()
                .get(0)
                .optional())
        .isFalse();

    programDefinition =
        ps.setProgramQuestionDefinitionOptionality(programId, 1L, nameQuestion.getId(), true);
    assertThat(
            programDefinition
                .getBlockDefinitionByIndex(0)
                .get()
                .programQuestionDefinitions()
                .get(0)
                .optional())
        .isTrue();

    programDefinition =
        ps.setProgramQuestionDefinitionOptionality(programId, 1L, nameQuestion.getId(), false);
    assertThat(
            programDefinition
                .getBlockDefinitionByIndex(0)
                .get()
                .programQuestionDefinitions()
                .get(0)
                .optional())
        .isFalse();

    // Checking that there's no problem
    assertThatThrownBy(
            () ->
                ps.setProgramQuestionDefinitionOptionality(
                    programId, 1L, nameQuestion.getId() + 1, false))
        .isInstanceOf(ProgramQuestionDefinitionNotFoundException.class);
  }

  @Test
  public void setProgramQuestionDefinitionAddressCorrectionEnabled() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(addressQuestion)
            .withBlock()
            .withVisibilityPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.builder()
                            .setQuestionId(addressQuestion.getId())
                            .setScalar(Scalar.SERVICE_AREAS)
                            .setOperator(Operator.IN_SERVICE_AREA)
                            .setComparedValue(PredicateValue.serviceArea("seattle"))
                            .build()),
                    PredicateAction.HIDE_BLOCK))
            .buildDefinition();

    assertThat(
            ps.setProgramQuestionDefinitionAddressCorrectionEnabled(
                    programDefinition.id(), 1L, addressQuestion.getId(), true)
                .getBlockDefinitionByIndex(0)
                .get()
                .programQuestionDefinitions()
                .get(0)
                .addressCorrectionEnabled())
        .isTrue();

    assertThatThrownBy(
            () ->
                ps.setProgramQuestionDefinitionAddressCorrectionEnabled(
                    programDefinition.id(), 1L, addressQuestion.getId(), false))
        .isInstanceOf(BadRequestException.class);
  }

  private void assertQuestionsOrder(ProgramDefinition program, QuestionDefinition... expectedOrder)
      throws Exception {
    var expectedQuestionNames = Arrays.stream(expectedOrder).map(QuestionDefinition::getName);
    var actualQuestionNames =
        program.getLastBlockDefinition().programQuestionDefinitions().stream()
            .map(q -> q.getQuestionDefinition().getName());
    assertThat(actualQuestionNames)
        .containsExactlyElementsOf(expectedQuestionNames.collect(Collectors.toList()));
  }

  @Test
  public void setProgramQuestionDefinitionPosition() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(nameQuestion)
            .withRequiredQuestionDefinition(addressQuestion)
            .withRequiredQuestionDefinition(colorQuestion)
            .buildDefinition();
    BlockDefinition block = programDefinition.getLastBlockDefinition();

    // move address to the beginning
    programDefinition =
        ps.setProgramQuestionDefinitionPosition(
            programDefinition.id(), block.id(), addressQuestion.getId(), 0);
    assertQuestionsOrder(programDefinition, addressQuestion, nameQuestion, colorQuestion);

    // move address to the end
    programDefinition =
        ps.setProgramQuestionDefinitionPosition(
            programDefinition.id(), block.id(), addressQuestion.getId(), 2);
    assertQuestionsOrder(programDefinition, nameQuestion, colorQuestion, addressQuestion);

    // move name to itself (shouldn't change position)
    programDefinition =
        ps.setProgramQuestionDefinitionPosition(
            programDefinition.id(), block.id(), nameQuestion.getId(), 0);
    assertQuestionsOrder(programDefinition, nameQuestion, colorQuestion, addressQuestion);
  }

  @Test
  public void setProgramQuestionDefinitionPosition_invalidPosition() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(nameQuestion)
            .withRequiredQuestionDefinition(addressQuestion)
            .withRequiredQuestionDefinition(colorQuestion)
            .buildDefinition();
    BlockDefinition block = programDefinition.getLastBlockDefinition();

    assertThatThrownBy(
            () ->
                ps.setProgramQuestionDefinitionPosition(
                    programDefinition.id(), block.id(), addressQuestion.getId(), -1))
        .isInstanceOf(InvalidQuestionPositionException.class);
    assertThatThrownBy(
            () ->
                ps.setProgramQuestionDefinitionPosition(
                    programDefinition.id(), block.id(), addressQuestion.getId(), 3))
        .isInstanceOf(InvalidQuestionPositionException.class);
  }

  @Test
  public void deleteBlock_invalidProgram_throwsProgramNotfoundException() {
    assertThatThrownBy(() -> ps.deleteBlock(1L, 2L))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessage("Program not found for ID: 1");
  }

  @Test
  public void deleteBlock_lastBlock_throwsProgramNeedsABlockException() throws Exception {
    ProgramModel program = ProgramBuilder.newDraftProgram().build();

    assertThatThrownBy(() -> ps.deleteBlock(program.id, 1L))
        .isInstanceOf(ProgramNeedsABlockException.class);
  }

  @Test
  public void deleteBlock_removesEligibilityPredicateQuestion_throwsException() {
    QuestionDefinition question = nameQuestion;
    EligibilityDefinition eligibility =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.create(
                            question.getId(),
                            Scalar.FIRST_NAME,
                            Operator.EQUAL_TO,
                            PredicateValue.of(""))),
                    PredicateAction.HIDE_BLOCK))
            .build();
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(addressQuestion)
            .withBlock()
            .withEligibilityDefinition(eligibility)
            .buildDefinition();

    // This predicate depends on a question that doesn't exist in a prior block.
    assertThatExceptionOfType(IllegalPredicateOrderingException.class)
        .isThrownBy(() -> ps.deleteBlock(program.id(), 1L))
        .withMessage("This action would invalidate a block condition");
  }

  @Test
  public void deleteBlock_removesVisibilityPredicateQuestion_throwsException() {
    QuestionDefinition question = nameQuestion;
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    question.getId(), Scalar.FIRST_NAME, Operator.EQUAL_TO, PredicateValue.of(""))),
            PredicateAction.HIDE_BLOCK);
    ProgramDefinition program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestionDefinition(addressQuestion)
            .withBlock()
            .withVisibilityPredicate(predicate)
            .buildDefinition();

    // This predicate depends on a question that doesn't exist in a prior block.
    assertThatExceptionOfType(IllegalPredicateOrderingException.class)
        .isThrownBy(() -> ps.deleteBlock(program.id(), 1L))
        .withMessage("This action would invalidate a block condition");
  }

  @Test
  public void deleteBlock_constructsQuestionDefinitions() throws Exception {
    QuestionDefinition question = nameQuestion;
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram()
            .withBlock("screen one")
            .withRequiredQuestionDefinition(question)
            .withBlock()
            .buildDefinition();
    Long programId = programDefinition.id();

    ProgramDefinition result = ps.deleteBlock(programId, 2L);

    assertThat(result.blockDefinitions()).hasSize(1);

    BlockDefinition blockResult = result.blockDefinitions().get(0);
    assertThat(blockResult.name()).isEqualTo("screen one");
    assertThat(blockResult.programQuestionDefinitions()).hasSize(1);

    QuestionDefinition questionResult =
        blockResult.programQuestionDefinitions().get(0).getQuestionDefinition();
    assertThat(questionResult).isInstanceOf(NameQuestionDefinition.class);
  }

  @Test
  public void newProgramFromExisting() throws Exception {
    ProgramModel program = ProgramBuilder.newActiveProgram().build();
    program.save();

    ProgramDefinition newDraft = ps.newDraftOf(program.id);
    assertThat(newDraft.adminName()).isEqualTo(program.getProgramDefinition().adminName());
    assertThat(newDraft.blockDefinitions())
        .isEqualTo(program.getProgramDefinition().blockDefinitions());
    assertThat(newDraft.localizedDescription())
        .isEqualTo(program.getProgramDefinition().localizedDescription());
    assertThat(newDraft.id()).isNotEqualTo(program.id);

    ProgramDefinition secondNewDraft = ps.newDraftOf(program.id);
    assertThat(secondNewDraft.id()).isEqualTo(newDraft.id());
  }

  @Test
  public void updateLocalizations_blockTranslationsProvided() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("English name", "English description")
            .withLocalizedName(Locale.FRENCH, "existing French name")
            .withLocalizedDescription(Locale.FRENCH, "existing French description")
            .withLocalizedConfirmationMessage(Locale.FRENCH, "")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(
                    Locale.US,
                    "English image description",
                    Locale.FRENCH,
                    "existing French image description"))
            .withBlock("first block", "a description")
            .withBlock("second block", "another description")
            .build();

    LocalizationUpdate updateData =
        LocalizationUpdate.builder()
            .setLocalizedDisplayName("new French name")
            .setLocalizedDisplayDescription("new French description")
            .setLocalizedSummaryImageDescription("new French image description")
            .setLocalizedConfirmationMessage("")
            .setStatuses(ImmutableList.of())
            .setScreens(
                ImmutableList.of(
                    LocalizationUpdate.ScreenUpdate.builder()
                        .setBlockIdToUpdate(1L)
                        .setLocalizedName("a french screen name")
                        .setLocalizedDescription("a french description")
                        .build(),
                    LocalizationUpdate.ScreenUpdate.builder()
                        .setBlockIdToUpdate(2L)
                        .setLocalizedName("a second french screen name")
                        .setLocalizedDescription("another french description")
                        .build()))
            .build();
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateLocalization(program.id, Locale.FRENCH, updateData);

    assertThat(result.isError()).isFalse();
    ProgramDefinition definition = result.getResult();
    BlockDefinition firstBlock = definition.getBlockDefinition(1L);
    assertThat(firstBlock.localizedName().get(Locale.FRENCH)).isEqualTo("a french screen name");
    assertThat(firstBlock.localizedDescription().get(Locale.FRENCH))
        .isEqualTo("a french description");
    BlockDefinition secondBlock = definition.getBlockDefinition(2L);
    assertThat(secondBlock.localizedName().get(Locale.FRENCH))
        .isEqualTo("a second french screen name");
    assertThat(secondBlock.localizedDescription().get(Locale.FRENCH))
        .isEqualTo("another french description");
  }

  @Test
  public void updateLocalizations_blockTranslationsForNonexistantBlock() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("English name", "English description")
            .withLocalizedName(Locale.FRENCH, "existing French name")
            .withLocalizedDescription(Locale.FRENCH, "existing French description")
            .withLocalizedConfirmationMessage(Locale.FRENCH, "")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(
                    Locale.US,
                    "English image description",
                    Locale.FRENCH,
                    "existing French image description"))
            .withBlock("first block", "a description")
            .build();

    LocalizationUpdate updateData =
        LocalizationUpdate.builder()
            .setLocalizedDisplayName("new French name")
            .setLocalizedDisplayDescription("new French description")
            .setLocalizedSummaryImageDescription("new French image description")
            .setLocalizedConfirmationMessage("")
            .setStatuses(ImmutableList.of())
            .setScreens(
                ImmutableList.of(
                    LocalizationUpdate.ScreenUpdate.builder()
                        .setBlockIdToUpdate(3L)
                        .setLocalizedName("a second french screen name")
                        .setLocalizedDescription("another french description")
                        .build()))
            .build();
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateLocalization(program.id, Locale.FRENCH, updateData);

    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors()).containsExactly(CiviFormError.of("Found invalid block id 3"));
  }

  //  TODO: Issue 8109, re-enabled after transition period
  //  @Test
  //  public void updateLocalizations_blockTranslationsEmpty() throws Exception {
  //    ProgramModel program =
  //        ProgramBuilder.newDraftProgram("English name", "English description")
  //            .withLocalizedName(Locale.FRENCH, "existing French name")
  //            .withLocalizedDescription(Locale.FRENCH, "existing French description")
  //            .withLocalizedConfirmationMessage(Locale.FRENCH, "")
  //            .setLocalizedSummaryImageDescription(
  //                LocalizedStrings.of(
  //                    Locale.US,
  //                    "English image description",
  //                    Locale.FRENCH,
  //                    "existing French image description"))
  //            .withBlock("first block", "a description")
  //            .build();
  //
  //    LocalizationUpdate updateData =
  //        LocalizationUpdate.builder()
  //            .setLocalizedDisplayName("new French name")
  //            .setLocalizedDisplayDescription("new French description")
  //            .setLocalizedSummaryImageDescription("new French image description")
  //            .setLocalizedConfirmationMessage("")
  //            .setStatuses(ImmutableList.of())
  //            .setScreens(
  //                ImmutableList.of(
  //                    LocalizationUpdate.ScreenUpdate.builder()
  //                        .setBlockIdToUpdate(1L)
  //                        .setLocalizedName("")
  //                        .setLocalizedDescription("")
  //                        .build()))
  //            .build();
  //    ErrorAnd<ProgramDefinition, CiviFormError> result =
  //        ps.updateLocalization(program.id, Locale.FRENCH, updateData);
  //
  //    assertThat(result.isError()).isTrue();
  //    assertThat(result.getErrors())
  //        .containsExactly(
  //            CiviFormError.of("program screen-name-1 cannot be blank"),
  //            CiviFormError.of("program screen-description-1 cannot be blank"));
  //  }

  @Test
  public void updateLocalizations_returnsErrorMessages() throws Exception {
    ProgramModel program = ProgramBuilder.newDraftProgram().build();

    LocalizationUpdate updateData =
        LocalizationUpdate.builder()
            .setLocalizedDisplayName("")
            .setLocalizedDisplayDescription("")
            .setLocalizedConfirmationMessage("")
            .setStatuses(ImmutableList.of())
            .setScreens(ImmutableList.of())
            .build();
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateLocalization(program.id, Locale.FRENCH, updateData);

    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors())
        .containsExactly(
            CiviFormError.of("program display name cannot be blank"),
            CiviFormError.of("program display description cannot be blank"));
  }

  @Test
  public void updateLocalizations_programNotFound_throws() {
    LocalizationUpdate updateData =
        LocalizationUpdate.builder()
            .setLocalizedDisplayName("a name")
            .setLocalizedDisplayDescription("a description")
            .setLocalizedConfirmationMessage("")
            .setStatuses(ImmutableList.of())
            .setScreens(ImmutableList.of())
            .build();
    assertThatThrownBy(() -> ps.updateLocalization(1000L, Locale.FRENCH, updateData))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining("Program not found for ID: 1000");
  }

  @Test
  public void updateLocalizations_imageDescriptionProvidedWithNoImageConfigured_notUsed()
      throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("English name", "English description").build();

    LocalizationUpdate updateData =
        LocalizationUpdate.builder()
            .setLocalizedDisplayName("new French name")
            .setLocalizedDisplayDescription("new French description")
            .setLocalizedConfirmationMessage("")
            .setLocalizedSummaryImageDescription("invalid French image description")
            .setStatuses(ImmutableList.of())
            .setScreens(ImmutableList.of())
            .build();

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateLocalization(program.id, Locale.FRENCH, updateData);

    assertThat(result.isError()).isFalse();
    ProgramDefinition definition = result.getResult();
    // Verify we didn't save the French image description because we don't have any image
    // description.
    assertThat(definition.localizedSummaryImageDescription().isPresent()).isFalse();
  }

  @Test
  public void updateLocalizations_addsNewLocale() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.withDefaultValue("default image description"))
            .build();

    LocalizationUpdate updateData =
        LocalizationUpdate.builder()
            .setLocalizedDisplayName("German Name")
            .setLocalizedDisplayDescription("German Description")
            .setLocalizedConfirmationMessage("")
            .setLocalizedSummaryImageDescription("German Image Description")
            .setStatuses(ImmutableList.of())
            .setScreens(ImmutableList.of())
            .build();
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateLocalization(program.id, Locale.GERMAN, updateData);

    assertThat(result.isError()).isFalse();
    ProgramDefinition definition = result.getResult();
    assertThat(definition.localizedName().get(Locale.GERMAN)).isEqualTo("German Name");
    assertThat(definition.localizedDescription().get(Locale.GERMAN))
        .isEqualTo("German Description");
    assertThat(definition.localizedSummaryImageDescription().isPresent()).isTrue();
    assertThat(definition.localizedSummaryImageDescription().get().get(Locale.GERMAN))
        .isEqualTo("German Image Description");
  }

  @Test
  public void updateLocalizations_updatesExistingLocale() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("English name", "English description")
            .withLocalizedName(Locale.FRENCH, "existing French name")
            .withLocalizedDescription(Locale.FRENCH, "existing French description")
            .withLocalizedConfirmationMessage(Locale.FRENCH, "")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(
                    Locale.US,
                    "English image description",
                    Locale.FRENCH,
                    "existing French image description"))
            .build();

    LocalizationUpdate updateData =
        LocalizationUpdate.builder()
            .setLocalizedDisplayName("new French name")
            .setLocalizedDisplayDescription("new French description")
            .setLocalizedSummaryImageDescription("new French image description")
            .setLocalizedConfirmationMessage("")
            .setStatuses(ImmutableList.of())
            .setScreens(ImmutableList.of())
            .build();
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        ps.updateLocalization(program.id, Locale.FRENCH, updateData);

    assertThat(result.isError()).isFalse();
    ProgramDefinition definition = result.getResult();
    assertThat(definition.localizedName().get(Locale.FRENCH)).isEqualTo("new French name");
    assertThat(definition.localizedDescription().get(Locale.FRENCH))
        .isEqualTo("new French description");
    assertThat(definition.localizedSummaryImageDescription().isPresent()).isTrue();
    assertThat(definition.localizedSummaryImageDescription().get().get(Locale.FRENCH))
        .isEqualTo("new French image description");
  }

  @Test
  public void getNotificationEmailAddresses() {
    String programName = "administered program";
    ProgramModel program = resourceCreator.insertActiveProgram(programName);
    program.save();

    // If there are no admins (uncommon), return empty.
    assertThat(ps.getNotificationEmailAddresses(programName)).isEmpty();

    String globalAdminEmail = "global@admin";
    AccountModel globalAdmin = new AccountModel();
    globalAdmin.setEmailAddress(globalAdminEmail);
    globalAdmin.setGlobalAdmin(true);
    globalAdmin.save();

    // If there are no program admins, return global admins.
    assertThat(ps.getNotificationEmailAddresses(programName)).containsExactly(globalAdminEmail);

    String programAdminEmail = "program@admin";
    AccountModel programAdmin = new AccountModel();
    programAdmin.setEmailAddress(programAdminEmail);
    programAdmin.addAdministeredProgram(program.getProgramDefinition());
    programAdmin.save();

    // Return program admins when there are.
    assertThat(ps.getNotificationEmailAddresses(programName)).containsExactly(programAdminEmail);
  }

  @Test
  public void getProgramDefinitionAsync_reordersBlocksOnRead() throws Exception {
    long programId = ProgramBuilder.newActiveProgram().build().id;
    ImmutableList<BlockDefinition> unorderedBlockDefinitions =
        ImmutableList.<BlockDefinition>builder()
            .add(
                BlockDefinition.builder()
                    .setId(1L)
                    .setName("enumerator")
                    .setDescription("description")
                    .setLocalizedName(LocalizedStrings.withDefaultValue("enumerator"))
                    .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank
                                .enumeratorApplicantHouseholdMembers()
                                .getQuestionDefinition(),
                            Optional.of(programId)))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(2L)
                    .setName("top level")
                    .setDescription("description")
                    .setLocalizedName(LocalizedStrings.withDefaultValue("top level"))
                    .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank.emailApplicantEmail().getQuestionDefinition(),
                            Optional.of(programId)))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(3L)
                    .setName("nested enumerator")
                    .setDescription("description")
                    .setLocalizedName(LocalizedStrings.withDefaultValue("nested enumerator"))
                    .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
                    .setEnumeratorId(Optional.of(1L))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank
                                .enumeratorNestedApplicantHouseholdMemberJobs()
                                .getQuestionDefinition(),
                            Optional.of(programId)))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(4L)
                    .setName("repeated")
                    .setDescription("description")
                    .setLocalizedName(LocalizedStrings.withDefaultValue("repeated"))
                    .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
                    .setEnumeratorId(Optional.of(1L))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank
                                .nameRepeatedApplicantHouseholdMemberName()
                                .getQuestionDefinition(),
                            Optional.of(programId)))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(5L)
                    .setName("nested repeated")
                    .setDescription("description")
                    .setLocalizedName(LocalizedStrings.withDefaultValue("nested repeated"))
                    .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
                    .setEnumeratorId(Optional.of(3L))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank
                                .numberNestedRepeatedApplicantHouseholdMemberDaysWorked()
                                .getQuestionDefinition(),
                            Optional.of(programId)))
                    .build())
            .add(
                BlockDefinition.builder()
                    .setId(6L)
                    .setName("top level 2")
                    .setDescription("description")
                    .setLocalizedName(LocalizedStrings.withDefaultValue("top level 2"))
                    .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
                    .addQuestion(
                        ProgramQuestionDefinition.create(
                            testQuestionBank.nameApplicantName().getQuestionDefinition(),
                            Optional.of(programId)))
                    .build())
            .build();
    ObjectMapper mapper =
        new ObjectMapper().registerModule(new GuavaModule()).registerModule(new Jdk8Module());

    // Directly update the table with DB.sqlUpdate and execute. We can't save it through
    // the ebean model because the preupdate method will correct block ordering, and we
    // want to test that legacy block order is corrected on read.
    String updateString =
        String.format(
            "UPDATE programs SET block_definitions='%s' WHERE id=%d;",
            mapper.writeValueAsString(unorderedBlockDefinitions), programId);
    DB.sqlUpdate(updateString).execute();

    ProgramDefinition found =
        ps.getFullProgramDefinitionAsync(programId).toCompletableFuture().get();

    assertThat(found.hasOrderedBlockDefinitions()).isTrue();
  }

  @Test
  public void setProgramQuestionDefinitionAddressCorrectionEnabled_alreadyEnabled_throws()
      throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newDraftProgram()
            .withBlock("screen one")
            .withQuestionDefinition(addressQuestion, false)
            .withQuestionDefinition(secondaryAddressQuestion, false)
            .buildDefinition();

    Long programId = programDefinition.id();
    Long blockDefinitionId = programDefinition.getLastBlockDefinition().id();
    ps.setProgramQuestionDefinitionAddressCorrectionEnabled(
        programId, blockDefinitionId, addressQuestion.getId(), true);
    assertThatExceptionOfType(ProgramQuestionDefinitionInvalidException.class)
        .isThrownBy(
            () ->
                ps.setProgramQuestionDefinitionAddressCorrectionEnabled(
                    programId, blockDefinitionId, secondaryAddressQuestion.getId(), true));
  }

  @Test
  public void getCommonIntakeForm_ignoresObsoletePrograms() {
    // No common intake form in the most recent version of any program, although some programs
    // were previously marked as common intake.
    ProgramBuilder.newObsoleteProgram("one")
        .withProgramType(ProgramType.COMMON_INTAKE_FORM)
        .build();
    ProgramBuilder.newActiveProgram("two").withProgramType(ProgramType.COMMON_INTAKE_FORM).build();
    ProgramBuilder.newDraftProgram("two").withProgramType(ProgramType.DEFAULT).build();

    assertThat(ps.getCommonIntakeForm()).isNotPresent();
  }

  @Test
  public void getCommonIntakeForm_activeCommonIntake() {
    ProgramBuilder.newObsoleteProgram("one")
        .withProgramType(ProgramType.COMMON_INTAKE_FORM)
        .build();
    ProgramBuilder.newActiveProgram("two").withProgramType(ProgramType.COMMON_INTAKE_FORM).build();

    assertThat(ps.getCommonIntakeForm()).isPresent();
    assertThat(ps.getCommonIntakeForm().get().adminName()).isEqualTo("two");
  }

  @Test
  public void getCommonIntakeForm_draftCommonIntake() {
    ProgramBuilder.newObsoleteProgram("one")
        .withProgramType(ProgramType.COMMON_INTAKE_FORM)
        .build();
    ProgramBuilder.newActiveProgram("two").withProgramType(ProgramType.DEFAULT).build();
    ProgramBuilder.newDraftProgram("two").withProgramType(ProgramType.COMMON_INTAKE_FORM).build();

    assertThat(ps.getCommonIntakeForm()).isPresent();
    assertThat(ps.getCommonIntakeForm().get().adminName()).isEqualTo("two");
  }

  @Test
  public void setSummaryImageDescription_missingProgram_throws() {
    assertThatThrownBy(() -> ps.setSummaryImageDescription(Long.MAX_VALUE, Locale.US, "test"))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining("Program not found for ID:");
  }

  @Test
  public void setSummaryImageDescription_defaultLocale_createsNewStrings()
      throws ProgramNotFoundException, TranslationNotFoundException {
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();

    ProgramDefinition result =
        ps.setSummaryImageDescription(program.id(), DEFAULT_LOCALE, "fake description");

    assertThat(result.localizedSummaryImageDescription().isPresent()).isTrue();
    LocalizedStrings descriptions = result.localizedSummaryImageDescription().get();
    assertThat(descriptions.get(DEFAULT_LOCALE)).isEqualTo("fake description");
    assertThatThrownBy(() -> descriptions.get(Locale.ITALIAN))
        .isInstanceOf(TranslationNotFoundException.class);
  }

  @Test
  public void setSummaryImageDescription_additionalLocale_addsToExisting()
      throws ProgramNotFoundException, TranslationNotFoundException {
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();
    ps.setSummaryImageDescription(program.id(), Locale.US, "US description");

    ProgramDefinition result =
        ps.setSummaryImageDescription(program.id(), Locale.CANADA, "Canada description");

    assertThat(result.localizedSummaryImageDescription().isPresent()).isTrue();
    LocalizedStrings descriptions = result.localizedSummaryImageDescription().get();
    assertThat(descriptions.get(Locale.US)).isEqualTo("US description");
    assertThat(descriptions.get(Locale.CANADA)).isEqualTo("Canada description");
  }

  @Test
  public void setSummaryImageDescription_existingLocale_updates()
      throws ProgramNotFoundException, TranslationNotFoundException {
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();
    ps.setSummaryImageDescription(program.id(), Locale.US, "Old description");

    ProgramDefinition result =
        ps.setSummaryImageDescription(program.id(), Locale.US, "New description");

    assertThat(result.localizedSummaryImageDescription().isPresent()).isTrue();
    LocalizedStrings descriptions = result.localizedSummaryImageDescription().get();
    assertThat(descriptions.get(Locale.US)).isEqualTo("New description");
  }

  @Test
  public void setSummaryImageDescription_defaultLocaleAndBlank_removesAllTranslations()
      throws ProgramNotFoundException {
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();
    ps.setSummaryImageDescription(program.id(), Locale.US, "US description");
    ps.setSummaryImageDescription(program.id(), Locale.CANADA, "Canada description");
    ps.setSummaryImageDescription(program.id(), Locale.KOREAN, "Korean description");

    ProgramDefinition result = ps.setSummaryImageDescription(program.id(), DEFAULT_LOCALE, "");

    assertThat(result.localizedSummaryImageDescription().isPresent()).isFalse();
  }

  @Test
  public void setSummaryImageDescription_blank_hasImageFile_throwsNotRemovableException()
      throws ProgramNotFoundException {
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();
    ps.setSummaryImageDescription(program.id(), Locale.US, "US description");
    ps.setSummaryImageFileKey(program.id(), "fileKey.png");

    assertThatThrownBy(() -> ps.setSummaryImageDescription(program.id(), Locale.US, ""))
        .isInstanceOf(ImageDescriptionNotRemovableException.class)
        .hasMessageContaining("Description can't be removed because an image is present");
  }

  @Test
  public void setSummaryImageFileKey_missingProgram_throws() {
    assertThatThrownBy(() -> ps.setSummaryImageFileKey(Long.MAX_VALUE, "fileKey"))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining("Program not found for ID:");
  }

  @Test
  public void setSummaryImageFileKey_keySet() throws ProgramNotFoundException {
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();

    ProgramDefinition result1 = ps.setSummaryImageFileKey(program.id(), "fileKey1.png");

    assertThat(result1.summaryImageFileKey().isPresent()).isTrue();
    assertThat(result1.summaryImageFileKey().get()).isEqualTo("fileKey1.png");

    ProgramDefinition result2 = ps.setSummaryImageFileKey(program.id(), "fileKey2.png");

    assertThat(result2.summaryImageFileKey().isPresent()).isTrue();
    assertThat(result2.summaryImageFileKey().get()).isEqualTo("fileKey2.png");
  }

  @Test
  public void deleteSummaryImageFileKey_missingProgram_throws() {
    assertThatThrownBy(() -> ps.deleteSummaryImageFileKey(Long.MAX_VALUE))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining("Program not found for ID:");
  }

  @Test
  public void deleteSummaryImageFileKey_noFileKeyPresent_stillNoKey()
      throws ProgramNotFoundException {
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();

    ProgramDefinition result = ps.deleteSummaryImageFileKey(program.id());

    assertThat(result.summaryImageFileKey()).isEmpty();
  }

  @Test
  public void deleteSummaryImageFileKey_hadFileKey_keyRemoved() throws ProgramNotFoundException {
    ProgramDefinition program = ProgramBuilder.newDraftProgram().buildDefinition();
    ProgramDefinition setResult = ps.setSummaryImageFileKey(program.id(), "fileKey1.png");
    assertThat(setResult.summaryImageFileKey()).isPresent();

    ProgramDefinition deleteResult = ps.deleteSummaryImageFileKey(program.id());

    assertThat(deleteResult.summaryImageFileKey()).isEmpty();
  }
}
