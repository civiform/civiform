package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import j2html.tags.specialized.DivTag;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.errors.ErrorComponent;

/** renders a info page for applicants trying to access a disabled program via its deep link */
public final class ApplicantDisabledProgramView extends BaseHtmlView {

  private final ApplicantLayout layout;

  @Inject
  public ApplicantDisabledProgramView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(
      Messages messages,
      Http.Request request,
      long applicantId,
      ApplicantPersonalInfo personalInfo) {
    HtmlBundle bundle = layout.getBundle(request);
    bundle.setTitle("Disabled Program");
    bundle.addMainContent(mainContent(messages));
    return layout.renderWithNav(request, personalInfo, messages, bundle, Optional.of(applicantId));
  }

  private DivTag mainContent(Messages messages) {
    return ErrorComponent.renderErrorComponent(
        messages.at(MessageKey.TITLE_PROGRAM_NOT_AVAILABLE.getKeyName()),
        Optional.of(messages.at(MessageKey.CONTENT_DISABLED_PROGRAM_INFO.getKeyName())),
        Optional.empty(),
        messages.at(MessageKey.BUTTON_HOME_PAGE.getKeyName()),
        messages,
        Optional.empty());
  }
}
