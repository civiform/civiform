package controllers.applicant;

import static controllers.applicant.ApplicantRequestedAction.DEFAULT_ACTION;

import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.slf4j.LoggerFactory;
import play.mvc.PathBindable;

/**
 * This class allows us to include {@link ApplicantRequestedAction} directly in routes with type
 * safety. It does this by implementing {@link PathBindable}, an interface that tells Play how to
 * convert between a {@link ApplicantRequestedAction} and a string representation of the action. The
 * string representation is used in URLs but is automatically converted to this wrapper
 * implementation in route methods.
 *
 * <p>See https://www.playframework.com/documentation/2.9.x/RequestBinders for more details about
 * binding and unbinding objects in URLs.
 *
 * <p>We can't have the {@link ApplicantRequestedAction} enum implement {@link PathBindable}
 * directly because the class needs to have a no-arg constructor, which isn't allowed for enums. See
 * https://www.playframework.com/documentation/2.9.x/api/java/play/mvc/PathBindable.html and
 * https://groups.google.com/g/play-framework/c/5EniZ5nAaMQ for more details.
 */
public final class ApplicantRequestedActionWrapper
    implements PathBindable<ApplicantRequestedActionWrapper> {
  private Optional<ApplicantRequestedAction> action = Optional.empty();

  public ApplicantRequestedActionWrapper() {}

  public ApplicantRequestedActionWrapper(ApplicantRequestedAction action) {
    this.action = Optional.of(action);
  }

  /** Sets the given action on this wrapper and returns the wrapper. */
  public ApplicantRequestedActionWrapper setAction(ApplicantRequestedAction action) {
    this.action = Optional.of(action);
    return this;
  }

  @NotNull
  public ApplicantRequestedAction getAction() {
    if (this.action.isEmpty()) {
      // A lot of instances of ApplicantRequestedActionWrapper will be created because it's used as
      // a user is filling out an application, so only create the logger if we have to.
      LoggerFactory.getLogger(ApplicantRequestedActionWrapper.class)
          .error(
              "ApplicantRequestedActionWrapper had empty action in #getAction; returning default");
      return DEFAULT_ACTION;
    }
    return this.action.get();
  }

  @Override
  public ApplicantRequestedActionWrapper bind(String key, String txt) {
    try {
      this.action = Optional.of(ApplicantRequestedAction.valueOf(txt));
    } catch (IllegalArgumentException e) {
      // Reset the action if the text we received doesn't match a ApplicantRequestedAction value
      this.action = Optional.empty();
    }
    return this;
  }

  @Override
  public String unbind(String key) {
    return javascriptUnbind();
  }

  @Override
  public String javascriptUnbind() {
    if (this.action.isEmpty()) {
      LoggerFactory.getLogger(ApplicantRequestedActionWrapper.class)
          .error("ApplicantRequestedActionWrapper had empty action in #unbind; returning default");
      return DEFAULT_ACTION.name();
    }
    return this.action.get().name();
  }
}
