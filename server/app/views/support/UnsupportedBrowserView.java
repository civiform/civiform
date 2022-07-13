package views.support;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.img;

import j2html.tags.ContainerTag;
import javax.inject.Inject;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.applicant.ApplicantLayout;
import views.style.StyleUtils;
import views.style.Styles;

public class UnsupportedBrowserView extends BaseHtmlView {

  private final ApplicantLayout applicantLayout;

  @Inject
  public UnsupportedBrowserView(ApplicantLayout applicantLayout) {
    this.applicantLayout = checkNotNull(applicantLayout);
  }

  public Content render() {
    HtmlBundle bundle = applicantLayout.getBundle();

    bundle.setTitle("The browser you are currently using is not supported");
    ContainerTag container =
        div().withClasses(Styles.CONTAINER, Styles.MX_AUTO, Styles.MAX_W_XL, Styles.P_4);
    container.with(
        h1("Unsupported browser").withClasses(Styles.TEXT_CENTER, Styles.MB_4),
        div(
            "Please use one of the browsers listed below. If you are using Windows you should have"
                + " Microsoft Edge installed on your computer."));
    ContainerTag browsers =
        div().withClasses(Styles.FLEX, Styles.FLEX_WRAP, Styles.JUSTIFY_AROUND, Styles.MT_6);
    browsers.with(
        createBrowserIcon("Microsoft Edge", "edge-logo-128px.png"),
        createBrowserIcon("Firefox", "firefox-logo-128px.png"),
        createBrowserIcon("Safari", "safari-logo-128px.png"),
        createBrowserIcon("Google Chrome", "chrome-logo-128px.png"));
    container.with(browsers);

    bundle.addMainContent(container);
    return applicantLayout.render(bundle);
  }

  private static ContainerTag createBrowserIcon(String name, String logo) {
    return div(
            img()
                .withSrc("/assets/images/" + logo)
                .withClasses(Styles.W_24, Styles.H_24, Styles.MX_AUTO, Styles.MB_2),
            div(name).withClasses(Styles.TEXT_CENTER))
        .withClasses(Styles.W_1_2, StyleUtils.responsiveMedium(Styles.W_1_4), Styles.MB_6);
  }
}
