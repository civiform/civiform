package views.admin.settings;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.input;
import static j2html.TagCreator.rawHtml;
import static play.mvc.Http.HttpVerbs.POST;
import static services.settings.AbstractSettingsManifest.FEATURE_FLAG_SETTING_SECTION_NAME;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import javax.inject.Inject;
import modules.MainModule;
import play.mvc.Http;
import play.twirl.api.Content;
import services.settings.SettingDescription;
import services.settings.SettingMode;
import services.settings.SettingsManifest;
import services.settings.SettingsSection;
import views.BaseHtmlView;
import views.CiviFormMarkdown;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.components.FieldWithLabel;
import views.components.ToastMessage;
import views.style.StyleUtils;

/** Displays application settings for the CiviForm Admin role. */
public final class AdminSettingsIndexView extends BaseHtmlView {

  private static final String FORM_ID = "settings-update-form";
  private static final String SECTION_STYLES = "grid grid-flow-row-dense grid-cols-2 gap-8";
  private final SettingsManifest settingsManifest;
  private final AdminLayout layout;
  private final CiviFormMarkdown civiFormMarkdown;

  private static final ImmutableList<String> SECTIONS =
      ImmutableList.of(
          FEATURE_FLAG_SETTING_SECTION_NAME,
          "Branding",
          "Custom Text",
          "Email Addresses",
          "Data Export API",
          "Observability",
          "External Services");

  @Inject
  public AdminSettingsIndexView(
      SettingsManifest settingsManifest,
      AdminLayoutFactory layoutFactory,
      CiviFormMarkdown civiFormMarkdown) {
    this.settingsManifest = checkNotNull(settingsManifest);
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.SETTINGS);
    this.civiFormMarkdown = checkNotNull(civiFormMarkdown);
  }

  public Content render(Http.Request request) {
    var settingsManifestContent =
        form(makeCsrfTokenInputTag(request))
            .withAction(routes.AdminSettingsController.update().url())
            .withMethod(POST)
            .withId(FORM_ID)
            .withClasses("my-10");
    var settingsContent =
        div(renderSideNav(), settingsManifestContent).withClasses("flex", "flex-grow");
    var mainContent =
        div(settingsContent)
            .withClasses(
                "flex", "flex-grow", "flex-col", "px-2", StyleUtils.responsive2XLarge("px-16"));

    SECTIONS.forEach(
        sectionName ->
            settingsManifestContent.with(
                renderTopSection(request, settingsManifest.getSections().get(sectionName))));

    HtmlBundle bundle = layout.getBundle().addMainContent(mainContent);

    request
        .flash()
        .get("success")
        .ifPresent(successMessage -> bundle.addToastMessages(ToastMessage.success(successMessage)));
    request
        .flash()
        .get("warning")
        .ifPresent(warningMessage -> bundle.addToastMessages(ToastMessage.warning(warningMessage)));

    return layout.render(bundle);
  }

  private DivTag renderSideNav() {
    var container = div().withClasses("relative", "p-6", "w-2/12");
    var subContainer =
        div(h1("Settings").withClasses("text-3xl", "py-6"))
            .withId("admin-settings-side-nav")
            .withClasses("fixed");

    SECTIONS.forEach(
        sectionName ->
            subContainer.with(
                a(sectionName)
                    .withClasses("block", "bold", "py-1")
                    .withHref("#" + MainModule.SLUGIFIER.slugify(sectionName))));

    subContainer.with(submitButton("Save changes").withClasses("mt-12").withForm(FORM_ID));

    return container.with(subContainer);
  }

  private DivTag renderTopSection(Http.Request request, SettingsSection settingsSection) {
    var container = div();

    container.with(
        h2(settingsSection.sectionName())
            .withId(MainModule.SLUGIFIER.slugify(settingsSection.sectionName()))
            .withClasses("text-xl font-bold mt-4 mb-2 leading-8 pt-4 border-b-2"));
    return renderSectionContents(request, settingsSection, container);
  }

  private DivTag renderSubSection(Http.Request request, SettingsSection settingsSection) {
    var container = div();

    container.with(h3(settingsSection.sectionName()).withClasses("text-l font-bold py-2 mt-4"));
    return renderSectionContents(request, settingsSection, container);
  }

  private DivTag renderSectionContents(
      Http.Request request, SettingsSection settingsSection, DivTag container) {
    var settingsContainer = div().withClasses(SECTION_STYLES);
    container.with(settingsContainer);
    settingsSection.settings().stream()
        .filter(SettingDescription::shouldDisplay)
        .forEach(
            settingDescription ->
                settingsContainer.with(renderSetting(request, settingDescription)));

    settingsSection.subsections().stream()
        .filter(SettingsSection::shouldDisplay)
        .forEach(subsection -> container.with(renderSubSection(request, subsection)));

    return container;
  }

  private DivTag renderSetting(Http.Request request, SettingDescription settingDescription) {
    String renderedDescriptionHtml =
        civiFormMarkdown.render(settingDescription.settingDescription());

    return div(
            div(settingDescription.variableName()).withClasses("font-semibold", "break-all"),
            div(rawHtml(renderedDescriptionHtml)).withClasses("text-sm"),
            renderSettingInput(request, settingDescription))
        .withData("testid", String.format("%s-container", settingDescription.variableName()))
        .withClasses("max-w-md");
  }

  private DomContent renderSettingInput(
      Http.Request request, SettingDescription settingDescription) {
    Optional<String> value =
        settingsManifest
            .getSettingDisplayValue(request, settingDescription)
            .filter(v -> !v.isBlank());

    if (settingDescription.settingMode().equals(SettingMode.ADMIN_READABLE)) {
      return input()
          .withValue(value.orElse("empty"))
          .isReadonly()
          .isDisabled()
          .withClasses("px-1", "w-full", "bg-slate-200", "mt-2", value.isEmpty() ? "italic" : "");
    }

    boolean isEnabled = value.map("TRUE"::equals).orElse(false);

    return div(
            FieldWithLabel.radio()
                .setFieldName(settingDescription.variableName())
                .setLabelText("Enabled")
                .setChecked(isEnabled)
                .setValue("true")
                .addStyleClass("mr-4")
                .getRadioTag()
                .withData("testid", String.format("enable-%s", settingDescription.variableName())),
            FieldWithLabel.radio()
                .setFieldName(settingDescription.variableName())
                .setLabelText("Disabled")
                .setChecked(!isEnabled)
                .setValue("false")
                .getRadioTag()
                .withData("testid", String.format("disable-%s", settingDescription.variableName())))
        .withClasses("flex", "mt-2");
  }
}
