package annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/** Annotations that indicate if application wide features are enabled. */
public class FeatureFlags {
  @Qualifier
  @Target({METHOD, PARAMETER})
  @Retention(RUNTIME)
  public @interface ApplicationStatusTrackingEnabled {}
}
