package controllers.applicant;

import static controllers.applicant.ApplicantRequestedAction.DEFAULT_ACTION;

import play.mvc.PathBindable;
import util.PathBindableHelper;

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
  private final PathBindableHelper<ApplicantRequestedAction> pathBindableHelper =
      new PathBindableHelper<>(DEFAULT_ACTION, ApplicantRequestedAction.class);

  public ApplicantRequestedActionWrapper() {}

  public ApplicantRequestedActionWrapper(ApplicantRequestedAction action) {
    pathBindableHelper.setItem(action);
  }

  public ApplicantRequestedAction getAction() {
    return pathBindableHelper.getItem();
  }

  @Override
  public ApplicantRequestedActionWrapper bind(String key, String txt) {
    pathBindableHelper.bind(txt);
    return this;
  }

  @Override
  public String unbind(String key) {
    return pathBindableHelper.unbind();
  }

  @Override
  public String javascriptUnbind() {
    return pathBindableHelper.unbind();
  }
}
