package auth;

import com.typesafe.config.Config;
import java.util.Random;
import org.pac4j.core.context.FrameworkParameters;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.context.session.SessionStoreFactory;
import org.pac4j.play.store.PlayCookieSessionStore;
import org.pac4j.play.store.ShiroAesDataEncrypter;

public class CiviFormSessionStoreFactory implements SessionStoreFactory {
  private final Config config;

  public CiviFormSessionStoreFactory(Config config) {
    this.config = config;
  }

  @Override
  public SessionStore newSessionStore(FrameworkParameters parameters) {
    return newSessionStore();
  }

  public SessionStore newSessionStore() {
    // This is a weird one.  :)  The cookie session store refuses to serialize any
    // classes it doesn't explicitly trust.  A bug in pac4j interacts badly with
    // sbt's autoreload, so we have a little workaround here.  configure() gets called on every
    // startup,
    // but the JAVA_SERIALIZER object is only initialized on initial startup.
    // So, on a second startup, we'll add the CiviFormProfileData a second time.  The
    // trusted classes set should dedupe CiviFormProfileData against the old CiviFormProfileData,
    // but it's technically a different class with the same name at that point,
    // which triggers the bug.  So, we just clear the classes, which will be empty
    // on first startup and will contain the profile on subsequent startups,
    // so that it's always safe to add the profile.
    // We will need to do this for every class we want to store in the cookie.
    var serializer = new org.pac4j.core.util.serializer.JavaSerializer();
    serializer.clearTrustedClasses();
    serializer.addTrustedClass(CiviFormProfileData.class);

    // We need to use the secret key to generate the encrypter / decrypter for the
    // session store, so that cookies from version n of the application can be
    // read by version n + 1.  This is especially important for dev, otherwise
    // we're going to spend a lot of time deleting cookies.
    Random r = new Random();
    r.setSeed(this.config.getString("play.http.secret.key").hashCode());
    byte[] aesKey = new byte[32];
    r.nextBytes(aesKey);

    var sessionStore = new PlayCookieSessionStore(new ShiroAesDataEncrypter(aesKey));
    sessionStore.setSerializer(serializer);

    return sessionStore;
  }
}
