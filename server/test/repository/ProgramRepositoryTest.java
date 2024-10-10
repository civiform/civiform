package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.util.Providers;
import io.ebean.DataIntegrityException;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationModel;
import models.DisplayMode;
import models.ProgramModel;
import models.QuestionModel;
import models.VersionModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import play.cache.NamedCacheImpl;
import play.cache.SyncCacheApi;
import play.inject.BindingKey;
import services.LocalizedStrings;
import services.PaginationResult;
import services.WellKnownPaths;
import services.applicant.ApplicantData;
import services.application.ApplicationEventDetails.StatusEvent;
import services.pagination.PageNumberPaginationSpec;
import services.pagination.RowIdPaginationSpec;
import services.pagination.SubmitTimePaginationSpec;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramType;
import services.question.QuestionAnswerer;
import services.settings.SettingsManifest;
import services.statuses.StatusDefinitions;
import support.CfTestHelpers;
import support.ProgramBuilder;
import support.TestQuestionBank;

@RunWith(JUnitParamsRunner.class)
public class ProgramRepositoryTest extends ResetPostgres {

  private ProgramRepository repo;
  private VersionRepository versionRepo;
  private SyncCacheApi programCache;
  private SyncCacheApi programDefCache;
  private SyncCacheApi versionsByProgramCache;
  private SettingsManifest mockSettingsManifest;
  private ApplicationStatusesRepository appRepo;
  private ApplicationEventRepository eventRepo;

  @Before
  public void setup() {
    versionRepo = instanceOf(VersionRepository.class);
    mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    programCache = instanceOf(SyncCacheApi.class);
    versionsByProgramCache = instanceOf(SyncCacheApi.class);
    appRepo = instanceOf(ApplicationStatusesRepository.class);
    eventRepo = instanceOf(ApplicationEventRepository.class);

    BindingKey<SyncCacheApi> programDefKey =
        new BindingKey<>(SyncCacheApi.class)
            .qualifiedWith(new NamedCacheImpl("full-program-definition"));
    programDefCache = instanceOf(programDefKey.asScala());

    repo =
        new ProgramRepository(
            instanceOf(DatabaseExecutionContext.class),
            Providers.of(versionRepo),
            mockSettingsManifest,
            programCache,
            programDefCache,
            versionsByProgramCache);
  }

  @Test
  public void lookupProgram_returnsEmptyOptionalWhenProgramNotFound() {
    Optional<ProgramModel> found = repo.lookupProgram(1L).toCompletableFuture().join();

    assertThat(found).isEmpty();
  }

  @Test
  public void lookupProgram_findsCorrectProgram() {
    resourceCreator.insertActiveProgram("one");
    ProgramModel two = resourceCreator.insertActiveProgram("two");

    Optional<ProgramModel> found = repo.lookupProgram(two.id).toCompletableFuture().join();

    assertThat(found).hasValue(two);
  }

  @Test
  public void lookupProgram_usesCacheWhenEnabled() {
    Mockito.when(mockSettingsManifest.getProgramCacheEnabled()).thenReturn(true);

    resourceCreator.insertActiveProgram("one");
    ProgramModel two = resourceCreator.insertActiveProgram("two");

    assertThat(programCache.get(String.valueOf(two.id))).isEmpty();

    Optional<ProgramModel> found = repo.lookupProgram(two.id).toCompletableFuture().join();

    assertThat(found).hasValue(two);
    assertThat(programCache.get(String.valueOf(two.id))).hasValue(found);
  }

  @Test
  public void setFullProgramDefinitionFromCache_doesNotSetWhenDraft() {
    Mockito.when(mockSettingsManifest.getQuestionCacheEnabled()).thenReturn(true);
    ProgramModel program = resourceCreator.insertDraftProgram("testDraftInCache");

    repo.setFullProgramDefinitionCache(program.id, program.getProgramDefinition());
    Optional<ProgramDefinition> programDefFromCache =
        repo.getFullProgramDefinitionFromCache(program);

    assertThat(programDefFromCache).isEmpty();
  }

  @Test
  public void setFullProgramDefinitionFromCache_doesNotSetWhenNullQuestionsIncluded() {
    Mockito.when(mockSettingsManifest.getQuestionCacheEnabled()).thenReturn(true);
    QuestionModel nullQuestion = new TestQuestionBank(false).nullQuestion();
    ProgramModel program =
        ProgramBuilder.newActiveProgram("programWithNullQuestion")
            .withBlock("Screen 1")
            .withRequiredQuestion(nullQuestion)
            .build();

    repo.setFullProgramDefinitionCache(program.id, program.getProgramDefinition());
    Optional<ProgramDefinition> programDefFromCache =
        repo.getFullProgramDefinitionFromCache(program);

    assertThat(
        program.getProgramDefinition().blockDefinitions().stream()
            .anyMatch(block -> block.hasNullQuestion()));
    assertThat(programDefFromCache).isEmpty();
  }

  @Test
  public void setFullProgramDefinitionFromCache_doesSetWhenNoNullQuestionsIncluded() {
    Mockito.when(mockSettingsManifest.getQuestionCacheEnabled()).thenReturn(true);
    QuestionModel question = resourceCreator.insertQuestion("testQuestion");
    ProgramModel program =
        ProgramBuilder.newActiveProgram("programWithQuestion")
            .withBlock("Screen 1")
            .withRequiredQuestion(question)
            .build();

    repo.setFullProgramDefinitionCache(program.id, program.getProgramDefinition());
    Optional<ProgramDefinition> programDefFromCache =
        repo.getFullProgramDefinitionFromCache(program);

    assertThat(
        program.getProgramDefinition().blockDefinitions().stream()
            .noneMatch(block -> block.hasNullQuestion()));
    assertThat(programDefFromCache).isNotEmpty();
  }

  @Test
  public void getFullProgramDefinitionFromCache_getsFromCacheWhenPresent() {
    Mockito.when(mockSettingsManifest.getQuestionCacheEnabled()).thenReturn(true);
    ProgramModel program = resourceCreator.insertActiveProgram("testInCache");
    repo.setFullProgramDefinitionCache(program.id, program.getProgramDefinition());

    Optional<ProgramDefinition> programDefFromCache =
        repo.getFullProgramDefinitionFromCache(program);

    assertThat(programDefFromCache).isPresent();
    assertThat(programDefFromCache.get().getQuestionIdsInProgram())
        .isEqualTo(program.getProgramDefinition().getQuestionIdsInProgram());
  }

  @Test
  public void getFullProgramDefinitionFromCache_returnsEmptyOptionalWhenNotPresent() {
    Mockito.when(mockSettingsManifest.getQuestionCacheEnabled()).thenReturn(true);
    ProgramModel program = resourceCreator.insertActiveProgram("testNotInCache");

    // We don't set the cache, but we try to get it here.
    Optional<ProgramDefinition> programDefFromCache =
        repo.getFullProgramDefinitionFromCache(program);

    assertThat(programDefFromCache).isEmpty();
  }

  @Test
  public void getFullProgramDefinitionFromCache_returnsEmptyOptionalWhenCacheDisabled() {
    Mockito.when(mockSettingsManifest.getQuestionCacheEnabled()).thenReturn(false);
    ProgramModel program = resourceCreator.insertActiveProgram("testCacheDisabled");
    repo.setFullProgramDefinitionCache(program.id, program.getProgramDefinition());

    Optional<ProgramDefinition> programDefFromCache =
        repo.getFullProgramDefinitionFromCache(program);

    assertThat(programDefFromCache).isEmpty();
  }

  @Test
  public void getForSlug_findsCorrectProgram() {
    ProgramModel program = resourceCreator.insertActiveProgram("Something With A Name");

    ProgramModel found =
        repo.getActiveProgramFromSlug("something-with-a-name").toCompletableFuture().join();

    assertThat(found).isEqualTo(program);
  }

  /* This test is meant to exercise the database trigger defined in server/conf/evolutions/default/56.sql */
  @Test
  public void insertingDuplicateDraftPrograms_raisesDatabaseException() throws Exception {
    var versionRepo = instanceOf(VersionRepository.class);
    var draftVersion = versionRepo.getDraftVersionOrCreate();

    ProgramModel program = resourceCreator.insertActiveProgram("test");
    assertThat(program.id).isNotNull();

    var draftOne =
        new ProgramModel(
            "test-program",
            "desc",
            "test-program",
            "description",
            "",
            "",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            ImmutableList.of(),
            draftVersion,
            ProgramType.DEFAULT,
            /* eligibilityIsGating= */ true,
            new ProgramAcls(),
            /* categories= */ ImmutableList.of());
    draftOne.save();

    var draftTwo =
        new ProgramModel(
            "test-program",
            "desc",
            "test-program",
            "description",
            "",
            "",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            ImmutableList.of(),
            draftVersion,
            ProgramType.DEFAULT,
            /* eligibilityIsGating= */ true,
            new ProgramAcls(),
            /* categories= */ ImmutableList.of());

    var throwableAssert = assertThatThrownBy(() -> draftTwo.save());
    throwableAssert.hasMessageContaining("Program test-program already has a draft!");
    throwableAssert.isExactlyInstanceOf(DataIntegrityException.class);
  }

  @Test
  public void insertProgramSync() throws Exception {
    ProgramModel program =
        new ProgramModel(
            "ProgramRepository",
            "desc",
            "name",
            "description",
            "",
            "",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            ImmutableList.of(),
            versionRepo.getDraftVersionOrCreate(),
            ProgramType.DEFAULT,
            /* eligibilityIsGating= */ true,
            new ProgramAcls(),
            /* categories= */ ImmutableList.of());
    ProgramModel withId = repo.insertProgramSync(program);

    ProgramModel found = repo.lookupProgram(withId.id).toCompletableFuture().join().get();
    assertThat(found.getProgramDefinition().localizedName().get(Locale.US)).isEqualTo("name");
  }

  @Test
  public void updateProgramSync() {
    ProgramModel existing = resourceCreator.insertActiveProgram("old name");
    ProgramModel updates =
        new ProgramModel(
            existing.getProgramDefinition().toBuilder()
                .setLocalizedName(LocalizedStrings.of(Locale.US, "new name"))
                .build());

    ProgramModel updated = repo.updateProgramSync(updates);

    assertThat(updated.id).isEqualTo(existing.id);
    assertThat(updated.getProgramDefinition().localizedName())
        .isEqualTo(LocalizedStrings.of(Locale.US, "new name"));
  }

  @Test
  public void getAllProgramNames() {
    resourceCreator.insertActiveProgram("old name");
    resourceCreator.insertDraftProgram("old name");
    resourceCreator.insertDraftProgram("new name");

    ImmutableSet<String> result = repo.getAllProgramNames();

    assertThat(result).isEqualTo(ImmutableSet.of("old name", "new name"));
  }

  @Test
  public void getVersionsForProgram() {
    ProgramModel program = resourceCreator.insertActiveProgram("old name");

    ImmutableList<VersionModel> versions = repo.getVersionsForProgram(program);

    assertThat(versions).hasSize(1);
  }

  @Test
  public void getVersionsForProgram_usesCacheWhenEnabled() {
    Mockito.when(mockSettingsManifest.getProgramCacheEnabled()).thenReturn(true);
    ProgramModel program = resourceCreator.insertActiveProgram("old name");

    ImmutableList<VersionModel> versions = repo.getVersionsForProgram(program);

    assertThat(versionsByProgramCache.get(String.valueOf(program.id)).get()).isEqualTo(versions);
  }

  @Test
  public void returnsAllAdmins() throws ProgramNotFoundException {
    ProgramModel withAdmins = resourceCreator.insertActiveProgram("with admins");
    AccountModel admin = new AccountModel();
    admin.save();
    assertThat(repo.getProgramAdministrators(withAdmins.id)).isEmpty();
    admin.addAdministeredProgram(withAdmins.getProgramDefinition());
    admin.save();
    assertThat(repo.getProgramAdministrators(withAdmins.id)).containsExactly(admin);

    // This draft, despite not existing when the admin association happened, should
    // still have the same admin associated.
    ProgramModel newDraft = repo.createOrUpdateDraft(withAdmins);
    assertThat(repo.getProgramAdministrators(newDraft.id)).containsExactly(admin);
  }

  @Test
  public void getApplicationsForAllProgramVersions_searchById() {
    ProgramModel program = resourceCreator.insertActiveProgram("test program");

    ApplicantModel bob = resourceCreator.insertApplicantWithAccount(Optional.of("bob@example.com"));
    ApplicationModel bobApp =
        makeApplicationWithName(bob, program, "Bob", "MiddleName", "Doe", "Suffix");
    ApplicantModel jane =
        resourceCreator.insertApplicantWithAccount(Optional.of("jane@example.com"));
    makeApplicationWithName(jane, program, "Jane", "MiddleName", "Doe", "Suffix");

    PaginationResult<ApplicationModel> paginationResult =
        repo.getApplicationsForAllProgramVersions(
            program.id,
            RowIdPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.builder()
                .setSearchNameFragment(Optional.of(bobApp.id.toString()))
                .setSubmitTimeFilter(TimeFilter.EMPTY)
                .build());

    assertThat(
            paginationResult.getPageContents().stream()
                .map(a -> a.getApplicant().getAccount().getEmailAddress())
                .collect(ImmutableSet.toImmutableSet()))
        .containsExactly("bob@example.com");
    assertThat(paginationResult.getNumPages()).isEqualTo(1);
  }

  private static ImmutableList<Object[]> getSearchByNameOrEmailData() {
    // Assumes that the test has been seeded with three applications:
    // 1. bob@example.com - Bob Doe
    // 2. jane@example.com - Jane Doe
    // 3. chris@exAMPLE.com - Chris Person
    return ImmutableList.<Object[]>of(
        new Object[] {"Bob Doe", ImmutableSet.of("bob@example.com")},
        new Object[] {"Doe Bob", ImmutableSet.of("bob@example.com")},
        new Object[] {"Doe, Bob", ImmutableSet.of("bob@example.com")},
        new Object[] {"Doe", ImmutableSet.of("bob@example.com", "jane@example.com")},
        new Object[] {"Bob", ImmutableSet.of("bob@example.com")},
        new Object[] {"Person", ImmutableSet.of("chris@exAMPLE.com")},
        new Object[] {"Other Person", ImmutableSet.of()},

        // Searching by applicant email or TI email returns the application
        new Object[] {"bob@example.com", ImmutableSet.of("bob@example.com")},
        new Object[] {"bobs_ti@example.com", ImmutableSet.of("bob@example.com")},

        // Searching by partial email returns the application
        new Object[] {
          "example", ImmutableSet.of("bob@example.com", "jane@example.com", "chris@exAMPLE.com")
        },
        new Object[] {"bobs_ti", ImmutableSet.of("bob@example.com")},

        // Case insensitive search.
        new Object[] {"bOb dOe", ImmutableSet.of("bob@example.com")},
        new Object[] {"CHRIS@example.com", ImmutableSet.of("chris@exAMPLE.com")},

        // Leading and trailing whitespace is ignored.
        new Object[] {"    Bob Doe    ", ImmutableSet.of("bob@example.com")},

        // Degenerate cases.
        // Email isn't found.
        new Object[] {"fake@example.com", ImmutableSet.of()},
        // Only match a single space between first and last name.
        new Object[] {"Bob  Doe", ImmutableSet.of()});
  }

  // TODO (#5503): Remove this test when we remove the feature flag
  @Test
  @Parameters(method = "getSearchByNameOrEmailData")
  public void getApplicationsForAllProgramVersions_searchByNameOrEmailUsingWellKnownPaths(
      String searchFragment, ImmutableSet<String> wantEmails) {
    ProgramModel program = resourceCreator.insertActiveProgram("test program");

    ApplicantModel bob = resourceCreator.insertApplicantWithAccount(Optional.of("bob@example.com"));
    makeApplicationWithName(bob, program, "Bob", "MiddleName", "Doe", "Suffix")
        .setSubmitterEmail("bobs_ti@example.com")
        .save();
    ApplicantModel jane =
        resourceCreator.insertApplicantWithAccount(Optional.of("jane@example.com"));
    makeApplicationWithName(jane, program, "Jane", "MiddleName", "Doe", "Suffix");
    // Note: The mixed casing on the email is intentional for tests of case insensitivity.
    ApplicantModel chris =
        resourceCreator.insertApplicantWithAccount(Optional.of("chris@exAMPLE.com"));
    makeApplicationWithName(chris, program, "Chris", "MiddleName", "Person", "Suffix");

    ApplicantModel otherApplicant =
        resourceCreator.insertApplicantWithAccount(Optional.of("other@example.com"));
    resourceCreator.insertDraftApplication(otherApplicant, program);

    PaginationResult<ApplicationModel> paginationResult =
        repo.getApplicationsForAllProgramVersions(
            program.id,
            RowIdPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.builder()
                .setSearchNameFragment(Optional.of(searchFragment))
                .setSubmitTimeFilter(TimeFilter.EMPTY)
                .build());

    assertThat(
            paginationResult.getPageContents().stream()
                .map(a -> a.getApplicant().getAccount().getEmailAddress())
                .collect(ImmutableSet.toImmutableSet()))
        .isEqualTo(wantEmails);
    assertThat(paginationResult.getNumPages()).isEqualTo(wantEmails.isEmpty() ? 0 : 1);
  }

  @Test
  public void getApplicationsForAllProgramVersions_searchesByNameEmailPhone() {
    Mockito.when(mockSettingsManifest.getPrimaryApplicantInfoQuestionsEnabled()).thenReturn(true);

    ProgramModel program = resourceCreator.insertActiveProgram("test program");

    String emailOne = "one@email.com";
    String emailTwo = "two@email.com";
    makeApplicantWithAccountAndApplication("OneFirst", "OneLast", emailOne, "1234567890", program);
    makeApplicantWithAccountAndApplication("TwoFirst", "TwoLast", emailTwo, "0987654321", program);

    // should only return the applicant with first name "OneFirst"
    PaginationResult<ApplicationModel> paginationResultOne =
        repo.getApplicationsForAllProgramVersions(
            program.id,
            RowIdPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.builder()
                .setSearchNameFragment(Optional.of("One"))
                .setSubmitTimeFilter(TimeFilter.EMPTY)
                .build());

    assertThat(
            paginationResultOne.getPageContents().stream()
                .map(a -> a.getApplicant().getEmailAddress().get())
                .collect(ImmutableSet.toImmutableSet()))
        .containsExactly(emailOne);

    // should return both applicants with "Last" in their last names
    PaginationResult<ApplicationModel> paginationResultTwo =
        repo.getApplicationsForAllProgramVersions(
            program.id,
            RowIdPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.builder()
                .setSearchNameFragment(Optional.of("Last"))
                .setSubmitTimeFilter(TimeFilter.EMPTY)
                .build());

    assertThat(
            paginationResultTwo.getPageContents().stream()
                .map(a -> a.getApplicant().getEmailAddress().get())
                .collect(ImmutableSet.toImmutableSet()))
        .isEqualTo(ImmutableSet.of(emailOne, emailTwo));

    // should only return the applicant with email = "two@email.com"
    PaginationResult<ApplicationModel> paginationResultThree =
        repo.getApplicationsForAllProgramVersions(
            program.id,
            RowIdPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.builder()
                .setSearchNameFragment(Optional.of(emailTwo))
                .setSubmitTimeFilter(TimeFilter.EMPTY)
                .build());

    assertThat(
            paginationResultThree.getPageContents().stream()
                .map(a -> a.getApplicant().getEmailAddress().get())
                .collect(ImmutableSet.toImmutableSet()))
        .containsExactly(emailTwo);

    // should only return the applicant whose phone number contains "1234"
    PaginationResult<ApplicationModel> paginationResultFour =
        repo.getApplicationsForAllProgramVersions(
            program.id,
            RowIdPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.builder()
                .setSearchNameFragment(Optional.of("1234"))
                .setSubmitTimeFilter(TimeFilter.EMPTY)
                .build());

    assertThat(
            paginationResultFour.getPageContents().stream()
                .map(a -> a.getApplicant().getEmailAddress().get())
                .collect(ImmutableSet.toImmutableSet()))
        .containsExactly(emailOne);

    // special characters (including spaces) in phone numbers are ignored in search
    PaginationResult<ApplicationModel> paginationResultFive =
        repo.getApplicationsForAllProgramVersions(
            program.id,
            RowIdPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            SubmittedApplicationFilter.builder()
                .setSearchNameFragment(Optional.of("(1.23)- 456"))
                .setSubmitTimeFilter(TimeFilter.EMPTY)
                .build());

    assertThat(
            paginationResultFive.getPageContents().stream()
                .map(a -> a.getApplicant().getEmailAddress().get())
                .collect(ImmutableSet.toImmutableSet()))
        .containsExactly(emailOne);
  }

  private void makeApplicantWithAccountAndApplication(
      String firstName, String lastName, String email, String phoneNumber, ProgramModel program) {

    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount(Optional.of(email));

    applicant.setFirstName(firstName);
    applicant.setLastName(lastName);
    applicant.setEmailAddress(email);
    applicant.setPhoneNumber(phoneNumber);
    applicant.save();

    resourceCreator.insertActiveApplication(applicant, program);
  }

  // TODO (#5503): Remove this when we remove the PRIMARY_APPLICANT_INFO_QUESTIONS_ENABLED feature
  // flag
  private ApplicationModel makeApplicationWithName(
      ApplicantModel applicant,
      ProgramModel program,
      String firstName,
      String middleName,
      String lastName,
      String suffix) {
    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);
    ApplicantData applicantData = application.getApplicantData();
    QuestionAnswerer.answerNameQuestion(
        applicantData, WellKnownPaths.APPLICANT_NAME, firstName, middleName, lastName, suffix);
    application.setApplicantData(applicantData);
    application.save();
    return application;
  }

  private static final StatusDefinitions.Status FIRST_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("First status")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("First status"))
          .build();

  private static final StatusDefinitions.Status SECOND_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Second status")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Second status"))
          .build();

  private static final StatusDefinitions.Status THIRD_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Third status")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Third status"))
          .build();

  @Test
  public void getApplicationsForAllProgramVersions_filterByStatus() throws Exception {
    ProgramModel program = ProgramBuilder.newActiveProgram("test program", "description").build();
    appRepo.createOrUpdateStatusDefinitions(
        program.getProgramDefinition().adminName(),
        new StatusDefinitions(ImmutableList.of(FIRST_STATUS, SECOND_STATUS, THIRD_STATUS)));

    AccountModel adminAccount = resourceCreator.insertAccountWithEmail("admin@example.com");

    ApplicationModel firstStatusApplication =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    createStatusEvents(
        adminAccount, firstStatusApplication, ImmutableList.of(Optional.of(FIRST_STATUS)));

    ApplicationModel secondStatusApplication =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    createStatusEvents(
        adminAccount, secondStatusApplication, ImmutableList.of(Optional.of(SECOND_STATUS)));

    ApplicationModel thirdStatusApplication =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    // Create a few status events before-hand to ensure that the latest status is used.
    createStatusEvents(
        adminAccount,
        thirdStatusApplication,
        ImmutableList.of(
            Optional.of(FIRST_STATUS), Optional.of(SECOND_STATUS), Optional.of(THIRD_STATUS)));

    ApplicationModel noStatusApplication =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);

    ApplicationModel backToNoStatusApplication =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    // Application has transitioned through statuses and arrived back at an unset status.
    createStatusEvents(
        adminAccount,
        backToNoStatusApplication,
        ImmutableList.of(Optional.of(SECOND_STATUS), Optional.empty()));

    // Unspecified status returns all results.
    assertThat(applicationIdsForProgramAndFilter(program, SubmittedApplicationFilter.EMPTY))
        .isEqualTo(
            ImmutableSet.of(
                firstStatusApplication.id,
                secondStatusApplication.id,
                thirdStatusApplication.id,
                noStatusApplication.id,
                backToNoStatusApplication.id));

    // Empty status returns all results.
    assertThat(
            applicationIdsForProgramAndFilter(
                program,
                SubmittedApplicationFilter.builder()
                    .setApplicationStatus(Optional.of(""))
                    .setSubmitTimeFilter(TimeFilter.EMPTY)
                    .build()))
        .isEqualTo(
            ImmutableSet.of(
                firstStatusApplication.id,
                secondStatusApplication.id,
                thirdStatusApplication.id,
                noStatusApplication.id,
                backToNoStatusApplication.id));

    // Specific status returns only statuses with that result.
    assertThat(
            applicationIdsForProgramAndFilter(
                program,
                SubmittedApplicationFilter.builder()
                    .setApplicationStatus(Optional.of(SECOND_STATUS.statusText()))
                    .setSubmitTimeFilter(TimeFilter.EMPTY)
                    .build()))
        .isEqualTo(ImmutableSet.of(secondStatusApplication.id));

    // Specifies only with no status set.
    assertThat(
            applicationIdsForProgramAndFilter(
                program,
                SubmittedApplicationFilter.builder()
                    .setApplicationStatus(
                        Optional.of(SubmittedApplicationFilter.NO_STATUS_FILTERS_OPTION_UUID))
                    .setSubmitTimeFilter(TimeFilter.EMPTY)
                    .build()))
        .isEqualTo(ImmutableSet.of(noStatusApplication.id, backToNoStatusApplication.id));

    // Unrecognized status option.
    assertThat(
            applicationIdsForProgramAndFilter(
                program,
                SubmittedApplicationFilter.builder()
                    .setApplicationStatus(Optional.of("some-random-status"))
                    .setSubmitTimeFilter(TimeFilter.EMPTY)
                    .build()))
        .isEqualTo(ImmutableSet.of());
  }

  private ImmutableSet<Long> applicationIdsForProgramAndFilter(
      ProgramModel program, SubmittedApplicationFilter filter) {
    PaginationResult<ApplicationModel> result =
        repo.getApplicationsForAllProgramVersions(
            program.id, RowIdPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC, filter);
    assertThat(result.hasMorePages()).isEqualTo(false);
    return result.getPageContents().stream()
        .map(app -> app.id)
        .collect(ImmutableSet.toImmutableSet());
  }

  private void createStatusEvents(
      AccountModel actorAccount,
      ApplicationModel application,
      ImmutableList<Optional<StatusDefinitions.Status>> statuses)
      throws InterruptedException {
    for (Optional<StatusDefinitions.Status> status : statuses) {
      String statusText = status.map(StatusDefinitions.Status::statusText).orElse("");
      eventRepo
          .insertStatusEvent(
              application,
              Optional.of(actorAccount),
              StatusEvent.builder().setStatusText(statusText).setEmailSent(true).build())
          .toCompletableFuture()
          .join();

      // When persisting models with @WhenModified fields, EBean
      // truncates the persisted timestamp to milliseconds:
      // https://github.com/seattle-uat/civiform/pull/2499#issuecomment-1133325484.
      // Sleep for a few milliseconds to ensure that a subsequent
      // update would have a distinct timestamp.
      TimeUnit.MILLISECONDS.sleep(5);
    }
  }

  @Test
  public void getApplicationsForAllProgramVersions_withDateRange() {
    ProgramModel program = resourceCreator.insertActiveProgram("test program");

    ApplicantModel applicantTwo =
        resourceCreator.insertApplicantWithAccount(Optional.of("two@example.com"));
    ApplicantModel applicantThree =
        resourceCreator.insertApplicantWithAccount(Optional.of("three@example.com"));
    ApplicantModel applicantOne =
        resourceCreator.insertApplicantWithAccount(Optional.of("one@example.com"));

    var applicationOne = resourceCreator.insertActiveApplication(applicantOne, program);
    CfTestHelpers.withMockedInstantNow(
        "2022-01-01T00:00:00Z",
        () -> {
          applicationOne.setSubmitTimeToNow();
          applicationOne.save();
        });

    var applicationTwo = resourceCreator.insertActiveApplication(applicantTwo, program);
    CfTestHelpers.withMockedInstantNow(
        "2022-02-01T00:00:00Z",
        () -> {
          applicationTwo.setSubmitTimeToNow();
          applicationTwo.save();
        });

    var applicationThree = resourceCreator.insertActiveApplication(applicantThree, program);
    CfTestHelpers.withMockedInstantNow(
        "2022-03-01T00:00:00Z",
        () -> {
          applicationThree.setSubmitTimeToNow();
          applicationThree.save();
        });

    PaginationResult<ApplicationModel> paginationResult =
        repo.getApplicationsForAllProgramVersions(
            program.id,
            new PageNumberPaginationSpec(
                /* pageSize= */ 10, PageNumberPaginationSpec.OrderByEnum.SUBMIT_TIME),
            SubmittedApplicationFilter.builder()
                .setSubmitTimeFilter(
                    TimeFilter.builder()
                        .setFromTime(Optional.of(Instant.parse("2022-01-25T00:00:00Z")))
                        .setUntilTime(Optional.of(Instant.parse("2022-02-10T00:00:00Z")))
                        .build())
                .build());

    assertThat(paginationResult.hasMorePages()).isFalse();
    assertThat(
            paginationResult.getPageContents().stream()
                .map(a -> a.id)
                .collect(ImmutableList.toImmutableList()))
        .isEqualTo(ImmutableList.of(applicationTwo.id));
  }

  @Test
  public void getApplicationsForAllProgramVersions_sortedBySubmitDate() {
    ApplicantModel applicantOne =
        resourceCreator.insertApplicantWithAccount(Optional.of("one@example.com"));
    ProgramModel originalVersion = resourceCreator.insertActiveProgram("test program");

    ApplicationModel applicationOne =
        resourceCreator.insertActiveApplication(applicantOne, originalVersion);

    ProgramModel nextVersion = resourceCreator.insertDraftProgram("test program");
    resourceCreator.publishNewSynchronizedVersion();

    ApplicantModel applicantTwo =
        resourceCreator.insertApplicantWithAccount(Optional.of("two@example.com"));
    ApplicantModel applicantThree =
        resourceCreator.insertApplicantWithAccount(Optional.of("three@example.com"));
    ApplicationModel applicationTwo =
        resourceCreator.insertActiveApplication(applicantTwo, nextVersion);
    ApplicationModel applicationThree =
        resourceCreator.insertActiveApplication(applicantThree, nextVersion);

    /* Set the submit date such that the results come back in this order: 2, 1, 3 */
    applicationThree.setSubmitTimeToNow();
    applicationThree.save();
    applicationOne.setSubmitTimeToNow();
    applicationOne.save();
    applicationTwo.setSubmitTimeToNow();
    applicationTwo.save();

    PaginationResult<ApplicationModel> paginationResult =
        repo.getApplicationsForAllProgramVersions(
            nextVersion.id,
            new PageNumberPaginationSpec(
                /* pageSize= */ 2, PageNumberPaginationSpec.OrderByEnum.SUBMIT_TIME),
            SubmittedApplicationFilter.EMPTY);

    assertThat(paginationResult.getNumPages()).isEqualTo(2);
    assertThat(paginationResult.getPageContents().size()).isEqualTo(2);

    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantTwo);
    assertThat(paginationResult.getPageContents().get(1).getApplicant()).isEqualTo(applicantOne);

    paginationResult =
        repo.getApplicationsForAllProgramVersions(
            nextVersion.id,
            new PageNumberPaginationSpec(
                /* pageSize= */ 2,
                /* currentPage= */ 2,
                PageNumberPaginationSpec.OrderByEnum.SUBMIT_TIME),
            SubmittedApplicationFilter.EMPTY);

    assertThat(paginationResult.getNumPages()).isEqualTo(2);
    assertThat(paginationResult.getPageContents().size()).isEqualTo(1);

    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantThree);
  }

  @Test
  public void getApplicationsForAllProgramVersions_sortedAndPagedBySubmitTime() {
    ApplicantModel applicantOne =
        resourceCreator.insertApplicantWithAccount(Optional.of("one@example.com"));
    ProgramModel originalVersion = resourceCreator.insertActiveProgram("test program");

    ApplicationModel applicationOne =
        resourceCreator.insertActiveApplication(applicantOne, originalVersion);

    ProgramModel nextVersion = resourceCreator.insertDraftProgram("test program");
    resourceCreator.publishNewSynchronizedVersion();

    ApplicantModel applicantTwo =
        resourceCreator.insertApplicantWithAccount(Optional.of("two@example.com"));
    ApplicantModel applicantThree =
        resourceCreator.insertApplicantWithAccount(Optional.of("three@example.com"));
    ApplicationModel applicationTwo =
        resourceCreator.insertActiveApplication(applicantTwo, nextVersion);
    ApplicationModel applicationThree =
        resourceCreator.insertActiveApplication(applicantThree, nextVersion);

    /* Set the submit date such that the results come back in this order: 2, 1, 3 */
    applicationThree.setSubmitTimeToNow();
    applicationThree.save();
    applicationOne.setSubmitTimeToNow();
    applicationOne.save();
    applicationTwo.setSubmitTimeToNow();
    applicationTwo.save();

    PaginationResult<ApplicationModel> paginationResult =
        repo.getApplicationsForAllProgramVersions(
            nextVersion.id,
            new SubmitTimePaginationSpec(
                /* pageSize= */ 2,
                /* currentSubmitTime= */ Instant.MAX,
                /* currentRowId= */Long.MAX_VALUE),
            SubmittedApplicationFilter.EMPTY);

    assertThat(paginationResult.getNumPages()).isEqualTo(2);
    assertThat(paginationResult.getPageContents().size()).isEqualTo(2);

    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantTwo);
    assertThat(paginationResult.getPageContents().get(1).getApplicant()).isEqualTo(applicantOne);

    paginationResult =
        repo.getApplicationsForAllProgramVersions(
            nextVersion.id,
            new SubmitTimePaginationSpec(
                /* pageSize= */ 2,
                /* currentSubmitTime= */ paginationResult.getPageContents().get(1).getSubmitTime(),
                /* curentRowId= */ paginationResult.getPageContents().get(1).id),
            SubmittedApplicationFilter.EMPTY);

    // Sequential paging returns (1) in the numpages, it only counts the pages from the starting point
    assertThat(paginationResult.getNumPages()).isEqualTo(1);
    assertThat(paginationResult.getPageContents().size()).isEqualTo(1);

    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantThree);
  }

  @Test
  public void getApplicationsForAllProgramVersions_multipleVersions_pageNumberBasedPagination() {
    ApplicantModel applicantOne =
        resourceCreator.insertApplicantWithAccount(Optional.of("one@example.com"));
    ProgramModel originalVersion = resourceCreator.insertActiveProgram("test program");

    resourceCreator.insertActiveApplication(applicantOne, originalVersion);

    ProgramModel nextVersion = resourceCreator.insertDraftProgram("test program");
    resourceCreator.publishNewSynchronizedVersion();

    ApplicantModel applicantTwo =
        resourceCreator.insertApplicantWithAccount(Optional.of("two@example.com"));
    ApplicantModel applicantThree =
        resourceCreator.insertApplicantWithAccount(Optional.of("three@example.com"));
    resourceCreator.insertActiveApplication(applicantTwo, nextVersion);
    resourceCreator.insertActiveApplication(applicantThree, nextVersion);

    PaginationResult<ApplicationModel> paginationResult =
        repo.getApplicationsForAllProgramVersions(
            nextVersion.id,
            new PageNumberPaginationSpec(
                /* pageSize= */ 2, PageNumberPaginationSpec.OrderByEnum.SUBMIT_TIME),
            SubmittedApplicationFilter.EMPTY);

    assertThat(paginationResult.getNumPages()).isEqualTo(2);
    assertThat(paginationResult.getPageContents().size()).isEqualTo(2);

    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantThree);
    assertThat(paginationResult.getPageContents().get(1).getApplicant()).isEqualTo(applicantTwo);

    paginationResult =
        repo.getApplicationsForAllProgramVersions(
            nextVersion.id,
            new PageNumberPaginationSpec(
                /* pageSize= */ 2,
                /* currentPage= */ 2,
                PageNumberPaginationSpec.OrderByEnum.SUBMIT_TIME),
            SubmittedApplicationFilter.EMPTY);

    assertThat(paginationResult.getNumPages()).isEqualTo(2);
    assertThat(paginationResult.getPageContents().size()).isEqualTo(1);

    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantOne);
  }

  @Test
  public void getApplicationsForAllProgramVersions_multipleVersions_offsetBasedPagination() {
    ApplicantModel applicantOne =
        resourceCreator.insertApplicantWithAccount(Optional.of("one@example.com"));
    ProgramModel originalVersion = resourceCreator.insertActiveProgram("test program");

    resourceCreator.insertActiveApplication(applicantOne, originalVersion);

    ProgramModel nextVersion = resourceCreator.insertDraftProgram("test program");
    resourceCreator.publishNewSynchronizedVersion();

    ApplicantModel applicantTwo =
        resourceCreator.insertApplicantWithAccount(Optional.of("two@example.com"));
    ApplicantModel applicantThree =
        resourceCreator.insertApplicantWithAccount(Optional.of("three@example.com"));
    resourceCreator.insertActiveApplication(applicantTwo, nextVersion);
    resourceCreator.insertActiveApplication(applicantThree, nextVersion);

    PaginationResult<ApplicationModel> paginationResult =
        repo.getApplicationsForAllProgramVersions(
            nextVersion.id,
            new RowIdPaginationSpec(/* pageSize= */ 2, Long.MAX_VALUE),
            SubmittedApplicationFilter.EMPTY);

    assertThat(paginationResult.getNumPages()).isEqualTo(2);
    assertThat(paginationResult.getPageContents().size()).isEqualTo(2);

    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantThree);
    assertThat(paginationResult.getPageContents().get(1).getApplicant()).isEqualTo(applicantTwo);

    paginationResult =
        repo.getApplicationsForAllProgramVersions(
            nextVersion.id,
            new RowIdPaginationSpec(
                /* pageSize= */ 2, paginationResult.getPageContents().get(1).id),
            SubmittedApplicationFilter.EMPTY);

    assertThat(paginationResult.getPageContents().size()).isEqualTo(1);
    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantOne);
  }

  @Test
  public void getMostRecentActiveProgramVersion_returnsDifferentProgramIdWhichIsTheLatest() {
    ProgramModel programModel1 = resourceCreator.insertActiveProgram("program-name-1");
    ProgramModel programModel2 = resourceCreator.insertActiveProgram("program-name-2");
    ProgramModel programModel3 = resourceCreator.insertActiveProgram("program-name-1");
    ProgramModel programModel4 = resourceCreator.insertActiveProgram("program-name-1");
    ProgramModel programModel5 = resourceCreator.insertDraftProgram("program-name-1");

    Optional<Long> latestId = repo.getMostRecentActiveProgramId(programModel1.id);

    assertThat(latestId.isPresent()).isTrue();
    assertThat(latestId.get()).isEqualTo(programModel4.id);
  }

  @Test
  public void getMostRecentActiveProgramVersion_returnsSameProgramIdWhichIsTheLatest() {
    ProgramModel programModel1 = resourceCreator.insertActiveProgram("program-name-1");
    ProgramModel programModel2 = resourceCreator.insertActiveProgram("program-name-2");
    ProgramModel programModel3 = resourceCreator.insertActiveProgram("program-name-3");
    ProgramModel programModel4 = resourceCreator.insertActiveProgram("program-name-4");
    ProgramModel programModel5 = resourceCreator.insertDraftProgram("program-name-1");

    Optional<Long> latestId = repo.getMostRecentActiveProgramId(programModel1.id);

    assertThat(latestId.isPresent()).isTrue();
    assertThat(latestId.get()).isEqualTo(programModel1.id);
  }

  @Test
  public void getMostRecentActiveProgramVersion_returnsEmptyWhenIdDoesNotExist() {
    ProgramModel programModel1 = resourceCreator.insertActiveProgram("program-name-1");
    ProgramModel programModel2 = resourceCreator.insertActiveProgram("program-name-2");
    ProgramModel programModel3 = resourceCreator.insertActiveProgram("program-name-3");
    ProgramModel programModel4 = resourceCreator.insertActiveProgram("program-name-4");
    ProgramModel programModel5 = resourceCreator.insertDraftProgram("program-name-1");

    Optional<Long> latestId = repo.getMostRecentActiveProgramId(-1);

    assertThat(latestId.isEmpty()).isTrue();
  }

  @Test
  public void getMostRecentActiveProgramVersion_returnsEmptyWhenNoActiveProgramExists() {
    ProgramModel programModel1 = resourceCreator.insertDraftProgram("program-name-1");
    ProgramModel programModel2 = resourceCreator.insertActiveProgram("program-name-2");
    ProgramModel programModel3 = resourceCreator.insertActiveProgram("program-name-3");
    ProgramModel programModel4 = resourceCreator.insertActiveProgram("program-name-4");

    Optional<Long> latestId = repo.getMostRecentActiveProgramId(programModel1.id);

    assertThat(latestId.isEmpty()).isTrue();
  }

  @Test
  public void checkProgramAdminNameExists_returnsTrueIfAdminNameExistsFalseOtherwise() {
    ProgramModel programModel1 = resourceCreator.insertDraftProgram("program-name-1");

    boolean existsOne =
        repo.checkProgramAdminNameExists("program-name-1"); // same admin name as saved program
    boolean existsTwo = repo.checkProgramAdminNameExists("another-admin-name");

    assertThat(existsOne).isTrue();
    assertThat(existsTwo).isFalse();
  }
}
