package annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

public class FeatureFlags {


  @Qualifier
  @Target({METHOD, PARAMETER})
  @Retention(RUNTIME)
  public @interface StatusTrackingEnabled {}
}
