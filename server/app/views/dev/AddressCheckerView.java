package views.dev;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.header;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;
import static j2html.TagCreator.ul;

import com.google.inject.Inject;
import controllers.dev.seeding.routes;
import j2html.tags.specialized.DivTag;
import play.mvc.Http;
import play.twirl.api.Content;
import services.settings.SettingsManifest;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.JsBundle;
import views.components.Icons;
import views.dev.hx.CorrectAddressFormPartial;
import views.dev.hx.ServiceAreaFormPartial;
import views.style.BaseStyles;

public class AddressCheckerView extends BaseHtmlView {
  private final BaseHtmlLayout layout;
  private final SettingsManifest settingsManifest;
  private final CorrectAddressFormPartial correctAddressFormPartial;
  private final ServiceAreaFormPartial serviceAreaFormPartial;

  @Inject
  public AddressCheckerView(
      BaseHtmlLayout layout,
      SettingsManifest settingsManifest,
      CorrectAddressFormPartial correctAddressFormPartial,
      ServiceAreaFormPartial serviceAreaFormPartial) {
    this.layout = checkNotNull(layout);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.correctAddressFormPartial = checkNotNull(correctAddressFormPartial);
    this.serviceAreaFormPartial = checkNotNull(serviceAreaFormPartial);
  }

  public Content render(Http.Request request) {
    DivTag content =
        div()
            .withClasses("m-8", "max-w-6xl")
            .with(
                header(
                        a().withHref(routes.DevDatabaseSeedController.index().url())
                            .with(
                                span()
                                    .with(
                                        Icons.svg(Icons.ARROW_LEFT)
                                            .withClasses("w-6", "h-6", "inline-block", "mb-1.5")),
                                text("Back to dev tools"))
                            .withClasses(
                                BaseStyles.LINK_TEXT, "hover:" + BaseStyles.LINK_HOVER_TEXT))
                    .withClasses("mb-8"),
                h1("Address Checker").withClasses("mb-4"),
                renderUrls(request),
                div()
                    .withClasses("grid", "grid-cols-1", "md:grid-cols-2", "gap-8")
                    .with(
                        renderCorrectAddressForm(request),
                        renderCheckServiceAreaForm(request),
                        div().withId("correctAddressDebugOutput"),
                        div().withId("checkServiceAreaDebugOutput")));

    HtmlBundle bundle =
        layout
            .getBundle(request)
            .setTitle("Address Checker")
            .addMainContent(content)
            .setJsBundle(JsBundle.ADMIN);
    return layout.render(bundle);
  }

  private DivTag renderUrls(Http.Request request) {
    var divTag =
        div(
                h2("Current Address Settings").withClasses("mb-4"),
                div(
                    span("Address Correction: ").withClasses("font-bold"),
                    span(
                        settingsManifest.getEsriAddressCorrectionEnabled(request)
                            ? "Enabled"
                            : "Disabled")),
                div(
                        span("Address Validation: ").withClasses("font-bold"),
                        span(
                            settingsManifest.getEsriAddressServiceAreaValidationEnabled(request)
                                ? "Enabled"
                                : "Disabled"))
                    .withClasses("mt-2"))
            .withClasses("mt-8", "mb-8", "bg-gray-200", "p-4");

    if (settingsManifest.getEsriFindAddressCandidatesUrls().isPresent()) {
      divTag.with(h3("Find Address Candidates Url(s)").withClasses("mt-2", "font-bold"));
      for (var url : settingsManifest.getEsriFindAddressCandidatesUrls().get()) {
        divTag.with(div(url));
      }
    }

    if (settingsManifest.getEsriAddressServiceAreaValidationUrls().isPresent()) {
      divTag.with(h3("Service Area Validation Url(s)").withClasses("mt-2", "font-bold"));
      for (var url : settingsManifest.getEsriAddressServiceAreaValidationUrls().get()) {
        divTag.with(div(url));
      }
    }

    return divTag;
  }

  private DivTag renderCorrectAddressForm(Http.Request request) {
    boolean showForm =
        settingsManifest.getEsriFindAddressCandidatesUrls().isPresent()
            && (long) settingsManifest.getEsriFindAddressCandidatesUrls().get().size() > 0
            && settingsManifest.getEsriAddressCorrectionEnabled(request);

    if (showForm) {
      return correctAddressFormPartial.render(request);
    }

    return div(
        h2("Cannot show address correction tool"),
        p("Likely reasons:"),
        ul(
                li("ESRI_FIND_ADDRESS_CANDIDATES_URLS has no endpoints").withClass("ml-4"),
                li("ESRI_ADDRESS_CORRECTION_ENABLED is not enabled").withClass("ml-4"))
            .withClasses("list-disc"));
  }

  private DivTag renderCheckServiceAreaForm(Http.Request request) {
    boolean showForm =
        settingsManifest.getEsriAddressServiceAreaValidationUrls().isPresent()
            && (long) settingsManifest.getEsriAddressServiceAreaValidationUrls().get().size() > 0
            && settingsManifest.getEsriAddressServiceAreaValidationEnabled(request);

    if (showForm) {
      return serviceAreaFormPartial.render(request);
    }

    return div(
        h2("Cannot show service area validation tool"),
        p("Likely reasons:"),
        ul(
                li("ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS has no endpoints").withClass("ml-4"),
                li("ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ENABLED is not enabled").withClass("ml-4"))
            .withClasses("list-disc"));
  }
}
