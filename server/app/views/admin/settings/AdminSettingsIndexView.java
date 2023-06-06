package views.admin.settings;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.span;

import j2html.tags.specialized.DivTag;
import javax.inject.Inject;
import play.twirl.api.Content;
import services.settings.SettingDescription;
import services.settings.SettingsManifest;
import services.settings.SettingsSection;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;

/** Displays application settings for the CiviForm Admin role. */
public final class AdminSettingsIndexView extends BaseHtmlView {

  private final SettingsManifest settingsManifest;
  private final AdminLayout layout;

  @Inject
  public AdminSettingsIndexView(
      SettingsManifest settingsManifest, AdminLayoutFactory layoutFactory) {
    this.settingsManifest = checkNotNull(settingsManifest);
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.REPORTING);
  }

  public Content render() {
    var mainContent = div();

    mainContent.with(renderTopSection(settingsManifest.getSections().get("Feature Flags")));
    mainContent.with(renderTopSection(settingsManifest.getSections().get("Branding")));
    mainContent.with(renderTopSection(settingsManifest.getSections().get("Custom Text")));
    mainContent.with(renderTopSection(settingsManifest.getSections().get("Email Addresses")));
    mainContent.with(renderTopSection(settingsManifest.getSections().get("Data Export API")));
    mainContent.with(renderTopSection(settingsManifest.getSections().get("Observability")));
    mainContent.with(renderTopSection(settingsManifest.getSections().get("External Services")));

    return layout.render(layout.getBundle().addMainContent(mainContent));
  }

  private DivTag renderTopSection(SettingsSection settingsSection) {
    var container = div();

    container.with(h3(settingsSection.sectionName()));
    settingsSection
        .settings()
        .forEach(settingDescription -> container.with(renderSetting(settingDescription)));

    settingsSection
        .subsections()
        .forEach(subsection -> container.with(renderSubSection(subsection)));

    return container;
  }

  private DivTag renderSubSection(SettingsSection settingsSection) {
    var container = div();

    container.with(h3(settingsSection.sectionName()));
    settingsSection
        .settings()
        .forEach(settingDescription -> container.with(renderSetting(settingDescription)));

    settingsSection
        .subsections()
        .forEach(subsection -> container.with(renderSubSection(subsection)));

    return container;
  }

  private DivTag renderSetting(SettingDescription settingDescription) {
    return div(
        span(settingDescription.variableName()),
        span(settingDescription.settingDescription()),
        span(settingsManifest.getSettingDisplayValue(settingDescription).orElse("-")));
  }
}
