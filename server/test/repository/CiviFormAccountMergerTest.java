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
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.ApplicantModel;
import models.ApplicationModel;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.settings.SettingsManifest;

@RunWith(JUnitParamsRunner.class)
public class CiviFormAccountMergerTest extends ResetPostgres {
  private CiviFormAccountMerger merger;
  private AccountRepository acctRepo;
  private ApplicationRepository applRepo;

  @Before
  public void setupApplicantRepository() {
    SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);
    merger = new CiviFormAccountMerger();
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

  @Test
  @Parameters({"CIVIFORM", "GUEST"})
  public void mergeApplicants_dryRun_bothActive_oneNewer_noChanges(Newer newerApplication) {
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
    ImmutableMap<Long, ApplicationModel> cfIdToApplication =
        acctRepo.lookupApplicantSync(cfUser.id).orElseThrow().getApplications().stream()
            .collect(ImmutableMap.toImmutableMap(a -> a.id, a -> a));
    assertThat(cfIdToApplication.keySet())
        .containsExactlyInAnyOrder(cfObsolete.id, cfActive.id, cfDraft.id);
    assertThat(cfIdToApplication.get(cfObsolete.id).getLifecycleStage()).isEqualTo(OBSOLETE);
    assertThat(cfIdToApplication.get(cfActive.id).getLifecycleStage()).isEqualTo(ACTIVE);
    assertThat(cfIdToApplication.get(cfDraft.id).getLifecycleStage()).isEqualTo(DRAFT);

    // Verify guestUser's applications are unchanged.
    ImmutableMap<Long, ApplicationModel> guestIdToApplication =
        acctRepo.lookupApplicantSync(guestUser.id).orElseThrow().getApplications().stream()
            .collect(ImmutableMap.toImmutableMap(a -> a.id, a -> a));
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
  public void mergeApplicants_bothActive_oneNewer(Newer newerApplication) {
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
    ImmutableMap<Long, ApplicationModel> idToApplication =
        acctRepo.lookupApplicantSync(cfUser.id).orElseThrow().getApplications().stream()
            .collect(ImmutableMap.toImmutableMap(a -> a.id, a -> a));

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

    // Guest draft is moved to cfUser.
    assertThat(cfApps).extracting(a -> a.id).containsExactly(guestDraft.id);
    // CF draft is deleted
    assertThat(applRepo.getApplication(cfDraft.id).toCompletableFuture().join()).isEmpty();
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
    ImmutableMap<Long, ApplicationModel> idToApplication =
        acctRepo.lookupApplicantSync(cfUser.id).orElseThrow().getApplications().stream()
            .collect(ImmutableMap.toImmutableMap(a -> a.id, a -> a));

    // CF's active and draft are preserved with their original lifecycle stages.
    assertThat(idToApplication.keySet()).containsExactlyInAnyOrder(cfActive.id, cfDraft.id);
    assertThat(idToApplication.get(cfActive.id).getLifecycleStage()).isEqualTo(ACTIVE);
    assertThat(idToApplication.get(cfDraft.id).getLifecycleStage()).isEqualTo(DRAFT);

    // Guest draft is deleted.
    assertThat(applRepo.getApplication(guestDraft.id).toCompletableFuture().join()).isEmpty();
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
}
