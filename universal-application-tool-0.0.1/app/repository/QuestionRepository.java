package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.LifecycleStage;
import models.Program;
import models.Question;
import play.db.ebean.EbeanConfig;
import services.Path;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.UnsupportedQuestionTypeException;

public class QuestionRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;
  private final ProgramRepository programRepository;

  @Inject
  public QuestionRepository(
      EbeanConfig ebeanConfig,
      DatabaseExecutionContext executionContext,
      ProgramRepository programRepository) {
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.executionContext = checkNotNull(executionContext);
    this.programRepository = checkNotNull(programRepository);
  }

  public CompletionStage<Set<Question>> listQuestions() {
    return supplyAsync(() -> ebeanServer.find(Question.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<Question>> lookupQuestion(long id) {
    return supplyAsync(
        () -> ebeanServer.find(Question.class).setId(id).findOneOrEmpty(), executionContext);
  }

  /**
   * Set this question to ACTIVE state, and set any existing ACTIVE question with the same name to
   * the OBSOLETE state.
   */
  public void setQuestionLive(long questionId) {
    Question published = ebeanServer.find(Question.class, questionId);
    Optional<Question> existingMaybe =
        findActiveVersionOfQuestion(questionId, published.getQuestionDefinition().getName());
    if (existingMaybe.isPresent()) {
      Question existing = existingMaybe.get();
      existing.setLifecycleStage(LifecycleStage.OBSOLETE);
      existing.save();
    }
    published.setLifecycleStage(LifecycleStage.ACTIVE);
    published.save();
  }

  /** Find an ACTIVE version of the question specified, other than the given ID. */
  public Optional<Question> findActiveVersionOfQuestion(long questionId, String questionName) {
    return ebeanServer
        .find(Question.class)
        .where()
        .eq("lifecycle_stage", LifecycleStage.ACTIVE.getValue())
        .eq("name", questionName)
        .ne("id", questionId)
        .findOneOrEmpty();
  }

  /**
   * Find and update the draft of the question with this name, if one already exists. Create a new
   * draft if there isn't one.
   */
  public Question updateOrCreateDraft(QuestionDefinition definition) {
    try {
      Transaction transaction = ebeanServer.beginTransaction();
      Optional<Question> draftMaybe =
          ebeanServer
              .find(Question.class)
              .usingTransaction(transaction)
              .where()
              .eq("name", definition.getName())
              .eq("lifecycle_stage", LifecycleStage.DRAFT.getValue())
              .findOneOrEmpty();
      if (draftMaybe.isPresent()) {
        definition = new QuestionDefinitionBuilder(definition).setId(draftMaybe.get().id).build();
        Question updatedDraft = new Question(definition);
        updatedDraft.setLifecycleStage(LifecycleStage.DRAFT);
        updatedDraft.save();
        return updatedDraft;
      }

      // Next set the version of the new question.
      int existingQuestionCount =
          ebeanServer
              .find(Question.class)
              .usingTransaction(transaction)
              .where()
              .eq("name", definition.getName())
              .findCount();
      Question newDraft =
          new Question(
              new QuestionDefinitionBuilder(definition)
                  .setId(null)
                  .setVersion(existingQuestionCount + 1L)
                  .build());
      newDraft.setLifecycleStage(LifecycleStage.DRAFT);
      newDraft.id = null;
      ebeanServer.insert(newDraft, transaction);

      // Next, update all existing draft programs so that they don't have old versions of the
      // question anymore.  We have an invariant that draft programs always have the most up-to-date
      // version of a question.
      for (Program draftProgram :
          ebeanServer
              .find(Program.class)
              .where()
              .eq("lifecycle_stage", LifecycleStage.DRAFT.getValue())
              .findList()) {
        programRepository.updateQuestionVersions(draftProgram, transaction);
      }
      ebeanServer.commitTransaction();
      newDraft.refresh();
      return newDraft;
    } catch (UnsupportedQuestionTypeException e) {
      // The question already exists - it should not be unsupported.  Wrap with runtime exception
      // so the crash is maintained but callers do not have to deal with this.
      throw new RuntimeException(e);
    } finally {
      ebeanServer.endTransaction();
    }
  }

  static class PathConflictDetector {
    private Optional<Question> conflictedQuestion = Optional.empty();
    private final String newPath;

    PathConflictDetector(Path newPath) {
      this.newPath = checkNotNull(newPath).path();
    }

    Optional<Question> getConflictedQuestion() {
      return conflictedQuestion;
    }

    boolean checkConflict(Question question) {
      boolean proceed = true;
      if (pathConflicts(question.getPath(), newPath)) {
        conflictedQuestion = Optional.of(question);
        proceed = false;
      }
      return proceed;
    }

    static boolean pathConflicts(String path, String otherPath) {
      path = path.toLowerCase() + ".";
      otherPath = otherPath.toLowerCase() + ".";
      return path.startsWith(otherPath) || otherPath.startsWith(path);
    }
  }

  public CompletionStage<Optional<Question>> findConflictingQuestion(Path newPath) {
    return supplyAsync(
        () -> {
          PathConflictDetector detector = new PathConflictDetector(newPath);
          ebeanServer.find(Question.class).findEachWhile(detector::checkConflict);
          return detector.getConflictedQuestion();
        },
        executionContext);
  }

  public CompletionStage<Optional<Question>> lookupQuestionByPath(String path) {
    return supplyAsync(
        () -> ebeanServer.find(Question.class).where().eq("path", path).findOneOrEmpty(),
        executionContext);
  }

  public CompletionStage<Question> insertQuestion(Question question) {
    return supplyAsync(
        () -> {
          ebeanServer.insert(question);
          return question;
        },
        executionContext);
  }

  public Question insertQuestionSync(Question question) {
    ebeanServer.insert(question);
    return question;
  }

  public CompletionStage<Question> updateQuestion(Question question) {
    return supplyAsync(
        () -> {
          ebeanServer.update(question);
          return question;
        },
        executionContext);
  }

  public Question updateQuestionSync(Question question) {
    ebeanServer.update(question);
    return question;
  }
}
