package util;

import controllers.applicant.ApplicantRequestedActionWrapper;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.slf4j.LoggerFactory;

/** TODO */
public class PathBindableHelper<T extends Enum<T>> {
  private final T defaultItem;
  private final Class<T> enumClass;
  private Optional<T> wrappedItem = Optional.empty();

  public PathBindableHelper(T defaultItem, Class<T> enumClass) {
    this.defaultItem = defaultItem;
    this.enumClass = enumClass;
  }

  /** Sets the given action on this wrapper and returns the wrapper. */
  public PathBindableHelper<T> setItem(T item) {
    this.wrappedItem = Optional.of(item);
    return this;
  }

  @NotNull
  public T getItem() {
    if (this.wrappedItem.isEmpty()) {
      // A lot of instances of the wrapper may be created because it may used as
      // a user is filling out an application, so only create the logger if we have to.
      LoggerFactory.getLogger(PathBindableHelper.class)
          .error("Wrapper had empty item in #getItem; returning default"); // TODO
      return this.defaultItem;
    }
    return this.wrappedItem.get();
  }

  public void bind(String txt) {
    try {
      this.wrappedItem = Optional.of(Enum.valueOf(enumClass, txt));
    } catch (IllegalArgumentException e) {
      // Reset the action if the text we received doesn't match a ApplicantRequestedAction value
      this.wrappedItem = Optional.empty();
    }
  }

  public String unbind() {
    if (this.wrappedItem.isEmpty()) {
      LoggerFactory.getLogger(ApplicantRequestedActionWrapper.class)
          // TODO
          .error("ApplicantRequestedActionWrapper had empty action in #unbind; returning default");
      return this.defaultItem.name();
    }
    return this.wrappedItem.get().name();
  }
}
