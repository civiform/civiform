package controllers.applicant;

import static controllers.applicant.ApplicantProgramBlocksController.ADDRESS_JSON_SESSION_KEY;
import static controllers.applicant.ApplicantRequestedAction.NEXT_BLOCK;
import static controllers.applicant.ApplicantRequestedAction.PREVIOUS_BLOCK;
import static controllers.applicant.ApplicantRequestedAction.REVIEW_PAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.stubMessagesApi;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import controllers.WithMockedProfiles;
import controllers.geo.AddressSuggestionJsonSerializer;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.AccountModel;
import models.ApplicantModel;
import models.LifecycleStage;
import models.ProgramModel;
import models.StoredFileModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import repository.StoredFileRepository;
import repository.VersionRepository;
import services.Address;
import services.LocalizedStrings;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ApplicantService;
import services.applicant.question.Scalar;
import services.cloud.ApplicantStorageClient;
import services.geo.AddressLocation;
import services.geo.AddressSuggestion;
import services.monitoring.MonitoringMetricCounters;
import services.program.ProgramService;
import services.question.QuestionAnswerer;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.applicant.AddressCorrectionBlockView;
import views.applicant.ApplicantFileUploadRenderer;
import views.applicant.ApplicantProgramBlockEditViewFactory;
import views.applicant.NorthStarAddressCorrectionBlockView;
import views.applicant.NorthStarApplicantIneligibleView;
import views.applicant.NorthStarApplicantProgramBlockEditView;

@RunWith(JUnitParamsRunner.class)
public class ApplicantProgramBlocksControllerTest extends WithMockedProfiles {
  private static final String SUGGESTED_ADDRESS = "456 Suggested Ave, Seattle, Washington, 99999";
  private static final String SUGGESTED_ADDRESS_STREET = "456 Suggested Ave";

  private ApplicantProgramBlocksController subject;
  private AddressSuggestionJsonSerializer addressSuggestionJsonSerializer;
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
            .withBlock()
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .withBlock()
            .withRequiredQuestion(testQuestionBank().radioApplicantFavoriteSeason())
            .build();
    applicant = createApplicantWithMockedProfile();

    settingsManifest = mock(SettingsManifest.class);
    addressSuggestionJsonSerializer = instanceOf(AddressSuggestionJsonSerializer.class);
    subject =
        new ApplicantProgramBlocksController(
            instanceOf(ApplicantService.class),
            instanceOf(MessagesApi.class),
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(ApplicantProgramBlockEditViewFactory.class),
            instanceOf(NorthStarApplicantProgramBlockEditView.class),
            instanceOf(FormFactory.class),
            instanceOf(ApplicantStorageClient.class),
            instanceOf(StoredFileRepository.class),
            instanceOf(ProfileUtils.class),
            instanceOf(Config.class),
            settingsManifest,
            instanceOf(ApplicantFileUploadRenderer.class),
            instanceOf(NorthStarApplicantIneligibleView.class),
            instanceOf(AddressCorrectionBlockView.class),
            instanceOf(NorthStarAddressCorrectionBlockView.class),
            addressSuggestionJsonSerializer,
            instanceOf(ProgramService.class),
            instanceOf(VersionRepository.class),
            instanceOf(ProgramSlugHandler.class),
            instanceOf(ApplicantRoutes.class),
            instanceOf(EligibilityAlertSettingsCalculator.class),
            instanceOf(MonitoringMetricCounters.class));
  }

  @Test
  public void edit_whenFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    ProgramModel program = ProgramBuilder.newActiveProgram().build();
    String programId = String.valueOf(program.id);

    Request request = fakeRequestBuilder().build();
    when(settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .edit(
                request,
                programId,
                "1",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  /**
   * Tests that edit() throws an error when the program param is a program slug but it should be the
   * program id since the program slug feature is disabled. edit() also throws error for other
   * combinations when the program param is not properly parsed. We don't test all combinations here
   * because ProgramSlugHandler has comprehensive tests coverage for them.
   */
  @Test
  public void edit_whenProgramSlugUrlsFeatureDisabledAndIsProgramSlugFromUrl_error() {
    String programSlug = program.getSlug();
    assertThatThrownBy(
            () ->
                subject.edit(
                    fakeRequest(),
                    programSlug,
                    /* blockId= */ "1",
                    /* questionName= */ Optional.empty(),
                    /* isFromUrlCall= */ true))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from '' to a numeric value");
  }

  /**
   * Tests that edit() returns OK when the feature is enabled and is from url call with a program
   * slug. edit() also returns OK for other combinations: when the feature is disabled OR when the
   * call is not from a URL OR when the program param is a program slug (not numeric), AND the
   * program ID was properly retrieved. We don't test all combinations here because the
   * ProgramSlugHandler has comprehensive tests coverage for them.
   */
  @Test
  public void edit_whenProgramSlugUrlsFeatureEnabledAndIsProgramSlugFromUrl_isOk() {
    String programSlug = program.getSlug();
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .edit(
                request,
                programSlug,
                /* blockId= */ "1",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void
      editWithApplicantId_whenProgramSlugUrlsFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);
    String programId = Long.toString(program.id);

    Result result =
        subject
            .editWithApplicantId(
                request,
                applicant.id,
                programId,
                /* blockId= */ "1",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  /**
   * Tests that editWithApplicantId() throws an error when the program param is a program slug but
   * it should be the program id since the program slug feature is disabled. editWithApplicantId()
   * also throws error for other combinations when the program param is not properly parsed. We
   * don't test all combinations here because ProgramSlugHandler have a comprehensive test cover for
   * them.
   */
  @Test
  public void
      editWithApplicantId_whenProgramSlugUrlsFeatureDisabledAndIsProgramSlugFromUrl_error() {
    String programSlug = program.getSlug();
    assertThatThrownBy(
            () ->
                subject.editWithApplicantId(
                    fakeRequest(),
                    applicant.id,
                    programSlug,
                    /* blockId= */ "1",
                    /* questionName= */ Optional.empty(),
                    /* isFromUrlCall= */ false))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from '' to a numeric value");
  }

  /**
   * Tests that editWithApplicantId() returns OK when the feature is enabled and is from url call
   * with a program slug. editWithApplicantId() also returns OK for other combinations: when the
   * feature is disabled OR when the call is not from a URL OR when the program param is a program
   * slug (not numeric), AND the program ID was properly retrieved. We don't test all combinations
   * here because the ProgramSlugHandler tests have a comprehensive test cover for them.
   */
  @Test
  public void editWithApplicantId_whenProgramSlugUrlsFeatureEnabledAndIsProgramSlugFromUrl_isOk() {
    String programSlug = program.getSlug();
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .editWithApplicantId(
                request,
                applicant.id,
                programSlug,
                /* blockId= */ "1",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void editWithApplicantId_invalidApplicant_returnsUnauthorized() {
    long badApplicantId = applicant.id + 1000;
    String programId = Long.toString(program.id);

    Result result =
        subject
            .editWithApplicantId(
                fakeRequest(),
                badApplicantId,
                programId,
                /* blockId= */ "1",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void editWithApplicantId_applicantAccessToDraftProgram_returnsUnauthorized() {
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    String draftProgramId = Long.toString(draftProgram.id);

    Result result =
        subject
            .editWithApplicantId(
                fakeRequest(),
                applicant.id,
                draftProgramId,
                /* blockId= */ "1",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void editWithApplicantId_civiformAdminAccessToDraftProgram_isOk() {

    AccountModel adminAccount = createGlobalAdminWithMockedProfile();
    applicant = adminAccount.representativeApplicant().orElseThrow();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    String draftProgramId = Long.toString(draftProgram.id);

    Result result =
        subject
            .editWithApplicantId(
                fakeRequest(),
                applicant.id,
                draftProgramId,
                /* blockId= */ "1",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void editWithApplicantId_obsoleteProgram_isOk() {
    ProgramModel obsoleteProgram = ProgramBuilder.newObsoleteProgram("program").build();
    String obsoleteProgramId = Long.toString(obsoleteProgram.id);

    Result result =
        subject
            .editWithApplicantId(
                fakeRequest(),
                applicant.id,
                obsoleteProgramId,
                /* blockId= */ "1",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void editWithApplicantId_toAProgramThatDoesNotExist_returns404() {
    String programId = Long.toString(program.id);

    Result result =
        subject
            .editWithApplicantId(
                fakeRequest(),
                applicant.id,
                programId + 1000,
                /* blockId= */ "1",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void editWithApplicantId_toAnExistingBlock_rendersTheBlock() {
    String programId = Long.toString(program.id);

    Result result =
        subject
            .editWithApplicantId(
                fakeRequest(),
                applicant.id,
                programId,
                /* blockId= */ "1",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void editWithApplicantId_toABlockThatDoesNotExist_returns404() {
    String programId = Long.toString(program.id);

    Result result =
        subject
            .editWithApplicantId(
                fakeRequest(),
                applicant.id,
                programId,
                /* blockId= */ "9999",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void editWithApplicantId_withMessages_returnsCorrectButtonText() {
    String programId = Long.toString(program.id);
    Request request =
        fakeRequestBuilder().langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi()).build();
    when(settingsManifest.getNorthStarApplicantUi()).thenReturn(false);

    Result result =
        subject
            .editWithApplicantId(
                request,
                applicant.id,
                programId,
                /* blockId= */ "1",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Guardar y continuar");
  }

  @Test
  public void northStar_editWithApplicantId_withMessages_returnsCorrectButtonText() {
    String programId = Long.toString(program.id);

    Request request =
        fakeRequestBuilder().langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi()).build();
    when(settingsManifest.getNorthStarApplicantUi()).thenReturn(true);

    Result result =
        subject
            .editWithApplicantId(
                request,
                applicant.id,
                programId,
                /* blockId= */ "1",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Continuar");
  }

  @Test
  public void review_whenProgramSlugUrlsFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .review(
                request,
                Long.toString(program.id),
                /* blockId= */ "1",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  /**
   * Tests that review() throws an error when the program param is a program slug but it should be
   * the program id since the program slug feature is disabled. review() also throws error for other
   * combinations when the program param is not properly parsed. We don't test all combinations here
   * because ProgramSlugHandler have a comprehensive test cover for them.
   */
  @Test
  public void review_whenProgramSlugUrlsFeatureDisabledAndIsProgramSlugFromUrl_error() {
    String programSlug = program.getSlug();
    assertThatThrownBy(
            () ->
                subject.review(
                    fakeRequest(),
                    programSlug,
                    /* blockId= */ "1",
                    /* questionName= */ Optional.empty(),
                    /* isFromUrlCall= */ true))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from '' to a numeric value");
  }

  /**
   * Tests that review() returns OK when the feature is enabled and is from url call with a program
   * slug. review() also returns OK for other combinations: when the feature is disabled OR when the
   * call is not from a URL OR when the program param is a program slug (not numeric), AND the
   * program ID was properly retrieved. We don't test all combinations here because the
   * ProgramSlugHandler tests have a comprehensive test cover for them.
   */
  @Test
  public void review_whenProgramSlugUrlsFeatureEnabledAndIsProgramSlugFromUrl_isOk() {
    String programSlug = program.getSlug();
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .review(
                request,
                programSlug,
                /* blockId= */ "1",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void
      reviewWithApplicantId_whenProgramSlugUrlsFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);
    String programId = Long.toString(program.id);

    Result result =
        subject
            .reviewWithApplicantId(
                request,
                applicant.id,
                programId,
                /* blockId= */ "1",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  /**
   * Tests that reviewWithApplicantId() throws an error when the program param is a program slug but
   * it should be the program id since the program slug feature is disabled. reviewWithApplicantId()
   * also throws error for other combinations when the program param is not properly parsed. We
   * don't test all combinations here because ProgramSlugHandler have a comprehensive test cover for
   * them.
   */
  @Test
  public void
      reviewWithApplicantId_whenProgramSlugUrlsFeatureDisabledAndIsProgramSlugFromUrl_error() {
    ProgramModel activeProgram = ProgramBuilder.newActiveProgram("Program").build();
    String programSlug = activeProgram.getSlug();
    assertThatThrownBy(
            () ->
                subject.reviewWithApplicantId(
                    fakeRequest(),
                    applicant.id,
                    programSlug,
                    /* blockId= */ "1",
                    /* questionName= */ Optional.empty(),
                    /* isFromUrlCall= */ true))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from 'program' to a numeric value");
  }

  /**
   * Tests that reviewWithApplicantId() returns OK when the feature is enabled and is from url call
   * with a program slug. reviewWithApplicantId() also returns OK for other combinations: when the
   * feature is disabled OR when the call is not from a URL OR when the program param is a program
   * slug (not numeric), AND the program ID was properly retrieved. We don't test all combinations
   * here because the ProgramSlugHandler tests have a comprehensive test cover for them.
   */
  @Test
  public void
      reviewWithApplicantId_whenProgramSlugUrlsFeatureEnabledAndIsProgramSlugFromUrl_isOk() {
    String programSlug = program.getSlug();
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .reviewWithApplicantId(
                request,
                applicant.id,
                programSlug,
                /* blockId= */ "1",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void previous_whenProgramSlugUrlsFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .previous(
                request,
                Long.toString(program.id),
                /* previousBlockIndex= */ 0,
                /* inReview= */ true,
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  /**
   * Tests that previous() throws an error when the program param is a program slug but it should be
   * the program id since the program slug feature is disabled. previous() also throws error for
   * other combinations when the program param is not properly parsed. We don't test all
   * combinations here because ProgramSlugHandler have a comprehensive test cover for them.
   */
  @Test
  public void previous_whenProgramSlugUrlsFeatureDisabledAndIsProgramSlugFromUrl_error() {
    ProgramModel activeProgram = ProgramBuilder.newActiveProgram("Program").build();
    String programSlug = activeProgram.getSlug();
    assertThatThrownBy(
            () ->
                subject.previous(
                    fakeRequest(),
                    programSlug,
                    /* previousBlockIndex= */ 0,
                    /* inReview= */ true,
                    /* isFromUrlCall= */ true))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from 'program' to a numeric value");
  }

  /**
   * Tests that previous() returns OK when the feature is enabled and is from url call with a
   * program slug. previous() also returns OK for other combinations: when the feature is disabled
   * OR when the call is not from a URL OR when the program param is a program slug (not numeric),
   * AND the program ID was properly retrieved. We don't test all combinations here because the
   * ProgramSlugHandler tests have a comprehensive test cover for them.
   */
  @Test
  public void previous_whenProgramSlugUrlsFeatureEnabledAndIsProgramSlugFromUrl_isOk() {
    String programSlug = program.getSlug();
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .previous(
                request,
                programSlug,
                /* previousBlockIndex= */ 0,
                /* inReview= */ true,
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void
      previousWithApplicantId_whenProgramSlugUrlsFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .previousWithApplicantId(
                request,
                applicant.id,
                Long.toString(program.id),
                /* previousBlockIndex= */ 0,
                /* inReview= */ true,
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  /**
   * Tests that previousWithApplicantId() throws an error when the program param is a program slug
   * but it should be the program id since the program slug feature is disabled.
   * previousWithApplicantId() also throws error for other combinations when the program param is
   * not properly parsed. We don't test all combinations here because ProgramSlugHandler have a
   * comprehensive test cover for them.
   */
  @Test
  public void
      previousWithApplicantId_whenProgramSlugUrlsFeatureDisabledAndIsProgramSlugFromUrl_error() {
    ProgramModel activeProgram = ProgramBuilder.newActiveProgram("Program").build();
    String programSlug = activeProgram.getSlug();
    assertThatThrownBy(
            () ->
                subject.previousWithApplicantId(
                    fakeRequest(),
                    applicant.id,
                    programSlug,
                    /* previousBlockIndex= */ 0,
                    /* inReview= */ true,
                    /* isFromUrlCall= */ true))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from 'program' to a numeric value");
  }

  /**
   * Tests that previousWithApplicantId() returns OK when the feature is enabled and is from url
   * call with a program slug. previousWithApplicantId() also returns OK for other combinations:
   * when the feature is disabled OR when the call is not from a URL OR when the program param is a
   * program slug (not numeric), AND the program ID was properly retrieved. We don't test all
   * combinations here because the ProgramSlugHandler tests have a comprehensive test cover for
   * them.
   */
  @Test
  public void
      previousWithApplicantId_whenProgramSlugUrlsFeatureEnabledAndIsProgramSlugFromUrl_isOk() {
    String programSlug = program.getSlug();
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .previousWithApplicantId(
                request,
                applicant.id,
                programSlug,
                /* previousBlockIndex= */ 0,
                /* inReview= */ true,
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void previousWithApplicantId_toAnExistingBlock_rendersTheBlock() {
    String programId = Long.toString(program.id);

    Result result =
        subject
            .previousWithApplicantId(
                fakeRequest(), applicant.id, programId, 0, true, /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void previousWithApplicantId_applicantAccessToDraftProgram_returnsUnauthorized() {
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    String draftProgramId = Long.toString(draftProgram.id);

    Result result =
        subject
            .previousWithApplicantId(
                fakeRequest(), applicant.id, draftProgramId, 0, true, /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void previousWithApplicantId_civiformAdminAccessToDraftProgram_isOk() {
    AccountModel adminAccount = createGlobalAdminWithMockedProfile();
    applicant = adminAccount.representativeApplicant().orElseThrow();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    String draftProgramId = Long.toString(draftProgram.id);

    Result result =
        subject
            .previousWithApplicantId(
                fakeRequest(), applicant.id, draftProgramId, 0, true, /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void previousWithApplicantId_obsoleteProgram_isOk() {
    ProgramModel obsoleteProgram = ProgramBuilder.newObsoleteProgram("program").build();
    String obsoleteProgramId = Long.toString(obsoleteProgram.id);

    Result result =
        subject
            .previousWithApplicantId(
                fakeRequest(), applicant.id, obsoleteProgramId, 0, true, /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void update_invalidApplicant_returnsUnauthorized() {
    long badApplicantId = applicant.id + 1000;
    Request request = fakeRequestBuilder().build();

    Result result =
        subject
            .updateWithApplicantId(
                request,
                badApplicantId,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void update_applicantAccessToDraftProgram_returnsUnauthorized() {
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();

    Request request = fakeRequestBuilder().build();
    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                draftProgram.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void update_civiformAdminAccessToDraftProgram_isOk() {
    AccountModel adminAccount = createGlobalAdminWithMockedProfile();
    applicant = adminAccount.representativeApplicant().orElseThrow();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();

    Request request = fakeRequestBuilder().build();
    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                draftProgram.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void update_obsoleteProgram_isOk() {
    ProgramModel obsoleteProgram =
        ProgramBuilder.newObsoleteProgram("program")
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();

    Request request = fakeRequestBuilder().build();
    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                obsoleteProgram.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void update_invalidProgram_returnsBadRequest() {
    long badProgramId = program.id + 1000;
    Request request = fakeRequestBuilder().build();

    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                badProgramId,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_invalidBlock_returnsBadRequest() {
    String badBlockId = "1000";
    Request request = fakeRequestBuilder().build();

    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                program.id,
                badBlockId,
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_invalidPathsInRequest_returnsBadRequest() {
    Request request = fakeRequestBuilder().bodyForm(ImmutableMap.of("fake.path", "value")).build();

    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_reservedPathsInRequest_returnsBadRequest() {
    String reservedPath = Path.create("metadata").join(Scalar.PROGRAM_UPDATED_IN).toString();
    Request request = fakeRequestBuilder().bodyForm(ImmutableMap.of(reservedPath, "value")).build();

    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_name_withValidationErrors_showsError() {
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "FirstName",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    ""))
            .build();

    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("FirstName");
    assertThat(contentAsString(result)).contains("Please enter your last name.");
  }

  // The question has 4 options 1-4, Options are 1 based so 0 is not valid.
  @Test
  @Parameters({"0", "5", "-1", "1.1", "11111", "Not a Number", "&nbsp;"})
  public void update_radio_withValidationErrors_showsError(String errorValue) {
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_favorite_season")
                        .join(Scalar.SELECTION)
                        .toString(),
                    errorValue))
            .build();

    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "3",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(contentAsString(result)).contains("Please enter valid input");
  }

  @Test
  public void update_withValidationErrors_requestedActionReview_staysOnBlockAndShowsErrors() {
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "FirstName",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    ""))
            .build();

    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(ApplicantRequestedAction.REVIEW_PAGE))
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("FirstName");
    assertThat(contentAsString(result)).contains("Please enter your last name.");
  }

  @Test
  public void update_withValidationErrors_requestedActionPrevious_staysOnBlockAndShowsErrors() {
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "FirstName",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    ""))
            .build();

    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(ApplicantRequestedAction.PREVIOUS_BLOCK))
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("FirstName");
    assertThat(contentAsString(result)).contains("Please enter your last name.");
  }

  @Test
  public void update_noAnswerToRequiredQuestion_requestedActionNext_staysOnBlockAndShowsErrors() {
    ApplicantRequestedActionWrapper requestedAction =
        new ApplicantRequestedActionWrapper(NEXT_BLOCK);

    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    ""))
            .build();

    when(settingsManifest.getNorthStarApplicantUi()).thenReturn(false);

    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                requestedAction)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Please enter your first name.");
    assertThat(contentAsString(result)).contains("Please enter your last name.");
  }

  @Test
  public void
      northstar_update_noAnswerToRequiredQuestion_requestedActionNext_staysOnBlockAndShowsErrors() {
    ApplicantRequestedActionWrapper requestedAction =
        new ApplicantRequestedActionWrapper(NEXT_BLOCK);

    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    ""))
            .build();

    when(settingsManifest.getNorthStarApplicantUi()).thenReturn(true);

    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                requestedAction)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Error: This question is required.");
  }

  // See issue #6987.
  @Test
  public void update_noAnswerToRequiredQuestion_requestedActionPrevious_goesToPrevious() {
    ApplicantRequestedActionWrapper requestedAction =
        new ApplicantRequestedActionWrapper(PREVIOUS_BLOCK);

    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().addressApplicantAddress())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    ""))
            .build();

    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "2",
                /* inReview= */ false,
                requestedAction)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String previousRoute =
        routes.ApplicantProgramBlocksController.previous(
                Long.toString(program.id),
                /* previousBlockIndex= */ 0,
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .url();
    assertThat(result.redirectLocation()).hasValue(previousRoute);
  }

  // See issue #6987.
  @Test
  public void update_noAnswerToRequiredQuestion_requestedActionReview_goesToReview() {
    ApplicantRequestedActionWrapper requestedAction =
        new ApplicantRequestedActionWrapper(REVIEW_PAGE);

    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    ""))
            .build();

    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                requestedAction)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String reviewRoute =
        routes.ApplicantProgramReviewController.review(
                Long.toString(program.id), /* isFromUrlCall= */ false)
            .url();
    assertThat(result.redirectLocation()).hasValue(reviewRoute);
  }

  @Test
  @Parameters({"NEXT_BLOCK", "REVIEW_PAGE", "PREVIOUS_BLOCK"})
  public void update_noAnswerToOptionalQuestion_questionSeen(String action) {
    ApplicantRequestedActionWrapper requestedAction =
        new ApplicantRequestedActionWrapper(ApplicantRequestedAction.valueOf(action));

    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withOptionalQuestion(testQuestionBank().nameApplicantName())
            .build();
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    ""))
            .build();

    subject
        .updateWithApplicantId(
            request,
            applicant.id,
            program.id,
            /* blockId= */ "1",
            /* inReview= */ false,
            requestedAction)
        .toCompletableFuture()
        .join();

    applicant.refresh();
    // We mark as question as seen by including the "updated_at" metadata.
    assertThat(applicant.getApplicantData().asJsonString())
        .containsPattern("\"applicant_name\":\\{\"updated_at\":.");
  }

  @Test
  @Parameters({"NEXT_BLOCK", "REVIEW_PAGE", "PREVIOUS_BLOCK"})
  public void update_deletePreviousAnswerToRequiredQuestion_staysOnBlockAndShowsErrors(
      String action) {
    ApplicantRequestedActionWrapper requestedAction =
        new ApplicantRequestedActionWrapper(ApplicantRequestedAction.valueOf(action));

    // First, provide an answer
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    Request requestWithAnswer =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "InitialFirstName",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    "InitialLastName"))
            .build();

    when(settingsManifest.getNorthStarApplicantUi()).thenReturn(false);

    subject
        .updateWithApplicantId(
            requestWithAnswer,
            applicant.id,
            program.id,
            /* blockId= */ "1",
            /* inReview= */ false,
            requestedAction)
        .toCompletableFuture()
        .join();

    // Then, try to delete the answer
    Request requestWithoutAnswer =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    ""))
            .build();

    when(settingsManifest.getNorthStarApplicantUi()).thenReturn(false);

    Result result =
        subject
            .updateWithApplicantId(
                requestWithoutAnswer,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                requestedAction)
            .toCompletableFuture()
            .join();

    // Verify errors are shown because required questions must be answered
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Please enter your first name.");
    assertThat(contentAsString(result)).contains("Please enter your last name.");
  }

  @Test
  @Parameters({"NEXT_BLOCK", "REVIEW_PAGE", "PREVIOUS_BLOCK"})
  public void northstar_update_deletePreviousAnswerToRequiredQuestion_staysOnBlockAndShowsErrors(
      String action) {
    ApplicantRequestedActionWrapper requestedAction =
        new ApplicantRequestedActionWrapper(ApplicantRequestedAction.valueOf(action));

    // First, provide an answer
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    Request requestWithAnswer =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "InitialFirstName",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    "InitialLastName"))
            .build();

    when(settingsManifest.getNorthStarApplicantUi()).thenReturn(true);

    subject
        .updateWithApplicantId(
            requestWithAnswer,
            applicant.id,
            program.id,
            /* blockId= */ "1",
            /* inReview= */ false,
            requestedAction)
        .toCompletableFuture()
        .join();

    // Then, try to delete the answer
    Request requestWithoutAnswer =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    ""))
            .build();

    when(settingsManifest.getNorthStarApplicantUi()).thenReturn(true);

    Result result =
        subject
            .updateWithApplicantId(
                requestWithoutAnswer,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                requestedAction)
            .toCompletableFuture()
            .join();

    // Verify errors are shown because required questions must be answered
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Error: This question is required.");
  }

  @Test
  @Parameters({"NEXT_BLOCK", "REVIEW_PAGE", "PREVIOUS_BLOCK"})
  public void update_deletePreviousAnswerToOptionalQuestion_saves(String action) {
    ApplicantRequestedActionWrapper requestedAction =
        new ApplicantRequestedActionWrapper(ApplicantRequestedAction.valueOf(action));

    // First, provide an answer
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withOptionalQuestion(testQuestionBank().nameApplicantName())
            .build();
    Request requestWithAnswer =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "InitialFirstName",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    "InitialLastName"))
            .build();
    subject
        .updateWithApplicantId(
            requestWithAnswer,
            applicant.id,
            program.id,
            /* blockId= */ "1",
            /* inReview= */ false,
            requestedAction)
        .toCompletableFuture()
        .join();

    // Then, delete the answer
    Request requestWithoutAnswer =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    ""))
            .build();

    subject
        .updateWithApplicantId(
            requestWithoutAnswer,
            applicant.id,
            program.id,
            /* blockId= */ "1",
            /* inReview= */ false,
            requestedAction)
        .toCompletableFuture()
        .join();

    // Verify the deletion is saved successfully (no answer is fine since it's an optional question)
    applicant.refresh();
    assertThat(applicant.getApplicantData().asJsonString()).doesNotContain("first_name");
    assertThat(applicant.getApplicantData().asJsonString()).doesNotContain("last_name");
    assertThat(applicant.getApplicantData().asJsonString()).doesNotContain("InitialFirstName");
    assertThat(applicant.getApplicantData().asJsonString()).doesNotContain("InitialLastName");
  }

  @Test
  public void update_withNextBlock_requestedActionNext_redirectsToEditNextBlock() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().addressApplicantAddress())
            .build();
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "FirstName",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    "LastName"))
            .build();

    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK))
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String nextBlockEditRoute =
        routes.ApplicantProgramBlocksController.edit(
                Long.toString(program.id),
                /* blockId= */ "2",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ false)
            .url();
    assertThat(result.redirectLocation()).hasValue(nextBlockEditRoute);
  }

  @Test
  public void update_withNextBlock_requestedActionReview_redirectsToReview() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().addressApplicantAddress())
            .build();
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "FirstName",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    "LastName"))
            .build();

    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(ApplicantRequestedAction.REVIEW_PAGE))
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String reviewRoute =
        routes.ApplicantProgramReviewController.review(
                Long.toString(program.id), /* isFromUrlCall= */ false)
            .url();
    assertThat(result.redirectLocation()).hasValue(reviewRoute);
  }

  @Test
  public void update_requestedActionReview_answersSaved() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "FakeFirstNameHere",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    "FakeLastNameHere"))
            .build();

    subject
        .updateWithApplicantId(
            request,
            applicant.id,
            program.id,
            /* blockId= */ "1",
            /* inReview= */ false,
            new ApplicantRequestedActionWrapper(ApplicantRequestedAction.REVIEW_PAGE))
        .toCompletableFuture()
        .join();

    applicant.refresh();
    assertThat(applicant.getApplicantData().asJsonString()).contains("FakeFirstNameHere");
    assertThat(applicant.getApplicantData().asJsonString()).contains("FakeLastNameHere");
  }

  @Test
  public void update_requestedActionPrevious_redirectsToPreviousBlock() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().textApplicantFavoriteColor())
            .withBlock("block 3")
            .withRequiredQuestion(testQuestionBank().dropdownApplicantIceCream())
            .withBlock("block 4")
            .withRequiredQuestion(testQuestionBank().emailApplicantEmail())
            .build();
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_email_address.email").toString(),
                    "test@gmail.com"))
            .build();

    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "4",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(ApplicantRequestedAction.PREVIOUS_BLOCK))
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    // The 4th block was filled in, which is index 3. So, the previous block would be
    // index 2.
    Integer previousBlockIndex = 2;
    String previousRoute =
        routes.ApplicantProgramBlocksController.previous(
                Long.toString(program.id),
                previousBlockIndex,
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .url();
    assertThat(result.redirectLocation()).hasValue(previousRoute);
  }

  @Test
  public void update_requestedActionPrevious_isFirstBlock_redirectsToReview() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "FirstName",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    "LastName"))
            .build();

    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(ApplicantRequestedAction.PREVIOUS_BLOCK))
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String reviewRoute =
        routes.ApplicantProgramReviewController.review(
                Long.toString(program.id), /* isFromUrlCall= */ false)
            .url();
    assertThat(result.redirectLocation()).hasValue(reviewRoute);
  }

  @Test
  public void update_onFirstBlock_requestedActionPrevious_answersSaved() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "FakeFirstNameHere",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    "FakeLastNameHere"))
            .build();

    subject
        .updateWithApplicantId(
            request,
            applicant.id,
            program.id,
            /* blockId= */ "1",
            /* inReview= */ false,
            new ApplicantRequestedActionWrapper(ApplicantRequestedAction.PREVIOUS_BLOCK))
        .toCompletableFuture()
        .join();

    applicant.refresh();
    assertThat(applicant.getApplicantData().asJsonString()).contains("FakeFirstNameHere");
    assertThat(applicant.getApplicantData().asJsonString()).contains("FakeLastNameHere");
  }

  @Test
  public void update_onLaterBlock_requestedActionPrevious_answersSaved() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().dropdownApplicantIceCream())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().textApplicantFavoriteColor())
            .withBlock("block 3")
            .withRequiredQuestion(testQuestionBank().emailApplicantEmail())
            .build();
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_email_address.email").toString(),
                    "test@gmail.com"))
            .build();

    subject
        .updateWithApplicantId(
            request,
            applicant.id,
            program.id,
            /* blockId= */ "3",
            /* inReview= */ false,
            new ApplicantRequestedActionWrapper(ApplicantRequestedAction.PREVIOUS_BLOCK))
        .toCompletableFuture()
        .join();

    applicant.refresh();
    assertThat(applicant.getApplicantData().asJsonString()).contains("test@gmail.com");
  }

  @Test
  public void update_savesCorrectedAddressWhenValidAddressIsEntered() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredCorrectedAddressQuestion(testQuestionBank().addressApplicantAddress())
            .build();
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_address").join(Scalar.STREET).toString(),
                    "Address In Area",
                    Path.create("applicant.applicant_address").join(Scalar.LINE2).toString(),
                    "",
                    Path.create("applicant.applicant_address").join(Scalar.CITY).toString(),
                    "Redlands",
                    Path.create("applicant.applicant_address").join(Scalar.STATE).toString(),
                    "CA",
                    Path.create("applicant.applicant_address").join(Scalar.ZIP).toString(),
                    "92373"))
            .build();
    when(settingsManifest.getEsriAddressCorrectionEnabled(any())).thenReturn(true);
    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    // check that the address correction screen is skipped and the user is redirected to the review
    // screen
    String reviewRoute =
        routes.ApplicantProgramReviewController.review(
                Long.toString(program.id), /* isFromUrlCall= */ false)
            .url();
    assertThat(result.redirectLocation()).hasValue(reviewRoute);
    assertThat(result.status()).isEqualTo(SEE_OTHER);

    // assert that the corrected address is saved
    applicant.refresh();
    assertThat(applicant.getApplicantData().asJsonString()).contains("Corrected");
  }

  @Test
  public void update_completedProgram_redirectsToReviewPage() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();

    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "FirstName",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    "LastName"))
            .build();

    Result result =
        subject
            .updateWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    String reviewRoute =
        routes.ApplicantProgramReviewController.review(
                Long.toString(program.id), /* isFromUrlCall= */ false)
            .url();

    assertThat(result.redirectLocation()).hasValue(reviewRoute);
  }

  @Test
  public void updateFile_whenProgramSlugUrlsFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);
    String programId = Long.toString(program.id);

    Result result =
        subject
            .updateFile(
                request,
                programId,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  /**
   * Tests that updateFile() throws an error when the program param is a program slug but it should
   * be the program id since the program slug feature is disabled. updateFile() also throws error
   * for other combinations when the program param is not properly parsed. We don't test all
   * combinations here because ProgramSlugHandler have a comprehensive test cover for them.
   */
  @Test
  public void updateFile_whenProgramSlugUrlsFeatureDisabledAndIsProgramSlugFromUrl_error() {
    program = ProgramBuilder.newActiveProgram("Program").build();
    String programSlug = program.getSlug();
    assertThatThrownBy(
            () ->
                subject.updateFile(
                    fakeRequest(),
                    programSlug,
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper(NEXT_BLOCK),
                    /* isFromUrlCall= */ true))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from 'program' to a numeric value");
  }

  /**
   * Tests that updateFile() redirects to another page when the feature is enabled and is from url
   * call with a program slug. updateFile() also redirects for other combinations: when the feature
   * is disabled OR when the call is not from a URL OR when the program param is a program slug (not
   * numeric), AND the program ID was properly retrieved. We don't test all combinations here
   * because the ProgramSlugHandler tests have a comprehensive test cover for them.
   */
  @Test
  public void updateFile_whenProgramSlugUrlsFeatureEnabledAndIsProgramSlugFromUrl_works() {
    ProgramModel activeProgram =
        ProgramBuilder.newActiveProgram("Program with file upload")
            .withBlock()
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String programSlug = activeProgram.getSlug();

    RequestBuilder requestBuilder = fakeRequestBuilder();
    addQueryString(requestBuilder, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));
    Request request = requestBuilder.build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .updateFile(
                request,
                programSlug,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // When updateFile() is successful, it calls updateFileWithApplicantId() which redirects to a
    // different route. We don't test which specific route here since that is covered on the
    // updateFileWithApplicantId() unit tests. Instead, we can just verify the redirect route is
    // for the same program.
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation().get()).contains(Long.toString(activeProgram.id));
  }

  @Test
  public void updateFileWithApplicantId_invalidApplicant_returnsUnauthorized() {
    long badApplicantId = applicant.id + 1000;
    String programId = Long.toString(program.id);

    Result result =
        subject
            .updateFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                badApplicantId,
                programId,
                /* blockId= */ "2",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void updateFileWithApplicantId_applicantAccessToDraftProgram_returnsUnauthorized() {
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String draftProgramId = Long.toString(draftProgram.id);

    Result result =
        subject
            .updateFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                applicant.id,
                draftProgramId,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void updateFileWithApplicantId_civiformAdminAccessToDraftProgram_works() {
    AccountModel adminAccount = createGlobalAdminWithMockedProfile();
    applicant = adminAccount.representativeApplicant().orElseThrow();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String draftProgramId = Long.toString(draftProgram.id);

    Result result =
        subject
            .updateFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                applicant.id,
                draftProgramId,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void updateFileWithApplicantId_obsoleteProgram_works() {
    ProgramModel obsoleteProgram =
        ProgramBuilder.newObsoleteProgram("program")
            .withBlock()
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String obsoleteProgramId = Long.toString(obsoleteProgram.id);

    Result result =
        subject
            .updateFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                applicant.id,
                obsoleteProgramId,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void updateFileWithApplicantId_invalidProgram_returnsBadRequest() {
    String badProgramId = Long.toString(program.id + 1000);

    Result result =
        subject
            .updateFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                applicant.id,
                badProgramId,
                /* blockId= */ "2",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void updateFileWithApplicantId_invalidBlock_returnsBadRequest() {
    String badBlockId = "1000";
    String programId = Long.toString(program.id);

    Result result =
        subject
            .updateFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                applicant.id,
                programId,
                badBlockId,
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void updateFileWithApplicantId_notFileUploadBlock_returnsBadRequest() {
    String badBlockId = "1";
    String programId = Long.toString(program.id);

    Result result =
        subject
            .updateFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                applicant.id,
                programId,
                badBlockId,
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void updateFileWithApplicantId_missingFileKeyAndBucket_returnsBadRequest() {
    String programId = Long.toString(program.id);

    Result result =
        subject
            .updateFileWithApplicantId(
                fakeRequest(),
                applicant.id,
                programId,
                /* blockId= */ "2",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void updateFileWithApplicantId_requestedActionNext_redirectsToNextAndStoresFileKey() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().addressApplicantAddress())
            .build();
    String programId = Long.toString(program.id);

    Result result =
        subject
            .updateFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                applicant.id,
                programId,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String nextBlockEditRoute =
        routes.ApplicantProgramBlocksController.edit(
                programId,
                /* blockId= */ "2",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ false)
            .url();
    assertThat(result.redirectLocation()).hasValue(nextBlockEditRoute);

    applicant.refresh();
    String applicantData = applicant.getApplicantData().asJsonString();
    assertThat(applicantData).contains("fake-key");
  }

  @Test
  public void
      updateFileWithApplicantId_requestedActionPrevious_redirectsToPreviousAndStoresFileKey() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().addressApplicantAddress())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String programId = Long.toString(program.id);

    Result result =
        subject
            .updateFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                applicant.id,
                programId,
                /* blockId= */ "2",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(PREVIOUS_BLOCK),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    // The 2nd block was filled in, which is index 1. So, the previous block would be
    // index 0.
    Integer previousBlockIndex = 0;
    String previousRoute =
        routes.ApplicantProgramBlocksController.previous(
                programId, previousBlockIndex, /* inReview= */ false, /* isFromUrlCall= */ false)
            .url();
    assertThat(result.redirectLocation()).hasValue(previousRoute);

    applicant.refresh();
    String applicantData = applicant.getApplicantData().asJsonString();
    assertThat(applicantData).contains("fake-key");
  }

  @Test
  public void updateFileWithApplicantId_requestedActionReview_redirectsToReviewAndStoresFileKey() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().addressApplicantAddress())
            .build();
    String programId = Long.toString(program.id);

    Result result =
        subject
            .updateFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                applicant.id,
                programId,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(REVIEW_PAGE),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String reviewRoute =
        routes.ApplicantProgramReviewController.review(programId, /* isFromUrlCall= */ false).url();
    assertThat(result.redirectLocation()).hasValue(reviewRoute);

    applicant.refresh();
    String applicantData = applicant.getApplicantData().asJsonString();
    assertThat(applicantData).contains("fake-key");
  }

  @Test
  public void updateFileWithApplicantId_completedProgram_redirectsToReviewPage() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String programId = Long.toString(program.id);

    Result result =
        subject
            .updateFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                applicant.id,
                programId,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    // The requested action is NEXT_BLOCK, but since file upload is the only question they should be
    // redirected to the review page.
    String reviewRoute =
        routes.ApplicantProgramReviewController.review(programId, /* isFromUrlCall= */ false).url();
    assertThat(result.redirectLocation()).hasValue(reviewRoute);
  }

  /**
   * This test guards regression for the bugfix to
   * https://github.com/seattle-uat/civiform/issues/2818
   */
  @Test
  public void updateFileWithApplicantId_storedFileAlreadyExists_doesNotCreateDuplicateStoredFile() {
    var storedFileRepo = instanceOf(StoredFileRepository.class);

    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String programId = Long.toString(program.id);

    var fileKey = "fake-key";
    var storedFile = new StoredFileModel();
    storedFile.setName(fileKey);
    storedFile.save();

    RequestBuilder request = fakeRequestBuilder();
    addQueryString(request, ImmutableMap.of("key", fileKey, "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                applicant.id,
                programId,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(REVIEW_PAGE),
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    int storedFileCount =
        storedFileRepo.lookupFiles(ImmutableList.of(fileKey)).toCompletableFuture().join().size();
    assertThat(storedFileCount).isEqualTo(1);
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void addFile_whenProgramSlugUrlsFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);
    String programId = Long.toString(program.id);

    Result result =
        subject
            .addFile(
                request,
                programId,
                /* blockId= */ "1",
                /* inReview= */ false,
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  /**
   * Tests that addFile() throws an error when the program param is a program slug but it should be
   * the program id since the program slug feature is disabled. addFile() also throws error for
   * other combinations when the program param is not properly parsed. We don't test all
   * combinations here because ProgramSlugHandler have a comprehensive test cover for them.
   */
  @Test
  public void addFile_whenProgramSlugUrlsFeatureDisabledAndIsProgramSlugFromUrl_error() {
    program = ProgramBuilder.newActiveProgram("Program").build();
    String programSlug = program.getSlug();
    assertThatThrownBy(
            () ->
                subject.addFile(
                    fakeRequest(),
                    programSlug,
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    /* isFromUrlCall= */ true))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from 'program' to a numeric value");
  }

  /**
   * Tests that addFile() redirects to another page when the feature is enabled and is from url call
   * with a program slug. addFile() also redirects for other combinations: when the feature is
   * disabled OR when the call is not from a URL OR when the program param is a program slug (not
   * numeric), AND the program ID was properly retrieved. We don't test all combinations here
   * because the ProgramSlugHandler tests have a comprehensive test cover for them.
   */
  @Test
  public void addFile_whenProgramSlugUrlsFeatureEnabledAndIsProgramSlugFromUrl_works() {
    ProgramModel activeProgram =
        ProgramBuilder.newActiveProgram("Program with file upload")
            .withBlock()
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String programSlug = activeProgram.getSlug();

    Request request = fakeRequestWithKeyAndBucket();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .addFile(
                request,
                programSlug,
                /* blockId= */ "1",
                /* inReview= */ false,
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // When addFile() is successful, it calls addFileWithApplicantId() which redirects to a
    // different route. We don't test which specific route here since that is covered on the
    // updateFileWithApplicantId() unit tests. Instead, we can just verify the redirect route is
    // for the same program.
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation().get()).contains(Long.toString(activeProgram.id));
  }

  @Test
  public void
      addFileWithApplicantId_whenProgramSlugUrlsFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);
    String programId = Long.toString(program.id);

    Result result =
        subject
            .addFileWithApplicantId(
                request,
                applicant.id,
                programId,
                /* blockId= */ "1",
                /* inReview= */ false,
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  /**
   * Tests that addFileWithApplicantId() throws an error when the program param is a program slug
   * but it should be the program id since the program slug feature is disabled.
   * addFileWithApplicantId() also throws error for other combinations when the program param is not
   * properly parsed. We don't test all combinations here because ProgramSlugHandler have a
   * comprehensive test cover for them.
   */
  @Test
  public void
      addFileWithApplicantId_whenProgramSlugUrlsFeatureDisabledAndIsProgramSlugFromUrl_error() {
    program = ProgramBuilder.newActiveProgram("Program").build();
    String programSlug = program.getSlug();
    assertThatThrownBy(
            () ->
                subject.addFileWithApplicantId(
                    fakeRequest(),
                    applicant.id,
                    programSlug,
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    /* isFromUrlCall= */ true))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from 'program' to a numeric value");
  }

  /**
   * Tests that addFileWithApplicantId() redirects to another page when the feature is enabled and
   * is from url call with a program slug. addFile() also redirects for other combinations: when the
   * feature is disabled OR when the call is not from a URL OR when the program param is a program
   * slug (not numeric), AND the program ID was properly retrieved. We don't test all combinations
   * here because the ProgramSlugHandler tests have a comprehensive test cover for them.
   */
  @Test
  public void
      addFileWithApplicantId_whenProgramSlugUrlsFeatureEnabledAndIsProgramSlugFromUrl_works() {
    ProgramModel activeProgram =
        ProgramBuilder.newActiveProgram("Program with file upload")
            .withBlock()
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String programSlug = activeProgram.getSlug();

    Request request = fakeRequestWithKeyAndBucket();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .addFileWithApplicantId(
                request,
                applicant.id,
                programSlug,
                /* blockId= */ "1",
                /* inReview= */ false,
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // When addFileWithApplicantId() it redirects to a different route. Next test verify the
    // possible outcomes in more detail.
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation().get()).contains(Long.toString(activeProgram.id));
  }

  @Test
  public void addFileWithApplicantId_invalidApplicant_returnsUnauthorized() {
    long badApplicantId = applicant.id + 1000;
    String programId = Long.toString(program.id);

    Result result =
        subject
            .addFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                badApplicantId,
                programId,
                /* blockId= */ "2",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void addFileWithApplicantId_applicantAccessToDraftProgram_returnsUnauthorized() {
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String draftProgramId = Long.toString(draftProgram.id);

    Result result =
        subject
            .addFileWithApplicantId(
                fakeRequest(),
                applicant.id,
                draftProgramId,
                /* blockId= */ "1",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void addFileWithApplicantId_civiformAdminAccessToDraftProgram_redirects() {
    AccountModel adminAccount = createGlobalAdminWithMockedProfile();
    applicant = adminAccount.representativeApplicant().orElseThrow();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String draftProgramId = Long.toString(draftProgram.id);

    Result result =
        subject
            .addFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                applicant.id,
                draftProgramId,
                /* blockId= */ "1",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void addFileWithApplicantId_obsoleteProgram_redirects() {
    ProgramModel obsoleteProgram =
        ProgramBuilder.newObsoleteProgram("program")
            .withBlock()
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String obsoleteProgramId = Long.toString(obsoleteProgram.id);

    Result result =
        subject
            .addFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                applicant.id,
                obsoleteProgramId,
                /* blockId= */ "1",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void addFileWithApplicantId_invalidProgram_returnsBadRequest() {
    String badProgramId = Long.toString(program.id + 1000);

    Result result =
        subject
            .addFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                applicant.id,
                badProgramId,
                /* blockId= */ "2",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void addFileWithApplicantId_invalidBlock_returnsBadRequest() {
    String badBlockId = "1000";
    String programId = Long.toString(program.id);

    Result result =
        subject
            .addFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                applicant.id,
                programId,
                badBlockId,
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void addFileWithApplicantId_notFileUploadBlock_returnsBadRequest() {
    String badBlockId = "1";
    String programId = Long.toString(program.id);

    Result result =
        subject
            .addFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                applicant.id,
                programId,
                badBlockId,
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void addFileWithApplicantId_missingFileKeyAndBucket_returnsBadRequest() {
    String programId = Long.toString(program.id);

    Result result =
        subject
            .addFileWithApplicantId(
                fakeRequest(),
                applicant.id,
                programId,
                /* blockId= */ "2",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void addFileWithApplicantId_addsFileAndRerendersSameBlock() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().addressApplicantAddress())
            .build();
    String programId = Long.toString(program.id);

    RequestBuilder request = fakeRequestBuilder();
    addQueryString(
        request,
        ImmutableMap.of(
            "key", "fake-key", "bucket", "fake-bucket", "originalFileName", "fake-file-name"));

    Result result =
        subject
            .addFileWithApplicantId(
                request.build(),
                applicant.id,
                programId,
                /* blockId= */ "1",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .contains(String.format("/programs/%s/blocks/1/edit?isFromUrlCall=false", program.id));

    applicant.refresh();
    String applicantData = applicant.getApplicantData().asJsonString();
    assertThat(applicantData).contains("fake-key", "fake-file-name");
  }

  @Test
  public void addFileWithApplicantId_failsIfAddingMoreThanMax() {
    FileUploadQuestionDefinition fileUploadWithMaxDefinition =
        new FileUploadQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("question name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
                .setId(OptionalLong.of(1))
                .setValidationPredicates(
                    FileUploadQuestionDefinition.FileUploadValidationPredicates.builder()
                        .setMaxFiles(OptionalInt.of(1))
                        .build())
                .setLastModifiedTime(Optional.empty())
                .build());

    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(
                testQuestionBank().maybeSave(fileUploadWithMaxDefinition, LifecycleStage.ACTIVE))
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().addressApplicantAddress())
            .build();
    String programId = Long.toString(program.id);

    // Add the first file
    RequestBuilder firstRequest = fakeRequestBuilder();
    addQueryString(firstRequest, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));
    Result result =
        subject
            .addFileWithApplicantId(
                firstRequest.build(),
                applicant.id,
                programId,
                /* blockId= */ "1",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    // Now add the second file.
    RequestBuilder secondRequest = fakeRequestBuilder();
    addQueryString(secondRequest, ImmutableMap.of("key", "fake-key-2", "bucket", "fake-bucket"));
    result =
        subject
            .addFileWithApplicantId(
                secondRequest.build(),
                applicant.id,
                programId,
                /* blockId= */ "1",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);

    applicant.refresh();
    String applicantData = applicant.getApplicantData().asJsonString();
    assertThat(applicantData).contains("fake-key");
    assertThat(applicantData).doesNotContain("fake-key-2");
  }

  @Test
  public void addFileWithApplicantId_canAddAndRemoveMultipleFiles() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String programId = Long.toString(program.id);

    RequestBuilder requestOne = fakeRequestBuilder();
    addQueryString(
        requestOne,
        ImmutableMap.of(
            "key", "keyOne", "bucket", "fake-bucket", "originalFileName", "fileNameOne"));

    subject
        .addFileWithApplicantId(
            requestOne.build(),
            applicant.id,
            programId,
            /* blockId= */ "1",
            /* inReview= */ false,
            /* isFromUrlCall= */ false)
        .toCompletableFuture()
        .join();

    RequestBuilder requestTwo = fakeRequestBuilder();
    addQueryString(
        requestTwo,
        ImmutableMap.of(
            "key", "keyTwo", "bucket", "fake-bucket", "originalFileName", "fileNameTwo"));

    subject
        .addFileWithApplicantId(
            requestTwo.build(),
            applicant.id,
            programId,
            /* blockId= */ "1",
            /* inReview= */ false,
            /* isFromUrlCall= */ false)
        .toCompletableFuture()
        .join();

    RequestBuilder requestThree = fakeRequestBuilder();

    subject
        .removeFile(
            requestThree.build(),
            programId,
            /* blockId= */ "1",
            /* fileKeyToRemove= */ "keyTwo",
            /* inReview= */ false,
            /* isFromUrlCall= */ false)
        .toCompletableFuture()
        .join();

    RequestBuilder requestFour = fakeRequestBuilder();
    addQueryString(
        requestFour,
        ImmutableMap.of(
            "key", "keyThree", "bucket", "fake-bucket", "originalFileName", "fileNameThree"));

    subject
        .addFileWithApplicantId(
            requestFour.build(),
            applicant.id,
            programId,
            /* blockId= */ "1",
            /* inReview= */ false,
            /* isFromUrlCall= */ false)
        .toCompletableFuture()
        .join();

    applicant.refresh();
    String applicantData = applicant.getApplicantData().asJsonString();
    assertThat(applicantData).contains("keyOne", "fileNameOne");
    assertThat(applicantData).contains("keyThree", "fileNameThree");

    // Assert that corresponding entries were created in the stored file repo.
    var storedFileRepo = instanceOf(StoredFileRepository.class);
    int storedFileCount =
        storedFileRepo
            .lookupFiles(ImmutableList.of("keyOne", "keyTwo"))
            .toCompletableFuture()
            .join()
            .size();
    assertThat(storedFileCount).isEqualTo(2);
  }

  @Test
  public void addFileWithApplicantId_addingDuplicateFileDoesNothing() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String programId = Long.toString(program.id);

    var result =
        subject
            .addFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                applicant.id,
                programId,
                /* blockId= */ "1",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    result =
        subject
            .addFileWithApplicantId(
                fakeRequestWithKeyAndBucket(),
                applicant.id,
                programId,
                /* blockId= */ "1",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    applicant.refresh();
    String applicantData = applicant.getApplicantData().asJsonString();
    assertThat(applicantData).containsOnlyOnce("fake-key");

    // Assert that there aren't duplicate entries in the file permissions repo.
    var storedFileRepo = instanceOf(StoredFileRepository.class);
    int storedFileCount =
        storedFileRepo
            .lookupFiles(ImmutableList.of("fake-key"))
            .toCompletableFuture()
            .join()
            .size();
    assertThat(storedFileCount).isEqualTo(1);
  }

  @Test
  public void
      removeFileWithApplicantId_whenProgramSlugUrlsFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);
    String programId = Long.toString(program.id);

    Result result =
        subject
            .removeFileWithApplicantId(
                request,
                applicant.id,
                programId,
                /* blockId= */ "1",
                /* fileKeyToRemove= */ "fake-key",
                /* inReview= */ false,
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  /**
   * Tests that removeFileWithApplicantId() throws an error when the program param is a program slug
   * but it should be the program id since the program slug feature is disabled.
   * removeFileWithApplicantId() also throws error for other combinations when the program param is
   * not properly parsed. We don't test all combinations here because ProgramSlugHandler have a
   * comprehensive test cover for them.
   */
  @Test
  public void
      removeFileWithApplicantId_whenProgramSlugUrlsFeatureDisabledAndIsProgramSlugFromUrl_error() {
    program = ProgramBuilder.newActiveProgram("Program").build();
    String programSlug = program.getSlug();
    assertThatThrownBy(
            () ->
                subject.removeFileWithApplicantId(
                    fakeRequest(),
                    applicant.id,
                    programSlug,
                    /* blockId= */ "1",
                    /* fileKeyToRemove= */ "fake-key",
                    /* inReview= */ false,
                    /* isFromUrlCall= */ true))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from 'program' to a numeric value");
  }

  /**
   * Tests that removeFile() redirects to another page when the feature is enabled and is from url
   * call with a program slug. removeFileWithApplicantId() also redirects for other combinations:
   * when the feature is disabled OR when the call is not from a URL OR when the program param is a
   * program slug (not numeric), AND the program ID was properly retrieved. We don't test all
   * combinations here because the ProgramSlugHandler tests have a comprehensive test cover for
   * them.
   */
  @Test
  public void
      removeFileWithApplicantid_whenProgramSlugUrlsFeatureEnabledAndIsProgramSlugFromUrl_works() {
    ProgramModel activeProgram =
        ProgramBuilder.newActiveProgram("Program with file upload")
            .withBlock()
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String programSlug = activeProgram.getSlug();

    Request request = fakeRequestWithKeyAndBucket();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .removeFileWithApplicantId(
                request,
                applicant.id,
                programSlug,
                /* blockId= */ "1",
                /* fileKeyToRemove= */ "fake-key",
                /* inReview= */ false,
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation().get()).contains(Long.toString(activeProgram.id));
  }

  @Test
  public void removeFileWithApplicantId_invalidApplicant_returnsUnauthorized() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String programId = Long.toString(program.id);
    long badApplicantId = applicant.id + 1000;

    Result result =
        subject
            .removeFileWithApplicantId(
                fakeRequest(),
                badApplicantId,
                programId,
                /* blockId= */ "1",
                /* fileKeyToRemove= */ "fake-key",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void removeFile_applicantAccessToDraftProgram_returnsUnauthorized() {
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    String draftProgramId = Long.toString(draftProgram.id);

    Result result =
        subject
            .removeFile(
                fakeRequest(),
                draftProgramId,
                /* blockId= */ "2",
                /* fileKeyToRemove= */ "fake-key",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void removeFileWithApplicantId_civiformAdminAccessToDraftProgram_redirects() {
    AccountModel adminAccount = createGlobalAdminWithMockedProfile();
    applicant = adminAccount.representativeApplicant().orElseThrow();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String draftProgramId = Long.toString(draftProgram.id);

    Result result =
        subject
            .removeFileWithApplicantId(
                fakeRequest(),
                applicant.id,
                draftProgramId,
                /* blockId= */ "1",
                /* fileKeyToRemove= */ "fake-key",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void removeFile_whenProgramSlugUrlsFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);
    String programId = Long.toString(program.id);

    Result result =
        subject
            .removeFile(
                request,
                programId,
                /* blockId= */ "1",
                /* fileKeyToRemove= */ "fake-key",
                /* inReview= */ false,
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  /**
   * Tests that removeFile() throws an error when the program param is a program slug but it should
   * be the program id since the program slug feature is disabled. removeFile() also throws error
   * for other combinations when the program param is not properly parsed. We don't test all
   * combinations here because ProgramSlugHandler have a comprehensive test cover for them.
   */
  @Test
  public void removeFile_whenProgramSlugUrlsFeatureDisabledAndIsProgramSlugFromUrl_error() {
    program = ProgramBuilder.newActiveProgram("Program").build();
    String programSlug = program.getSlug();
    assertThatThrownBy(
            () ->
                subject.removeFile(
                    fakeRequest(),
                    programSlug,
                    /* blockId= */ "1",
                    /* fileKeyToRemove= */ "fake-key",
                    /* inReview= */ false,
                    /* isFromUrlCall= */ true))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from 'program' to a numeric value");
  }

  /**
   * Tests that removeFile() redirects to another page when the feature is enabled and is from url
   * call with a program slug. removeFile() also redirects for other combinations: when the feature
   * is disabled OR when the call is not from a URL OR when the program param is a program slug (not
   * numeric), AND the program ID was properly retrieved. We don't test all combinations here
   * because the ProgramSlugHandler tests have a comprehensive test cover for them.
   */
  @Test
  public void removeFile_whenProgramSlugUrlsFeatureEnabledAndIsProgramSlugFromUrl_works() {
    ProgramModel activeProgram =
        ProgramBuilder.newActiveProgram("Program with file upload")
            .withBlock()
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String programSlug = activeProgram.getSlug();

    Request request = fakeRequestWithKeyAndBucket();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .removeFile(
                request,
                programSlug,
                /* blockId= */ "1",
                /* fileKeyToRemove= */ "fake-key",
                /* inReview= */ false,
                /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // When removeFile() is successful, it calls removeFileWithApplicantId() which redirects to a
    // different route. We don't test which specific route here since that is covered on the
    // next unit tests in detail.
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation().get()).contains(Long.toString(activeProgram.id));
  }

  @Test
  public void removeFile_obsoleteProgram_redirects() {
    ProgramModel obsoleteProgram =
        ProgramBuilder.newObsoleteProgram("program")
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String obsoleteProgramId = Long.toString(obsoleteProgram.id);

    Result result =
        subject
            .removeFile(
                fakeRequest(),
                obsoleteProgramId,
                /* blockId= */ "1",
                /* fileKeyToRemove= */ "fake-key",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void removeFile_invalidProgram_returnsBadRequest() {
    String badProgramId = Long.toString(program.id + 1000);

    Result result =
        subject
            .removeFile(
                fakeRequest(),
                badProgramId,
                /* blockId= */ "2",
                /* fileKeyToRemove= */ "fake-key",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void removeFile_invalidBlock_returnsBadRequest() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String programId = Long.toString(program.id);
    String badBlockId = "1000";

    Result result =
        subject
            .removeFile(
                fakeRequest(),
                programId,
                badBlockId,
                /* fileKeyToRemove= */ "fake-key",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void removeFile_notFileUploadBlock_returnsBadRequest() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().dateApplicantBirthdate())
            .build();
    String programId = Long.toString(program.id);
    String dateQuestionBlockId = "1";

    Result result =
        subject
            .removeFile(
                fakeRequest(),
                programId,
                dateQuestionBlockId,
                /* fileKeyToRemove= */ "fake-key",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void removeFile_withoutUserProfileRedirectsHome() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String programId = Long.toString(program.id);

    Request request = fakeRequestBuilder().header(skipUserProfile, "true").build();

    Result result =
        subject
            .removeFile(
                request,
                programId,
                /* blockId= */ "1",
                /* fileKeyToRemove= */ "key-to-remove",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue(controllers.routes.HomeController.index().url());
  }

  @Test
  public void removeFile_removesFileAndRerenders() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String programId = Long.toString(program.id);

    QuestionAnswerer.answerFileQuestionWithMultipleUpload(
        applicant.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            testQuestionBank()
                .fileUploadApplicantFile()
                .getQuestionDefinition()
                .getQuestionPathSegment()),
        ImmutableList.of("file-key-1", "key-to-remove", "file-key-2"));

    applicant.save();

    Result result =
        subject
            .removeFile(
                fakeRequest(),
                programId,
                /* blockId= */ "1",
                /* fileKeyToRemove= */ "key-to-remove",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    applicant.refresh();
    String applicantDataString = applicant.getApplicantData().asJsonString();
    assertThat(applicantDataString).contains("file-key-1", "file-key-2");
    assertThat(applicantDataString).doesNotContain("key-to-remove");
  }

  @Test
  public void removeFile_removesFileWithOriginalNamesAndRerenders() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String programId = Long.toString(program.id);

    QuestionAnswerer.answerFileQuestionWithMultipleUpload(
        applicant.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            testQuestionBank()
                .fileUploadApplicantFile()
                .getQuestionDefinition()
                .getQuestionPathSegment()),
        ImmutableList.of("file-key-1", "key-to-remove", "file-key-2"));

    QuestionAnswerer.answerFileQuestionWithMultipleUploadOriginalNames(
        applicant.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            testQuestionBank()
                .fileUploadApplicantFile()
                .getQuestionDefinition()
                .getQuestionPathSegment()),
        ImmutableList.of("file-name-1", "key-name-to-remove", "file-name-2"));

    applicant.save();

    Result result =
        subject
            .removeFile(
                fakeRequest(),
                programId,
                /* blockId= */ "1",
                /* fileKeyToRemove= */ "key-to-remove",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    applicant.refresh();
    String applicantDataString = applicant.getApplicantData().asJsonString();
    assertThat(applicantDataString)
        .contains("file-key-1", "file-key-2", "file-name-1", "file-name-2");
    assertThat(applicantDataString).doesNotContain("key-to-remove", "file-name-to-remove");
  }

  @Test
  public void removeFile_removesLastFileWithRequiredQuestion() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String programId = Long.toString(program.id);

    QuestionAnswerer.answerFileQuestionWithMultipleUpload(
        applicant.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            testQuestionBank()
                .fileUploadApplicantFile()
                .getQuestionDefinition()
                .getQuestionPathSegment()),
        ImmutableList.of("key-to-remove"));
    applicant.save();

    Result result =
        subject
            .removeFile(
                fakeRequest(),
                programId,
                /* blockId= */ "1",
                /* fileKeyToRemove= */ "key-to-remove",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    applicant.refresh();
    assertThat(applicant.getApplicantData().asJsonString()).doesNotContain("key-to-remove");
  }

  @Test
  public void removeFile_removingNonExistantFileDoesNothing() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().fileUploadApplicantFile())
            .build();
    String programId = Long.toString(program.id);

    QuestionAnswerer.answerFileQuestionWithMultipleUpload(
        applicant.getApplicantData(),
        ApplicantData.APPLICANT_PATH.join(
            testQuestionBank()
                .fileUploadApplicantFile()
                .getQuestionDefinition()
                .getQuestionPathSegment()),
        ImmutableList.of("file-key-1", "file-key-2"));

    applicant.save();

    Result result =
        subject
            .removeFile(
                fakeRequest(),
                programId,
                /* blockId= */ "1",
                /* fileKeyToRemove= */ "does-not-exist",
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    applicant.refresh();
    String applicantDataString = applicant.getApplicantData().asJsonString();
    assertThat(applicantDataString).contains("file-key-1", "file-key-2");
    assertThat(applicantDataString).doesNotContain("does-not-exist");
  }

  @Test
  public void confirmAddress_invalidApplicant_returnsUnauthorized() {
    long badApplicantId = Long.MAX_VALUE;

    Request request =
        fakeRequestBuilder()
            .session(ADDRESS_JSON_SESSION_KEY, createAddressSuggestionsJson())
            .build();

    Result result =
        subject
            .confirmAddressWithApplicantId(
                request,
                badApplicantId,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void confirmAddress_applicantAccessToDraftProgram_returnsUnauthorized() {
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredCorrectedAddressQuestion(testQuestionBank().addressApplicantAddress())
            .build();

    Request request =
        fakeRequestBuilder()
            .session(ADDRESS_JSON_SESSION_KEY, createAddressSuggestionsJson())
            .build();
    Result result =
        subject
            .confirmAddressWithApplicantId(
                request,
                applicant.id,
                draftProgram.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void confirmAddress_civiformAdminAccessToDraftProgram_isOk() {
    AccountModel adminAccount = createGlobalAdminWithMockedProfile();
    applicant = adminAccount.representativeApplicant().orElseThrow();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredCorrectedAddressQuestion(testQuestionBank().addressApplicantAddress())
            .build();

    Request request =
        fakeRequestBuilder()
            .session(ADDRESS_JSON_SESSION_KEY, createAddressSuggestionsJson())
            .build();
    Result result =
        subject
            .confirmAddressWithApplicantId(
                request,
                applicant.id,
                draftProgram.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void confirmAddress_obsoleteProgram_isOk() {
    ProgramModel obsoleteProgram =
        ProgramBuilder.newObsoleteProgram("program")
            .withBlock()
            .withRequiredCorrectedAddressQuestion(testQuestionBank().addressApplicantAddress())
            .build();

    Request request =
        fakeRequestBuilder()
            .session(ADDRESS_JSON_SESSION_KEY, createAddressSuggestionsJson())
            .build();
    Result result =
        subject
            .confirmAddressWithApplicantId(
                request,
                applicant.id,
                obsoleteProgram.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void confirmAddress_toAProgramThatDoesNotExist_returns400() {
    long badProgramId = Long.MAX_VALUE;

    Request request =
        fakeRequestBuilder()
            .session(ADDRESS_JSON_SESSION_KEY, createAddressSuggestionsJson())
            .build();

    Result result =
        subject
            .confirmAddressWithApplicantId(
                request,
                applicant.id,
                badProgramId,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void confirmAddress_noAddressJson_throws() {
    Request request =
        fakeRequestBuilder()
            // Don't set the ADDRESS_JSON_SESSION_KEY on the session
            .bodyForm(
                ImmutableMap.of(
                    AddressCorrectionBlockView.SELECTED_ADDRESS_NAME,
                    "123 Main St, Boston, Massachusetts, 02111"))
            .build();

    assertThatThrownBy(
            () ->
                subject
                    .confirmAddressWithApplicantId(
                        request,
                        applicant.id,
                        program.id,
                        /* blockId= */ "1",
                        /* inReview= */ false,
                        new ApplicantRequestedActionWrapper())
                    .toCompletableFuture()
                    .join())
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  public void confirmAddress_noSelectedAddressInForm_originalAddressSavedAndCorrectionFailed() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredCorrectedAddressQuestion(testQuestionBank().addressApplicantAddress())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().dropdownApplicantIceCream())
            .build();

    // First, answer the address question
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
    when(settingsManifest.getEsriAddressCorrectionEnabled(any())).thenReturn(true);
    Result result =
        subject
            .updateWithApplicantId(
                answerAddressQuestionRequest,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();
    // Check that we're taken to the address correction screen with some suggestions
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.session().get(ADDRESS_JSON_SESSION_KEY)).isPresent();

    // Then, send a confirmAddress request but don't fill in SELECTED_ADDRESS_NAME in the form body
    Request request =
        fakeRequestBuilder()
            .session(ADDRESS_JSON_SESSION_KEY, createAddressSuggestionsJson())
            .build();

    Result confirmAddressResult =
        subject
            .confirmAddressWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    // Check that the user is redirected to the next block
    assertThat(confirmAddressResult.status()).isEqualTo(SEE_OTHER);
    String nextBlockEditRoute =
        routes.ApplicantProgramBlocksController.edit(
                Long.toString(program.id),
                /* blockId= */ "2",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ false)
            .url();
    assertThat(confirmAddressResult.redirectLocation()).hasValue(nextBlockEditRoute);

    // Check that the original address is saved but the address correction was marked as a failure
    applicant.refresh();
    String applicantData = applicant.getApplicantData().asJsonString();
    assertThat(applicantData).contains("Legit Address");
    assertThat(applicantData).contains("Boston");
    assertThat(applicantData).contains("02111");
    assertThat(applicantData).contains("Failed");

    // Check that the address suggestions are cleared from the session even after a failure
    assertThat(confirmAddressResult.session()).isNull();
  }

  @Test
  public void
      confirmAddress_suggestionChosen_requestedActionNext_savesSuggestionAndRedirectsToNext() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredCorrectedAddressQuestion(testQuestionBank().addressApplicantAddress())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().dropdownApplicantIceCream())
            .build();

    String address = "456 Suggested Ave, Seattle, Washington, 99999";
    AddressSuggestion addressSuggestion =
        AddressSuggestion.builder()
            .setAddress(
                Address.builder()
                    .setStreet("456 Suggested Ave")
                    .setLine2("")
                    .setCity("Seattle")
                    .setState("WA")
                    .setZip("99999")
                    .build())
            .setScore(90)
            .setLocation(
                AddressLocation.builder()
                    .setLatitude(3.0)
                    .setLongitude(3.1)
                    .setWellKnownId(4)
                    .build())
            .setSingleLineAddress(address)
            .build();
    String addressSuggestionString =
        addressSuggestionJsonSerializer.serialize(ImmutableList.of(addressSuggestion));

    // The selected address (set in the body form with the key SELECTED_ADDRESS_NAME) should match
    // one of the address
    // suggestions (set in the session with the key ADDRESS_JSON_SESSION_KEY).
    Request request =
        fakeRequestBuilder()
            .session(ADDRESS_JSON_SESSION_KEY, addressSuggestionString)
            .bodyForm(ImmutableMap.of(AddressCorrectionBlockView.SELECTED_ADDRESS_NAME, address))
            .build();
    Result result =
        subject
            .confirmAddressWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK))
            .toCompletableFuture()
            .join();

    // Check that the user is redirected to the next block
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String nextBlockEditRoute =
        routes.ApplicantProgramBlocksController.edit(
                Long.toString(program.id),
                /* blockId= */ "2",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ false)
            .url();
    assertThat(result.redirectLocation()).hasValue(nextBlockEditRoute);

    // Check that the selected suggested address is saved
    applicant.refresh();
    String applicantData = applicant.getApplicantData().asJsonString();
    assertThat(applicantData).contains("456 Suggested Ave");
    assertThat(applicantData).contains("Seattle");
    assertThat(applicantData).contains("99999");
    assertThat(applicantData).contains("Corrected");

    // Check that the address suggestions are cleared from the session
    assertThat(result.session()).isNull();
  }

  @Test
  public void confirmAddress_requestedActionReview_addressSavedAndRedirectedToReview() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredCorrectedAddressQuestion(testQuestionBank().addressApplicantAddress())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().dropdownApplicantIceCream())
            .build();

    Request request =
        fakeRequestBuilder()
            .session(ADDRESS_JSON_SESSION_KEY, createAddressSuggestionsJson())
            .bodyForm(
                ImmutableMap.of(
                    AddressCorrectionBlockView.SELECTED_ADDRESS_NAME, SUGGESTED_ADDRESS))
            .build();
    Result result =
        subject
            .confirmAddressWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(ApplicantRequestedAction.REVIEW_PAGE))
            .toCompletableFuture()
            .join();

    // Check that the user is redirected to the review page
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String reviewRoute =
        routes.ApplicantProgramReviewController.review(
                Long.toString(program.id), /* isFromUrlCall= */ false)
            .url();
    assertThat(result.redirectLocation()).hasValue(reviewRoute);

    // Check that the selected suggested address is saved
    applicant.refresh();
    String applicantData = applicant.getApplicantData().asJsonString();
    assertThat(applicantData).contains(SUGGESTED_ADDRESS_STREET);
    assertThat(applicantData).contains("Corrected");
  }

  @Test
  public void
      confirmAddress_requestedActionPrevious_addressSavedAndRedirectedToBlockBeforeAddressQuestion() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().emailApplicantEmail())
            .withBlock("block 2")
            .withRequiredCorrectedAddressQuestion(testQuestionBank().addressApplicantAddress())
            .withBlock("block 3")
            .withRequiredQuestion(testQuestionBank().dropdownApplicantIceCream())
            .build();

    Request request =
        fakeRequestBuilder()
            .session(ADDRESS_JSON_SESSION_KEY, createAddressSuggestionsJson())
            .bodyForm(
                ImmutableMap.of(
                    AddressCorrectionBlockView.SELECTED_ADDRESS_NAME, SUGGESTED_ADDRESS))
            .build();
    Result result =
        subject
            .confirmAddressWithApplicantId(
                request,
                applicant.id,
                program.id,
                /* blockId= */ "2",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(ApplicantRequestedAction.PREVIOUS_BLOCK))
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    // The 2nd block was filled in, which is index 1. So, the previous block would be index 0.
    Integer previousBlockIndex = 0;
    String previousBlockEditRoute =
        routes.ApplicantProgramBlocksController.previous(
                Long.toString(program.id),
                previousBlockIndex,
                /* inReview= */ false,
                /* isFromUrlCall= */ false)
            .url();
    assertThat(result.redirectLocation()).hasValue(previousBlockEditRoute);

    // Check that the selected suggested address is saved
    applicant.refresh();
    String applicantData = applicant.getApplicantData().asJsonString();
    assertThat(applicantData).contains(SUGGESTED_ADDRESS_STREET);
    assertThat(applicantData).contains("Corrected");
  }

  @Test
  public void confirmAddress_originalAddressChosen_savesOriginal() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredCorrectedAddressQuestion(testQuestionBank().addressApplicantAddress())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().dropdownApplicantIceCream())
            .build();

    // First, answer the address question
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
    when(settingsManifest.getEsriAddressCorrectionEnabled(any())).thenReturn(true);
    subject
        .updateWithApplicantId(
            answerAddressQuestionRequest,
            applicant.id,
            program.id,
            /* blockId= */ "1",
            /* inReview= */ false,
            new ApplicantRequestedActionWrapper())
        .toCompletableFuture()
        .join();

    // Then, choose the original address during address correction
    Request confirmAddressRequest =
        fakeRequestBuilder()
            .session(ADDRESS_JSON_SESSION_KEY, createAddressSuggestionsJson())
            .bodyForm(
                ImmutableMap.of(
                    AddressCorrectionBlockView.SELECTED_ADDRESS_NAME,
                    AddressCorrectionBlockView.USER_KEEPING_ADDRESS_VALUE))
            .build();

    Result confirmAddressResult =
        subject
            .confirmAddressWithApplicantId(
                confirmAddressRequest,
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    // Check that the user is redirected to the next page
    assertThat(confirmAddressResult.status()).isEqualTo(SEE_OTHER);
    String nextBlockEditRoute =
        routes.ApplicantProgramBlocksController.edit(
                Long.toString(program.id),
                /* blockId= */ "2",
                /* questionName= */ Optional.empty(),
                /* isFromUrlCall= */ false)
            .url();
    assertThat(confirmAddressResult.redirectLocation()).hasValue(nextBlockEditRoute);

    // Check that the original address is saved
    applicant.refresh();
    String applicantData = applicant.getApplicantData().asJsonString();
    assertThat(applicantData).contains("Legit Address");
    assertThat(applicantData).contains("Boston");
    assertThat(applicantData).contains("02111");
    assertThat(applicantData).contains("AsEnteredByUser");

    // Check that the address suggestions are cleared from the session
    assertThat(confirmAddressResult.session()).isNull();
  }

  private RequestBuilder addQueryString(
      RequestBuilder request, ImmutableMap<String, String> query) {
    String queryString =
        query.entrySet().stream()
            .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining("&"));
    return request.uri(request.uri() + "?" + queryString);
  }

  private String createAddressSuggestionsJson() {
    AddressSuggestion address =
        AddressSuggestion.builder()
            .setAddress(
                Address.builder()
                    .setStreet("456 Suggested Ave")
                    .setLine2("")
                    .setCity("Seattle")
                    .setState("WA")
                    .setZip("99999")
                    .build())
            .setScore(90)
            .setLocation(
                AddressLocation.builder()
                    .setLatitude(3.0)
                    .setLongitude(3.1)
                    .setWellKnownId(4)
                    .build())
            // This is the typical format for addresses we receive from ESRI.
            .setSingleLineAddress("456 Suggested Ave, Seattle, Washington, 99999")
            .build();
    return addressSuggestionJsonSerializer.serialize(ImmutableList.of(address));
  }

  private Request fakeRequestWithKeyAndBucket() {
    RequestBuilder requestBuilder = fakeRequestBuilder();
    addQueryString(requestBuilder, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));
    return requestBuilder.build();
  }
}
