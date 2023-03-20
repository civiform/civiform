package views.dev;

import static j2html.TagCreator.a;
import static j2html.TagCreator.caption;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.table;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.tr;

import controllers.dev.routes;
import featureflags.FeatureFlag;
import featureflags.FeatureFlags;
import j2html.tags.Tag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.TableTag;
import j2html.tags.specialized.TdTag;
import java.util.Map;
import java.util.Map.Entry;
import javax.inject.Inject;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.JsBundle;
import views.style.BaseStyles;

public class FeatureFlagView extends BaseHtmlView {

  private final BaseHtmlLayout layout;
  private final FeatureFlags featureFlags;

  @Inject
  public FeatureFlagView(BaseHtmlLayout layout, FeatureFlags featureFlags) {
    this.layout = layout;
    this.featureFlags = featureFlags;
  }

  /**
   * Renders a view of the current feature flag state.
   *
   * <p>Includes:
   *
   * <ul>
   *   <li>Server controls for allowing overrides
   *   <li>Individual flags and their constituent values, including a link to toggle the session
   *       value.
   * </ul>
   */
  public Content render(Request request, boolean isDevOrStagingEnvironment) {
    // Create system level control view.
    TableTag serverSettingTable =
        table()
            .with(
                tr().with(
                        configureCell(td("Server environment: ")),
                        configureCell(td(Boolean.toString(isDevOrStagingEnvironment)))),
                tr().with(
                        configureCell(td("Configuration: ")),
                        configureCell(td(Boolean.toString(featureFlags.areOverridesEnabled())))));

    // Create per flag view.
    Map<FeatureFlag, Boolean> sortedFlags = featureFlags.getAllFlagsSorted(request);
    TableTag flagTable =
        table()
            .withClasses("p-6", "mt-10", "text-left")
            .with(
                caption("Current flag values"),
                tr().with(
                        configureCell(th("Flag name")),
                        configureCell(th("Server value")),
                        configureCell(th("Session value")),
                        configureCell(th("Effective value")),
                        configureCell(th("Flip flag"))));

    // For each flag show its config value, session value (if different), the effective value for
    // the user and a control to toggle the value.
    for (Entry<FeatureFlag, Boolean> flagEntry : sortedFlags.entrySet()) {
      Boolean configValue = featureFlags.getFlagEnabledFromConfig(flagEntry.getKey()).orElse(false);
      Boolean sessionValue = flagEntry.getValue();
      Boolean sessionOverrides = !configValue.equals(sessionValue);
      // Only show values that override the system default for clarity.
      String sessionDisplay = sessionOverrides ? sessionValue.toString() : "";
      Tag flagFlipLink =
          sessionValue
              ? a().withHref(
                      routes.FeatureFlagOverrideController.disable(flagEntry.getKey().toString())
                          .url())
                  .withText("disable")
              : a().withHref(
                      routes.FeatureFlagOverrideController.enable(flagEntry.getKey().toString())
                          .url())
                  .withText("enable");
      flagFlipLink.withClasses(BaseStyles.LINK_TEXT, BaseStyles.LINK_HOVER_TEXT);

      // If the session value is different highlight that.
      // There's no easy way to add the bold conditionally since withX() overwrites values.
      TdTag sessionValueTD = td(sessionValue.toString());
      if (sessionOverrides) {
        sessionValueTD.withClasses(BaseStyles.TABLE_CELL_STYLES, "font-bold");
      } else {
        sessionValueTD.withClasses(BaseStyles.TABLE_CELL_STYLES);
      }

      flagTable.with(
          tr().with(
                  configureCell(td(flagEntry.getKey().toString())),
                  configureCell(td(configValue.toString())),
                  // If the session value is different highlight that.
                  td(sessionDisplay).withClasses(BaseStyles.TABLE_CELL_STYLES, "font-bold"),
                  sessionValueTD,
                  configureCell(td(flagFlipLink))));
    }

    // Build the page.
    DivTag serverSettingSection =
        div()
            .with(
                h1("Feature Flags").withClasses("py-6"),
                h2("Overrides are allowed if all are true:"))
            .with(serverSettingTable)
            .withClass("p-6");
    HtmlBundle bundle =
        layout
            .getBundle()
            .setTitle("Feature Flags")
            .addMainContent(serverSettingSection, div().withClass("px-4").with(flagTable))
            .setJsBundle(JsBundle.ADMIN);
    return layout.render(bundle);
  }

  Tag configureCell(Tag tag) {
    return tag.withClasses(BaseStyles.TABLE_CELL_STYLES);
  }
}
