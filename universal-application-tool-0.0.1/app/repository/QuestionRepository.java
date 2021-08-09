package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.collect.ImmutableList;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.TxScope;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Provider;
import models.Question;
import models.QuestionTag;
import models.Version;
import play.db.ebean.EbeanConfig;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

/**
 * QuestionRepository performs complicated operations on {@link Question} that often involve other
 * EBean models or asynchronous handling.
 */
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
    Version draftVersion = versionRepositoryProvider.get().getDraftVersion();
    try (Transaction transaction = ebeanServer.beginTransaction(TxScope.requiresNew())) {
      Optional<Question> existingDraft = draftVersion.getQuestionByName(definition.getName());
      try {
        if (existingDraft.isPresent()) {
          Question updatedDraft =
              new Question(
                  new QuestionDefinitionBuilder(definition).setId(existingDraft.get().id).build());
          this.updateQuestionSync(updatedDraft);
          transaction.commit();
          return updatedDraft;
        } else {
          Question newDraft =
              new Question(new QuestionDefinitionBuilder(definition).setId(null).build());
          insertQuestionSync(newDraft);
          // Fetch the tags off the old question.
          Question oldQuestion = new Question(definition);
          oldQuestion.refresh();
          for (QuestionTag tag : oldQuestion.getQuestionTags()) {
            newDraft.addTag(tag);
          }
          newDraft.addVersion(draftVersion);
          newDraft.save();
          draftVersion.refresh();

          if (definition.isEnumerator()) {
            transaction.setNestedUseSavepoint();
            updateAllRepeatedQuestions(newDraft.id, definition.getId());
          }

          transaction.setNestedUseSavepoint();
          versionRepositoryProvider.get().updateProgramsForNewDraftQuestion(definition.getId());
          transaction.commit();
          return newDraft;
        }
      } catch (UnsupportedQuestionTypeException e) {
        // This should not be able to happen since the provided question definition is inherently
        // valid.
        // Throw runtime exception so callers don't have to deal with it.
        throw new RuntimeException(e);
      }
    }
  }

  private void updateAllRepeatedQuestions(long newEnumeratorId, long oldEnumeratorId) {
    Stream.concat(
            versionRepositoryProvider.get().getDraftVersion().getQuestions().stream(),
            versionRepositoryProvider.get().getActiveVersion().getQuestions().stream())
        .filter(
            question ->
                question
                    .getQuestionDefinition()
                    .getEnumeratorId()
                    .equals(Optional.of(oldEnumeratorId)))
        .forEach(
            question -> {
              try {
                updateOrCreateDraft(
                    new QuestionDefinitionBuilder(question.getQuestionDefinition())
                        .setEnumeratorId(Optional.of(newEnumeratorId))
                        .build());
              } catch (UnsupportedQuestionTypeException e) {
                // All question definitions are looked up and should be valid.
                throw new RuntimeException(e);
              }
            });
  }

  /**
   * Maybe find a {@link Question} that conflicts with {@link QuestionDefinition}.
   *
   * <p>This is intended to be used for new question definitions, since updates will collide with
   * themselves and previous versions, and new versions of an old question will conflict with the
   * old question.
   *
   * <p>Questions collide if they share a {@link QuestionDefinition#getQuestionPathSegment()} and
   * {@link QuestionDefinition#getEnumeratorId()}.
   */
  public Optional<Question> findConflictingQuestion(QuestionDefinition newQuestionDefinition) {
    ConflictDetector conflictDetector =
        new ConflictDetector(
            newQuestionDefinition.getEnumeratorId(),
            newQuestionDefinition.getQuestionPathSegment(),
            newQuestionDefinition.getName());
    ebeanServer
        .find(Question.class)
        .findEachWhile(question -> !conflictDetector.hasConflict(question));
    return conflictDetector.getConflictedQuestion();
  }

  /** Get the questions with the specified tag which are in the active version. */
  public ImmutableList<QuestionDefinition> getAllQuestionsForTag(QuestionTag tag) {
    Version active = versionRepositoryProvider.get().getActiveVersion();
    return ebeanServer
        .find(Question.class)
        .where()
        .arrayContains("question_tags", tag)
        .findList()
        .stream()
        .filter(
            question ->
                active.getQuestions().stream()
                    .anyMatch(activeQuestion -> activeQuestion.id.equals(question.id)))
        .sorted(Comparator.comparing(question -> question.getQuestionDefinition().getName()))
        .map(Question::getQuestionDefinition)
        .collect(ImmutableList.toImmutableList());
  }

  private static class ConflictDetector {
    private Optional<Question> conflictedQuestion = Optional.empty();
    private final Optional<Long> enumeratorId;
    private final String questionPathSegment;
    private final String questionName;

    private ConflictDetector(
        Optional<Long> enumeratorId, String questionPathSegment, String questionName) {
      this.enumeratorId = checkNotNull(enumeratorId);
      this.questionPathSegment = checkNotNull(questionPathSegment);
      this.questionName = checkNotNull(questionName);
    }

    private Optional<Question> getConflictedQuestion() {
      return conflictedQuestion;
    }

    private boolean hasConflict(Question question) {
      if (question.getQuestionDefinition().getName().equals(questionName)
          || (question.getQuestionDefinition().getEnumeratorId().equals(enumeratorId)
              && question
                  .getQuestionDefinition()
                  .getQuestionPathSegment()
                  .equals(questionPathSegment))) {
        conflictedQuestion = Optional.of(question);
        return true;
      }
      return false;
    }
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
