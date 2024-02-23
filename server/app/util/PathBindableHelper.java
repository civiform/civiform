package util;

import controllers.applicant.ApplicantRequestedActionWrapper;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.slf4j.LoggerFactory;
import play.mvc.PathBindable;

/**
 * Helper class for turning enums into {@link play.mvc.PathBindable} instances, so that enums can be
 * included in a route with type safety. The type T should be the enum that needs to be included in
 * a route.
 *
 * <p>{@link PathBindable} is an interface provided by Play that tells Play how to convert between a
 * specified enum and a string representation of that enum. The string representation is used in
 * URLs but is automatically converted to a wrapper implementation in route methods.
 *
 * <p>See https://www.playframework.com/documentation/2.9.x/RequestBinders for more details about
 * binding and unbinding objects in URLs.
 *
 * <p>Note: We can't have enums implement {@link PathBindable} directly because the class needs to
 * have a no-arg constructor, which isn't allowed for enums. See
 * https://www.playframework.com/documentation/2.9.x/api/java/play/mvc/PathBindable.html and
 * https://groups.google.com/g/play-framework/c/5EniZ5nAaMQ for more details.
 *
 * <p>This class maintains the current enum value internally. The current value can be fetched using
 * {@link #getItem()}.
 *
 * <p>Each enum that needs to be included in a route should define a new wrapper class that
 * internally uses this helper class to bind and unbind the enum. See {@link
 * ApplicantRequestedActionWrapper} for an example of how to use this helper class to wrap an enum
 * and use it in routes.
 */
public class PathBindableHelper<T extends Enum<T>> {
  private final Class<T> enumClass;
  private final T defaultItem;
  private Optional<T> wrappedItem = Optional.empty();

  /**
   * Constructs a helper instance for a specific enum. Should be used inside a wrapper class.
   *
   * @param defaultItem the default enum value that should be used if the value couldn't be unbound
   *     for any reason.
   */
  public PathBindableHelper(Class<T> enumClass, T defaultItem) {
    this.enumClass = enumClass;
    this.defaultItem = defaultItem;
  }

  /** Sets to a new enum value. */
  public void setItem(T item) {
    this.wrappedItem = Optional.of(item);
  }

  /** Returns the current stored enum value. */
  @NotNull
  public T getItem() {
    if (this.wrappedItem.isEmpty()) {
      // A lot of instances of the wrappers may be created because they may be used as
      // a user is filling out an application, so only create the logger if we have to.
      LoggerFactory.getLogger(PathBindableHelper.class)
          .error(enumClass.getCanonicalName() + " had empty item in #getItem; returning default");
      return this.defaultItem;
    }
    return this.wrappedItem.get();
  }

  /** See {@link PathBindable#bind}. */
  public void bind(String txt) {
    try {
      this.wrappedItem = Optional.of(Enum.valueOf(enumClass, txt));
    } catch (IllegalArgumentException e) {
      // Reset the item if the text we received doesn't match an enum value
      this.wrappedItem = Optional.empty();
    }
  }

  /** See {@link PathBindable#unbind}. */
  public String unbind() {
    if (this.wrappedItem.isEmpty()) {
      LoggerFactory.getLogger(PathBindableHelper.class)
          .error(enumClass.getCanonicalName() + " had empty action in #unbind; returning default");
      return this.defaultItem.name();
    }
    return this.wrappedItem.get().name();
  }
}

// TODO: Tests for this class
