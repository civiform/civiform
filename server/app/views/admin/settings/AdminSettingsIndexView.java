package views.admin.settings;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h3;

import com.google.common.collect.ImmutableList;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import javax.inject.Inject;
import modules.MainModule;
import play.twirl.api.Content;
import services.settings.SettingDescription;
import services.settings.SettingsManifest;
import services.settings.SettingsSection;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.components.TextFormatter;
import views.style.StyleUtils;

/** Displays application settings for the CiviForm Admin role. */
public final class AdminSettingsIndexView extends BaseHtmlView {

  private final SettingsManifest settingsManifest;
  private final AdminLayout layout;

  private static final ImmutableList<String> SECTIONS =
      ImmutableList.of(
          "Feature Flags",
          "Branding",
          "Custom Text",
          "Email Addresses",
          "Data Export API",
          "Observability",
          "External Services");

  @Inject
  public AdminSettingsIndexView(
      SettingsManifest settingsManifest, AdminLayoutFactory layoutFactory) {
    this.settingsManifest = checkNotNull(settingsManifest);
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.SETTINGS);
  }

  public Content render() {
    var settingsManifestContent = div();
    var settingsContent =
        div(renderSideNav(), settingsManifestContent).withClasses("flex", "flex-grow");
    var mainContent =
        div(div("Settings").withClasses("text-3xl", "pb-3"), settingsContent)
            .withClasses(
                "flex", "flex-grow", "flex-col", "px-2", StyleUtils.responsive2XLarge("px-16"));

    SECTIONS.forEach(
        sectionName ->
            settingsManifestContent.with(
                renderTopSection(settingsManifest.getSections().get(sectionName))));

    return layout.render(layout.getBundle().addMainContent(mainContent));
  }

  private DivTag renderSideNav() {
    var container = div().withClasses("relative", "pt-6", "w-2/12");
    var subContainer = div().withClasses("fixed");

    SECTIONS.forEach(
        sectionName ->
            subContainer.with(
                a(sectionName)
                    .withClasses("block")
                    .withHref("#" + MainModule.SLUGIFIER.slugify(sectionName))));

    return container.with(subContainer);
  }

  private DivTag renderTopSection(SettingsSection settingsSection) {
    var container = div().withId(MainModule.SLUGIFIER.slugify(settingsSection.sectionName()));

    container.with(h3(settingsSection.sectionName()).withClasses("text-xl font-bold py-2"));
    var settingsContainer = div().withClasses("grid grid-flow-row-dense grid-cols-3 gap-8");
    container.with(settingsContainer);
    settingsSection
        .settings()
        .forEach(settingDescription -> settingsContainer.with(renderSetting(settingDescription)));

    settingsSection
        .subsections()
        .forEach(subsection -> container.with(renderSubSection(subsection)));

    return container;
  }

  private DivTag renderSubSection(SettingsSection settingsSection) {
    var container = div();

    container.with(h3(settingsSection.sectionName()).withClasses("text-l font-bold py-2"));
    var settingsContainer = div().withClasses("grid grid-flow-row-dense grid-cols-3 gap-8");
    container.with(settingsContainer);

    settingsSection
        .settings()
        .forEach(settingDescription -> settingsContainer.with(renderSetting(settingDescription)));

    settingsSection
        .subsections()
        .forEach(subsection -> container.with(renderSubSection(subsection)));

    return container;
  }

  private DivTag renderSetting(SettingDescription settingDescription) {
    return div(
            div(settingDescription.variableName()).withClasses("font-semibold"),
            div(TextFormatter.formatText(
                        settingDescription.settingDescription(), /* preserveEmptyLines= */ false)
                    .toArray(DomContent[]::new))
                .withClasses("text-sm"),
            div(settingsManifest.getSettingDisplayValue(settingDescription).orElse("-")))
        .withClasses("max-w-md");
  }
}
