package controllers.applicant;

import static controllers.applicant.ApplicantProgramBlocksController.ADDRESS_JSON_SESSION_KEY;
import static controllers.applicant.ApplicantRequestedAction.NEXT_BLOCK;
import static controllers.applicant.ApplicantRequestedAction.PREVIOUS_BLOCK;
import static controllers.applicant.ApplicantRequestedAction.REVIEW_PAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import controllers.geo.AddressSuggestionJsonSerializer;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.AccountModel;
import models.ApplicantModel;
import models.ProgramModel;
import models.StoredFileModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import repository.StoredFileRepository;
import services.Address;
import services.Path;
import services.applicant.question.Scalar;
import services.geo.AddressLocation;
import services.geo.AddressSuggestion;
import support.ProgramBuilder;
import views.applicant.AddressCorrectionBlockView;

@RunWith(JUnitParamsRunner.class)
public class ApplicantProgramBlocksControllerTest extends WithMockedProfiles {
  private static final String SUGGESTED_ADDRESS = "456 Suggested Ave, Seattle, Washington, 99999";
  private static final String SUGGESTED_ADDRESS_STREET = "456 Suggested Ave";
  private ApplicantProgramBlocksController subject;
  private AddressSuggestionJsonSerializer addressSuggestionJsonSerializer;
  private ProgramModel program;
  private ApplicantModel applicant;

  @Before
  public void setUpWithFreshApplicant() {
    resetDatabase();

    subject = instanceOf(ApplicantProgramBlocksController.class);
    addressSuggestionJsonSerializer = instanceOf(AddressSuggestionJsonSerializer.class);
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
                        applicant.id, draftProgram.id, 0, true)))
            .build();
    Result result =
        subject
            .previousWithApplicantId(request, applicant.id, draftProgram.id, 0, true)
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
                        applicant.id, draftProgram.id, 0, true)))
            .build();
    Result result =
        subject
            .previousWithApplicantId(request, applicant.id, draftProgram.id, 0, true)
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
                        applicant.id, obsoleteProgram.id, 0, true)))
            .build();
    Result result =
        subject
            .previousWithApplicantId(request, applicant.id, obsoleteProgram.id, 0, true)
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
                        draftProgram.id,
                        /* blockId= */ "1",
                        /* inReview= */ false,
                        new ApplicantRequestedActionWrapper())))
            .build();
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
                        draftProgram.id,
                        /* blockId= */ "1",
                        /* inReview= */ false,
                        new ApplicantRequestedActionWrapper())))
            .build();
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
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.updateWithApplicantId(
                        applicant.id,
                        obsoleteProgram.id,
                        /* blockId= */ "1",
                        /* inReview= */ false,
                        new ApplicantRequestedActionWrapper())))
            .build();
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
  public void update_noAnswerToRequiredQuestion_requestedActionNext_staysOnBlockAndShowsErrors() {
    ApplicantRequestedActionWrapper requestedAction =
        new ApplicantRequestedActionWrapper(NEXT_BLOCK);

    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
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
                            requestedAction))
                    .bodyForm(
                        ImmutableMap.of(
                            Path.create("applicant.applicant_name")
                                .join(Scalar.FIRST_NAME)
                                .toString(),
                            "",
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
                requestedAction)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Please enter your first name.");
    assertThat(contentAsString(result)).contains("Please enter your last name.");
  }

  // See issue #6987.
  @Test
  public void update_noAnswerToRequiredQuestion_requestedActionPrevious_goesToPrevious() {
    ApplicantRequestedActionWrapper requestedAction =
        new ApplicantRequestedActionWrapper(PREVIOUS_BLOCK);

    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().applicantAddress())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();
    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.updateWithApplicantId(
                            applicant.id,
                            program.id,
                            /* blockId= */ "2",
                            /* inReview= */ false,
                            requestedAction))
                    .bodyForm(
                        ImmutableMap.of(
                            Path.create("applicant.applicant_name")
                                .join(Scalar.FIRST_NAME)
                                .toString(),
                            "",
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
                /* blockId= */ "2",
                /* inReview= */ false,
                requestedAction)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String previousRoute =
        routes.ApplicantProgramBlocksController.previous(
                program.id, /* previousBlockIndex= */ 0, /* inReview= */ false)
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
                            requestedAction))
                    .bodyForm(
                        ImmutableMap.of(
                            Path.create("applicant.applicant_name")
                                .join(Scalar.FIRST_NAME)
                                .toString(),
                            "",
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
                requestedAction)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String reviewRoute = routes.ApplicantProgramReviewController.review(program.id).url();
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
            .withOptionalQuestion(testQuestionBank().applicantName())
            .build();
    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.updateWithApplicantId(
                            applicant.id,
                            program.id,
                            /* blockId= */ "1",
                            /* inReview= */ false,
                            requestedAction))
                    .bodyForm(
                        ImmutableMap.of(
                            Path.create("applicant.applicant_name")
                                .join(Scalar.FIRST_NAME)
                                .toString(),
                            "",
                            Path.create("applicant.applicant_name")
                                .join(Scalar.LAST_NAME)
                                .toString(),
                            "")))
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
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();
    Request requestWithAnswer =
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.updateWithApplicantId(
                            applicant.id,
                            program.id,
                            /* blockId= */ "1",
                            /* inReview= */ false,
                            requestedAction))
                    .bodyForm(
                        ImmutableMap.of(
                            Path.create("applicant.applicant_name")
                                .join(Scalar.FIRST_NAME)
                                .toString(),
                            "InitialFirstName",
                            Path.create("applicant.applicant_name")
                                .join(Scalar.LAST_NAME)
                                .toString(),
                            "InitialLastName")))
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

    // Then, try to delete the answer
    Request requestWithoutAnswer =
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.updateWithApplicantId(
                            applicant.id,
                            program.id,
                            /* blockId= */ "1",
                            /* inReview= */ false,
                            requestedAction))
                    .bodyForm(
                        ImmutableMap.of(
                            Path.create("applicant.applicant_name")
                                .join(Scalar.FIRST_NAME)
                                .toString(),
                            "",
                            Path.create("applicant.applicant_name")
                                .join(Scalar.LAST_NAME)
                                .toString(),
                            "")))
            .build();

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
  public void update_deletePreviousAnswerToOptionalQuestion_saves(String action) {
    ApplicantRequestedActionWrapper requestedAction =
        new ApplicantRequestedActionWrapper(ApplicantRequestedAction.valueOf(action));

    // First, provide an answer
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withOptionalQuestion(testQuestionBank().applicantName())
            .build();
    Request requestWithAnswer =
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.updateWithApplicantId(
                            applicant.id,
                            program.id,
                            /* blockId= */ "1",
                            /* inReview= */ false,
                            requestedAction))
                    .bodyForm(
                        ImmutableMap.of(
                            Path.create("applicant.applicant_name")
                                .join(Scalar.FIRST_NAME)
                                .toString(),
                            "InitialFirstName",
                            Path.create("applicant.applicant_name")
                                .join(Scalar.LAST_NAME)
                                .toString(),
                            "InitialLastName")))
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
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.updateWithApplicantId(
                            applicant.id,
                            program.id,
                            /* blockId= */ "1",
                            /* inReview= */ false,
                            requestedAction))
                    .bodyForm(
                        ImmutableMap.of(
                            Path.create("applicant.applicant_name")
                                .join(Scalar.FIRST_NAME)
                                .toString(),
                            "",
                            Path.create("applicant.applicant_name")
                                .join(Scalar.LAST_NAME)
                                .toString(),
                            "")))
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
                    new ApplicantRequestedActionWrapper(NEXT_BLOCK)))
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
                badApplicantId,
                program.id,
                /* blockId= */ "2",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK)));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                badApplicantId,
                program.id,
                /* blockId= */ "2",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK))
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void updateFile_applicantAccessToDraftProgram_returnsUnauthorized() {
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantFile())
            .build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
                        applicant.id,
                        draftProgram.id,
                        /* blockId= */ "1",
                        /* inReview= */ false,
                        new ApplicantRequestedActionWrapper(NEXT_BLOCK))))
            .build();

    Result result =
        subject
            .updateFileWithApplicantId(
                request,
                applicant.id,
                draftProgram.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK))
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void updateFile_civiformAdminAccessToDraftProgram_works() {
    AccountModel adminAccount = createGlobalAdminWithMockedProfile();
    applicant = adminAccount.newestApplicant().orElseThrow();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantFile())
            .build();

    RequestBuilder request =
        addCSRFToken(
            requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
                    applicant.id,
                    draftProgram.id,
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper(NEXT_BLOCK))));

    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                applicant.id,
                draftProgram.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK))
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void updateFile_obsoleteProgram_works() {
    ProgramModel obsoleteProgram =
        ProgramBuilder.newObsoleteProgram("program")
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantFile())
            .build();

    RequestBuilder request =
        addCSRFToken(
            requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
                    applicant.id,
                    obsoleteProgram.id,
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper(NEXT_BLOCK))));

    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                applicant.id,
                obsoleteProgram.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK))
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void updateFile_invalidProgram_returnsBadRequest() {
    long badProgramId = program.id + 1000;
    RequestBuilder request =
        requestBuilderWithSettings(
            routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
                applicant.id,
                badProgramId,
                /* blockId= */ "2",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK)));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                applicant.id,
                badProgramId,
                /* blockId= */ "2",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK))
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
                applicant.id,
                program.id,
                badBlockId,
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK)));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                applicant.id,
                program.id,
                badBlockId,
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK))
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
                applicant.id,
                program.id,
                badBlockId,
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK)));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                applicant.id,
                program.id,
                badBlockId,
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK))
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void updateFile_missingFileKeyAndBucket_returnsBadRequest() {
    RequestBuilder request =
        requestBuilderWithSettings(
            routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
                applicant.id,
                program.id,
                /* blockId= */ "2",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK)));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                applicant.id,
                program.id,
                /* blockId= */ "2",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK))
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void updateFile_requestedActionNext_redirectsToNextAndStoresFileKey() {
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
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK)));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
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
                program.id, /* blockId= */ "2", /* questionName= */ Optional.empty())
            .url();
    assertThat(result.redirectLocation()).hasValue(nextBlockEditRoute);

    applicant.refresh();
    String applicantData = applicant.getApplicantData().asJsonString();
    assertThat(applicantData).contains("fake-key");
  }

  @Test
  public void updateFile_requestedActionPrevious_redirectsToPreviousAndStoresFileKey() {
    program =
        ProgramBuilder.newActiveProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().applicantAddress())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().applicantFile())
            .build();
    RequestBuilder request =
        requestBuilderWithSettings(
            routes.ApplicantProgramBlocksController.updateFileWithApplicantId(
                applicant.id,
                program.id,
                /* blockId= */ "2",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(PREVIOUS_BLOCK)));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                applicant.id,
                program.id,
                /* blockId= */ "2",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(PREVIOUS_BLOCK))
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String previousRoute =
        routes.ApplicantProgramBlocksController.previous(
                // The 2nd block was filled in, which is index 1. So, the previous block would be
                // index 0.
                program.id, /* previousBlockIndex= */ 0, /* inReview= */ false)
            .url();
    assertThat(result.redirectLocation()).hasValue(previousRoute);

    applicant.refresh();
    String applicantData = applicant.getApplicantData().asJsonString();
    assertThat(applicantData).contains("fake-key");
  }

  @Test
  public void updateFile_requestedActionReview_redirectsToReviewAndStoresFileKey() {
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
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(REVIEW_PAGE)));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(REVIEW_PAGE))
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String reviewRoute = routes.ApplicantProgramReviewController.review(program.id).url();
    assertThat(result.redirectLocation()).hasValue(reviewRoute);

    applicant.refresh();
    String applicantData = applicant.getApplicantData().asJsonString();
    assertThat(applicantData).contains("fake-key");
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
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK)));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(NEXT_BLOCK))
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    // The requested action is NEXT_BLOCK, but since file upload is the only question they should be
    // redirected to the review page.
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
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(REVIEW_PAGE)));
    addQueryString(request, ImmutableMap.of("key", fileKey, "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFileWithApplicantId(
                request.build(),
                applicant.id,
                program.id,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper(REVIEW_PAGE))
            .toCompletableFuture()
            .join();

    int storedFileCount =
        storedFileRepo.lookupFiles(ImmutableList.of(fileKey)).toCompletableFuture().join().size();
    assertThat(storedFileCount).isEqualTo(1);
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void confirmAddress_invalidApplicant_returnsUnauthorized() {
    long badApplicantId = Long.MAX_VALUE;

    Request request =
        requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.confirmAddressWithApplicantId(
                    badApplicantId,
                    program.id,
                    /* blockId= */ "1",
                    /* inReview= */ false,
                    new ApplicantRequestedActionWrapper()))
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
            .withRequiredCorrectedAddressQuestion(testQuestionBank().applicantAddress())
            .build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.confirmAddressWithApplicantId(
                        applicant.id,
                        program.id,
                        /* blockId= */ "1",
                        /* inReview= */ false,
                        new ApplicantRequestedActionWrapper())))
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
    applicant = adminAccount.newestApplicant().orElseThrow();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredCorrectedAddressQuestion(testQuestionBank().applicantAddress())
            .build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.confirmAddressWithApplicantId(
                        applicant.id,
                        program.id,
                        /* blockId= */ "1",
                        /* inReview= */ false,
                        new ApplicantRequestedActionWrapper())))
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
            .withRequiredCorrectedAddressQuestion(testQuestionBank().applicantAddress())
            .build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.confirmAddressWithApplicantId(
                        applicant.id,
                        program.id,
                        /* blockId= */ "1",
                        /* inReview= */ false,
                        new ApplicantRequestedActionWrapper())))
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
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramBlocksController.confirmAddressWithApplicantId(
                        applicant.id,
                        badProgramId,
                        /* blockId= */ "1",
                        /* inReview= */ false,
                        new ApplicantRequestedActionWrapper())))
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
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.confirmAddressWithApplicantId(
                            applicant.id,
                            program.id,
                            /* blockId= */ "1",
                            /* inReview= */ false,
                            new ApplicantRequestedActionWrapper()))
                    // Don't set the ADDRESS_JSON_SESSION_KEY on the session
                    .bodyForm(
                        ImmutableMap.of(
                            AddressCorrectionBlockView.SELECTED_ADDRESS_NAME,
                            "123 Main St, Boston, Massachusetts, 02111")))
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
            .withRequiredCorrectedAddressQuestion(testQuestionBank().applicantAddress())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().applicantIceCream())
            .build();

    // First, answer the address question
    Request answerAddressQuestionRequest =
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.updateWithApplicantId(
                            applicant.id,
                            program.id,
                            /* blockId= */ "1",
                            /* inReview= */ false,
                            new ApplicantRequestedActionWrapper()))
                    .session("ESRI_ADDRESS_CORRECTION_ENABLED", "true")
                    .bodyForm(
                        ImmutableMap.of(
                            Path.create("applicant.applicant_address")
                                .join(Scalar.STREET)
                                .toString(),
                            "Legit Address",
                            Path.create("applicant.applicant_address")
                                .join(Scalar.LINE2)
                                .toString(),
                            "",
                            Path.create("applicant.applicant_address").join(Scalar.CITY).toString(),
                            "Boston",
                            Path.create("applicant.applicant_address")
                                .join(Scalar.STATE)
                                .toString(),
                            "MA",
                            Path.create("applicant.applicant_address").join(Scalar.ZIP).toString(),
                            "02111")))
            .build();
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
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.confirmAddressWithApplicantId(
                            applicant.id,
                            program.id,
                            /* blockId= */ "1",
                            /* inReview= */ false,
                            new ApplicantRequestedActionWrapper()))
                    .session(ADDRESS_JSON_SESSION_KEY, createAddressSuggestionsJson()))
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
                program.id, /* blockId= */ "2", /* questionName= */ Optional.empty())
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
            .withRequiredCorrectedAddressQuestion(testQuestionBank().applicantAddress())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().applicantIceCream())
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
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.confirmAddressWithApplicantId(
                            applicant.id,
                            program.id,
                            /* blockId= */ "1",
                            /* inReview= */ false,
                            new ApplicantRequestedActionWrapper(NEXT_BLOCK)))
                    .session(ADDRESS_JSON_SESSION_KEY, addressSuggestionString)
                    .bodyForm(
                        ImmutableMap.of(AddressCorrectionBlockView.SELECTED_ADDRESS_NAME, address)))
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
                program.id, /* blockId= */ "2", /* questionName= */ Optional.empty())
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
            .withRequiredCorrectedAddressQuestion(testQuestionBank().applicantAddress())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().applicantIceCream())
            .build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.confirmAddressWithApplicantId(
                            applicant.id,
                            program.id,
                            /* blockId= */ "1",
                            /* inReview= */ false,
                            new ApplicantRequestedActionWrapper(
                                ApplicantRequestedAction.REVIEW_PAGE)))
                    .session(ADDRESS_JSON_SESSION_KEY, createAddressSuggestionsJson())
                    .bodyForm(
                        ImmutableMap.of(
                            AddressCorrectionBlockView.SELECTED_ADDRESS_NAME, SUGGESTED_ADDRESS)))
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
    String reviewRoute = routes.ApplicantProgramReviewController.review(program.id).url();
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
            .withRequiredQuestion(testQuestionBank().applicantEmail())
            .withBlock("block 2")
            .withRequiredCorrectedAddressQuestion(testQuestionBank().applicantAddress())
            .withBlock("block 3")
            .withRequiredQuestion(testQuestionBank().applicantIceCream())
            .build();

    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.confirmAddressWithApplicantId(
                            applicant.id,
                            program.id,
                            /* blockId= */ "2",
                            /* inReview= */ false,
                            new ApplicantRequestedActionWrapper(
                                ApplicantRequestedAction.PREVIOUS_BLOCK)))
                    .session(ADDRESS_JSON_SESSION_KEY, createAddressSuggestionsJson())
                    .bodyForm(
                        ImmutableMap.of(
                            AddressCorrectionBlockView.SELECTED_ADDRESS_NAME, SUGGESTED_ADDRESS)))
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
    String previousBlockEditRoute =
        routes.ApplicantProgramBlocksController.previous(
                // The 2nd block was filled in, which is index 1. So, the previous block would be
                // index 0.
                program.id, /* previousBlockIndex= */ 0, /* inReview= */ false)
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
            .withRequiredCorrectedAddressQuestion(testQuestionBank().applicantAddress())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().applicantIceCream())
            .build();

    // First, answer the address question
    Request answerAddressQuestionRequest =
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.updateWithApplicantId(
                            applicant.id,
                            program.id,
                            /* blockId= */ "1",
                            /* inReview= */ false,
                            new ApplicantRequestedActionWrapper()))
                    .session("ESRI_ADDRESS_CORRECTION_ENABLED", "true")
                    .bodyForm(
                        ImmutableMap.of(
                            Path.create("applicant.applicant_address")
                                .join(Scalar.STREET)
                                .toString(),
                            "Legit Address",
                            Path.create("applicant.applicant_address")
                                .join(Scalar.LINE2)
                                .toString(),
                            "",
                            Path.create("applicant.applicant_address").join(Scalar.CITY).toString(),
                            "Boston",
                            Path.create("applicant.applicant_address")
                                .join(Scalar.STATE)
                                .toString(),
                            "MA",
                            Path.create("applicant.applicant_address").join(Scalar.ZIP).toString(),
                            "02111")))
            .build();
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
        addCSRFToken(
                requestBuilderWithSettings(
                        routes.ApplicantProgramBlocksController.confirmAddressWithApplicantId(
                            applicant.id,
                            program.id,
                            /* blockId= */ "1",
                            /* inReview= */ false,
                            new ApplicantRequestedActionWrapper()))
                    .session(ADDRESS_JSON_SESSION_KEY, createAddressSuggestionsJson())
                    .bodyForm(
                        ImmutableMap.of(
                            AddressCorrectionBlockView.SELECTED_ADDRESS_NAME,
                            AddressCorrectionBlockView.USER_KEEPING_ADDRESS_VALUE)))
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
                program.id, /* blockId= */ "2", /* questionName= */ Optional.empty())
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
}
