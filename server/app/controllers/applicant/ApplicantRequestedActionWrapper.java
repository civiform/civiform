package controllers.applicant;

import static controllers.applicant.ApplicantRequestedAction.DEFAULT_ACTION;

import play.mvc.PathBindable;
import util.PathBindableHelper;

/**
 * This class allows us to include {@link ApplicantRequestedAction} directly in routes with type
 * safety. See {@link PathBindableHelper} for more details on how this wrapping works and why we
 * need it.
 */
public final class ApplicantRequestedActionWrapper
    implements PathBindable<ApplicantRequestedActionWrapper> {
  private final PathBindableHelper<ApplicantRequestedAction> pathBindableHelper =
      new PathBindableHelper<>(ApplicantRequestedAction.class, DEFAULT_ACTION);

  public ApplicantRequestedActionWrapper() {}

  public ApplicantRequestedActionWrapper(ApplicantRequestedAction action) {
    pathBindableHelper.setItem(action);
  }

  /** Returns the {@link ApplicantRequestedAction} stored in this wrapper. */
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
