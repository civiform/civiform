package controllers.applicant;

import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.stubMessagesApi;
import static support.CfTestHelpers.requestBuilderWithSettings;

import controllers.WithMockedProfiles;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import models.Account;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import models.Program;
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
import services.question.types.QuestionDefinition;
import support.ProgramBuilder;
import support.QuestionAnswerer;

public class ApplicantProgramsControllerTest extends WithMockedProfiles {

  private Applicant currentApplicant;
  private ApplicantProgramsController controller;
  private VersionRepository versionRepository;

  @Before
  public void setUp() {
    resetDatabase();
    controller = instanceOf(ApplicantProgramsController.class);
    currentApplicant = createApplicantWithMockedProfile();
  }

  @Test
  public void index_differentApplicant_returnsUnauthorizedResult() {
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result = controller.index(request, currentApplicant.id + 1).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void index_withNoPrograms_returnsEmptyResult() {
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result = controller.index(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(result.charset()).hasValue("utf-8");
    assertThat(contentAsString(result)).doesNotContain("program-card");
  }

  @Test
  public void index_withPrograms_returnsOnlyRelevantPrograms() {
    resourceCreator().insertActiveProgram("one");
    resourceCreator().insertActiveProgram("two");
    resourceCreator().insertDraftProgram("three");

    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result = controller.index(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("one");
    assertThat(contentAsString(result)).contains("two");
    assertThat(contentAsString(result)).doesNotContain("three");
  }

  @Test
  public void index_clearsRedirectToSessionKey() {
    Request request =
        addCSRFToken(Helpers.fakeRequest().session(REDIRECT_TO_SESSION_KEY, "redirect")).build();
    Result result = controller.index(request, currentApplicant.id).toCompletableFuture().join();
    assertThat(result.session().get(REDIRECT_TO_SESSION_KEY)).isEmpty();
  }

  @Test
  public void test_deduplicate_inProgressPrograms() {
    versionRepository = instanceOf(VersionRepository.class);
    String programName = "In Progress Program";
    Program program = resourceCreator().insertActiveProgram(programName);

    Application app = new Application(currentApplicant, program, LifecycleStage.DRAFT);
    app.save();

    resourceCreator().insertDraftProgram(programName);
    this.versionRepository.publishNewSynchronizedVersion();

    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result = controller.index(request, currentApplicant.id).toCompletableFuture().join();

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
  public void index_withProgram_includesApplyButtonWithRedirect() {
    Program program = resourceCreator().insertActiveProgram("program");

    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result = controller.index(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(routes.ApplicantProgramsController.view(currentApplicant.id, program.id).url());
  }

  @Test
  public void index_withCommonIntakeform_includesStartHereButtonWithRedirect() {
    Program program = resourceCreator().insertActiveCommonIntakeForm("benefits");

    Request request =
        addCSRFToken(requestBuilderWithSettings("INTAKE_FORM_ENABLED", "true")).build();
    Result result = controller.index(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(routes.ApplicantProgramsController.view(currentApplicant.id, program.id).url());
  }

  @Test
  public void index_usesMessagesForUserPreferredLocale() {
    // Set the PLAY_LANG cookie
    Http.Request request =
        addCSRFToken(requestBuilderWithSettings())
            .langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi())
            .build();

    Result result = controller.index(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Obtén beneficios");
  }

  @Test
  public void index_missingTranslationForProgram_defaultsToEnglish() {
    resourceCreator().insertActiveProgram(Locale.forLanguageTag("es-US"), "A different language!");
    resourceCreator().insertActiveProgram("English program"); // Missing translation

    // Set the PLAY_LANG cookie
    Http.Request request =
        addCSRFToken(requestBuilderWithSettings())
            .langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi())
            .build();

    Result result = controller.index(request, currentApplicant.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("A different language!");
    assertThat(contentAsString(result)).contains("English program");
  }

  @Test
  public void view_includesApplyButton() {
    Program program = resourceCreator().insertActiveProgram("program");

    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller.view(request, currentApplicant.id, program.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(
            routes.ApplicantProgramReviewController.review(currentApplicant.id, program.id).url());
  }

  @Test
  public void view_invalidProgram_returnsBadRequest() {
    Result result =
        controller
            .view(requestBuilderWithSettings().build(), currentApplicant.id, 9999L)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void edit_differentApplicant_returnsUnauthorizedResult() {
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller.edit(request, currentApplicant.id + 1, 1L).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void edit_applicantAccessToDraftProgram_returnsUnauthorized() {
    Program draftProgram = ProgramBuilder.newDraftProgram().build();
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller.edit(request, currentApplicant.id, draftProgram.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void edit_civiformAdminAccessToDraftProgram_isOk() {
    Account adminAccount = createGlobalAdminWithMockedProfile();
    long adminApplicantId = adminAccount.newestApplicant().orElseThrow().id;
    Program draftProgram = ProgramBuilder.newDraftProgram().build();
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller.edit(request, adminApplicantId, draftProgram.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void edit_invalidProgram_returnsBadRequest() {
    Result result =
        controller
            .edit(requestBuilderWithSettings().build(), currentApplicant.id, 9999L)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void edit_applicantAccessToObsoleteProgram_isOk() {
    Program obsoleteProgram = ProgramBuilder.newObsoleteProgram("name").build();
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller
            .edit(request, currentApplicant.id, obsoleteProgram.id)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void edit_withNewProgram_redirectsToFirstBlock() {
    Program program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();

    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller.edit(request, currentApplicant.id, program.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.ApplicantProgramBlocksController.edit(currentApplicant.id, program.id, "1")
                .url());
  }

  @Test
  public void edit_redirectsToFirstIncompleteBlock() {
    QuestionDefinition colorQuestion =
        testQuestionBank().applicantFavoriteColor().getQuestionDefinition();
    Program program =
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
        controller.edit(request, currentApplicant.id, program.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.ApplicantProgramBlocksController.edit(currentApplicant.id, program.id, "2")
                .url());
  }

  // TODO(https://github.com/seattle-uat/universal-application-tool/issues/256): Should redirect to
  //  end of program submission.
  @Ignore
  public void edit_whenNoMoreIncompleteBlocks_redirectsToListOfPrograms() {
    Program program = resourceCreator().insertActiveProgram("My Program");

    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result =
        controller.edit(request, currentApplicant.id, program.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation())
        .hasValue(routes.ApplicantProgramsController.index(currentApplicant.id).url());
  }
}
