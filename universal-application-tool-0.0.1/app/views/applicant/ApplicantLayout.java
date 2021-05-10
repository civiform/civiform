package views.applicant;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.head;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.span;
import static j2html.TagCreator.title;

import auth.ProfileUtils;
import auth.Roles;
import auth.UatProfile;
import com.google.common.base.Preconditions;
import controllers.ti.routes;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import views.BaseHtmlLayout;
import views.ViewUtils;
import views.style.ApplicantStyles;
import views.style.StyleUtils;
import views.style.Styles;

public class ApplicantLayout extends BaseHtmlLayout {

  private final ProfileUtils profileUtils;

  @Inject
  public ApplicantLayout(ViewUtils viewUtils, ProfileUtils profileUtils) {
    super(viewUtils);
    this.profileUtils = Preconditions.checkNotNull(profileUtils);
  }
    
  protected Content render(HtmlBundle bundle) {
    return htmlContent(bundle);
  }

  protected Content render(Http.Request request, Messages messages, DomContent... mainDomContents) {
    return render(profileUtils.currentUserProfile(request), messages, mainDomContents);
  }

  /** Renders mainDomContents within the main tag, in the context of the applicant layout. */
  protected Content render(
      Optional<UatProfile> profile, Messages messages, DomContent... mainDomContents) {
    return htmlContent(
        head(
          title("Applicant layout title"), 
          tailwindStyles()
        ),      
        body(
          renderNavBar(profile, messages),
          mainDomContents,
          viewUtils.makeLocalJsTag("main")
        )
    );
  }

}
