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
import javax.inject.Provider;
import models.LifecycleStage;
import models.Question;
import play.db.ebean.EbeanConfig;
import services.Path;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

public class QuestionRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;
  private final Provider<VersionRepository> versionRepositoryProvider;

  @Inject
  public QuestionRepository(
      EbeanConfig ebeanConfig,
      DatabaseExecutionContext executionContext,
      ProgramRepository programRepository,
      Provider<VersionRepository> versionRepositoryProvider) {
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.executionContext = checkNotNull(executionContext);
    this.versionRepositoryProvider = checkNotNull(versionRepositoryProvider);
  }

  public CompletionStage<Set<Question>> listQuestions() {
    return supplyAsync(() -> ebeanServer.find(Question.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<Question>> lookupQuestion(long id) {
    return supplyAsync(
        () -> ebeanServer.find(Question.class).setId(id).findOneOrEmpty(), executionContext);
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
        Question draft = draftMaybe.get();
        definition =
            new QuestionDefinitionBuilder(definition)
                .setId(draft.id)
                // Overwrite the version placeholder.
                .setVersion(draft.getVersion())
                .build();
        Question updatedDraft = new Question(definition);
        updatedDraft.setLifecycleStage(LifecycleStage.DRAFT);
        ebeanServer.update(updatedDraft, transaction);
        ebeanServer.commitTransaction();
        updatedDraft.refresh();
        return updatedDraft;
      }

      Question newDraft =
          new Question(
              new QuestionDefinitionBuilder(definition)
                  .setId(null)
                  .setVersion(versionRepositoryProvider.get().getNextVersion())
                  .build());
      newDraft.setLifecycleStage(LifecycleStage.DRAFT);
      newDraft.id = null;
      ebeanServer.insert(newDraft, transaction);

      versionRepositoryProvider
          .get()
          .updateProgramsForNewDraftQuestion(newDraft, definition.getId(), transaction);

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

  /**
   * Maybe find a {@link Question} whose path conflicts with {@link QuestionDefinition}.
   *
   * <p>This is intended to be used for new question definitions, since updates will collide with
   * themselves and previous versions, and new versions of an old question will conflict with the
   * old question.
   *
   * <p>A new question definition's path conflicts with a stored question if it is NOT a {@link
   * QuestionType#REPEATER}, and starts with, or is started with, the stored question's path.
   */
  public Optional<Question> findPathConflictingQuestion(QuestionDefinition newQuestionDefinition) {
    PathConflictDetector pathConflictDetector =
        new PathConflictDetector(newQuestionDefinition.getPath());
    ebeanServer
        .find(Question.class)
        .findEachWhile(question -> !pathConflictDetector.hasConflict(question));
    return pathConflictDetector.getConflictedQuestion();
  }

  private static class PathConflictDetector {
    private Optional<Question> conflictedQuestion = Optional.empty();
    private final Path newPath;

    private PathConflictDetector(Path newPath) {
      this.newPath = checkNotNull(newPath);
    }

    private Optional<Question> getConflictedQuestion() {
      return conflictedQuestion;
    }

    private boolean hasConflict(Question question) {
      Path other = Path.create(question.getPath());

      // Conflict exists if paths are exactly the same
      if (newPath.equals(other)) {
        conflictedQuestion = Optional.of(question);
        return true;
      }

      // Conflict exists if an existing path starts with this new path
      if (other.startsWith(newPath)) {
        conflictedQuestion = Optional.of(question);
        return true;
      }

      // Conflict exists if this new path starts with an existing path for a non-repeater question,
      // but not for repeater questions, since that is a very common pattern.
      QuestionType otherType = question.getQuestionDefinition().getQuestionType();
      if (!otherType.equals(QuestionType.REPEATER) && newPath.startsWith(other)) {
        conflictedQuestion = Optional.of(question);
        return true;
      }

      return false;
    }
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
