package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import io.ebean.TxScope;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.persistence.NonUniqueResultException;
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
      Version draft = getDraftVersion();
      Version active = getActiveVersion();
      Preconditions.checkState(
          draft.getPrograms().size() > 0, "Must have at least 1 program in the draft version.");
      active.getPrograms().stream()
          .filter(
              activeProgram ->
                  draft.getPrograms().stream()
                      .noneMatch(
                          draftProgram ->
                              activeProgram
                                  .getProgramDefinition()
                                  .adminName()
                                  .equals(draftProgram.getProgramDefinition().adminName())))
          .forEach(
              activeProgramNotInDraft -> {
                activeProgramNotInDraft.addVersion(draft);
                activeProgramNotInDraft.save();
              });
      active.getQuestions().stream()
          .filter(
              activeQuestion ->
                  draft.getQuestions().stream()
                      .noneMatch(
                          draftQuestion ->
                              activeQuestion
                                  .getQuestionDefinition()
                                  .getName()
                                  .equals(draftQuestion.getQuestionDefinition().getName())))
          .forEach(
              activeQuestionNotInDraft -> {
                activeQuestionNotInDraft.addVersion(draft);
                activeQuestionNotInDraft.save();
              });
      active.setLifecycleStage(LifecycleStage.OBSOLETE);
      draft.setLifecycleStage(LifecycleStage.ACTIVE);
      active.save();
      draft.save();
      draft.refresh();
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
      try {
        // Suspends any existing transaction if one exists.
        ebeanServer.beginTransaction(TxScope.requiresNew());
        Version newDraftVersion = new Version(LifecycleStage.DRAFT);
        ebeanServer.insert(newDraftVersion);
        ebeanServer
            .find(Version.class)
            .forUpdate()
            .where()
            .eq("lifecycle_stage", LifecycleStage.DRAFT)
            .findOne();
        ebeanServer.commitTransaction();
        return newDraftVersion;
      } catch (NonUniqueResultException e) {
        ebeanServer.endTransaction();
        return getDraftVersion();
      }
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
   * up-to-date version of the question, using the given transaction. This method can only be called
   * on a draft program.
   */
  public void updateQuestionVersions(Program draftProgram) {
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
    ebeanServer.update(draftProgram);
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
        .anyMatch(draftQuestion -> draftQuestion.id.equals(question.id));
  }

  public boolean isDraft(Program program) {
    return getDraftVersion().getPrograms().stream()
        .anyMatch(draftProgram -> draftProgram.id.equals(program.id));
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

  public void updateProgramsForNewDraftQuestion(long oldId) {
    getDraftVersion().getPrograms().stream()
        .filter(program -> program.getProgramDefinition().hasQuestion(oldId))
        .forEach(program -> updateQuestionVersions(program));

    getActiveVersion().getPrograms().stream()
        .filter(program -> program.getProgramDefinition().hasQuestion(oldId))
        .filter(
            program ->
                getDraftVersion()
                    .getProgramByName(program.getProgramDefinition().adminName())
                    .isEmpty())
        .forEach(program -> programRepository.createOrUpdateDraft(program));
  }

  public List<Version> listAllVersions() {
    return ebeanServer.find(Version.class).findList();
  }

  public void setLive(long versionId) {
    try {
      ebeanServer.beginTransaction();
      Version draftVersion = getDraftVersion();
      Version activeVersion = getActiveVersion();
      Version newActiveVersion = ebeanServer.find(Version.class).setId(versionId).findOne();
      newActiveVersion.setLifecycleStage(LifecycleStage.ACTIVE);
      newActiveVersion.save();
      activeVersion.setLifecycleStage(LifecycleStage.OBSOLETE);
      activeVersion.save();
      draftVersion.setLifecycleStage(LifecycleStage.DELETED);
      draftVersion.save();
      ebeanServer.commitTransaction();
    } finally {
      ebeanServer.endTransaction();
    }
  }
}
