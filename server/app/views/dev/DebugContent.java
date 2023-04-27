package views.dev;

import static j2html.TagCreator.div;
import static j2html.TagCreator.p;

import auth.FakeAdminClient;
import j2html.tags.specialized.DivTag;
import views.BaseHtmlView;

public class DebugContent extends BaseHtmlView {
  public static DivTag debugContent() {
    return div()
        .withClasses("flex", "flex-col")
        .with(
            p("DEVELOPMENT MODE TOOLS:").withClasses("text-2xl"),
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
            redirectButton(
                "feature-flags",
                "Feature Flags",
                controllers.dev.routes.FeatureFlagOverrideController.index().url()),
            redirectButton(
                "database-seed",
                "Seed Database",
                controllers.dev.routes.DatabaseSeedController.index().url()));
  }
}
