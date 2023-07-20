package views.support;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.img;

import j2html.tags.ContainerTag;
import j2html.tags.specialized.DivTag;
import javax.inject.Inject;
import play.mvc.Http;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.applicant.ApplicantLayout;
import views.style.StyleUtils;

/** View responsible for rendering pages showing information regarding unsupported browsers. */
public final class UnsupportedBrowserView extends BaseHtmlView {

  private final ApplicantLayout applicantLayout;

  @Inject
  public UnsupportedBrowserView(ApplicantLayout applicantLayout) {
    this.applicantLayout = checkNotNull(applicantLayout);
  }

  public Content render(Http.Request request) {
    HtmlBundle bundle = applicantLayout.getBundle(request);

    bundle.setTitle("The browser you are currently using is not supported");
    DivTag container =
        div()
            .withClasses("container", "mx-auto", "max-w-xl", "p-4")
            .with(
                h1("Unsupported browser").withClasses("text-center", "mb-4"),
                div(
                    "Please use one of the browsers listed below. If you are using Windows you"
                        + " should have Microsoft Edge installed on your computer."));
    DivTag browsers =
        div()
            .withClasses("flex", "flex-wrap", "justify-around", "mt-6")
            .with(
                createBrowserIcon("Microsoft Edge", "edge-logo-128px.png"),
                createBrowserIcon("Firefox", "firefox-logo-128px.png"),
                createBrowserIcon("Safari", "safari-logo-128px.png"),
                createBrowserIcon("Google Chrome", "chrome-logo-128px.png"));
    container.with(browsers);

    bundle.addMainContent(container);
    return applicantLayout.render(bundle);
  }

  private static ContainerTag createBrowserIcon(String name, String logo) {
    return div()
        .withClasses("w-1/2", StyleUtils.responsiveMedium("w-1/4"), "mb-6")
        .with(
            img()
                .withSrc("/assets/images/" + logo)
                .withAlt(name + " logo")
                .withClasses("w-24", "h-24", "mx-auto", "mb-2"),
            div(name).withClasses("text-center"));
  }
}
