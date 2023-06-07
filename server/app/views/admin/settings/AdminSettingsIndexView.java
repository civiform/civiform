package views.admin.settings;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.input;
import static j2html.TagCreator.rawHtml;

import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.DivTag;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import modules.MainModule;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.AttributeProviderContext;
import org.commonmark.renderer.html.AttributeProviderFactory;
import org.commonmark.renderer.html.HtmlRenderer;
import play.twirl.api.Content;
import services.settings.SettingDescription;
import services.settings.SettingsManifest;
import services.settings.SettingsSection;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.style.StyleUtils;

/** Displays application settings for the CiviForm Admin role. */
public final class AdminSettingsIndexView extends BaseHtmlView {

  public static final String SECTION_STYLES = "grid grid-flow-row-dense grid-cols-2 gap-8";
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
    var settingsManifestContent = div().withClasses("my-10");
    var settingsContent =
        div(renderSideNav(), settingsManifestContent).withClasses("flex", "flex-grow");
    var mainContent =
        div(settingsContent)
            .withClasses(
                "flex", "flex-grow", "flex-col", "px-2", StyleUtils.responsive2XLarge("px-16"));

    SECTIONS.forEach(
        sectionName ->
            settingsManifestContent.with(
                renderTopSection(settingsManifest.getSections().get(sectionName))));

    return layout.render(layout.getBundle().addMainContent(mainContent));
  }

  private DivTag renderSideNav() {
    var container = div().withClasses("relative", "py-6", "w-2/12");
    var subContainer = div(div("Settings").withClasses("text-3xl", "py-6")).withClasses("fixed");

    SECTIONS.forEach(
        sectionName ->
            subContainer.with(
                a(sectionName)
                    .withClasses("block", "bold", "py-1")
                    .withHref("#" + MainModule.SLUGIFIER.slugify(sectionName))));

    return container.with(subContainer);
  }

  private DivTag renderTopSection(SettingsSection settingsSection) {
    var container = div().withId(MainModule.SLUGIFIER.slugify(settingsSection.sectionName()));

    container.with(h3(settingsSection.sectionName()).withClasses("text-xl font-bold py-4"));
    var settingsContainer = div().withClasses(SECTION_STYLES);
    container.with(settingsContainer);
    settingsSection.settings().stream()
        .filter(SettingDescription::shouldDisplay)
        .forEach(settingDescription -> settingsContainer.with(renderSetting(settingDescription)));

    settingsSection.subsections().stream()
        .filter(SettingsSection::shouldDisplay)
        .forEach(subsection -> container.with(renderSubSection(subsection)));

    return container;
  }

  private DivTag renderSubSection(SettingsSection settingsSection) {
    var container = div();

    container.with(h3(settingsSection.sectionName()).withClasses("text-l font-bold py-4"));
    var settingsContainer = div().withClasses(SECTION_STYLES);
    container.with(settingsContainer);

    settingsSection.settings().stream()
        .filter(SettingDescription::shouldDisplay)
        .forEach(settingDescription -> settingsContainer.with(renderSetting(settingDescription)));

    settingsSection.subsections().stream()
        .filter(SettingsSection::shouldDisplay)
        .forEach(subsection -> container.with(renderSubSection(subsection)));

    return container;
  }

  private static class LinkAttributeProvider implements AttributeProvider {
    @Override
    public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
      if (node instanceof Link) {
        attributes.put("class", "text-blue-600");
      }
    }
  }

  private DivTag renderSetting(SettingDescription settingDescription) {
    Optional<String> value = settingsManifest.getSettingDisplayValue(settingDescription);
    Parser parser = Parser.builder().build();
    Node descriptionNode = parser.parse(settingDescription.settingDescription());
    HtmlRenderer renderer =
        HtmlRenderer.builder()
            .attributeProviderFactory(
                new AttributeProviderFactory() {
                  @Override
                  public AttributeProvider create(AttributeProviderContext context) {
                    return new LinkAttributeProvider();
                  }
                })
            .build();

    String renderedDescriptionHtml = renderer.render(descriptionNode);

    return div(
            div(settingDescription.variableName()).withClasses("font-semibold"),
            div(rawHtml(renderedDescriptionHtml)).withClasses("text-sm"),
            input()
                .withValue(value.orElse("empty"))
                .isReadonly()
                .isDisabled()
                .withClasses("w-full", "bg-slate-200", "mt-2", value.isEmpty() ? "italic" : ""))
        .withClasses("max-w-md");
  }
}
