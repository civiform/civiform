package auth;

import org.pac4j.core.context.FrameworkParameters;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.context.session.SessionStoreFactory;
import org.pac4j.play.store.PlayCookieSessionStore;
import org.pac4j.play.store.ShiroAesDataEncrypter;

public class CiviFormSessionStoreFactory implements SessionStoreFactory {
  private final byte[] aesKey;

  public CiviFormSessionStoreFactory(byte[] aesKey) {
    this.aesKey = aesKey;
  }

  public SessionStore newSessionStore(FrameworkParameters parameters) {
    return new PlayCookieSessionStore(new ShiroAesDataEncrypter(aesKey));
  }
}
