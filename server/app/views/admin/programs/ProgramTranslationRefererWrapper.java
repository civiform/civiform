package views.admin.programs;

import static views.admin.programs.ProgramTranslationReferer.PROGRAM_EDIT;

import play.mvc.PathBindable;
import util.PathBindableHelper;

/** TODO */
public class ProgramTranslationRefererWrapper
    implements PathBindable<ProgramTranslationRefererWrapper> {
  private final PathBindableHelper<ProgramTranslationReferer> pathBindableHelper =
      new PathBindableHelper<>(/* defaultItem= */ PROGRAM_EDIT, ProgramTranslationReferer.class);

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
