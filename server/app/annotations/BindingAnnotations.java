package annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

public final class BindingAnnotations {
  /**
   * Now represents "right now". Consumers should typically inject a @Now Provider<T> in the
   * constructor, otherwise the returned value will be "right now" when the constructor was called.
   */
  @Qualifier
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface Now {}
}
