package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.FORBIDDEN;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import controllers.WithMockedProfiles;
import controllers.geo.AddressSuggestionJsonSerializer;
import junitparams.JUnitParamsRunner;
import models.ApplicantModel;
import models.ProgramModel;
import models.QuestionModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.StoredFileRepository;
import repository.VersionRepository;
import services.Path;
import services.applicant.ApplicantService;
import services.applicant.question.Scalar;
import services.cloud.ApplicantStorageClient;
import services.monitoring.MonitoringMetricCounters;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.applicant.addresscorrection.AddressCorrectionBlockView;
import views.applicant.blocks.ApplicantProgramBlockEditView;

@RunWith(JUnitParamsRunner.class)
public class ApplicantAddressCorrectionControllerTest extends WithMockedProfiles {

  private ApplicantAddressCorrectionController subject;
  private ProgramModel program;
  private ApplicantModel applicant;
  private SettingsManifest settingsManifest;
  private ApplicantProgramBlocksController programBlocksController;

  @Before
  public void setUpWithFreshApplicant() {
    resetDatabase();

    QuestionModel addressQuestion = testQuestionBank().addressApplicantAddress();
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredCorrectedAddressQuestion(addressQuestion)
            .build();
    applicant = createApplicantWithMockedProfile();

    settingsManifest = mock(SettingsManifest.class);

    programBlocksController =
        new ApplicantProgramBlocksController(
            instanceOf(ApplicantService.class),
            instanceOf(MessagesApi.class),
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(ApplicantProgramBlockEditView.class),
            instanceOf(FormFactory.class),
            instanceOf(ApplicantStorageClient.class),
            instanceOf(StoredFileRepository.class),
            instanceOf(ProfileUtils.class),
            instanceOf(Config.class),
            settingsManifest,
            instanceOf(AddressSuggestionJsonSerializer.class),
            instanceOf(ProgramService.class),
            instanceOf(VersionRepository.class),
            instanceOf(ProgramSlugHandler.class),
            instanceOf(ApplicantRoutes.class),
            instanceOf(EligibilityAlertSettingsCalculator.class),
            instanceOf(MonitoringMetricCounters.class));

    subject =
        new ApplicantAddressCorrectionController(
            instanceOf(ApplicantService.class),
            instanceOf(MessagesApi.class),
            instanceOf(AddressCorrectionBlockView.class),
            instanceOf(ProfileUtils.class),
            settingsManifest,
            instanceOf(VersionRepository.class),
            instanceOf(ProgramSlugHandler.class),
            instanceOf(AddressSuggestionJsonSerializer.class),
            instanceOf(MonitoringMetricCounters.class));
  }

  @Test
  public void addressCorrection_whenProgramSlugUrlsEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    Request request = fakeRequestBuilder().build();
    when(settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .addressCorrection(
                request,
                String.valueOf(program.id),
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();
    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void addressCorrection_whenProgramSlugUrlsFeatureDisabledAndIsProgramSlugFromUrl_error() {
    assertThatThrownBy(
            () ->
                subject.addressCorrection(
                    fakeRequest(),
                    program.getSlug(),
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper()))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from '' to a numeric value");
  }

  @Test
  public void addressCorrection_whenProgramSlugUrlsFeatureEnabledAndIsProgramSlugFromUrl_isOk() {
    Request request = fakeRequestBuilder().build();
    when(settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);
    answerAddressQuestion();

    Result result =
        subject
            .addressCorrection(
                request,
                program.getSlug(),
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void addressCorrection_whenProgramSlugUrlsFeatureDisabledAndIsProgramIdFromUrl_isOk() {
    answerAddressQuestion();

    Result result =
        subject
            .addressCorrection(
                fakeRequest(),
                String.valueOf(program.id),
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void addressCorrection_noApplicantIdFound_redirectsToHome() {
    Request request = fakeRequestBuilder().header(skipUserProfile, "true").build();

    Result result =
        subject
            .addressCorrection(
                request,
                String.valueOf(program.id),
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void
      addressCorrectionWithApplicantId_whenProgramSlugUrlsFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    Request request = fakeRequestBuilder().build();
    when(settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .addressCorrectionWithApplicantId(
                request,
                applicant.id,
                String.valueOf(program.id),
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void
      addressCorrectionWithApplicantId_whenProgramSlugUrlsFeatureDisabledAndIsProgramSlugFromUrl_error() {
    assertThatThrownBy(
            () ->
                subject.addressCorrectionWithApplicantId(
                    fakeRequest(),
                    applicant.id,
                    program.getSlug(),
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper()))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from '' to a numeric value");
  }

  @Test
  public void
      addressCorrectionWithApplicantId_whenProgramSlugUrlsFeatureEnabledAndIsProgramSlugFromUrl_isOk() {
    Request request = fakeRequestBuilder().build();
    when(settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);
    answerAddressQuestion();

    Result result =
        subject
            .addressCorrectionWithApplicantId(
                request,
                applicant.id,
                program.getSlug(),
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void
      addressCorrectionWithApplicantId_whenProgramSlugUrlsFeatureDisabledAndIsProgramIdFromUrl_isOk() {
    answerAddressQuestion();

    Result result =
        subject
            .addressCorrectionWithApplicantId(
                fakeRequest(),
                applicant.id,
                String.valueOf(program.id),
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void addressCorrectionWithApplicantId_returnsForbiddenForApplicantNotOwned() {
    createTIWithMockedProfile(applicant);
    ApplicantModel otherApplicant = createApplicant();

    Result result =
        subject
            .addressCorrectionWithApplicantId(
                fakeRequest(),
                otherApplicant.id,
                String.valueOf(program.id),
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(FORBIDDEN);
  }

  public void answerAddressQuestion() {
    Request answerAddressQuestionRequest =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_address").join(Scalar.STREET).toString(),
                    "Legit Address",
                    Path.create("applicant.applicant_address").join(Scalar.LINE2).toString(),
                    "",
                    Path.create("applicant.applicant_address").join(Scalar.CITY).toString(),
                    "Boston",
                    Path.create("applicant.applicant_address").join(Scalar.STATE).toString(),
                    "MA",
                    Path.create("applicant.applicant_address").join(Scalar.ZIP).toString(),
                    "02111"))
            .build();
    programBlocksController
        .updateWithApplicantId(
            answerAddressQuestionRequest,
            applicant.id,
            String.valueOf(program.id),
            /* blockId= */ "1",
            /* inReview= */ false,
            new ApplicantRequestedActionWrapper())
        .toCompletableFuture()
        .join();
  }
}
