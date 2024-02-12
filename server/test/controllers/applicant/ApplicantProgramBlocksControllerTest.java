package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.stubMessagesApi;
import static support.CfTestHelpers.requestBuilderWithSettings;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.WithMockedProfiles;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import models.AccountModel;
import models.ApplicantModel;
import models.ProgramModel;
import models.StoredFileModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import repository.StoredFileRepository;
import services.Path;
import services.applicant.question.Scalar;
import support.ProgramBuilder;

public class ApplicantProgramBlocksControllerTest extends WithMockedProfiles {

  private ApplicantProgramBlocksController subject;
  private ProgramModel program;
  private ApplicantModel applicant;

  @Before
  public void setUpWithFreshApplicant() {
    resetDatabase();

    subject = instanceOf(ApplicantProgramBlocksController.class);
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantFile())
            .build();
    applicant = createApplicantWithMockedProfile();
  }

  @Test
  public void edit_invalidApplicant_returnsUnauthorized() {
    long badApplicantId = applicant.id + 1000;
    Request request =
        requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.editWithApplicantId(
                    badApplicantId, program.id, "1", /* questionName= */ Optional.empty()))
            .build();

    Result result =
        subject
            .editWithApplicantId(
                request, badApplicantId, program.id, "1", /* questionName= */ Optional.empty())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void edit_applicantAccessToDraftProgram_returnsUnauthorized() {
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.editWithApplicantId(
                        applicant.id, program.id, "1", /* questionName= */ Optional.empty())))
            .build();
    Result result =
        subject
            .editWithApplicantId(
                request, applicant.id, draftProgram.id, "1", /* questionName= */ Optional.empty())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void edit_civiformAdminAccessToDraftProgram_isOk() {
    AccountModel adminAccount = createGlobalAdminWithMockedProfile();
    applicant = adminAccount.newestApplicant().orElseThrow();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.editWithApplicantId(
                        applicant.id, program.id, "1", /* questionName= */ Optional.empty())))
            .build();
    Result result =
        subject
            .editWithApplicantId(
                request, applicant.id, draftProgram.id, "1", /* questionName= */ Optional.empty())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void edit_obsoleteProgram_isOk() {
    ProgramModel obsoleteProgram = ProgramBuilder.newObsoleteProgram("program").build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.editWithApplicantId(
                        applicant.id, program.id, "1", /* questionName= */ Optional.empty())))
            .build();
    Result result =
        subject
            .editWithApplicantId(
                request,
                applicant.id,
                obsoleteProgram.id,
                "1",
                /* questionName= */ Optional.empty())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void edit_toAProgramThatDoesNotExist_returns404() {
    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.editWithApplicantId(
                        applicant.id,
                        program.id + 1000,
                        "1",
                        /* questionName= */ Optional.empty())))
            .build();

    Result result =
        subject
            .editWithApplicantId(
                request, applicant.id, program.id + 1000, "1", /* questionName= */ Optional.empty())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_toAnExistingBlock_rendersTheBlock() {
    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.editWithApplicantId(
                        applicant.id, program.id, "1", /* questionName= */ Optional.empty())))
            .build();

    Result result =
        subject
            .editWithApplicantId(
                request, applicant.id, program.id, "1", /* questionName= */ Optional.empty())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void edit_toABlockThatDoesNotExist_returns404() {
    Request request =
        requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.editWithApplicantId(
                    applicant.id, program.id, "9999", /* questionName= */ Optional.empty()))
            .build();

    Result result =
        subject
            .editWithApplicantId(
                request, applicant.id, program.id, "9999", /* questionName= */ Optional.empty())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_withMessages_returnsCorrectButtonText() {
    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.editWithApplicantId(
                            applicant.id, program.id, "1", /* questionName= */ Optional.empty()))
                    .langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi()))
            .build();

    Result result =
        subject
            .editWithApplicantId(
                request, applicant.id, program.id, "1", /* questionName= */ Optional.empty())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Guardar y continuar");
  }

  @Test
  public void previous_toAnExistingBlock_rendersTheBlock() {
    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.previousWithApplicantId(
                        applicant.id, program.id, 0, true)))
            .build();

    Result result =
        subject
            .previousWithApplicantId(request, applicant.id, program.id, 0, true)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void previous_applicantAccessToDraftProgram_returnsUnauthorized() {
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.previousWithApplicantId(
                        applicant.id, program.id, 0, true)))
            .build();
    Result result =
        subject
            .editWithApplicantId(
                request, applicant.id, draftProgram.id, "1", /* questionName= */ Optional.empty())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void previous_civiformAdminAccessToDraftProgram_isOk() {
    AccountModel adminAccount = createGlobalAdminWithMockedProfile();
    applicant = adminAccount.newestApplicant().orElseThrow();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.previousWithApplicantId(
                        applicant.id, program.id, 0, true)))
            .build();
    Result result =
        subject
            .editWithApplicantId(
                request, applicant.id, draftProgram.id, "1", /* questionName= */ Optional.empty())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void previous_obsoleteProgram_isOk() {
    ProgramModel obsoleteProgram = ProgramBuilder.newObsoleteProgram("program").build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.previousWithApplicantId(
                        applicant.id, program.id, 0, true)))
            .build();
    Result result =
        subject
            .editWithApplicantId(
                request,
                applicant.id,
                obsoleteProgram.id,
                "1",
                /* questionName= */ Optional.empty())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void update_invalidApplicant_returnsUnauthorized() {
    long badApplicantId = applicant.id + 1000;
    Request request =
        requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.updateWithApplicantId(
                    badApplicantId,
                    program.id,
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper()))
            .build();

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
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.updateWithApplicantId(
                        applicant.id,
                        program.id,
                        /* blockId= */ "1",
                        /* inReview= */ false,
                        new ApplicantRequestedActionWrapper())))
            .build();
    Result result =
        subject
            .editWithApplicantId(
                request, applicant.id, draftProgram.id, "1", /* questionName= */ Optional.empty())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void update_civiformAdminAccessToDraftProgram_isOk() {
    AccountModel adminAccount = createGlobalAdminWithMockedProfile();
    applicant = adminAccount.newestApplicant().orElseThrow();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.updateWithApplicantId(
                        applicant.id,
                        program.id,
                        /* blockId= */ "1",
                        /* inReview= */ false,
                        new ApplicantRequestedActionWrapper())))
            .build();
    Result result =
        subject
            .editWithApplicantId(
                request, applicant.id, draftProgram.id, "1", /* questionName= */ Optional.empty())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void update_obsoleteProgram_isOk() {
    ProgramModel obsoleteProgram = ProgramBuilder.newObsoleteProgram("program").build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.updateWithApplicantId(
                        applicant.id,
                        program.id,
                        /* blockId= */ "1",
                        /* inReview= */ false,
                        new ApplicantRequestedActionWrapper())))
            .build();
    Result result =
        subject
            .editWithApplicantId(
                request,
                applicant.id,
                obsoleteProgram.id,
                "1",
                /* questionName= */ Optional.empty())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void update_invalidProgram_returnsBadRequest() {
    long badProgramId = program.id + 1000;
    Request request =
        requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.updateWithApplicantId(
                    applicant.id,
                    badProgramId,
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper()))
            .build();

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
    Request request =
        requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.updateWithApplicantId(
                    applicant.id,
                    program.id,
                    badBlockId,
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper()))
            .build();

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
    Request request =
        requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.updateWithApplicantId(
                    applicant.id,
                    program.id,
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper()))
            .bodyForm(ImmutableMap.of("fake.path", "value"))
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

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_reservedPathsInRequest_returnsBadRequest() {
    String reservedPath = Path.create("metadata").join(Scalar.PROGRAM_UPDATED_IN).toString();
    Request request =
        requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.updateWithApplicantId(
                    applicant.id,
                    program.id,
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper()))
            .bodyForm(ImmutableMap.of(reservedPath, "value"))
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

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_withValidationErrors_isOK() {
    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.updateWithApplicantId(
                            applicant.id,
                            program.id,
                            /* blockId= */ "1",
                            /* inReview= */ false,
                            new ApplicantRequestedActionWrapper()))
                    .bodyForm(
                        ImmutableMap.of(
                            Path.create("applicant.applicant_name")
                                .join(Scalar.FIRST_NAME)
                                .toString(),
                            "FirstName",
                            Path.create("applicant.applicant_name")
                                .join(Scalar.LAST_NAME)
                                .toString(),
                            "")))
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

  @Test
  public void update_withValidationErrors_requestedActionReview_staysOnBlockAndShowsErrors() {
    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.updateWithApplicantId(
                            applicant.id,
                            program.id,
                            /* blockId= */ "1",
                            /* inReview= */ false,
                            new ApplicantRequestedActionWrapper(
                                ApplicantRequestedAction.REVIEW_PAGE)))
                    .bodyForm(
                        ImmutableMap.of(
                            Path.create("applicant.applicant_name")
                                .join(Scalar.FIRST_NAME)
                                .toString(),
                            "FirstName",
                            Path.create("applicant.applicant_name")
                                .join(Scalar.LAST_NAME)
                                .toString(),
                            "")))
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
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.updateWithApplicantId(
                            applicant.id,
                            program.id,
                            /* blockId= */ "1",
                            /* inReview= */ false,
                            new ApplicantRequestedActionWrapper(
                                ApplicantRequestedAction.PREVIOUS_BLOCK)))
                    .bodyForm(
                        ImmutableMap.of(
                            Path.create("applicant.applicant_name")
                                .join(Scalar.FIRST_NAME)
                                .toString(),
                            "FirstName",
                            Path.create("applicant.applicant_name")
                                .join(Scalar.LAST_NAME)
                                .toString(),
                            "")))
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
  public void update_withNextBlock_requestedActionNext_redirectsToEditNextBlock() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().applicantName())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().applicantAddress())
            .build();
    Request request =
        requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.updateWithApplicantId(
                    applicant.id,
                    program.id,
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper(ApplicantRequestedAction.NEXT_BLOCK)))
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
                new ApplicantRequestedActionWrapper(ApplicantRequestedAction.NEXT_BLOCK))
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String nextBlockEditRoute =
        routes.ApplicantProgramBlocksController.edit(
                program.id, /* blockId= */ "2", /* questionName= */ Optional.empty())
            .url();
    assertThat(result.redirectLocation()).hasValue(nextBlockEditRoute);
  }

  @Test
  public void update_withNextBlock_requestedActionReview_redirectsToReview() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().applicantName())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().applicantAddress())
            .build();
    Request request =
        requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.updateWithApplicantId(
                    applicant.id,
                    program.id,
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper(ApplicantRequestedAction.REVIEW_PAGE)))
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
    String reviewRoute = routes.ApplicantProgramReviewController.review(program.id).url();
    assertThat(result.redirectLocation()).hasValue(reviewRoute);
  }

  @Test
  public void update_requestedActionReview_answersSaved() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();
    Request request =
        requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.updateWithApplicantId(
                    applicant.id,
                    program.id,
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper(ApplicantRequestedAction.REVIEW_PAGE)))
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
            .withRequiredQuestion(testQuestionBank().applicantName())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().applicantFavoriteColor())
            .withBlock("block 3")
            .withRequiredQuestion(testQuestionBank().applicantIceCream())
            .withBlock("block 4")
            .withRequiredQuestion(testQuestionBank().applicantEmail())
            .build();
    Request request =
        requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.updateWithApplicantId(
                    applicant.id,
                    program.id,
                    /* blockId= */ "4",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper(ApplicantRequestedAction.PREVIOUS_BLOCK)))
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
    String previousRoute =
        routes.ApplicantProgramBlocksController.previous(
                // The 4th block was filled in, which is index 3. So, the previous block would be
                // index 2.
                program.id, /* previousBlockIndex= */ 2, /* inReview= */ false)
            .url();
    assertThat(result.redirectLocation()).hasValue(previousRoute);
  }

  @Test
  public void update_requestedActionPrevious_isFirstBlock_redirectsToReview() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();
    Request request =
        requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.updateWithApplicantId(
                    applicant.id,
                    program.id,
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper(ApplicantRequestedAction.PREVIOUS_BLOCK)))
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
    String reviewRoute = routes.ApplicantProgramReviewController.review(program.id).url();
    assertThat(result.redirectLocation()).hasValue(reviewRoute);
  }

  @Test
  public void update_onFirstBlock_requestedActionPrevious_answersSaved() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();
    Request request =
        requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.updateWithApplicantId(
                    applicant.id,
                    program.id,
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper(ApplicantRequestedAction.PREVIOUS_BLOCK)))
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
            .withRequiredQuestion(testQuestionBank().applicantIceCream())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().applicantFavoriteColor())
            .withBlock("block 3")
            .withRequiredQuestion(testQuestionBank().applicantEmail())
            .build();
    Request request =
        requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.updateWithApplicantId(
                    applicant.id,
                    program.id,
                    /* blockId= */ "3",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper(ApplicantRequestedAction.PREVIOUS_BLOCK)))
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
            .withRequiredCorrectedAddressQuestion(testQuestionBank().applicantAddress())
            .build();
    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.updateWithApplicantId(
                            applicant.id,
                            program.id,
                            "1",
                            false,
                            new ApplicantRequestedActionWrapper()))
                    .session("ESRI_ADDRESS_CORRECTION_ENABLED", "true")
                    .bodyForm(
                        ImmutableMap.of(
                            Path.create("applicant.applicant_address")
                                .join(Scalar.STREET)
                                .toString(),
                            "Address In Area",
                            Path.create("applicant.applicant_address")
                                .join(Scalar.LINE2)
                                .toString(),
                            "",
                            Path.create("applicant.applicant_address").join(Scalar.CITY).toString(),
                            "Redlands",
                            Path.create("applicant.applicant_address")
                                .join(Scalar.STATE)
                                .toString(),
                            "CA",
                            Path.create("applicant.applicant_address").join(Scalar.ZIP).toString(),
                            "92373")))
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

    // check that the address correction screen is skipped and the user is redirected to the review
    // screen
    String reviewRoute = routes.ApplicantProgramReviewController.review(program.id).url();
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
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();

    Request request =
        requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.updateWithApplicantId(
                    applicant.id,
                    program.id,
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper()))
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

    String reviewRoute = routes.ApplicantProgramReviewController.review(program.id).url();

    assertThat(result.redirectLocation()).hasValue(reviewRoute);
  }

  @Test
  public void updateFile_invalidApplicant_returnsUnauthorized() {
    long badApplicantId = applicant.id + 1000;
    RequestBuilder request =
        requestBuilderWithSettings(
            routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
                badApplicantId, program.id, /* blockId= */ "2", /* inReview= */ false));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                badApplicantId,
                program.id,
                /* blockId= */ "2",
                /* inReview= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void updateFile_applicantAccessToDraftProgram_returnsUnauthorized() {
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
                        applicant.id, program.id, /* blockId= */ "1", /* inReview= */ false)))
            .build();
    Result result =
        subject
            .editWithApplicantId(
                request, applicant.id, draftProgram.id, "1", /* questionName= */ Optional.empty())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void updateFile_civiformAdminAccessToDraftProgram_isOk() {
    AccountModel adminAccount = createGlobalAdminWithMockedProfile();
    applicant = adminAccount.newestApplicant().orElseThrow();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
                        applicant.id, program.id, /* blockId= */ "1", /* inReview= */ false)))
            .build();
    Result result =
        subject
            .editWithApplicantId(
                request, applicant.id, draftProgram.id, "1", /* questionName= */ Optional.empty())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void updateFile_obsoleteProgram_isOk() {
    ProgramModel obsoleteProgram = ProgramBuilder.newObsoleteProgram("program").build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
                        applicant.id, program.id, /* blockId= */ "1", /* inReview= */ false)))
            .build();
    Result result =
        subject
            .editWithApplicantId(
                request,
                applicant.id,
                obsoleteProgram.id,
                "1",
                /* questionName= */ Optional.empty())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void updateFile_invalidProgram_returnsBadRequest() {
    long badProgramId = program.id + 1000;
    RequestBuilder request =
        requestBuilderWithSettings(
            routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
                applicant.id, badProgramId, /* blockId= */ "2", /* inReview= */ false));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                applicant.id,
                badProgramId,
                /* blockId= */ "2",
                /* inReview= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void updateFile_invalidBlock_returnsBadRequest() {
    String badBlockId = "1000";
    RequestBuilder request =
        requestBuilderWithSettings(
            routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
                applicant.id, program.id, badBlockId, /* inReview= */ false));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(), applicant.id, program.id, badBlockId, /* inReview= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void updateFile_notFileUploadBlock_returnsBadRequest() {
    String badBlockId = "1";
    RequestBuilder request =
        requestBuilderWithSettings(
            routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
                applicant.id, program.id, badBlockId, /* inReview= */ false));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(), applicant.id, program.id, badBlockId, /* inReview= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void updateFile_missingFileKeyAndBucket_returnsBadRequest() {
    RequestBuilder request =
        requestBuilderWithSettings(
            routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
                applicant.id, program.id, /* blockId= */ "2", /* inReview= */ false));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                applicant.id,
                program.id,
                /* blockId= */ "2",
                /* inReview= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void updateFile_withNextBlock_redirectsToEdit() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().applicantFile())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().applicantAddress())
            .build();
    RequestBuilder request =
        requestBuilderWithSettings(
            routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
                applicant.id, program.id, /* blockId= */ "1", /* inReview= */ false));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String nextBlockEditRoute =
        routes.ApplicantProgramBlocksController.edit(
                program.id, /* blockId= */ "2", /* questionName= */ Optional.empty())
            .url();
    assertThat(result.redirectLocation()).hasValue(nextBlockEditRoute);
  }

  @Test
  public void updateFile_completedProgram_redirectsToReviewPage() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().applicantFile())
            .build();

    RequestBuilder request =
        requestBuilderWithSettings(
            routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
                applicant.id, program.id, /* blockId= */ "1", /* inReview= */ false));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    String reviewRoute = routes.ApplicantProgramReviewController.review(program.id).url();

    assertThat(result.redirectLocation()).hasValue(reviewRoute);
  }

  /**
   * This test guards regression for the bugfix to
   * https://github.com/seattle-uat/civiform/issues/2818
   */
  @Test
  public void updateFile_storedFileAlreadyExists_doesNotCreateDuplicateStoredFile() {
    var storedFileRepo = instanceOf(StoredFileRepository.class);

    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().applicantFile())
            .build();

    var fileKey = "fake-key";
    var storedFile = new StoredFileModel();
    storedFile.setName(fileKey);
    storedFile.save();

    RequestBuilder request =
        requestBuilderWithSettings(
            routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
                applicant.id, program.id, /* blockId= */ "1", /* inReview= */ false));
    addQueryString(request, ImmutableMap.of("key", fileKey, "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false)
            .toCompletableFuture()
            .join();

    int storedFileCount =
        storedFileRepo.lookupFiles(ImmutableList.of(fileKey)).toCompletableFuture().join().size();
    assertThat(storedFileCount).isEqualTo(1);
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  private RequestBuilder addQueryString(
      RequestBuilder request, ImmutableMap<String, String> query) {
    String queryString =
        query.entrySet().stream()
            .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining("&"));
    return request.uri(request.uri() + "?" + queryString);
  }
}
