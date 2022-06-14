package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import akka.actor.ActorSystem;
import annotations.BindingAnnotations.Now;
import auth.Authorizers;
import controllers.CiviFormController;
import java.time.Duration;
import java.time.LocalDateTime;
import javax.inject.Inject;
import javax.inject.Provider;
import org.pac4j.play.java.Secure;
import play.mvc.Result;
import scala.concurrent.ExecutionContext;
import tasks.StoredFileAclMigrationTask;

/**
 * Controller enabling admins to trigger data migrations in production. Need not serve a UI. all
 * actions must be protected by @Secure(authorizers = Authorizers.Labels.ANY_ADMIN). Actions should
 * not directly migrate data, but rather schedule migration tasks to be performed in a background
 * thread using the {@link ActorSystem}.
 */
public class DataMigrationsController extends CiviFormController {

  private final ActorSystem actorSystem;
  private final ExecutionContext executionContext;
  private final LocalDateTime now;
  private final Provider<StoredFileAclMigrationTask> storedFileAclMigrationTaskProvider;

  @Inject
  public DataMigrationsController(
      ActorSystem actorSystem,
      ExecutionContext executionContext,
      @Now LocalDateTime now,
      Provider<StoredFileAclMigrationTask> storedFileAclMigrationTaskProvider) {
    this.actorSystem = checkNotNull(actorSystem);
    this.executionContext = checkNotNull(executionContext);
    this.now = checkNotNull(now);
    this.storedFileAclMigrationTaskProvider = checkNotNull(storedFileAclMigrationTaskProvider);
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result migrateStoredFileAcls() {
    actorSystem
        .scheduler()
        .scheduleOnce(
            Duration.ofMillis(10),
            () -> storedFileAclMigrationTaskProvider.get().run(),
            executionContext);

    return ok(
        String.format(
            "Migration started at %s. Check server logs for StoredFileMigrationCompleted and"
                + " StoredFileMigrationError.",
            now));
  }
}
