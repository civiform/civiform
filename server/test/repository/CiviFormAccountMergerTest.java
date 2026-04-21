package repository;

import static models.LifecycleStage.ACTIVE;
import static models.LifecycleStage.DRAFT;
import static models.LifecycleStage.OBSOLETE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import auth.NewGuestMergeLaunchStage;
import com.google.common.collect.ImmutableMap;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.ApplicantModel;
import models.ApplicationModel;
import models.LifecycleStage;
import models.StoredFileModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.Path;
import services.applicant.ApplicantData;
import services.cloud.ApplicantFileNameFormatter;
import services.settings.SettingsManifest;

@RunWith(JUnitParamsRunner.class)
public class CiviFormAccountMergerTest extends ResetPostgres {
  private CiviFormAccountMerger merger;
  private AccountRepository acctRepo;
  private ApplicationRepository applRepo;
  private StoredFileRepository storedFileRepository;

  @Before
  public void setupApplicantRepository() {
    SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);
    storedFileRepository = instanceOf(StoredFileRepository.class);
    merger = new CiviFormAccountMerger(() -> storedFileRepository);
    acctRepo =
        new AccountRepository(
            instanceOf(DatabaseExecutionContext.class),
            instanceOf(Clock.class),
            mockSettingsManifest);
    applRepo =
        new ApplicationRepository(
            instanceOf(ProgramRepository.class),
            acctRepo,
            instanceOf(DatabaseExecutionContext.class));
  }

  public enum Newer {
    CIVIFORM,
    GUEST
  }

  private ImmutableMap<Long, ApplicationModel> buildIdToApplications(Long applicantId) {
    return acctRepo.lookupApplicantSync(applicantId).orElseThrow().getApplications().stream()
        .collect(ImmutableMap.toImmutableMap(a -> a.id, a -> a));
  }

  @Test
  @Parameters({"CIVIFORM", "GUEST"})
  public void mergeApplicants_bothActive_oneNewer_dryRun_noChanges(Newer newerApplication) {
    var program = resourceCreator.insertActiveProgram("test-program");
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    Instant older = Instant.now().minusSeconds(60);
    Instant newer = Instant.now();

    ApplicationModel cfObsolete =
        resourceCreator.insertApplication(cfUser, program, LifecycleStage.OBSOLETE);
    ApplicationModel cfActive = resourceCreator.insertApplication(cfUser, program, ACTIVE);
    ApplicationModel cfDraft =
        resourceCreator.insertApplication(cfUser, program, LifecycleStage.DRAFT);
    ApplicationModel guestObsolete =
        resourceCreator.insertApplication(guestUser, program, LifecycleStage.OBSOLETE);
    ApplicationModel guestActive = resourceCreator.insertApplication(guestUser, program, ACTIVE);
    ApplicationModel guestDraft =
        resourceCreator.insertApplication(guestUser, program, LifecycleStage.DRAFT);

    switch (newerApplication) {
      case CIVIFORM -> {
        guestActive.setSubmitTimeForTest(older).save();
        cfActive.setSubmitTimeForTest(newer).save();
      }
      case GUEST -> {
        cfActive.setSubmitTimeForTest(older).save();
        guestActive.setSubmitTimeForTest(newer).save();
      }
    }
    cfUser.refresh();
    guestUser.refresh();

    // Execute with DRY_RUN — no DB changes should occur.
    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.DRY_RUN);

    // Verify cfUser's applications are unchanged.
    ImmutableMap<Long, ApplicationModel> cfIdToApplication = buildIdToApplications(cfUser.id);
    assertThat(cfIdToApplication.keySet())
        .containsExactlyInAnyOrder(cfObsolete.id, cfActive.id, cfDraft.id);
    assertThat(cfIdToApplication.get(cfObsolete.id).getLifecycleStage()).isEqualTo(OBSOLETE);
    assertThat(cfIdToApplication.get(cfActive.id).getLifecycleStage()).isEqualTo(ACTIVE);
    assertThat(cfIdToApplication.get(cfDraft.id).getLifecycleStage()).isEqualTo(DRAFT);

    // Verify guestUser's applications are unchanged.
    ImmutableMap<Long, ApplicationModel> guestIdToApplication = buildIdToApplications(guestUser.id);
    assertThat(guestIdToApplication.keySet())
        .containsExactlyInAnyOrder(guestObsolete.id, guestActive.id, guestDraft.id);
    assertThat(guestIdToApplication.get(guestObsolete.id).getLifecycleStage()).isEqualTo(OBSOLETE);
    assertThat(guestIdToApplication.get(guestActive.id).getLifecycleStage()).isEqualTo(ACTIVE);
    assertThat(guestIdToApplication.get(guestDraft.id).getLifecycleStage()).isEqualTo(DRAFT);

    // Verify no originalApplicantId was set on any application.
    assertThat(cfIdToApplication.values())
        .extracting(ApplicationModel::getOriginalApplicantId)
        .allMatch(Optional::isEmpty);
    assertThat(guestIdToApplication.values())
        .extracting(ApplicationModel::getOriginalApplicantId)
        .allMatch(Optional::isEmpty);
  }

  @Test
  @Parameters({"CIVIFORM", "GUEST"})
  public void mergeApplicants_bothActive_oneNewer_enabled_appliesChanges(Newer newerApplication) {
    var program = resourceCreator.insertActiveProgram("test-program");
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    Instant older = Instant.now().minusSeconds(60);
    Instant newer = Instant.now();

    // Create Actives for both as the main test.
    // Add Obsolete and Drafts to verify also for completeness.
    // Drafts are not so important that we need to test every combination of their existence
    // alongside the Actives.
    ApplicationModel cfObsolete =
        resourceCreator.insertApplication(cfUser, program, LifecycleStage.OBSOLETE);
    ApplicationModel cfActive = resourceCreator.insertApplication(cfUser, program, ACTIVE);
    ApplicationModel cfDraft =
        resourceCreator.insertApplication(cfUser, program, LifecycleStage.DRAFT);
    ApplicationModel guestObsolete =
        resourceCreator.insertApplication(guestUser, program, LifecycleStage.OBSOLETE);
    ApplicationModel guestActive = resourceCreator.insertApplication(guestUser, program, ACTIVE);
    ApplicationModel guestDraft =
        resourceCreator.insertApplication(guestUser, program, LifecycleStage.DRAFT);

    switch (newerApplication) {
      case CIVIFORM -> {
        guestActive.setSubmitTimeForTest(older).save();
        cfActive.setSubmitTimeForTest(newer).save();
      }
      case GUEST -> {
        cfActive.setSubmitTimeForTest(older).save();
        guestActive.setSubmitTimeForTest(newer).save();
      }
    }
    cfUser.refresh();
    guestUser.refresh();

    // Execute the code.
    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.ENABLED);
    cfUser.refresh();

    // Verify.
    List<ApplicationModel> cfApps =
        acctRepo.lookupApplicantSync(cfUser.id).orElseThrow().getApplications();
    ImmutableMap<Long, ApplicationModel> idToApplication =
        cfApps.stream().collect(ImmutableMap.toImmutableMap(a -> a.id, a -> a));

    if (newerApplication.equals(Newer.CIVIFORM)) {
      assertThat(cfApps)
          .extracting(a -> a.id)
          .containsExactlyInAnyOrder(
              cfObsolete.id, guestObsolete.id, guestActive.id, cfActive.id, cfDraft.id);

      assertThat(idToApplication.get(guestObsolete.id).getOriginalApplicantId())
          .hasValue(guestUser.id);
      assertThat(idToApplication.get(guestActive.id).getOriginalApplicantId())
          .hasValue(guestUser.id);

      assertThat(idToApplication.get(cfObsolete.id).getLifecycleStage()).isEqualTo(OBSOLETE);
      assertThat(idToApplication.get(guestObsolete.id).getLifecycleStage()).isEqualTo(OBSOLETE);
      assertThat(idToApplication.get(guestActive.id).getLifecycleStage()).isEqualTo(OBSOLETE);
      assertThat(idToApplication.get(cfActive.id).getLifecycleStage()).isEqualTo(ACTIVE);
      assertThat(idToApplication.get(cfDraft.id).getLifecycleStage()).isEqualTo(DRAFT);
    } else if (newerApplication.equals(Newer.GUEST)) {
      // Guest active is moved to cfUser; CF active is obsoleted.
      assertThat(cfApps)
          .extracting(a -> a.id)
          .containsExactlyInAnyOrder(
              cfObsolete.id, guestObsolete.id, cfActive.id, guestActive.id, guestDraft.id);

      assertThat(idToApplication.get(guestObsolete.id).getOriginalApplicantId())
          .hasValue(guestUser.id);
      assertThat(idToApplication.get(guestActive.id).getOriginalApplicantId())
          .hasValue(guestUser.id);

      assertThat(idToApplication.get(cfObsolete.id).getLifecycleStage()).isEqualTo(OBSOLETE);
      assertThat(idToApplication.get(guestObsolete.id).getLifecycleStage()).isEqualTo(OBSOLETE);
      assertThat(idToApplication.get(cfActive.id).getLifecycleStage()).isEqualTo(OBSOLETE);
      assertThat(idToApplication.get(guestActive.id).getLifecycleStage()).isEqualTo(ACTIVE);
      assertThat(idToApplication.get(guestDraft.id).getLifecycleStage()).isEqualTo(DRAFT);
    }
  }

  @Test
  public void mergeApplicants_differentPrograms_dryRun_noChanges() {
    var programA = resourceCreator.insertActiveProgram("program-a");
    var programB = resourceCreator.insertActiveProgram("program-b");
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    ApplicationModel cfApp = resourceCreator.insertApplication(cfUser, programA, ACTIVE);
    ApplicationModel guestObsolete =
        resourceCreator.insertApplication(guestUser, programB, LifecycleStage.OBSOLETE);
    ApplicationModel guestActive = resourceCreator.insertApplication(guestUser, programB, ACTIVE);
    ApplicationModel guestDraft =
        resourceCreator.insertApplication(guestUser, programB, LifecycleStage.DRAFT);
    cfUser.refresh();
    guestUser.refresh();

    // Execute with DRY_RUN — no DB changes should occur.
    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.DRY_RUN);

    // Verify cfUser's applications are unchanged.
    ImmutableMap<Long, ApplicationModel> cfIdToApplication = buildIdToApplications(cfUser.id);
    assertThat(cfIdToApplication.keySet()).containsExactlyInAnyOrder(cfApp.id);
    assertThat(cfIdToApplication.get(cfApp.id).getLifecycleStage()).isEqualTo(ACTIVE);

    // Verify guestUser's applications are unchanged.
    ImmutableMap<Long, ApplicationModel> guestIdToApplication = buildIdToApplications(guestUser.id);
    assertThat(guestIdToApplication.keySet())
        .containsExactlyInAnyOrder(guestObsolete.id, guestActive.id, guestDraft.id);
    assertThat(guestIdToApplication.get(guestObsolete.id).getLifecycleStage()).isEqualTo(OBSOLETE);
    assertThat(guestIdToApplication.get(guestActive.id).getLifecycleStage()).isEqualTo(ACTIVE);
    assertThat(guestIdToApplication.get(guestDraft.id).getLifecycleStage()).isEqualTo(DRAFT);

    // Verify no originalApplicantId was set on any application.
    assertThat(cfIdToApplication.values())
        .extracting(ApplicationModel::getOriginalApplicantId)
        .allMatch(Optional::isEmpty);
    assertThat(guestIdToApplication.values())
        .extracting(ApplicationModel::getOriginalApplicantId)
        .allMatch(Optional::isEmpty);
  }

  @Test
  public void mergeApplicants_differentPrograms_allApplicationsOnCfUser() {
    var programA = resourceCreator.insertActiveProgram("program-a");
    var programB = resourceCreator.insertActiveProgram("program-b");
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    ApplicationModel cfApp = resourceCreator.insertApplication(cfUser, programA, ACTIVE);
    ApplicationModel guestObsolete =
        resourceCreator.insertApplication(guestUser, programB, LifecycleStage.OBSOLETE);
    ApplicationModel guestActive = resourceCreator.insertApplication(guestUser, programB, ACTIVE);
    ApplicationModel guestDraft =
        resourceCreator.insertApplication(guestUser, programB, LifecycleStage.DRAFT);
    cfUser.refresh();
    guestUser.refresh();

    // Execute.
    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.ENABLED);

    // Verify.
    ImmutableMap<Long, ApplicationModel> idToApplication = buildIdToApplications(cfUser.id);

    assertThat(idToApplication.keySet())
        .containsExactlyInAnyOrder(cfApp.id, guestObsolete.id, guestActive.id, guestDraft.id);

    assertThat(idToApplication.get(cfApp.id).getLifecycleStage()).isEqualTo(ACTIVE);
    assertThat(idToApplication.get(guestObsolete.id).getLifecycleStage())
        .isEqualTo(LifecycleStage.OBSOLETE);
    assertThat(idToApplication.get(guestActive.id).getLifecycleStage()).isEqualTo(ACTIVE);
    assertThat(idToApplication.get(guestDraft.id).getLifecycleStage()).isEqualTo(DRAFT);

    assertThat(idToApplication.get(guestObsolete.id).getOriginalApplicantId())
        .hasValue(guestUser.id);
    assertThat(idToApplication.get(guestActive.id).getOriginalApplicantId()).hasValue(guestUser.id);
  }

  @Test
  public void mergeApplicants_bothDraft_dryRun_noChanges() {
    var program = resourceCreator.insertActiveProgram("test-program");
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    ApplicationModel cfDraft =
        resourceCreator.insertApplication(cfUser, program, LifecycleStage.DRAFT);
    ApplicationModel guestDraft =
        resourceCreator.insertApplication(guestUser, program, LifecycleStage.DRAFT);
    cfUser.refresh();
    guestUser.refresh();

    // Execute with DRY_RUN — no DB changes should occur.
    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.DRY_RUN);

    // Verify cfUser's applications are unchanged.
    ImmutableMap<Long, ApplicationModel> cfIdToApplication = buildIdToApplications(cfUser.id);
    assertThat(cfIdToApplication.keySet()).containsExactlyInAnyOrder(cfDraft.id);
    assertThat(cfIdToApplication.get(cfDraft.id).getLifecycleStage()).isEqualTo(DRAFT);

    // Verify guestUser's applications are unchanged.
    ImmutableMap<Long, ApplicationModel> guestIdToApplication = buildIdToApplications(guestUser.id);
    assertThat(guestIdToApplication.keySet()).containsExactlyInAnyOrder(guestDraft.id);
    assertThat(guestIdToApplication.get(guestDraft.id).getLifecycleStage()).isEqualTo(DRAFT);

    // Verify no originalApplicantId was set on any application.
    assertThat(cfIdToApplication.values())
        .extracting(ApplicationModel::getOriginalApplicantId)
        .allMatch(Optional::isEmpty);
    assertThat(guestIdToApplication.values())
        .extracting(ApplicationModel::getOriginalApplicantId)
        .allMatch(Optional::isEmpty);
  }

  @Test
  public void mergeApplicants_bothDraft_guestDraftKept() {
    var program = resourceCreator.insertActiveProgram("test-program");
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    ApplicationModel cfDraft =
        resourceCreator.insertApplication(cfUser, program, LifecycleStage.DRAFT);
    ApplicationModel guestDraft =
        resourceCreator.insertApplication(guestUser, program, LifecycleStage.DRAFT);
    cfUser.refresh();
    guestUser.refresh();

    // Execute.
    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.ENABLED);

    // Verify.
    List<ApplicationModel> cfApps =
        acctRepo.lookupApplicantSync(cfUser.id).orElseThrow().getApplications();

    // The guest's Draft is kept and the CiviForm users is not because the
    // user saw the guest's most recently.
    assertThat(cfApps).extracting(a -> a.id).containsExactly(guestDraft.id);
    assertThat(applRepo.getApplication(cfDraft.id).toCompletableFuture().join()).isEmpty();
  }

  @Test
  public void mergeApplicants_cfActiveAndDraft_guestDraft_dryRun_noChanges() {
    var program = resourceCreator.insertActiveProgram("test-program");
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    ApplicationModel cfActive = resourceCreator.insertApplication(cfUser, program, ACTIVE);
    ApplicationModel cfDraft =
        resourceCreator.insertApplication(cfUser, program, LifecycleStage.DRAFT);
    ApplicationModel guestDraft =
        resourceCreator.insertApplication(guestUser, program, LifecycleStage.DRAFT);
    cfUser.refresh();
    guestUser.refresh();

    // Execute with DRY_RUN — no DB changes should occur.
    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.DRY_RUN);

    // Verify cfUser's applications are unchanged.
    ImmutableMap<Long, ApplicationModel> cfIdToApplication = buildIdToApplications(cfUser.id);
    assertThat(cfIdToApplication.keySet()).containsExactlyInAnyOrder(cfActive.id, cfDraft.id);
    assertThat(cfIdToApplication.get(cfActive.id).getLifecycleStage()).isEqualTo(ACTIVE);
    assertThat(cfIdToApplication.get(cfDraft.id).getLifecycleStage()).isEqualTo(DRAFT);

    // Verify guestUser's applications are unchanged.
    ImmutableMap<Long, ApplicationModel> guestIdToApplication = buildIdToApplications(guestUser.id);
    assertThat(guestIdToApplication.keySet()).containsExactlyInAnyOrder(guestDraft.id);
    assertThat(guestIdToApplication.get(guestDraft.id).getLifecycleStage()).isEqualTo(DRAFT);

    // Verify no originalApplicantId was set on any application.
    assertThat(cfIdToApplication.values())
        .extracting(ApplicationModel::getOriginalApplicantId)
        .allMatch(Optional::isEmpty);
    assertThat(guestIdToApplication.values())
        .extracting(ApplicationModel::getOriginalApplicantId)
        .allMatch(Optional::isEmpty);
  }

  @Test
  public void mergeApplicants_cfActiveAndDraft_guestDraft_cfApplicationsKept() {
    var program = resourceCreator.insertActiveProgram("test-program");
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    ApplicationModel cfActive = resourceCreator.insertApplication(cfUser, program, ACTIVE);
    ApplicationModel cfDraft =
        resourceCreator.insertApplication(cfUser, program, LifecycleStage.DRAFT);
    ApplicationModel guestDraft =
        resourceCreator.insertApplication(guestUser, program, LifecycleStage.DRAFT);
    cfUser.refresh();
    guestUser.refresh();

    // Execute.
    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.ENABLED);

    // Verify.
    ImmutableMap<Long, ApplicationModel> idToApplication = buildIdToApplications(cfUser.id);

    // CF's active and draft are preserved with their original lifecycle stages.
    assertThat(idToApplication.keySet()).containsExactlyInAnyOrder(cfActive.id, cfDraft.id);
    assertThat(idToApplication.get(cfActive.id).getLifecycleStage()).isEqualTo(ACTIVE);
    assertThat(idToApplication.get(cfDraft.id).getLifecycleStage()).isEqualTo(DRAFT);

    // Guest draft is deleted.
    assertThat(applRepo.getApplication(guestDraft.id).toCompletableFuture().join()).isEmpty();
  }

  @Test
  public void mergeApplicants_cfDraft_guestActiveAndDraft_dryRun_noChanges() {
    var program = resourceCreator.insertActiveProgram("test-program");
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    ApplicationModel cfDraft =
        resourceCreator.insertApplication(cfUser, program, LifecycleStage.DRAFT);
    ApplicationModel guestActive = resourceCreator.insertApplication(guestUser, program, ACTIVE);
    ApplicationModel guestDraft =
        resourceCreator.insertApplication(guestUser, program, LifecycleStage.DRAFT);
    cfUser.refresh();
    guestUser.refresh();

    // Execute with DRY_RUN — no DB changes should occur.
    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.DRY_RUN);

    // Verify cfUser's applications are unchanged.
    ImmutableMap<Long, ApplicationModel> cfIdToApplication = buildIdToApplications(cfUser.id);
    assertThat(cfIdToApplication.keySet()).containsExactlyInAnyOrder(cfDraft.id);
    assertThat(cfIdToApplication.get(cfDraft.id).getLifecycleStage()).isEqualTo(DRAFT);

    // Verify guestUser's applications are unchanged.
    ImmutableMap<Long, ApplicationModel> guestIdToApplication = buildIdToApplications(guestUser.id);
    assertThat(guestIdToApplication.keySet())
        .containsExactlyInAnyOrder(guestActive.id, guestDraft.id);
    assertThat(guestIdToApplication.get(guestActive.id).getLifecycleStage()).isEqualTo(ACTIVE);
    assertThat(guestIdToApplication.get(guestDraft.id).getLifecycleStage()).isEqualTo(DRAFT);

    // Verify no originalApplicantId was set on any application.
    assertThat(cfIdToApplication.values())
        .extracting(ApplicationModel::getOriginalApplicantId)
        .allMatch(Optional::isEmpty);
    assertThat(guestIdToApplication.values())
        .extracting(ApplicationModel::getOriginalApplicantId)
        .allMatch(Optional::isEmpty);
  }

  @Test
  public void mergeApplicants_cfDraft_guestActiveAndDraft_guestApplicationsKept() {
    var program = resourceCreator.insertActiveProgram("test-program");
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    ApplicationModel cfDraft =
        resourceCreator.insertApplication(cfUser, program, LifecycleStage.DRAFT);
    ApplicationModel guestActive = resourceCreator.insertApplication(guestUser, program, ACTIVE);
    ApplicationModel guestDraft =
        resourceCreator.insertApplication(guestUser, program, LifecycleStage.DRAFT);
    cfUser.refresh();
    guestUser.refresh();

    // Execute
    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.ENABLED);

    // Verify
    List<ApplicationModel> cfApps =
        acctRepo.lookupApplicantSync(cfUser.id).orElseThrow().getApplications();
    ImmutableMap<Long, ApplicationModel> idToApplication =
        cfApps.stream().collect(ImmutableMap.toImmutableMap(a -> a.id, a -> a));

    // Guest active and draft are moved to cfUser.
    assertThat(cfApps)
        .extracting(a -> a.id)
        .containsExactlyInAnyOrder(guestActive.id, guestDraft.id);
    assertThat(idToApplication.get(guestActive.id).getLifecycleStage()).isEqualTo(ACTIVE);
    assertThat(idToApplication.get(guestDraft.id).getLifecycleStage()).isEqualTo(DRAFT);
    assertThat(idToApplication.get(guestActive.id).getOriginalApplicantId()).hasValue(guestUser.id);
    // CF's draft is deleted.
    assertThat(applRepo.getApplication(cfDraft.id).toCompletableFuture().join()).isEmpty();
  }

  @Test
  public void mergeGuestFilesIntoCfUser_dryRun_doesNotAddCfUserToAcl() {
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();
    cfUser.refresh();
    guestUser.refresh();

    StoredFileModel guestFile = insertFileForApplicant(guestUser.id);

    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.DRY_RUN);

    StoredFileModel fileAfterMerge = lookupFile(guestFile.id);
    assertThat(fileAfterMerge.getAcls().getApplicantReadAcls()).doesNotContain(cfUser.id);
  }

  @Test
  @Parameters({"0", "1", "2"})
  public void mergeGuestFilesIntoCfUser_enabled_addsCfUserToAcl(int numFiles) {
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();
    cfUser.refresh();
    guestUser.refresh();
    List<Long> fileIds = new ArrayList<>();
    for (int i = 0; i < numFiles; i++) {
      fileIds.add(insertFileForApplicant(guestUser.id, /* programId= */ i).id);
    }

    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.ENABLED);

    for (Long fileId : fileIds) {
      StoredFileModel fileAfterMerge = lookupFile(fileId);
      assertThat(fileAfterMerge.getAcls().getApplicantReadAcls())
          .withFailMessage(() -> "File id %d".formatted(fileId))
          .containsExactly(cfUser.id);
    }

    assertThat(lookupUserFiles(cfUser.id)).isEmpty();
    assertThat(lookupUserFiles(guestUser.id)).containsExactlyElementsOf(fileIds);
  }

  @Test
  public void mergeGuestFilesIntoCfUser_enabled_cfUserFilesNotAffected() {
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();
    cfUser.refresh();
    guestUser.refresh();

    StoredFileModel cfFile = insertFileForApplicant(cfUser.id);
    StoredFileModel guestFile = insertFileForApplicant(guestUser.id);

    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.ENABLED);

    // CF user's own file ACLs are not modified.
    assertThat(lookupFile(cfFile.id).getAcls().getApplicantReadAcls())
        .doesNotContain(cfUser.id, guestUser.id);
    // Guest file gets cfUser added.
    assertThat(lookupFile(guestFile.id).getAcls().getApplicantReadAcls())
        .containsExactly(cfUser.id);
  }

  private StoredFileModel insertFileForApplicant(long applicantId) {
    return insertFileForApplicant(applicantId, /* programId= */ 1L);
  }

  private StoredFileModel insertFileForApplicant(long applicantId, long programId) {
    StoredFileModel file = new StoredFileModel();
    file.setName(
        ApplicantFileNameFormatter.formatFileUploadQuestionFilename(
            applicantId, programId, /* blockId= */ "1"));
    file.save();
    return file;
  }

  private StoredFileModel lookupFile(long fileId) {
    return storedFileRepository.lookupFile(fileId).toCompletableFuture().join().orElseThrow();
  }

  private List<Long> lookupUserFiles(long applicantId) {
    return storedFileRepository
        .lookupFilesByApplicant(applicantId)
        .toCompletableFuture()
        .join()
        .stream()
        .map(f -> f.id)
        .toList();
  }

  // Paths used in mergeQuestionAnswers tests. Using the applicant namespace
  // with distinct question-like sub-paths.
  private static final Path GUEST_ONLY_PATH =
      ApplicantData.APPLICANT_PATH.join("favorite_color").join("text");
  private static final Path CF_ONLY_PATH =
      ApplicantData.APPLICANT_PATH.join("occupation").join("text");
  private static final Path SHARED_PATH = ApplicantData.APPLICANT_PATH.join("city").join("text");

  @Test
  public void mergeQuestionAnswers_guestOnlyPath_copiedToCfUser() {
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    String guestColor = "blue";
    guestUser.getApplicantData().putString(GUEST_ONLY_PATH, guestColor);
    guestUser.save();
    cfUser.refresh();
    guestUser.refresh();

    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.ENABLED);

    ApplicantModel updatedCfUser = acctRepo.lookupApplicantSync(cfUser.id).orElseThrow();
    assertThat(updatedCfUser.getApplicantData().readString(GUEST_ONLY_PATH)).hasValue(guestColor);
  }

  @Test
  public void mergeQuestionAnswers_cfOnlyPath_retainedOnCfUser() {
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    String cfJob = "engineer";
    cfUser.getApplicantData().putString(CF_ONLY_PATH, cfJob);
    cfUser.save();
    cfUser.refresh();
    guestUser.refresh();

    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.ENABLED);

    ApplicantModel updatedCfUser = acctRepo.lookupApplicantSync(cfUser.id).orElseThrow();
    assertThat(updatedCfUser.getApplicantData().readString(CF_ONLY_PATH)).hasValue(cfJob);
  }

  @Test
  public void mergeQuestionAnswers_sharedPath_differentValues_guestValueRetained() {
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    cfUser.getApplicantData().putString(SHARED_PATH, "Springfield");
    cfUser.save();
    String retainedCity = "Shelbyville";
    guestUser.getApplicantData().putString(SHARED_PATH, retainedCity);
    guestUser.save();
    cfUser.refresh();
    guestUser.refresh();

    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.ENABLED);

    ApplicantModel updatedCfUser = acctRepo.lookupApplicantSync(cfUser.id).orElseThrow();
    assertThat(updatedCfUser.getApplicantData().readString(SHARED_PATH)).hasValue(retainedCity);
  }

  @Test
  public void mergeQuestionAnswers_sharedPath_sameValue_valuePreserved() {
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    String retainedCity = "Springfield";
    cfUser.getApplicantData().putString(SHARED_PATH, retainedCity);
    cfUser.save();
    guestUser.getApplicantData().putString(SHARED_PATH, retainedCity);
    guestUser.save();
    cfUser.refresh();
    guestUser.refresh();

    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.ENABLED);

    ApplicantModel updatedCfUser = acctRepo.lookupApplicantSync(cfUser.id).orElseThrow();
    assertThat(updatedCfUser.getApplicantData().readString(SHARED_PATH)).hasValue(retainedCity);
  }

  @Test
  public void mergeQuestionAnswers_dryRun_cfUserDataUnchanged() {
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    cfUser.getApplicantData().putString(CF_ONLY_PATH, "engineer");
    cfUser.save();
    guestUser.getApplicantData().putString(GUEST_ONLY_PATH, "blue");
    guestUser.getApplicantData().putString(SHARED_PATH, "Shelbyville");
    guestUser.save();
    cfUser.refresh();
    guestUser.refresh();

    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.DRY_RUN);

    ApplicantModel updatedCfUser = acctRepo.lookupApplicantSync(cfUser.id).orElseThrow();
    assertThat(updatedCfUser.getApplicantData().readString(CF_ONLY_PATH)).hasValue("engineer");
    assertThat(updatedCfUser.getApplicantData().readString(GUEST_ONLY_PATH)).isEmpty();
    assertThat(updatedCfUser.getApplicantData().readString(SHARED_PATH)).isEmpty();
  }

  @Test
  public void mergeQuestionAnswers_guestDataUnchanged() {
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    guestUser.getApplicantData().putString(GUEST_ONLY_PATH, "blue");
    guestUser.getApplicantData().putString(SHARED_PATH, "Shelbyville");
    guestUser.save();
    cfUser.getApplicantData().putString(CF_ONLY_PATH, "engineer");
    cfUser.getApplicantData().putString(SHARED_PATH, "Springfield");
    cfUser.save();
    cfUser.refresh();
    guestUser.refresh();

    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.ENABLED);

    ApplicantModel updatedGuest = acctRepo.lookupApplicantSync(guestUser.id).orElseThrow();
    assertThat(updatedGuest.getApplicantData().readString(GUEST_ONLY_PATH)).hasValue("blue");
    assertThat(updatedGuest.getApplicantData().readString(SHARED_PATH)).hasValue("Shelbyville");
    assertThat(updatedGuest.getApplicantData().readString(CF_ONLY_PATH)).isEmpty();
  }

  // ── PAI merge tests ───────────────────────────────────────────────────────
  // These test the PAI (Primary Applicant Information) fields: name, email,
  // phone/country code, and date of birth. Guest values win when present.

  @Test
  @Parameters({"true", "false"})
  public void mergeQuestionAnswers_pais_guestHasAll_guestRetained(boolean cfUserHasPais) {
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    if (cfUserHasPais) {
      cfUser.setUserName(
          "CfFirst", Optional.of("CfMiddle"), Optional.of("CfLast"), Optional.of("Sr."));
      cfUser.setEmailAddress("cf@example.com");
      cfUser.setPhoneNumber("2535559999");
      // Setting phone number sets the country code, but only US numbers are
      // accepted in practice. So to test that this is changed we force it to be
      // different from the expectation.
      cfUser.setCountryCode("CA");
      cfUser.setDateOfBirth(LocalDate.of(1985, 6, 20));
      cfUser.save();
    }

    guestUser.setUserName(
        "GuestFirst", Optional.of("GuestMiddle"), Optional.of("GuestLast"), Optional.of("Jr."));
    guestUser.setEmailAddress("guest@example.com");
    guestUser.setPhoneNumber("4035551122");
    guestUser.setDateOfBirth(LocalDate.of(1990, 1, 15));
    guestUser.save();
    cfUser.refresh();
    guestUser.refresh();

    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.ENABLED);

    ApplicantModel updated = acctRepo.lookupApplicantSync(cfUser.id).orElseThrow();
    assertThat(updated.getFirstName()).hasValue("GuestFirst");
    assertThat(updated.getMiddleName()).hasValue("GuestMiddle");
    assertThat(updated.getLastName()).hasValue("GuestLast");
    assertThat(updated.getSuffix()).hasValue("Jr.");
    assertThat(updated.getEmailAddress()).hasValue("guest@example.com");
    assertThat(updated.getPhoneNumber()).hasValue("4035551122");
    assertThat(updated.getCountryCode()).hasValue("US");
    assertThat(updated.getDateOfBirth()).hasValue(LocalDate.of(1990, 1, 15));
  }

  @Test
  public void mergeQuestionAnswers_pais_guestEmpty_cfUserRetained() {
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    cfUser.setUserName(
        "CfFirst", Optional.of("CfMiddle"), Optional.of("CfLast"), Optional.of("Sr."));
    cfUser.setEmailAddress("cf@example.com");
    cfUser.setPhoneNumber("2535559999");
    cfUser.setCountryCode("US");
    cfUser.setDateOfBirth(LocalDate.of(1985, 6, 20));
    cfUser.save();
    cfUser.refresh();
    guestUser.refresh();

    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.ENABLED);

    ApplicantModel refreshedCfUser = acctRepo.lookupApplicantSync(cfUser.id).orElseThrow();
    assertThat(refreshedCfUser.getFirstName()).hasValue("CfFirst");
    assertThat(refreshedCfUser.getMiddleName()).hasValue("CfMiddle");
    assertThat(refreshedCfUser.getLastName()).hasValue("CfLast");
    assertThat(refreshedCfUser.getSuffix()).hasValue("Sr.");
    assertThat(refreshedCfUser.getEmailAddress()).hasValue("cf@example.com");
    assertThat(refreshedCfUser.getPhoneNumber()).hasValue("2535559999");
    assertThat(refreshedCfUser.getCountryCode()).hasValue("US");
    assertThat(refreshedCfUser.getDateOfBirth()).hasValue(LocalDate.of(1985, 6, 20));
  }

  @Test
  public void mergeQuestionAnswers_pais_nameRequiresBothFirstAndLast() {
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    cfUser.setUserName("CfFirst", Optional.empty(), Optional.of("CfLast"), Optional.empty());
    cfUser.save();

    // Guest has first name but no last name — name should NOT be copied.
    guestUser.setFirstName("GuestFirst");
    guestUser.save();
    cfUser.refresh();
    guestUser.refresh();

    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.ENABLED);

    ApplicantModel updated = acctRepo.lookupApplicantSync(cfUser.id).orElseThrow();
    assertThat(updated.getFirstName()).hasValue("CfFirst");
    assertThat(updated.getLastName()).hasValue("CfLast");
  }

  @Test
  public void mergeQuestionAnswers_pais_bothHavePhone_guestRetained() {
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    cfUser.setPhoneNumber("2535559999");
    cfUser.save();
    String guestPhoneNum = "2535551122";
    guestUser.setPhoneNumber(guestPhoneNum);
    guestUser.save();
    cfUser.refresh();
    guestUser.refresh();

    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.ENABLED);

    ApplicantModel updated = acctRepo.lookupApplicantSync(cfUser.id).orElseThrow();
    assertThat(updated.getPhoneNumber()).hasValue(guestPhoneNum);
  }

  @Test
  public void mergeQuestionAnswers_pais_dryRun_noChanges() {
    ApplicantModel cfUser =
        resourceCreator.insertApplicantWithAccount(Optional.of("cf@example.com"));
    ApplicantModel guestUser = resourceCreator.insertApplicantWithAccount();

    guestUser.setUserName(
        "GuestFirst", Optional.of("GuestMiddle"), Optional.of("GuestLast"), Optional.of("Jr."));
    guestUser.setEmailAddress("guest@example.com");
    guestUser.setPhoneNumber("2535551122");
    guestUser.setCountryCode("US");
    guestUser.setDateOfBirth(LocalDate.of(1990, 1, 15));
    guestUser.save();
    cfUser.refresh();
    guestUser.refresh();

    merger.mergeApplicants(cfUser, guestUser, NewGuestMergeLaunchStage.DRY_RUN);

    ApplicantModel updated = acctRepo.lookupApplicantSync(cfUser.id).orElseThrow();
    assertThat(updated.getFirstName()).isEmpty();
    assertThat(updated.getLastName()).isEmpty();
    assertThat(updated.getEmailAddress()).isEmpty();
    assertThat(updated.getPhoneNumber()).isEmpty();
    assertThat(updated.getDateOfBirth()).isEmpty();
  }
}
