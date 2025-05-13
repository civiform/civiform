package services.apibridge;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.pekko.actor.ActorSystem;
import play.libs.concurrent.CustomExecutionContext;

/**
 * Custom execution context wired to "api-bridge.dispatcher" thread pool
 *
 * <p>This is only to be used with the API Bridge
 */
@Singleton
public class ApiBridgeExecutionContext extends CustomExecutionContext {
  @Inject
  public ApiBridgeExecutionContext(ActorSystem actorSystem) {
    super(checkNotNull(actorSystem), "api-bridge.dispatcher");
  }
}
