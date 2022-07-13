package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.WithMockedProfiles;
import java.util.Locale;
import java.util.stream.Collectors;
import models.Applicant;
import models.Program;
import models.StoredFile;
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
  private Program program;
  private Applicant applicant;

  @Before
  public void setUpWithFreshApplicant() {
    resetDatabase();

    subject = instanceOf(ApplicantProgramBlocksController.class);
    program =
        ProgramBuilder.newDraftProgram()
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
        fakeRequest(routes.ApplicantProgramBlocksController.edit(badApplicantId, program.id, "1"))
            .build();

    Result result =
        subject.edit(request, badApplicantId, program.id, "1").toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void edit_toAProgramThatDoesNotExist_returns404() {
    Request request =
        addCSRFToken(
                fakeRequest(
                    routes.ApplicantProgramBlocksController.edit(
                        applicant.id, program.id + 1000, "1")))
            .build();

    Result result =
        subject.edit(request, applicant.id, program.id + 1000, "1").toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_toAnExistingBlock_rendersTheBlock() {
    Request request =
        addCSRFToken(
                fakeRequest(
                    routes.ApplicantProgramBlocksController.edit(applicant.id, program.id, "1")))
            .build();

    Result result =
        subject.edit(request, applicant.id, program.id, "1").toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void edit_toABlockThatDoesNotExist_returns404() {
    Request request =
        fakeRequest(routes.ApplicantProgramBlocksController.edit(applicant.id, program.id, "9999"))
            .build();

    Result result =
        subject.edit(request, applicant.id, program.id, "9999").toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_withMessages_returnsCorrectButtonText() {
    Request request =
        addCSRFToken(
                fakeRequest(
                        routes.ApplicantProgramBlocksController.edit(applicant.id, program.id, "1"))
                    .langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi()))
            .build();

    Result result =
        subject.edit(request, applicant.id, program.id, "1").toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Siguiente");
  }

  @Test
  public void previous_toAnExistingBlock_rendersTheBlock() {
    Request request =
        addCSRFToken(
                fakeRequest(
                    routes.ApplicantProgramBlocksController.previous(
                        applicant.id, program.id, 0, true)))
            .build();

    Result result =
        subject.previous(request, applicant.id, program.id, 0, true).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void update_invalidApplicant_returnsUnauthorized() {
    long badApplicantId = applicant.id + 1000;
    Request request =
        fakeRequest(
                routes.ApplicantProgramBlocksController.update(
                    badApplicantId, program.id, /* blockId = */ "1", /* inReview = */ false))
            .build();

    Result result =
        subject
            .update(
                request, badApplicantId, program.id, /* blockId = */ "1", /* inReview = */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void update_invalidProgram_returnsBadRequest() {
    long badProgramId = program.id + 1000;
    Request request =
        fakeRequest(
                routes.ApplicantProgramBlocksController.update(
                    applicant.id, badProgramId, /* blockId = */ "1", /* inReview = */ false))
            .build();

    Result result =
        subject
            .update(
                request, applicant.id, badProgramId, /* blockId = */ "1", /* inReview = */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_invalidBlock_returnsBadRequest() {
    String badBlockId = "1000";
    Request request =
        fakeRequest(
                routes.ApplicantProgramBlocksController.update(
                    applicant.id, program.id, badBlockId, /* inReview = */ false))
            .build();

    Result result =
        subject
            .update(request, applicant.id, program.id, badBlockId, /* inReview = */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_invalidPathsInRequest_returnsBadRequest() {
    Request request =
        fakeRequest(
                routes.ApplicantProgramBlocksController.update(
                    applicant.id, program.id, /* blockId = */ "1", /* inReview = */ false))
            .bodyForm(ImmutableMap.of("fake.path", "value"))
            .build();

    Result result =
        subject
            .update(request, applicant.id, program.id, /* blockId = */ "1", /* inReview = */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_reservedPathsInRequest_returnsBadRequest() {
    String reservedPath = Path.create("metadata").join(Scalar.PROGRAM_UPDATED_IN).toString();
    Request request =
        fakeRequest(
                routes.ApplicantProgramBlocksController.update(
                    applicant.id, program.id, /* blockId = */ "1", /* inReview = */ false))
            .bodyForm(ImmutableMap.of(reservedPath, "value"))
            .build();

    Result result =
        subject
            .update(request, applicant.id, program.id, /* blockId = */ "1", /* inReview = */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_withValidationErrors_isOK() {
    Request request =
        addCSRFToken(
                fakeRequest(
                        routes.ApplicantProgramBlocksController.update(
                            applicant.id, program.id, /* blockId = */ "1", /* inReview = */ false))
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
            .update(request, applicant.id, program.id, /* blockId = */ "1", /* inReview = */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("FirstName");
    assertThat(contentAsString(result)).contains("Please enter your last name.");
  }

  @Test
  public void update_withNextBlock_redirectsToEdit() {
    program =
        ProgramBuilder.newDraftProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().applicantName())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().applicantAddress())
            .build();
    Request request =
        fakeRequest(
                routes.ApplicantProgramBlocksController.update(
                    applicant.id, program.id, /* blockId = */ "1", /* inReview = */ false))
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "FirstName",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    "LastName"))
            .build();

    Result result =
        subject
            .update(request, applicant.id, program.id, /* blockId = */ "1", /* inReview = */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String nextBlockEditRoute =
        routes.ApplicantProgramBlocksController.edit(applicant.id, program.id, /* blockId = */ "2")
            .url();
    assertThat(result.redirectLocation()).hasValue(nextBlockEditRoute);
  }

  @Test
  public void update_completedProgram_redirectsToReviewPage() {
    program =
        ProgramBuilder.newDraftProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();

    Request request =
        fakeRequest(
                routes.ApplicantProgramBlocksController.update(
                    applicant.id, program.id, /* blockId = */ "1", /* inReview = */ false))
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "FirstName",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    "LastName"))
            .build();

    Result result =
        subject
            .update(request, applicant.id, program.id, /* blockId = */ "1", /* inReview = */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    String reviewRoute =
        routes.ApplicantProgramReviewController.review(applicant.id, program.id).url();

    assertThat(result.redirectLocation()).hasValue(reviewRoute);
  }

  @Test
  public void updateFile_invalidApplicant_returnsUnauthorized() {
    long badApplicantId = applicant.id + 1000;
    RequestBuilder request =
        fakeRequest(
            routes.ApplicantProgramBlocksController.updateFile(
                badApplicantId, program.id, /* blockId = */ "2", /* inReview = */ false));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFile(
                request.build(),
                badApplicantId,
                program.id,
                /* blockId = */ "2",
                /* inReview = */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void updateFile_invalidProgram_returnsBadRequest() {
    long badProgramId = program.id + 1000;
    RequestBuilder request =
        fakeRequest(
            routes.ApplicantProgramBlocksController.updateFile(
                applicant.id, badProgramId, /* blockId = */ "2", /* inReview = */ false));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFile(
                request.build(),
                applicant.id,
                badProgramId,
                /* blockId = */ "2",
                /* inReview = */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void updateFile_invalidBlock_returnsBadRequest() {
    String badBlockId = "1000";
    RequestBuilder request =
        fakeRequest(
            routes.ApplicantProgramBlocksController.updateFile(
                applicant.id, program.id, badBlockId, /* inReview = */ false));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFile(
                request.build(), applicant.id, program.id, badBlockId, /* inReview = */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void updateFile_notFileUploadBlock_returnsBadRequest() {
    String badBlockId = "1";
    RequestBuilder request =
        fakeRequest(
            routes.ApplicantProgramBlocksController.updateFile(
                applicant.id, program.id, badBlockId, /* inReview = */ false));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFile(
                request.build(), applicant.id, program.id, badBlockId, /* inReview = */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void updateFile_missingFileKeyAndBucket_returnsBadRequest() {
    RequestBuilder request =
        fakeRequest(
            routes.ApplicantProgramBlocksController.updateFile(
                applicant.id, program.id, /* blockId = */ "2", /* inReview = */ false));

    Result result =
        subject
            .updateFile(
                request.build(),
                applicant.id,
                program.id,
                /* blockId = */ "2",
                /* inReview = */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void updateFile_withNextBlock_redirectsToEdit() {
    program =
        ProgramBuilder.newDraftProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().applicantFile())
            .withBlock("block 2")
            .withRequiredQuestion(testQuestionBank().applicantAddress())
            .build();
    RequestBuilder request =
        fakeRequest(
            routes.ApplicantProgramBlocksController.updateFile(
                applicant.id, program.id, /* blockId = */ "1", /* inReview = */ false));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFile(
                request.build(),
                applicant.id,
                program.id,
                /* blockId = */ "1",
                /* inReview = */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String nextBlockEditRoute =
        routes.ApplicantProgramBlocksController.edit(applicant.id, program.id, /* blockId = */ "2")
            .url();
    assertThat(result.redirectLocation()).hasValue(nextBlockEditRoute);
  }

  @Test
  public void updateFile_completedProgram_redirectsToReviewPage() {
    program =
        ProgramBuilder.newDraftProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().applicantFile())
            .build();

    RequestBuilder request =
        fakeRequest(
            routes.ApplicantProgramBlocksController.updateFile(
                applicant.id, program.id, /* blockId = */ "1", /* inReview = */ false));
    addQueryString(request, ImmutableMap.of("key", "fake-key", "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFile(
                request.build(),
                applicant.id,
                program.id,
                /* blockId = */ "1",
                /* inReview = */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    String reviewRoute =
        routes.ApplicantProgramReviewController.review(applicant.id, program.id).url();

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
        ProgramBuilder.newDraftProgram()
            .withBlock("block 1")
            .withRequiredQuestion(testQuestionBank().applicantFile())
            .build();

    var fileKey = "fake-key";
    var storedFile = new StoredFile();
    storedFile.setName(fileKey);
    storedFile.save();

    RequestBuilder request =
        fakeRequest(
            routes.ApplicantProgramBlocksController.updateFile(
                applicant.id, program.id, /* blockId = */ "1", /* inReview = */ false));
    addQueryString(request, ImmutableMap.of("key", fileKey, "bucket", "fake-bucket"));

    Result result =
        subject
            .updateFile(
                request.build(),
                applicant.id,
                program.id,
                /* blockId = */ "1",
                /* inReview = */ false)
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
