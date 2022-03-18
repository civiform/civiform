package auth;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

/**
 * AdminAuthClient is the annotation for the auth client responsible for admin authentication. This
 * client must implement {@link org.pac4j.core.client.IndirectClient}.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminAuthClient {}
