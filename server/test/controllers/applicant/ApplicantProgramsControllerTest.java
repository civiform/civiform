package controllers.applicant;

import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.FOUND;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.stubMessagesApi;
import static support.CfTestHelpers.requestBuilderWithSettings;

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
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.test.Helpers;
import repository.VersionRepository;
import services.Path;
import services.applicant.ApplicantData;
import services.question.QuestionAnswerer;
import services.question.types.QuestionDefinition;
import support.ProgramBuilder;

public class ApplicantProgramsControllerTest extends WithMockedProfiles {

  private ApplicantModel currentApplicant;
  private ApplicantModel applicantWithoutProfile;
  private ApplicantProgramsController controller;
  private VersionRepository versionRepository;

  @Before
  public void setUp() {
    resetDatabase();
    controller = instanceOf(ApplicantProgramsController.class);
    currentApplicant = createApplicantWithMockedProfile();
    applicantWithoutProfile = createApplicant();
  }

  @Test
  public void indexWithApplicantId_differentApplicant_redirectsToHome() {
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller
            .indexWithApplicantId(request, currentApplicant.id + 1)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void indexWithApplicantId_applicantWithoutProfile_redirectsToHome() {
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller
            .indexWithApplicantId(request, applicantWithoutProfile.id)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void indexWithApplicantId_withNoPrograms_returnsEmptyResult() {
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller.indexWithApplicantId(request, currentApplicant.id).toCompletableFuture().join();

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

    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller.indexWithApplicantId(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("one");
    assertThat(contentAsString(result)).contains("two");
    assertThat(contentAsString(result)).doesNotContain("three");
  }

  @Test
  public void indexWithApplicantId_clearsRedirectToSessionKey() {
    Request request =
        addCSRFToken(Helpers.fakeRequest().session(REDIRECT_TO_SESSION_KEY, "redirect")).build();
    Result result =
        controller.indexWithApplicantId(request, currentApplicant.id).toCompletableFuture().join();
    assertThat(result.session().get(REDIRECT_TO_SESSION_KEY)).isEmpty();
  }

  @Test
  public void test_deduplicate_inProgressPrograms() {
    versionRepository = instanceOf(VersionRepository.class);
    String programName = "In Progress Program";
    ProgramModel program = resourceCreator().insertActiveProgram(programName);

    ApplicationModel app = new ApplicationModel(currentApplicant, program, LifecycleStage.DRAFT);
    app.save();

    resourceCreator().insertDraftProgram(programName);
    this.versionRepository.publishNewSynchronizedVersion();

    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller.indexWithApplicantId(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    // A program's name appears in the index view page content 3 times:
    //  1) Program card title
    //  2) Program details aria-label
    //  3) Apply button aria-label
    // If it appears 6 times, that means there is a duplicate of the program.
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

    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller.indexWithApplicantId(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(routes.ApplicantProgramsController.show(String.valueOf(program.id)).url());
  }

  @Test
  public void indexWithApplicantId_withCommonIntakeform_includesStartHereButtonWithRedirect() {
    ProgramModel program = resourceCreator().insertActiveCommonIntakeForm("benefits");

    Request request =
        addCSRFToken(requestBuilderWithSettings("INTAKE_FORM_ENABLED", "true")).build();
    Result result =
        controller.indexWithApplicantId(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(routes.ApplicantProgramsController.show(String.valueOf(program.id)).url());
  }

  @Test
  public void indexWithApplicantId_usesMessagesForUserPreferredLocale() {
    // Set the PLAY_LANG cookie
    Http.Request request =
        addCSRFToken(requestBuilderWithSettings())
            .langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi())
            .build();

    Result result =
        controller.indexWithApplicantId(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Buscar programas");
  }

  @Test
  public void indexWithApplicantId_missingTranslationForProgram_defaultsToEnglish() {
    resourceCreator().insertActiveProgram(Locale.forLanguageTag("es-US"), "A different language!");
    resourceCreator().insertActiveProgram("English program"); // Missing translation

    // Set the PLAY_LANG cookie
    Http.Request request =
        addCSRFToken(requestBuilderWithSettings())
            .langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi())
            .build();

    Result result =
        controller.indexWithApplicantId(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("A different language!");
    assertThat(contentAsString(result)).contains("English program");
  }

  @Test
  public void showWithApplicantId_includesApplyButton() {
    ProgramModel program = resourceCreator().insertActiveProgram("program");

    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller
            .showWithApplicantId(request, currentApplicant.id, program.id)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(routes.ApplicantProgramReviewController.review(program.id).url());
  }

  @Test
  public void showWithApplicantId_invalidProgram_returnsBadRequest() {
    Result result =
        controller
            .showWithApplicantId(requestBuilderWithSettings().build(), currentApplicant.id, 9999)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
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

    Request request = addCSRFToken(requestBuilderWithSettings()).build();

    String alphaNumProgramParam = program.getSlug();
    Result result = controller.show(request, alphaNumProgramParam).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .contains(routes.ApplicantProgramReviewController.review(program.id).url());
  }

  @Test
  public void showInfoDisabledProgram() {
    ProgramModel disabledProgram = resourceCreator.insertActiveDisabledProgram("disabledProgram");

    Map<String, String> flashData = new HashMap<>();
    flashData.put("redirected-from-program-slug", "disabledProgram");
    Request request = addCSRFToken(requestBuilderWithSettings()).flash(flashData).build();

    Result result =
        controller.showInfoDisabledProgram(request, "disabledProgram").toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_differentApplicant_redirectsToHome() {
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller
            .editWithApplicantId(request, currentApplicant.id + 1, 1L)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void edit_applicantWithoutProfile_redirectsToHome() {
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller
            .editWithApplicantId(request, applicantWithoutProfile.id, 1L)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void edit_applicantAccessToDraftProgram_redirectsToHome() {
    ProgramModel draftProgram = ProgramBuilder.newDraftProgram().build();
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller
            .editWithApplicantId(request, currentApplicant.id, draftProgram.id)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void edit_civiformAdminAccessToDraftProgram_isOk() {
    AccountModel adminAccount = createGlobalAdminWithMockedProfile();
    long adminApplicantId = adminAccount.newestApplicant().orElseThrow().id;
    ProgramModel draftProgram = ProgramBuilder.newDraftProgram().build();
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller
            .editWithApplicantId(request, adminApplicantId, draftProgram.id)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void edit_invalidProgram_returnsBadRequest() {
    Result result =
        controller
            .editWithApplicantId(requestBuilderWithSettings().build(), currentApplicant.id, 9999L)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void edit_applicantAccessToObsoleteProgram_isOk() {
    ProgramModel obsoleteProgram = ProgramBuilder.newObsoleteProgram("name").build();
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller
            .editWithApplicantId(request, currentApplicant.id, obsoleteProgram.id)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void edit_withNewProgram_redirectsToFirstBlock() {
    ProgramModel program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();

    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller
            .editWithApplicantId(request, currentApplicant.id, program.id)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.ApplicantProgramBlocksController.edit(
                    program.id, "1", /* questionName= */ Optional.empty())
                .url());
  }

  @Test
  public void edit_redirectsToFirstIncompleteBlock() {
    QuestionDefinition colorQuestion =
        testQuestionBank().applicantFavoriteColor().getQuestionDefinition();
    ProgramModel program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestionDefinition(colorQuestion)
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantAddress())
            .build();
    // Answer the color question
    Path colorPath = ApplicantData.APPLICANT_PATH.join("applicant_favorite_color");
    QuestionAnswerer.answerTextQuestion(
        currentApplicant.getApplicantData(), colorPath, "forest green");
    QuestionAnswerer.addMetadata(currentApplicant.getApplicantData(), colorPath, 456L, 12345L);
    currentApplicant.save();

    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller
            .editWithApplicantId(request, currentApplicant.id, program.id)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.ApplicantProgramBlocksController.edit(
                    program.id, "2", /* questionName= */ Optional.empty())
                .url());
  }

  // TODO(https://github.com/seattle-uat/universal-application-tool/issues/256): Should redirect to
  //  end of program submission.
  @Ignore
  public void edit_whenNoMoreIncompleteBlocks_redirectsToListOfPrograms() {
    ProgramModel program = resourceCreator().insertActiveProgram("My Program");

    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller
            .editWithApplicantId(request, currentApplicant.id, program.id)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation())
        .hasValue(routes.ApplicantProgramsController.index().url());
  }
}
