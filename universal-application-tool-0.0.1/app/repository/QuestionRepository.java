package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Question;
import play.db.ebean.EbeanConfig;

public class QuestionRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public QuestionRepository(EbeanConfig ebeanConfig, DatabaseExecutionContext executionContext) {
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.executionContext = checkNotNull(executionContext);
  }

  public CompletionStage<Set<Question>> listQuestions() {
    return supplyAsync(() -> ebeanServer.find(Question.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<Question>> lookupQuestion(long id) {
    return supplyAsync(
        () -> Optional.ofNullable(ebeanServer.find(Question.class).setId(id).findOne()),
        executionContext);
  }

  static class PathConflictDetector {
    Boolean conflict = Boolean.FALSE;
    String newPath;

    public PathConflictDetector(String newPath) {
      this.newPath = checkNotNull(newPath);
    }

    public Boolean hasConflict() {
      return conflict;
    }

    public boolean checkConflict(Question question) {
      boolean proceed = true;
      if (question.pathConflictsWith(newPath)) {
        conflict = Boolean.TRUE;
        proceed = false;
      }
      return proceed;
    }
  }

  public CompletionStage<Boolean> lookupPathConflict(String newPath) {
    return supplyAsync(
        () -> {
          PathConflictDetector detector = new PathConflictDetector(newPath);
          ebeanServer.find(Question.class).findEachWhile(detector::checkConflict);
          return detector.hasConflict();
        },
        executionContext);
  }

  public CompletionStage<Optional<Question>> lookupQuestionByPath(String path) {
    return supplyAsync(
        () ->
            Optional.ofNullable(
                ebeanServer
                    .find(Question.class)
                    .where()
                    .eq("path", path)
                    .orderBy()
                    .desc("version")
                    .setMaxRows(1)
                    .findOne()),
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
}
