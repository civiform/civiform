package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import java.util.Optional;
import javax.inject.Inject;
import models.LifecycleStage;
import models.Program;
import models.Question;
import models.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.db.ebean.EbeanConfig;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;

/** A repository object for dealing with versioning of questions and programs. */
public class VersionRepository {

  private final EbeanServer ebeanServer;
  private final Logger LOG = LoggerFactory.getLogger(VersionRepository.class);
  private final ProgramRepository programRepository;

  @Inject
  public VersionRepository(EbeanConfig ebeanConfig, ProgramRepository programRepository) {
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.programRepository = checkNotNull(programRepository);
  }

  /**
   * Publish a new version of all programs and all questions. All DRAFT programs will become ACTIVE,
   * and all ACTIVE programs without a draft will be copied to the next version.
   */
  public void publishNewSynchronizedVersion() {
    try {
      ebeanServer.beginTransaction();
      ebeanServer.commitTransaction();
    } finally {
      ebeanServer.endTransaction();
    }
  }

  /** Get the current draft version. Creates it if one does not exist. */
  public Version getDraftVersion() {
    Optional<Version> version =
        ebeanServer
            .find(Version.class)
            .where()
            .eq("lifecycle_stage", LifecycleStage.DRAFT)
            .findOneOrEmpty();
    if (version.isPresent()) {
      return version.get();
    } else {
      Version newDraftVersion = new Version(LifecycleStage.DRAFT);
      ebeanServer.insert(newDraftVersion);
      return newDraftVersion;
    }
  }

  public Version getActiveVersion() {
    return ebeanServer
        .find(Version.class)
        .where()
        .eq("lifecycle_stage", LifecycleStage.ACTIVE)
        .findOne();
  }

  private Optional<Question> getLatestVersionOfQuestion(long questionId) {
    String questionName =
        ebeanServer.find(Question.class).setId(questionId).select("name").findSingleAttribute();
    Optional<Question> draftQuestion =
        getDraftVersion().getQuestions().stream()
            .filter(question -> question.getQuestionDefinition().getName().equals(questionName))
            .findFirst();
    if (draftQuestion.isPresent()) {
      return draftQuestion;
    }
    return getActiveVersion().getQuestions().stream()
        .filter(question -> question.getQuestionDefinition().getName().equals(questionName))
        .findFirst();
  }

  /**
   * For each question in this program, check whether it is the most up-to-date version of the
   * question which is either DRAFT or ACTIVE. If it is not, update the pointer to the most
   * up-to-date version of the question. This version of the method creates its own internal
   * transaction. This method can only be called on a draft program.
   */
  public void updateQuestionVersions(Program draftProgram) {
    Transaction transaction = ebeanServer.beginTransaction();
    try {
      updateQuestionVersions(draftProgram, transaction);
      ebeanServer.commitTransaction();
    } finally {
      ebeanServer.endTransaction();
      draftProgram.refresh();
    }
  }

  /**
   * For each question in this program, check whether it is the most up-to-date version of the
   * question which is either DRAFT or ACTIVE. If it is not, update the pointer to the most
   * up-to-date version of the question, using the given transaction. This method can only be called
   * on a draft program.
   */
  public void updateQuestionVersions(Program draftProgram, Transaction transaction) {
    Preconditions.checkArgument(isInactive(draftProgram), "input program must not be active.");
    Preconditions.checkArgument(
        isDraft(draftProgram), "input program must be in the current draft version.");
    ProgramDefinition.Builder updatedDefinition =
        draftProgram.getProgramDefinition().toBuilder().setBlockDefinitions(ImmutableList.of());
    for (BlockDefinition block : draftProgram.getProgramDefinition().blockDefinitions()) {
      LOG.trace("Updating block {}.", block.id());
      updatedDefinition.addBlockDefinition(updateQuestionVersions(block));
    }
    draftProgram = new Program(updatedDefinition.build());
    LOG.trace("Submitting update.");
    ebeanServer.update(draftProgram, transaction);
  }

  public boolean isInactive(Question question) {
    return !getActiveVersion().getQuestions().stream()
        .anyMatch(activeQuestion -> activeQuestion.id.equals(question.id));
  }

  public boolean isInactive(Program program) {
    return !getActiveVersion().getPrograms().stream()
        .anyMatch(activeProgram -> activeProgram.id.equals(program.id));
  }

  public boolean isDraft(Question question) {
    return getDraftVersion().getQuestions().stream()
        .anyMatch(activeProgram -> activeProgram.id.equals(question.id));
  }

  public boolean isDraft(Program program) {
    return getDraftVersion().getPrograms().stream()
        .anyMatch(activeProgram -> activeProgram.id.equals(program.id));
  }

  private BlockDefinition updateQuestionVersions(BlockDefinition block) {
    BlockDefinition.Builder updatedBlock =
        block.toBuilder().setProgramQuestionDefinitions(ImmutableList.of());
    for (ProgramQuestionDefinition question : block.programQuestionDefinitions()) {
      Optional<Question> updatedQuestion = getLatestVersionOfQuestion(question.id());
      LOG.trace(
          "Updating question ID {} to new ID {}.", question.id(), updatedQuestion.orElseThrow().id);
      updatedBlock.addQuestion(
          ProgramQuestionDefinition.create(updatedQuestion.orElseThrow().getQuestionDefinition()));
    }
    return updatedBlock.build();
  }

  public void updateProgramsForNewDraftQuestion(long oldId, Transaction transaction) {
    getDraftVersion().getPrograms().stream()
        .filter(program -> program.getProgramDefinition().hasQuestion(oldId))
        .forEach(program -> updateQuestionVersions(program, transaction));

    getActiveVersion().getPrograms().stream()
        .filter(program -> program.getProgramDefinition().hasQuestion(oldId))
        .filter(
            program ->
                getDraftVersion()
                    .getProgramByName(program.getProgramDefinition().adminName())
                    .isEmpty())
        .forEach(program -> programRepository.createOrUpdateDraft(program));
  }
}
