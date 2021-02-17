package views.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import play.twirl.api.Content;
import services.program.ProgramService;
import views.BaseHtmlView;
import views.ViewUtils;

public final class ProgramList extends BaseHtmlView {

  private final ViewUtils viewUtils;

  @Inject
  public ProgramList(ViewUtils viewUtils) {
    this.viewUtils = checkNotNull(viewUtils);
  }

  public Content render(ProgramService service) {
    return htmlContent();
  }
}
