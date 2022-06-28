package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.ebean.DB;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import models.Account;
import models.Applicant;
import models.Application;
import models.DisplayMode;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.libs.F;
import services.IdentifierBasedPaginationSpec;
import services.LocalizedStrings;
import services.PageNumberBasedPaginationSpec;
import services.PaginationResult;
import services.program.ProgramNotFoundException;
import support.CfTestHelpers;

public class ProgramRepositoryTest extends ResetPostgres {

  private ProgramRepository repo;
  private VersionRepository versionRepo;

  @Before
  public void setupProgramRepository() {
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
            "insert into programs (name, description, block_definitions, export_definitions,"
                + " legacy_localized_name, legacy_localized_description) values ('Old Schema"
                + " Entry', 'Description', '[]', '[]', '{\"en_us\": \"name\"}', '{\"en_us\":"
                + " \"description\"}');")
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

  @Test
  public void getForSlug_withOldSchema() {
    DB.sqlUpdate(
            "insert into programs (name, description, block_definitions, export_definitions,"
                + " legacy_localized_name, legacy_localized_description) values ('Old Schema"
                + " Entry', 'Description', '[]', '[]', '{\"en_us\": \"a\"}', '{\"en_us\":"
                + " \"b\"}');")
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
            versionRepo.getDraftVersion());

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
            Optional.empty(),
            TimeFilter.builder()
                .setFromTime(Optional.of(Instant.parse("2022-01-25T00:00:00Z")))
                .setUntilTime(Optional.of(Instant.parse("2022-02-10T00:00:00Z")))
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
            Optional.empty(),
            TimeFilter.EMPTY);

    assertThat(paginationResult.getNumPages()).isEqualTo(2);
    assertThat(paginationResult.getPageContents().size()).isEqualTo(2);

    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantThree);
    assertThat(paginationResult.getPageContents().get(1).getApplicant()).isEqualTo(applicantTwo);

    paginationResult =
        repo.getApplicationsForAllProgramVersions(
            nextVersion.id,
            F.Either.Right(
                new PageNumberBasedPaginationSpec(/* pageSize= */ 2, /* currentPage= */ 2)),
            Optional.empty(),
            TimeFilter.EMPTY);

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
            Optional.empty(),
            TimeFilter.EMPTY);

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
            Optional.empty(),
            TimeFilter.EMPTY);

    assertThat(paginationResult.getPageContents().size()).isEqualTo(1);
    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantOne);
  }
}
