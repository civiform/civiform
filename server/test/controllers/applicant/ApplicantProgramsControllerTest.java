package controllers.applicant;

import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.FOUND;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.stubMessagesApi;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.WithMockedProfiles;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationModel;
import models.LifecycleStage;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import play.i18n.MessagesApi;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ApplicantService;
import services.monitoring.MonitoringMetricCounters;
import services.question.QuestionAnswerer;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.applicant.ApplicantDisabledProgramView;
import views.applicant.NorthStarFilteredProgramsViewPartial;
import views.applicant.NorthStarProgramIndexView;
import views.applicant.ProgramIndexView;

public class ApplicantProgramsControllerTest extends WithMockedProfiles {

  private ApplicantModel currentApplicant;
  private ApplicantModel applicantWithoutProfile;
  private ApplicantProgramsController controller;
  private VersionRepository versionRepository;
  private SettingsManifest settingsManifest;

  @Before
  public void setUp() {
    resetDatabase();

    currentApplicant = createApplicantWithMockedProfile();
    applicantWithoutProfile = createApplicant();

    settingsManifest = mock(SettingsManifest.class);
    controller =
        new ApplicantProgramsController(
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(ApplicantService.class),
            instanceOf(MessagesApi.class),
            instanceOf(ProgramIndexView.class),
            instanceOf(ApplicantDisabledProgramView.class),
            instanceOf(ProfileUtils.class),
            instanceOf(VersionRepository.class),
            instanceOf(ProgramSlugHandler.class),
            instanceOf(ApplicantRoutes.class),
            settingsManifest,
            instanceOf(NorthStarProgramIndexView.class),
            instanceOf(NorthStarFilteredProgramsViewPartial.class),
            instanceOf(MonitoringMetricCounters.class));
  }

  /**
   * Calls the controller's edit method with configurable settings.
   *
   * @param isProgramSlugEnabled whether the program slug URLs feature should be enabled
   * @param isFromUrlCall whether the call was made directly from the URL route
   * @param programParam the program parameter (either a program ID or program slug depending on
   *     context)
   * @return the Result from the controller's edit method
   */
  Result callEdit(Boolean isProgramSlugEnabled, Boolean isFromUrlCall, String programParam) {
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(isProgramSlugEnabled);

    return controller.edit(request, programParam, isFromUrlCall).toCompletableFuture().join();
  }

  @Test
  public void indexWithApplicantId_differentApplicant_redirectsToHome() {
    Result result =
        controller
            .indexWithApplicantId(fakeRequest(), currentApplicant.id + 1, ImmutableList.of())
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void indexWithApplicantId_applicantWithoutProfile_redirectsToHome() {
    Result result =
        controller
            .indexWithApplicantId(fakeRequest(), applicantWithoutProfile.id, ImmutableList.of())
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void indexWithApplicantId_applicantWithoutProfile_redirectsToHomeWithCategories() {
    Result result =
        controller
            .indexWithApplicantId(
                fakeRequest(),
                applicantWithoutProfile.id,
                ImmutableList.of("category1", "category2"))
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void indexWithApplicantId_withNoPrograms_returnsEmptyResult() {
    Result result =
        controller
            .indexWithApplicantId(fakeRequest(), currentApplicant.id, ImmutableList.of())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(result.charset()).hasValue("utf-8");
    assertThat(contentAsString(result)).doesNotContain("program-card");
  }

  @Test
  public void indexWithApplicantId_withPrograms_returnsOnlyRelevantPrograms() {
    resourceCreator().insertActiveProgram("one");
    resourceCreator().insertActiveProgram("two");
    resourceCreator().insertDraftProgram("three");

    Result result =
        controller
            .indexWithApplicantId(fakeRequest(), currentApplicant.id, ImmutableList.of())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("one");
    assertThat(contentAsString(result)).contains("two");
    assertThat(contentAsString(result)).doesNotContain("three");
  }

  @Test
  public void indexWithApplicantId_clearsRedirectToSessionKey() {
    Request request = fakeRequestBuilder().session(REDIRECT_TO_SESSION_KEY, "redirect").build();
    Result result =
        controller
            .indexWithApplicantId(request, currentApplicant.id, ImmutableList.of())
            .toCompletableFuture()
            .join();
    assertThat(result.session().get(REDIRECT_TO_SESSION_KEY)).isEmpty();
  }

  @Test
  public void test_deduplicate_inProgressProgram() {
    versionRepository = instanceOf(VersionRepository.class);
    String programName = "In Progress Program";
    ProgramModel program = resourceCreator().insertActiveProgram(programName);

    ApplicationModel app = new ApplicationModel(currentApplicant, program, LifecycleStage.DRAFT);
    app.save();

    resourceCreator().insertDraftProgram(programName);
    this.versionRepository.publishNewSynchronizedVersion();

    Result result =
        controller
            .indexWithApplicantId(fakeRequest(), currentApplicant.id, ImmutableList.of())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    // A program's name appears in the index view page content 3 times:
    //  1) Program card title
    //  2) Apply button aria-label
    //  3) Info link aria-label
    // If it appears 9 times, that means there is a duplicate of the program.
    assertThat(numberOfSubstringsInString(contentAsString(result), programName)).isEqualTo(3);
  }

  /** Returns the number of times a substring appears in the string. */
  private int numberOfSubstringsInString(final String s, String substring) {
    Pattern pattern = Pattern.compile(substring);
    Matcher matcher = pattern.matcher(s);
    int count = 0;
    while (matcher.find()) count++;
    return count;
  }

  @Test
  public void indexWithApplicantId_withProgram_includesApplyButtonWithRedirect() {
    ProgramModel program = resourceCreator().insertActiveProgram("program");

    Result result =
        controller
            .indexWithApplicantId(fakeRequest(), currentApplicant.id, ImmutableList.of())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(routes.ApplicantProgramsController.show(String.valueOf(program.id)).url());
  }

  @Test
  public void indexWithApplicantId_withCommonIntakeform_includesStartHereButtonWithRedirect() {
    ProgramModel program = resourceCreator().insertActiveCommonIntakeForm("benefits");

    Result result =
        controller
            .indexWithApplicantId(fakeRequest(), currentApplicant.id, ImmutableList.of())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(routes.ApplicantProgramsController.show(String.valueOf(program.id)).url());
  }

  @Test
  public void indexWithApplicantId_usesMessagesForUserPreferredLocale() {
    // Set the PLAY_LANG cookie
    Http.Request request =
        fakeRequestBuilder().langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi()).build();

    Result result =
        controller
            .indexWithApplicantId(request, currentApplicant.id, ImmutableList.of())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Buscar programas");
  }

  @Test
  public void indexWithApplicantId_missingTranslationForProgram_defaultsToEnglish() {
    resourceCreator().insertActiveProgram(Locale.forLanguageTag("es-US"), "A different language!");
    resourceCreator().insertActiveProgram("English program"); // Missing translation

    // Set the PLAY_LANG cookie
    Http.Request request =
        fakeRequestBuilder().langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi()).build();

    Result result =
        controller
            .indexWithApplicantId(request, currentApplicant.id, ImmutableList.of())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("A different language!");
    assertThat(contentAsString(result)).contains("English program");
  }

  @Test
  public void indexWithoutApplicantId_showsAllPubliclyVisiblePrograms_doesNotShowEndSession() {
    // We don't want to provide a profile when ProfileUtils functions are called
    resetMocks();

    ProgramModel activeProgram = resourceCreator().insertActiveProgram("program");
    ProgramModel disabledProgram = resourceCreator().insertActiveDisabledProgram("disabled");
    ProgramModel hiddenInIndexProgram =
        resourceCreator().insertActiveHiddenInIndexProgram("hidden");
    ProgramModel tiOnlyProgram = resourceCreator().insertActiveTiOnlyProgram("tiOnly");

    Result result =
        controller
            .indexWithoutApplicantId(fakeRequest(), ImmutableList.of())
            .toCompletableFuture()
            .join();

    String content = contentAsString(result);
    assertThat(result.status()).isEqualTo(OK);
    assertThat(content)
        .contains(routes.ApplicantProgramsController.show(String.valueOf(activeProgram.id)).url());
    assertThat(content)
        .doesNotContain(
            routes.ApplicantProgramsController.show(String.valueOf(disabledProgram.id)).url());
    assertThat(content)
        .doesNotContain(
            routes.ApplicantProgramsController.show(String.valueOf(hiddenInIndexProgram.id)).url());
    assertThat(content)
        .doesNotContain(
            routes.ApplicantProgramsController.show(String.valueOf(tiOnlyProgram.id)).url());
    assertThat(content).doesNotContain("End session");
    assertThat(content).doesNotContain("You're a guest user");
  }

  @Test
  // Tests the behavior of the `show()` method when the parameter contains an alphanumeric value,
  // representing a program slug.
  public void show_withStringProgramParam_showsByProgramSlug() {
    ProgramModel program = resourceCreator().insertActiveProgram("program");

    // Set preferred locale so that browser doesn't get redirected to set it. This way we get a
    // meaningful redirect location.
    currentApplicant.getApplicantData().setPreferredLocale(Locale.US);
    currentApplicant.save();

    String alphaNumProgramParam = program.getSlug();
    Request request =
        fakeRequestBuilder().addCiviFormSetting("NORTH_STAR_APPLICANT_UI", "false").build();
    Result result = controller.show(request, alphaNumProgramParam).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .contains(
            routes.ApplicantProgramReviewController.review(
                    Long.toString(program.id), /* isFromUrlCall= */ false)
                .url());
  }

  @Test
  public void show_withProgramIdRedirects() {
    Result result = controller.show(fakeRequest(), "123").toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void showWithApplicantId_withProgramIdRedirects() {
    Result result =
        controller.showWithApplicantId(fakeRequest(), 1, "123").toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void showWithApplicantId_withStringProgramParam_redirectsToReview() {
    ProgramModel program = resourceCreator().insertActiveProgram("program");

    // Set preferred locale so that browser doesn't get redirected to set it. This way we get a
    // meaningful redirect location.
    currentApplicant.getApplicantData().setPreferredLocale(Locale.US);
    currentApplicant.save();

    String alphaNumProgramParam = program.getSlug();
    Request request =
        fakeRequestBuilder().addCiviFormSetting("NORTH_STAR_APPLICANT_UI", "false").build();
    Result result =
        controller
            .showWithApplicantId(request, currentApplicant.id, alphaNumProgramParam)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .contains(
            routes.ApplicantProgramReviewController.review(
                    Long.toString(program.id), /* isFromUrlCall= */ false)
                .url());
  }

  @Test
  public void showInfoDisabledProgram() {
    resourceCreator.insertActiveDisabledProgram("disabledProgram");

    Map<String, String> flashData = new HashMap<>();
    flashData.put("redirected-from-program-slug", "disabledProgram");
    Request request = fakeRequestBuilder().flash(flashData).build();

    Result result =
        controller.showInfoDisabledProgram(request, "disabledProgram").toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_whenFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    ProgramModel program = ProgramBuilder.newActiveProgram().build();
    String programId = String.valueOf(program.id);

    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        controller.edit(request, programId, /* isFromUrlCall= */ true).toCompletableFuture().join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void edit_redirectToOtherUrl() {
    ProgramModel program = ProgramBuilder.newActiveProgram().build();
    String programId = String.valueOf(program.id);

    Result result =
        controller
            .edit(fakeRequest(), programId, /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // Successfully redirects to another route, which redirect to various routes. Thus, here we
    // only check the redirect happens and we make the final route check in other tests.
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void editWithApplicanId_whenFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    ProgramModel program = ProgramBuilder.newActiveProgram().build();
    String programId = String.valueOf(program.id);

    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        controller
            .editWithApplicantId(request, currentApplicant.id, programId, /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void editWithApplicantId_whenDifferentApplicant_redirectsToHome() {
    Result result =
        controller
            .editWithApplicantId(
                fakeRequest(),
                currentApplicant.id + 1,
                Long.toString(1L),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void editWithApplicantId_whenApplicantWithoutProfile_redirectsToHome() {
    Result result =
        controller
            .editWithApplicantId(
                fakeRequest(),
                applicantWithoutProfile.id,
                Long.toString(1L),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void editWithApplicantId_whenApplicantAccessToDraftProgram_redirectsToHome() {
    ProgramModel draftProgram = ProgramBuilder.newDraftProgram().build();
    Result result =
        controller
            .editWithApplicantId(
                fakeRequest(),
                currentApplicant.id,
                Long.toString(draftProgram.id),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void editWithApplicantId_whenCiviformAdminAccessToDraftProgram_success() {
    AccountModel adminAccount = createGlobalAdminWithMockedProfile();
    long adminApplicantId = adminAccount.newestApplicant().orElseThrow().id;
    ProgramModel draftProgram = ProgramBuilder.newDraftProgram().build();
    Result result =
        controller
            .editWithApplicantId(
                fakeRequest(),
                adminApplicantId,
                Long.toString(draftProgram.id),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void editWithApplicantId_whenInvalidProgram_error() {
    Result result =
        controller
            .editWithApplicantId(
                fakeRequest(),
                currentApplicant.id,
                Long.toString(9999L),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void editWithApplicantId_whenApplicantAccessToObsoleteProgram_success() {
    ProgramModel obsoleteProgram = ProgramBuilder.newObsoleteProgram("name").build();
    Result result =
        controller
            .editWithApplicantId(
                fakeRequest(),
                currentApplicant.id,
                Long.toString(obsoleteProgram.id),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void editWithApplicantId_whenNewProgram_redirectsToFirstBlock() {
    ProgramModel program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();

    Result result =
        controller
            .editWithApplicantId(
                fakeRequest(),
                currentApplicant.id,
                Long.toString(program.id),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.ApplicantProgramBlocksController.edit(
                    Long.toString(program.id),
                    "1",
                    /* questionName= */ Optional.empty(),
                    /* isFromUrlCall= */ false)
                .url());
  }

  @Test
  public void editWithApplicantId_whenIncompleteBlocks_redirectsToFirstIncompleteBlock() {
    QuestionDefinition colorQuestion =
        testQuestionBank().textApplicantFavoriteColor().getQuestionDefinition();
    ProgramModel program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestionDefinition(colorQuestion)
            .withBlock()
            .withRequiredQuestion(testQuestionBank().addressApplicantAddress())
            .build();
    // Answer the color question
    Path colorPath = ApplicantData.APPLICANT_PATH.join("applicant_favorite_color");
    QuestionAnswerer.answerTextQuestion(
        currentApplicant.getApplicantData(), colorPath, "forest green");
    QuestionAnswerer.addMetadata(currentApplicant.getApplicantData(), colorPath, 456L, 12345L);
    currentApplicant.save();

    Result result =
        controller
            .editWithApplicantId(
                fakeRequest(),
                currentApplicant.id,
                Long.toString(program.id),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.ApplicantProgramBlocksController.edit(
                    Long.toString(program.id),
                    "2",
                    /* questionName= */ Optional.empty(),
                    /* isFromUrlCall= */ false)
                .url());
  }

  // TODO(https://github.com/seattle-uat/universal-application-tool/issues/256): Should redirect to
  //  end of program submission.
  @Ignore
  public void editWithApplicantId_whenNoMoreIncompleteBlocks_redirectsToListOfPrograms() {
    ProgramModel program = resourceCreator().insertActiveProgram("My Program");

    Result result =
        controller
            .editWithApplicantId(
                fakeRequest(),
                currentApplicant.id,
                Long.toString(program.id),
                /* isFromUrlCall= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation())
        .hasValue(routes.ApplicantProgramsController.index(ImmutableList.of()).url());
  }

  @Test
  public void hxFilter_isOk() {
    Result result =
        controller.hxFilter(fakeRequest(), ImmutableList.of(), "").toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
  }
}
