package controllers.applicant;

import static controllers.applicant.ApplicantRequestedAction.DEFAULT_ACTION;

import javax.annotation.Nullable;
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
 * <p>See https://groups.google.com/g/play-framework/c/5EniZ5nAaMQ for information on why we need
 * this wrapper class around the enum and can't have {@link ApplicantRequestedAction} implement
 * {@link PathBindable} directly.
 */
public final class ApplicantRequestedActionWrapper
    implements PathBindable<ApplicantRequestedActionWrapper> {
  @Nullable private ApplicantRequestedAction action;

  public ApplicantRequestedActionWrapper() {}

  public ApplicantRequestedActionWrapper(ApplicantRequestedAction action) {
    this.action = action;
  }

  /** Sets the given action on this wrapper and returns the wrapper. */
  public ApplicantRequestedActionWrapper setAction(ApplicantRequestedAction action) {
    this.action = action;
    return this;
  }

  @NotNull
  public ApplicantRequestedAction getAction() {
    if (this.action == null) {
      // A lot of instances of ApplicantRequestedActionWrapper will be created because it's used as
      // a user is filling out an application, so only create the logger if we have to.
      LoggerFactory.getLogger(ApplicantRequestedActionWrapper.class)
          .error(
              "ApplicantRequestedActionWrapper had null action in #getAction; returning default of"
                  + " NEXT_BLOCK");
      return DEFAULT_ACTION;
    }
    return this.action;
  }

  @Override
  public ApplicantRequestedActionWrapper bind(String key, String txt) {
    try {
      this.action = ApplicantRequestedAction.valueOf(txt);
    } catch (IllegalArgumentException e) {
      // Reset the action if the text we received doesn't match a ApplicantRequestedAction value
      this.action = null;
    }
    return this;
  }

  @Override
  public String unbind(String key) {
    return javascriptUnbind();
  }

  @Override
  public String javascriptUnbind() {
    if (this.action == null) {
      LoggerFactory.getLogger(ApplicantRequestedActionWrapper.class)
          .error(
              "ApplicantRequestedActionWrapper had null action in #unbind; returning default of"
                  + " NEXT_BLOCK");
      return DEFAULT_ACTION.name();
    }
    return this.action.name();
  }
}
