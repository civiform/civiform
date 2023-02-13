package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.ebean.DB;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Account;
import models.Applicant;
import models.Application;
import models.ApplicationEvent;
import models.DisplayMode;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.libs.F;
import services.IdentifierBasedPaginationSpec;
import services.LocalizedStrings;
import services.PageNumberBasedPaginationSpec;
import services.PaginationResult;
import services.WellKnownPaths;
import services.applicant.ApplicantData;
import services.application.ApplicationEventDetails;
import services.application.ApplicationEventDetails.StatusEvent;
import services.program.ProgramNotFoundException;
import services.program.ProgramType;
import services.program.StatusDefinitions;
import support.CfTestHelpers;
import support.ProgramBuilder;
import support.QuestionAnswerer;

@RunWith(JUnitParamsRunner.class)
public class ProgramRepositoryTest extends ResetPostgres {

  private ProgramRepository repo;
  private VersionRepository versionRepo;

  @Before
  public void setup() {
    repo = instanceOf(ProgramRepository.class);
    versionRepo = instanceOf(VersionRepository.class);
  }

  @Test
  public void lookupProgram_returnsEmptyOptionalWhenProgramNotFound() {
    Optional<Program> found = repo.lookupProgram(1L).toCompletableFuture().join();

    assertThat(found).isEmpty();
  }

  @Test
  public void lookupProgram_findsCorrectProgram() {
    resourceCreator.insertActiveProgram("one");
    Program two = resourceCreator.insertActiveProgram("two");

    Optional<Program> found = repo.lookupProgram(two.id).toCompletableFuture().join();

    assertThat(found).hasValue(two);
  }

  @Test
  public void loadLegacy() {
    DB.sqlUpdate(
            "insert into programs (name, description, block_definitions, legacy_localized_name,"
                + " legacy_localized_description, program_type) values ('Old Schema Entry',"
                + " 'Description', '[]', '{\"en_us\": \"name\"}', '{\"en_us\": \"description\"}',"
                + " 'default');")
        .execute();
    DB.sqlUpdate(
            "insert into versions_programs (versions_id, programs_id) values ("
                + "(select id from versions where lifecycle_stage = 'active'),"
                + "(select id from programs where name = 'Old Schema Entry'));")
        .execute();

    Program found =
        versionRepo.getActiveVersion().getPrograms().stream()
            .filter(
                program -> program.getProgramDefinition().adminName().equals("Old Schema Entry"))
            .findFirst()
            .get();

    assertThat(found.getProgramDefinition().adminName()).isEqualTo("Old Schema Entry");
    assertThat(found.getProgramDefinition().adminDescription()).isEqualTo("Description");
    assertThat(found.getProgramDefinition().localizedName())
        .isEqualTo(LocalizedStrings.of(Locale.US, "name"));
    assertThat(found.getProgramDefinition().localizedDescription())
        .isEqualTo(LocalizedStrings.of(Locale.US, "description"));
  }

  // Verify the StatusDefinitions default value in evolution 40 loads.
  @Test
  public void loadStatusDefinitionsEvolution() {
    DB.sqlUpdate(
            "insert into programs (name, description, block_definitions, legacy_localized_name,"
                + " legacy_localized_description, status_definitions, program_type) values"
                + " ('Status Default', 'Description', '[]', '{\"en_us\": \"name\"}','{\"en_us\":"
                + " \"description\"}', '{\"statuses\": []}', 'default');")
        .execute();
    DB.sqlUpdate(
            "insert into versions_programs (versions_id, programs_id) values ("
                + "(select id from versions where lifecycle_stage = 'active'),"
                + "(select id from programs where name = 'Status Default'));")
        .execute();

    Program found =
        versionRepo.getActiveVersion().getPrograms().stream()
            .filter(program -> program.getProgramDefinition().adminName().equals("Status Default"))
            .findFirst()
            .get();

    assertThat(found.getProgramDefinition().adminName()).isEqualTo("Status Default");
    assertThat(found.getStatusDefinitions().getStatuses()).isEmpty();
  }

  @Test
  public void getForSlug_withOldSchema() {
    DB.sqlUpdate(
            "insert into programs (name, description, block_definitions, legacy_localized_name,"
                + " legacy_localized_description, program_type) values ('Old Schema Entry',"
                + " 'Description', '[]', '{\"en_us\": \"a\"}', '{\"en_us\": \"b\"}', 'default');")
        .execute();
    DB.sqlUpdate(
            "insert into versions_programs (versions_id, programs_id) values ("
                + "(select id from versions where lifecycle_stage = 'active'),"
                + "(select id from programs where name = 'Old Schema Entry'));")
        .execute();

    Program found = repo.getForSlug("old-schema-entry").toCompletableFuture().join();

    assertThat(found.getProgramDefinition().adminName()).isEqualTo("Old Schema Entry");
    assertThat(found.getProgramDefinition().adminDescription()).isEqualTo("Description");
  }

  @Test
  public void getForSlug_findsCorrectProgram() {
    Program program = resourceCreator.insertActiveProgram("Something With A Name");

    Program found = repo.getForSlug("something-with-a-name").toCompletableFuture().join();

    assertThat(found).isEqualTo(program);
  }

  @Test
  public void insertProgramSync() throws Exception {
    Program program =
        new Program(
            "ProgramRepository",
            "desc",
            "name",
            "description",
            "",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            versionRepo.getDraftVersion(),
            ProgramType.DEFAULT);
    Program withId = repo.insertProgramSync(program);

    Program found = repo.lookupProgram(withId.id).toCompletableFuture().join().get();
    assertThat(found.getProgramDefinition().localizedName().get(Locale.US)).isEqualTo("name");
  }

  @Test
  public void updateProgramSync() {
    Program existing = resourceCreator.insertActiveProgram("old name");
    Program updates =
        new Program(
            existing.getProgramDefinition().toBuilder()
                .setLocalizedName(LocalizedStrings.of(Locale.US, "new name"))
                .build());

    Program updated = repo.updateProgramSync(updates);

    assertThat(updated.getProgramDefinition().id()).isEqualTo(existing.id);
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
  public void returnsAllAdmins() throws ProgramNotFoundException {
    Program withAdmins = resourceCreator.insertActiveProgram("with admins");
    Account admin = new Account();
    admin.save();
    assertThat(repo.getProgramAdministrators(withAdmins.id)).isEmpty();
    admin.addAdministeredProgram(withAdmins.getProgramDefinition());
    admin.save();
    assertThat(repo.getProgramAdministrators(withAdmins.id)).containsExactly(admin);

    // This draft, despite not existing when the admin association happened, should
    // still have the same admin associated.
    Program newDraft = repo.createOrUpdateDraft(withAdmins);
    assertThat(repo.getProgramAdministrators(newDraft.id)).containsExactly(admin);
  }

  @Test
  public void getApplicationsForAllProgramVersions_searchById() {
    Program program = resourceCreator.insertActiveProgram("test program");

    Applicant bob = resourceCreator.insertApplicantWithAccount(Optional.of("bob@example.com"));
    Application bobApp = makeApplicationWithName(bob, program, "Bob", "MiddleName", "Doe");
    Applicant jane = resourceCreator.insertApplicantWithAccount(Optional.of("jane@example.com"));
    makeApplicationWithName(jane, program, "Jane", "MiddleName", "Doe");

    PaginationResult<Application> paginationResult =
        repo.getApplicationsForAllProgramVersions(
            program.id,
            F.Either.Left(IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG),
            SubmittedApplicationFilter.builder()
                .setSearchNameFragment(Optional.of(bobApp.id.toString()))
                .setSubmitTimeFilter(TimeFilter.EMPTY)
                .build());

    assertThat(
            paginationResult.getPageContents().stream()
                .map(a -> a.getSubmitterEmail().orElse(""))
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
        new Object[] {"bob@example.com", ImmutableSet.of("bob@example.com")},
        new Object[] {"Other Person", ImmutableSet.of()},

        // Case insensitive search.
        new Object[] {"bOb dOe", ImmutableSet.of("bob@example.com")},
        new Object[] {"CHRIS@example.com", ImmutableSet.of("chris@exAMPLE.com")},

        // Leading and trailing whitespace is ignored.
        new Object[] {"    Bob Doe    ", ImmutableSet.of("bob@example.com")},

        // Degenerate cases.
        // Email must be an exact match.
        new Object[] {"example.com", ImmutableSet.of()},
        // Only match a single space between first and last name.
        new Object[] {"Bob  Doe", ImmutableSet.of()});
  }

  @Test
  @Parameters(method = "getSearchByNameOrEmailData")
  public void getApplicationsForAllProgramVersions_searchByNameOrEmail(
      String searchFragment, ImmutableSet<String> wantEmails) {
    Program program = resourceCreator.insertActiveProgram("test program");

    Applicant bob = resourceCreator.insertApplicantWithAccount(Optional.of("bob@example.com"));
    makeApplicationWithName(bob, program, "Bob", "MiddleName", "Doe");
    Applicant jane = resourceCreator.insertApplicantWithAccount(Optional.of("jane@example.com"));
    makeApplicationWithName(jane, program, "Jane", "MiddleName", "Doe");
    // Note: The mixed casing on the email is intentional for tests of case insensitivity.
    Applicant chris = resourceCreator.insertApplicantWithAccount(Optional.of("chris@exAMPLE.com"));
    makeApplicationWithName(chris, program, "Chris", "MiddleName", "Person");

    Applicant otherApplicant =
        resourceCreator.insertApplicantWithAccount(Optional.of("other@example.com"));
    resourceCreator.insertDraftApplication(otherApplicant, program);

    PaginationResult<Application> paginationResult =
        repo.getApplicationsForAllProgramVersions(
            program.id,
            F.Either.Left(IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG),
            SubmittedApplicationFilter.builder()
                .setSearchNameFragment(Optional.of(searchFragment))
                .setSubmitTimeFilter(TimeFilter.EMPTY)
                .build());

    assertThat(
            paginationResult.getPageContents().stream()
                .map(a -> a.getSubmitterEmail().orElse(""))
                .collect(ImmutableSet.toImmutableSet()))
        .isEqualTo(wantEmails);
    assertThat(paginationResult.getNumPages()).isEqualTo(wantEmails.isEmpty() ? 0 : 1);
  }

  private Application makeApplicationWithName(
      Applicant applicant, Program program, String firstName, String middleName, String lastName) {
    Application application = resourceCreator.insertActiveApplication(applicant, program);
    ApplicantData applicantData = application.getApplicantData();
    QuestionAnswerer.answerNameQuestion(
        applicantData, WellKnownPaths.APPLICANT_NAME, firstName, middleName, lastName);
    application.setApplicantData(applicantData);
    application.setSubmitterEmail(applicant.getAccount().getEmailAddress());
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
    Program program =
        ProgramBuilder.newActiveProgram("test program", "description")
            .withStatusDefinitions(
                new StatusDefinitions(ImmutableList.of(FIRST_STATUS, SECOND_STATUS, THIRD_STATUS)))
            .build();

    Account adminAccount = resourceCreator.insertAccountWithEmail("admin@example.com");

    Application firstStatusApplication =
        resourceCreator.insertActiveApplication(resourceCreator.insertApplicant(), program);
    createStatusEvents(
        adminAccount, firstStatusApplication, ImmutableList.of(Optional.of(FIRST_STATUS)));

    Application secondStatusApplication =
        resourceCreator.insertActiveApplication(resourceCreator.insertApplicant(), program);
    createStatusEvents(
        adminAccount, secondStatusApplication, ImmutableList.of(Optional.of(SECOND_STATUS)));

    Application thirdStatusApplication =
        resourceCreator.insertActiveApplication(resourceCreator.insertApplicant(), program);
    // Create a few status events before-hand to ensure that the latest status is used.
    createStatusEvents(
        adminAccount,
        thirdStatusApplication,
        ImmutableList.of(
            Optional.of(FIRST_STATUS), Optional.of(SECOND_STATUS), Optional.of(THIRD_STATUS)));

    Application noStatusApplication =
        resourceCreator.insertActiveApplication(resourceCreator.insertApplicant(), program);

    Application backToNoStatusApplication =
        resourceCreator.insertActiveApplication(resourceCreator.insertApplicant(), program);
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
      Program program, SubmittedApplicationFilter filter) {
    PaginationResult<Application> result =
        repo.getApplicationsForAllProgramVersions(
            program.id,
            F.Either.Left(IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG),
            filter);
    assertThat(result.hasMorePages()).isEqualTo(false);
    return result.getPageContents().stream()
        .map(app -> app.id)
        .collect(ImmutableSet.toImmutableSet());
  }

  private void createStatusEvents(
      Account actorAccount,
      Application application,
      ImmutableList<Optional<StatusDefinitions.Status>> statuses)
      throws InterruptedException {
    for (Optional<StatusDefinitions.Status> status : statuses) {
      String statusText = status.map(StatusDefinitions.Status::statusText).orElse("");
      ApplicationEventDetails details =
          ApplicationEventDetails.builder()
              .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
              .setStatusEvent(
                  StatusEvent.builder().setStatusText(statusText).setEmailSent(true).build())
              .build();
      ApplicationEvent event = new ApplicationEvent(application, actorAccount, details);
      event.save();

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
    Program program = resourceCreator.insertActiveProgram("test program");

    Applicant applicantTwo =
        resourceCreator.insertApplicantWithAccount(Optional.of("two@example.com"));
    Applicant applicantThree =
        resourceCreator.insertApplicantWithAccount(Optional.of("three@example.com"));
    Applicant applicantOne =
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

    PaginationResult<Application> paginationResult =
        repo.getApplicationsForAllProgramVersions(
            program.id,
            F.Either.Right(new PageNumberBasedPaginationSpec(/* pageSize= */ 10)),
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
  public void getApplicationsForAllProgramVersions_multipleVersions_pageNumberBasedPagination() {
    Applicant applicantOne =
        resourceCreator.insertApplicantWithAccount(Optional.of("one@example.com"));
    Program originalVersion = resourceCreator.insertActiveProgram("test program");

    resourceCreator.insertActiveApplication(applicantOne, originalVersion);

    Program nextVersion = resourceCreator.insertDraftProgram("test program");
    resourceCreator.publishNewSynchronizedVersion();

    Applicant applicantTwo =
        resourceCreator.insertApplicantWithAccount(Optional.of("two@example.com"));
    Applicant applicantThree =
        resourceCreator.insertApplicantWithAccount(Optional.of("three@example.com"));
    resourceCreator.insertActiveApplication(applicantTwo, nextVersion);
    resourceCreator.insertActiveApplication(applicantThree, nextVersion);

    PaginationResult<Application> paginationResult =
        repo.getApplicationsForAllProgramVersions(
            nextVersion.id,
            F.Either.Right(new PageNumberBasedPaginationSpec(/* pageSize= */ 2)),
            SubmittedApplicationFilter.EMPTY);

    assertThat(paginationResult.getNumPages()).isEqualTo(2);
    assertThat(paginationResult.getPageContents().size()).isEqualTo(2);

    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantThree);
    assertThat(paginationResult.getPageContents().get(1).getApplicant()).isEqualTo(applicantTwo);

    paginationResult =
        repo.getApplicationsForAllProgramVersions(
            nextVersion.id,
            F.Either.Right(
                new PageNumberBasedPaginationSpec(/* pageSize= */ 2, /* currentPage= */ 2)),
            SubmittedApplicationFilter.EMPTY);

    assertThat(paginationResult.getNumPages()).isEqualTo(2);
    assertThat(paginationResult.getPageContents().size()).isEqualTo(1);

    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantOne);
  }

  @Test
  public void getApplicationsForAllProgramVersions_multipleVersions_offsetBasedPagination() {
    Applicant applicantOne =
        resourceCreator.insertApplicantWithAccount(Optional.of("one@example.com"));
    Program originalVersion = resourceCreator.insertActiveProgram("test program");

    resourceCreator.insertActiveApplication(applicantOne, originalVersion);

    Program nextVersion = resourceCreator.insertDraftProgram("test program");
    resourceCreator.publishNewSynchronizedVersion();

    Applicant applicantTwo =
        resourceCreator.insertApplicantWithAccount(Optional.of("two@example.com"));
    Applicant applicantThree =
        resourceCreator.insertApplicantWithAccount(Optional.of("three@example.com"));
    resourceCreator.insertActiveApplication(applicantTwo, nextVersion);
    resourceCreator.insertActiveApplication(applicantThree, nextVersion);

    PaginationResult<Application> paginationResult =
        repo.getApplicationsForAllProgramVersions(
            nextVersion.id,
            F.Either.Left(new IdentifierBasedPaginationSpec<>(2, Long.MAX_VALUE)),
            SubmittedApplicationFilter.EMPTY);

    assertThat(paginationResult.getNumPages()).isEqualTo(2);
    assertThat(paginationResult.getPageContents().size()).isEqualTo(2);

    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantThree);
    assertThat(paginationResult.getPageContents().get(1).getApplicant()).isEqualTo(applicantTwo);

    paginationResult =
        repo.getApplicationsForAllProgramVersions(
            nextVersion.id,
            F.Either.Left(
                new IdentifierBasedPaginationSpec<>(
                    2, paginationResult.getPageContents().get(1).id)),
            SubmittedApplicationFilter.EMPTY);

    assertThat(paginationResult.getPageContents().size()).isEqualTo(1);
    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantOne);
  }
}
