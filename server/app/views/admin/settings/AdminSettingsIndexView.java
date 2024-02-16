package views.admin.settings;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.rawHtml;
import static play.mvc.Http.HttpVerbs.POST;
import static services.settings.AbstractSettingsManifest.FEATURE_FLAG_SETTING_SECTION_NAME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalInt;
import javax.inject.Inject;
import modules.MainModule;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.twirl.api.Content;
import services.settings.SettingDescription;
import services.settings.SettingsManifest;
import services.settings.SettingsSection;
import services.settings.SettingsService.SettingsGroupUpdateResult.UpdateError;
import views.BaseHtmlView;
import views.CiviFormMarkdown;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.components.FieldWithLabel;
import views.components.SelectWithLabel;
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.StyleUtils;

/** Displays application settings for the CiviForm Admin role. */
public final class AdminSettingsIndexView extends BaseHtmlView {

  private static final String FORM_ID = "settings-update-form";
  private static final String SECTION_STYLES =
      "grid grid-flow-row-dense lg:grid-cols-2 grid-cols-1 gap-8";
  public static final DivTag READ_ONLY_TEXT =
      div("Read-only").withClasses("text-xs", "mt-2", "text-gray-600");

  private final SettingsManifest settingsManifest;
  private final AdminLayout layout;
  private final CiviFormMarkdown civiFormMarkdown;
  private final MessagesApi messagesApi;

  private static final ImmutableList<String> SECTIONS =
      ImmutableList.of(
          FEATURE_FLAG_SETTING_SECTION_NAME,
          "Branding",
          "Custom text",
          "Email addresses",
          "Data export API",
          "Observability",
          "External services",
          "Miscellaneous");

  @Inject
  public AdminSettingsIndexView(
      SettingsManifest settingsManifest,
      AdminLayoutFactory layoutFactory,
      CiviFormMarkdown civiFormMarkdown,
      MessagesApi messagesApi) {
    this.settingsManifest = checkNotNull(settingsManifest);
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.SETTINGS);
    this.civiFormMarkdown = checkNotNull(civiFormMarkdown);
    this.messagesApi = checkNotNull(messagesApi);
  }

  public Content render(Http.Request request) {
    return render(request, /* errorMessages= */ Optional.empty());
  }

  public Content render(
      Http.Request request, Optional<ImmutableMap<String, UpdateError>> errorMessages) {
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
                renderTopSection(
                    request, errorMessages, settingsManifest.getSections().get(sectionName))));

    HtmlBundle bundle = layout.getBundle(request).addMainContent(mainContent);

    if (errorMessages.isPresent()) {
      bundle.addToastMessages(
          ToastMessage.error(
              "That update didn't look quite right. Please fix the errors in the form and try"
                  + " saving again.",
              messagesApi.preferred(request)));
    }
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
    var container = div().withClasses("relative", "md:px-4", "px-0", "xl:w-2/12", "w-3/12");
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

  private DivTag renderTopSection(
      Http.Request request,
      Optional<ImmutableMap<String, UpdateError>> errorMessages,
      SettingsSection settingsSection) {
    var container = div();

    container.with(
        h2(settingsSection.sectionName())
            .withId(MainModule.SLUGIFIER.slugify(settingsSection.sectionName()))
            .withClasses("text-xl font-bold mt-4 mb-2 leading-8 pt-4 border-b-2"));
    return renderSectionContents(request, errorMessages, settingsSection, container);
  }

  private DivTag renderSubSection(
      Http.Request request,
      Optional<ImmutableMap<String, UpdateError>> errorMessages,
      SettingsSection settingsSection) {
    var container = div();

    container.with(h3(settingsSection.sectionName()).withClasses("text-l font-bold py-2 mt-4"));
    return renderSectionContents(request, errorMessages, settingsSection, container);
  }

  private DivTag renderSectionContents(
      Http.Request request,
      Optional<ImmutableMap<String, UpdateError>> errorMessages,
      SettingsSection settingsSection,
      DivTag container) {
    var settingsContainer = div().withClasses(SECTION_STYLES);
    container.with(settingsContainer);
    settingsSection.settings().stream()
        .filter(SettingDescription::shouldDisplay)
        .sorted(Comparator.comparing(SettingDescription::variableName))
        .forEach(
            settingDescription ->
                settingsContainer.with(renderSetting(request, errorMessages, settingDescription)));

    settingsSection.subsections().stream()
        .filter(SettingsSection::shouldDisplay)
        .sorted(Comparator.comparing(SettingsSection::sectionName))
        .forEach(
            subsection -> container.with(renderSubSection(request, errorMessages, subsection)));

    return container;
  }

  private DivTag renderSetting(
      Http.Request request,
      Optional<ImmutableMap<String, UpdateError>> errorMessages,
      SettingDescription settingDescription) {
    String renderedDescriptionHtml =
        civiFormMarkdown.render(settingDescription.settingDescription());

    return div(
            div(settingDescription.variableName()).withClasses("font-semibold", "break-all"),
            div(rawHtml(renderedDescriptionHtml)).withClasses("text-sm"),
            renderSettingInput(request, errorMessages, settingDescription))
        .withData("testid", String.format("%s-container", settingDescription.variableName()))
        .withClasses("md:max-w-lg", "max-w-md");
  }

  private DomContent renderSettingInput(
      Http.Request request,
      Optional<ImmutableMap<String, UpdateError>> errorMessages,
      SettingDescription settingDescription) {
    Optional<String> value =
        settingsManifest
            .getSettingDisplayValue(request, settingDescription)
            .filter(v -> !v.isBlank());

    switch (settingDescription.settingType()) {
      case BOOLEAN:
        return renderBoolInput(settingDescription, value);
      case LIST_OF_STRINGS:
      case STRING:
        return renderStringInput(settingDescription, value, errorMessages);
      case ENUM:
        return renderEnumInput(settingDescription, value);
      case INT:
        return renderIntInput(settingDescription, value);
      default:
        throw new IllegalStateException(
            String.format(
                "Settings of type %s are not writeable", settingDescription.settingType()));
    }
  }

  private static DivTag renderIntInput(
      SettingDescription settingDescription, Optional<String> value) {
    FieldWithLabel field =
        FieldWithLabel.number()
            .setFieldName(settingDescription.variableName())
            .setPlaceholderText("empty")
            .setDisabled(settingDescription.isReadOnly());

    value.ifPresent((val) -> field.setValue(OptionalInt.of(Integer.parseInt(val))));

    return div(field.getNumberTag().condWith(settingDescription.isReadOnly(), READ_ONLY_TEXT))
        .withClasses("mt-2");
  }

  private static DivTag renderStringInput(
      SettingDescription settingDescription,
      Optional<String> value,
      Optional<ImmutableMap<String, UpdateError>> maybeErrorMessages) {
    Optional<UpdateError> maybeUpdateError =
        maybeErrorMessages.flatMap(
            errorMessages ->
                Optional.ofNullable(
                    errorMessages.getOrDefault(settingDescription.variableName(), null)));
    Optional<DivTag> errors =
        maybeUpdateError.map(
            updateError ->
                div(updateError.errorMessage()).withClasses(BaseStyles.FORM_ERROR_TEXT_XS));

    return div(FieldWithLabel.input()
            .setFieldName(settingDescription.variableName())
            .setValue(maybeUpdateError.map(UpdateError::updatedValue).orElse(value.orElse("")))
            .setPlaceholderText("empty")
            .setDisabled(settingDescription.isReadOnly())
            .getInputTag()
            .condWith(settingDescription.isReadOnly(), READ_ONLY_TEXT)
            .with(errors.orElse(null)))
        .withClasses("mt-2");
  }

  private static DivTag renderEnumInput(
      SettingDescription settingDescription, Optional<String> value) {
    SelectWithLabel selectWithLabel =
        new SelectWithLabel()
            .setFieldName(settingDescription.variableName())
            .setPlaceholderVisible(true)
            .setOptions(
                settingDescription.allowableValues().get().stream()
                    .map(
                        optionValue ->
                            SelectWithLabel.OptionValue.builder()
                                .setLabel(optionValue)
                                .setValue(optionValue)
                                .build())
                    .collect(ImmutableList.toImmutableList()))
            .setDisabled(settingDescription.isReadOnly());

    value.ifPresent(val -> selectWithLabel.setValue(val));

    return div(selectWithLabel
            .getSelectTag()
            .condWith(settingDescription.isReadOnly(), READ_ONLY_TEXT))
        .withClasses("mt-2");
  }

  private static DivTag renderBoolInput(
      SettingDescription settingDescription, Optional<String> value) {
    boolean isEnabled = value.map("TRUE"::equals).orElse(false);

    return div(div(
                FieldWithLabel.radio()
                    .setFieldName(settingDescription.variableName())
                    .setLabelText("Enabled")
                    .setChecked(isEnabled)
                    .setValue("true")
                    .addStyleClass("mr-4")
                    .setDisabled(settingDescription.isReadOnly())
                    .getRadioTag()
                    .withData(
                        "testid", String.format("enable-%s", settingDescription.variableName())),
                FieldWithLabel.radio()
                    .setFieldName(settingDescription.variableName())
                    .setLabelText("Disabled")
                    .setChecked(!isEnabled)
                    .setValue("false")
                    .setDisabled(settingDescription.isReadOnly())
                    .getRadioTag()
                    .withData(
                        "testid", String.format("disable-%s", settingDescription.variableName())))
            .withClasses("flex", "mt-2"))
        .condWith(settingDescription.isReadOnly(), READ_ONLY_TEXT);
  }
}
