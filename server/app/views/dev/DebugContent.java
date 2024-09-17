package views.dev;

import static j2html.TagCreator.div;
import static j2html.TagCreator.p;

import auth.FakeAdminClient;
import com.typesafe.config.Config;
import j2html.tags.specialized.DivTag;
import javax.inject.Inject;
import views.BaseHtmlView;

/** All frontend debug content that we show when running CiviForm in dev mode. */
public final class DebugContent extends BaseHtmlView {

  private final String civiformVersion;
  private final String civiformImageTag;

  @Inject
  public DebugContent(Config config) {
    this.civiformVersion = config.getString("civiform_version");
    this.civiformImageTag = config.getString("civiform_image_tag");
  }

  public static DivTag devTools() {
    return div()
        .withClasses("flex", "flex-col")
        .with(
            p("Log in as:"),
            redirectButton(
                "admin",
                "CiviForm Admin",
                controllers.routes.CallbackController.fakeAdmin(
                        FakeAdminClient.CLIENT_NAME, FakeAdminClient.GLOBAL_ADMIN)
                    .url()),
            redirectButton(
                "program-admin",
                "Program Admin",
                controllers.routes.CallbackController.fakeAdmin(
                        FakeAdminClient.CLIENT_NAME, FakeAdminClient.PROGRAM_ADMIN)
                    .url()),
            redirectButton(
                "dual-admin",
                "Program and Civiform Admin",
                controllers.routes.CallbackController.fakeAdmin(
                        FakeAdminClient.CLIENT_NAME, FakeAdminClient.DUAL_ADMIN)
                    .url()),
            redirectButton(
                "trusted-intermediary",
                "Trusted Intermediary",
                controllers.routes.CallbackController.fakeAdmin(
                        FakeAdminClient.CLIENT_NAME, FakeAdminClient.TRUSTED_INTERMEDIARY)
                    .url()),
            p("Other:"),
            redirectButton(
                "additional-tools",
                "Additional tools",
                controllers.dev.routes.DevToolsController.index().url()));
  }

  public DivTag civiformVersionDiv() {
    // civiformVersion is the version the deployer requests, like "latest" or
    // "v1.18.0". civiformImageTag is set by bin/build-prod and is a string
    // like "SNAPSHOT-3af8997-1678895722".
    String version = civiformVersion;
    if (civiformVersion.equals("") || civiformVersion.equals("latest")) {
      version = civiformImageTag;
    }
    return div()
        .with(p("CiviForm version: " + version).withClasses("text-gray-600", "mx-auto"))
        .withClasses("flex", "flex-row");
  }
}
