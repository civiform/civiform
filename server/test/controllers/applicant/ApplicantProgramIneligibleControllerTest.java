package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import controllers.WithMockedProfiles;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import models.ApplicantModel;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.i18n.MessagesApi;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.applicant.ApplicantService;
import services.monitoring.MonitoringMetricCounters;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.applicant.ineligible.ApplicantIneligibleView;

@RunWith(JUnitParamsRunner.class)
public class ApplicantProgramIneligibleControllerTest extends WithMockedProfiles {

  private ApplicantProgramIneligibleController subject;
  private ProgramModel program;
  private ApplicantModel applicant;
  private SettingsManifest settingsManifest;

  @Before
  public void setUpWithFreshApplicant() {
    resetDatabase();

    program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    applicant = createApplicantWithMockedProfile();

    settingsManifest = mock(SettingsManifest.class);
    subject =
        new ApplicantProgramIneligibleController(
            instanceOf(ApplicantService.class),
            instanceOf(MessagesApi.class),
            instanceOf(ApplicantIneligibleView.class),
            instanceOf(ProfileUtils.class),
            settingsManifest,
            instanceOf(ProgramService.class),
            instanceOf(VersionRepository.class),
            instanceOf(ProgramSlugHandler.class),
            instanceOf(MonitoringMetricCounters.class));
  }

  @Test
  public void ineligible_whenFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    ProgramModel program = ProgramBuilder.newActiveProgram().build();
    String programId = String.valueOf(program.id);

    Request request = fakeRequestBuilder().build();
    when(settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject.ineligible(request, programId, Optional.empty()).toCompletableFuture().join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  /**
   * Tests that ineligible() throws an error when the program param is a program slug but it should
   * be the program id since the program slug feature is disabled. ineligible() also throws error
   * for other combinations when the program param is not properly parsed. We don't test all
   * combinations here because ProgramSlugHandler has comprehensive tests coverage for them.
   */
  @Test
  public void ineligible_whenProgramSlugUrlsFeatureDisabledAndIsProgramSlugFromUrl_error() {
    String programSlug = program.getSlug();
    assertThatThrownBy(
            () -> subject.ineligible(fakeRequest(), programSlug, /* blockId= */ Optional.empty()))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from '' to a numeric value");
  }

  /**
   * Tests that ineligible() returns OK when the feature is enabled and is from url call with a
   * program slug.
   */
  @Test
  public void ineligible_whenProgramSlugUrlsFeatureEnabledAndIsProgramSlugFromUrl_isOk() {
    String programSlug = program.getSlug();
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .ineligible(request, programSlug, /* blockId= */ Optional.empty())
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void ineligible_whenProgramSlugUrlsFeatureDisabledAndIsProgramIdFromUrl_isOk() {
    Result result =
        subject
            .ineligible(fakeRequest(), String.valueOf(program.id), /* blockId= */ Optional.empty())
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void ineligible_withBlockId_isOk() {
    String programSlug = program.getSlug();
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .ineligible(request, programSlug, /* blockId= */ Optional.of("1"))
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void ineligible_noApplicantIdFound_redirectsToHome() {
    String programSlug = program.getSlug();
    Request request = fakeRequestBuilder().header(skipUserProfile, "true").build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .ineligible(request, programSlug, /* blockId= */ Optional.of("1"))
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void
      ineligibleWithApplicantId_whenProgramSlugUrlsFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);
    String programId = Long.toString(program.id);

    Result result =
        subject
            .ineligibleWithApplicantId(
                request, applicant.id, programId, /* blockId= */ Optional.empty())
            .toCompletableFuture()
            .join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  /**
   * Tests that ineligibleWithApplicantId() throws an error when the program param is a program slug
   * but it should be the program id since the program slug feature is disabled.
   */
  @Test
  public void
      ineligibleWithApplicantId_whenProgramSlugUrlsFeatureDisabledAndIsProgramSlugFromUrl_error() {
    String programSlug = program.getSlug();
    assertThatThrownBy(
            () ->
                subject.ineligibleWithApplicantId(
                    fakeRequest(), applicant.id, programSlug, /* blockId= */ Optional.empty()))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from '' to a numeric value");
  }

  /**
   * Tests that ineligibleWithApplicantId() returns OK when the feature is enabled and is from url
   * call with a program slug.
   */
  @Test
  public void
      ineligibleWithApplicantId_whenProgramSlugUrlsFeatureEnabledAndIsProgramSlugFromUrl_isOk() {
    String programSlug = program.getSlug();
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .ineligibleWithApplicantId(
                request, applicant.id, programSlug, /* blockId= */ Optional.empty())
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void
      ineligibleWithApplicantId_whenProgramSlugUrlsFeatureDisabledAndIsProgramIdFromUrl_isOk() {
    Result result =
        subject
            .ineligibleWithApplicantId(
                fakeRequest(),
                applicant.id,
                String.valueOf(program.id),
                /* blockId= */ Optional.empty())
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }
}
