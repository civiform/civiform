package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Applicant;
import play.libs.concurrent.HttpExecutionContext;
import repository.ApplicantRepository;
import services.ErrorAnd;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;

public class ApplicantServiceImpl implements ApplicantService {

  private final ApplicantRepository applicantRepository;
  private final ProgramService programService;
  private final QuestionService questionService;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public ApplicantServiceImpl(ApplicantRepository applicantRepository,
      QuestionService questionService, ProgramService programService,
      HttpExecutionContext httpExecutionContext) {
    this.applicantRepository = checkNotNull(applicantRepository);
    this.questionService = checkNotNull(questionService);
    this.programService = checkNotNull(programService);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
  }

  @Override
  public CompletionStage<ErrorAnd<ReadOnlyApplicantProgramService, UpdateError>> update(
      long applicantId, long programId, ImmutableSet<Update> updates) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletionStage<Applicant> createApplicant(long userId) {
    Applicant applicant = new Applicant();
    return applicantRepository.insertApplicant(applicant).thenApply((unused) -> applicant);
  }

  @Override
  public CompletionStage<ReadOnlyApplicantProgramService> getReadOnlyApplicantProgramService(
      long applicantId, long programId) {
    CompletableFuture<Optional<Applicant>> applicantCompletableFuture = applicantRepository
        .lookupApplicant(applicantId).toCompletableFuture();
    CompletableFuture<Optional<ProgramDefinition>> programDefinitionCompletableFuture = programService
        .getProgramDefinitionAsync(
            programId
        ).toCompletableFuture();
    CompletableFuture<ReadOnlyQuestionService> readOnlyQuestionServiceCompletableFuture = questionService
        .getReadOnlyQuestionService().toCompletableFuture();

    return CompletableFuture.allOf(applicantCompletableFuture, programDefinitionCompletableFuture,
        readOnlyQuestionServiceCompletableFuture)
        .thenApplyAsync((v) -> {
          Applicant applicant = applicantCompletableFuture.join().get();
          ProgramDefinition programDefinition = programDefinitionCompletableFuture.join().get();
          ReadOnlyQuestionService readOnlyQuestionService = readOnlyQuestionServiceCompletableFuture
              .join();

          return new ReadOnlyApplicantProgramServiceImpl(applicant.getApplicantData(),
              programDefinition,
              readOnlyQuestionService);
        }, httpExecutionContext.current());
  }
}
