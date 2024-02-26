package views.admin.programs;

import static views.admin.programs.ProgramTranslationReferer.DEFAULT_ACTION;

import play.mvc.PathBindable;
import util.PathBindableHelper;

/**
 * This class allows us to include {@link ProgramTranslationReferer} directly in routes with type
 * safety. See {@link PathBindableHelper} for more details on how this wrapping works and why we
 * need it.
 */
public class ProgramTranslationRefererWrapper
    implements PathBindable<ProgramTranslationRefererWrapper> {
  private final PathBindableHelper<ProgramTranslationReferer> pathBindableHelper =
      new PathBindableHelper<>(ProgramTranslationReferer.class, DEFAULT_ACTION);

  public ProgramTranslationRefererWrapper() {}

  public ProgramTranslationRefererWrapper(ProgramTranslationReferer referer) {
    pathBindableHelper.setItem(referer);
  }

  public ProgramTranslationReferer getReferer() {
    return pathBindableHelper.getItem();
  }

  @Override
  public ProgramTranslationRefererWrapper bind(String key, String txt) {
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
