package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import akka.actor.ActorSystem;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.libs.concurrent.CustomExecutionContext;

/** Custom execution context wired to "database.dispatcher" thread pool */
@Singleton
public class DatabaseExecutionContext extends CustomExecutionContext {
  @Inject
  public DatabaseExecutionContext(ActorSystem actorSystem) {
    super(checkNotNull(actorSystem), "database.dispatcher");
  }
}
