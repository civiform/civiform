package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers.Labels;
import com.google.common.collect.ImmutableList;
import forms.ProgramQuestionDefinitionOptionalityForm;
import java.util.Map.Entry;
import java.util.Optional;
import javax.inject.Inject;
import models.Question;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.program.DuplicateProgramQuestionException;
import services.program.IllegalPredicateOrderingException;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinitionNotFoundException;
import services.program.ProgramService;
import services.question.exceptions.QuestionNotFoundException;

/** Controller for admins editing questions on a screen (block) of a program. */
public class AdminProgramBlockQuestionsController extends Controller {

  private final ProgramService programService;
  private final VersionRepository versionRepository;
  private final FormFactory formFactory;

  @Inject
  public AdminProgramBlockQuestionsController(
      ProgramService programService, VersionRepository versionRepository, FormFactory formFactory) {
    this.programService = checkNotNull(programService);
    this.versionRepository = checkNotNull(versionRepository);
    this.formFactory = checkNotNull(formFactory);
  }

  /** POST endpoint for adding one or more questions to a screen. */
  @Secure(authorizers = Labels.CIVIFORM_ADMIN)
  public Result create(Request request, long programId, long blockId) {
    DynamicForm requestData = formFactory.form().bindFromRequest(request);
    ImmutableList<Long> questionIds =
        requestData.rawData().entrySet().stream()
            .filter(formField -> formField.getKey().startsWith("question-"))
            .map(Entry::getValue)
            .map(Long::valueOf)
            .collect(ImmutableList.toImmutableList());

    // The users' browser may be out of date. Find the last revision of each question.
    ImmutableList.Builder<Long> idBuilder = new ImmutableList.Builder<Long>();
    for (Long qId : questionIds) {
      Optional<Question> latestQuestion = versionRepository.getLatestVersionOfQuestion(qId);
      if (latestQuestion.isEmpty()) {
        return notFound(String.format("Question ID %s not found", qId));
      }
      idBuilder.add(latestQuestion.get().id);
    }
    ImmutableList<Long> latestQuestionIds = idBuilder.build();

    try {
      programService.addQuestionsToBlock(programId, blockId, latestQuestionIds);
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(String.format("Block ID %d not found for Program %d", blockId, programId));
    } catch (QuestionNotFoundException e) {
      return notFound(String.format("Question IDs %s not found", latestQuestionIds));
    } catch (DuplicateProgramQuestionException e) {
      return notFound(
          String.format(
              "Some Question IDs %s already exist in Program ID %d", latestQuestionIds, programId));
    }

    return redirect(controllers.admin.routes.AdminProgramBlocksController.edit(programId, blockId));
  }

  /** POST endpoint for removing a question from a screen. */
  @Secure(authorizers = Labels.CIVIFORM_ADMIN)
  public Result destroy(long programId, long blockDefinitionId, long questionDefinitionId) {
    try {
      programService.removeQuestionsFromBlock(
          programId, blockDefinitionId, ImmutableList.of(questionDefinitionId));
    } catch (IllegalPredicateOrderingException e) {
      return redirect(
              controllers.admin.routes.AdminProgramBlocksController.edit(
                  programId, blockDefinitionId))
          .flashing("error", e.getLocalizedMessage());
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    } catch (QuestionNotFoundException e) {
      return notFound(String.format("Question ID %s not found", questionDefinitionId));
    }

    return redirect(
        controllers.admin.routes.AdminProgramBlocksController.edit(programId, blockDefinitionId));
  }

  /** POST endpoint for editing whether or not a question is optional on a screen. */
  @Secure(authorizers = Labels.CIVIFORM_ADMIN)
  public Result setOptional(
      Request request, long programId, long blockDefinitionId, long questionDefinitionId) {
    ProgramQuestionDefinitionOptionalityForm programQuestionDefinitionOptionalityForm =
        formFactory
            .form(ProgramQuestionDefinitionOptionalityForm.class)
            .bindFromRequest(request)
            .get();

    try {
      programService.setProgramQuestionDefinitionOptionality(
          programId,
          blockDefinitionId,
          questionDefinitionId,
          programQuestionDefinitionOptionalityForm.getOptional());
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    } catch (ProgramQuestionDefinitionNotFoundException e) {
      return notFound(
          String.format(
              "Question ID %d not found in Block %d for program %d",
              questionDefinitionId, blockDefinitionId, programId));
    }

    return redirect(
        controllers.admin.routes.AdminProgramBlocksController.edit(programId, blockDefinitionId));
  }
}
