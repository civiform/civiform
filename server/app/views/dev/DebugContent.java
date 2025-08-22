package views.dev;

import static j2html.TagCreator.div;
import static j2html.TagCreator.p;

import auth.FakeAdminClient;
import j2html.tags.specialized.DivTag;
import views.BaseHtmlView;

/** All frontend debug content that we show when running CiviForm in dev mode. */
public final class DebugContent extends BaseHtmlView {
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
}
