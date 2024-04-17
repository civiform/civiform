package views.admin.apikeys;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;
import static j2html.TagCreator.text;

import annotations.BindingAnnotations.EnUsLang;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import java.util.Optional;
import modules.MainModule;
import play.data.DynamicForm;
import play.i18n.Messages;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.apikey.ApiKeyService;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.LinkElement;
import views.components.ToastMessage;

/** Renders a page for adding a new ApiKey. */
public final class ApiKeyNewOneView extends BaseHtmlView {
  private final AdminLayout layout;
  private final Messages enUsMessages;

  private static final String EXPIRATION_DESCRIPTION =
      "Specify a date when this API key will no longer be valid. The expiration date"
          + " may be edited after the API key is created. It is strongly recommended to"
          + " allow API keys to expire and create new ones to ensure that if a key"
          + " has been stolen, malicious users will eventually lose access. For"
          + " highly sensitive data, expiring keys once a week or once a month is"
          + " recommended. Once a year is acceptable in most situations.";
  private static final DomContent[] SUBNET_DESCRIPTION = {
    text("Specify one or more comma-separated subnets using "),
    new LinkElement()
        .setText("CIDR notation")
        .setHref("https://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing")
        .opensInNewTab()
        .asAnchorText(),
    text(
        " that is allowed to call the API. The"
            + " subnet may not be edited after the API key is created. A single IP address"
            + " can be specified with a mask of /32. For example, \"8.8.8.8/32\""
            + " allows only the IP 8.8.8.8 to use the API key. All IP addresses can be"
            + " allowed, but this is strongly discouraged for security reasons because"
            + " it would allow any computer connected to the internet to use the key"
            + " if they obtain it. All IP addresses can be allowed with a value of"
            + " 0.0.0.0/0, though this is not recommended.")
  };

  @Inject
  public ApiKeyNewOneView(AdminLayoutFactory layoutFactory, @EnUsLang Messages enUsMessages) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.API_KEYS);
    this.enUsMessages = checkNotNull(enUsMessages);
  }

  public Content render(Request request, ImmutableSet<String> programNames) {
    return render(
        request,
        programNames,
        /* dynamicForm= */ Optional.empty(),
        /* toastMessage= */ Optional.empty());
  }

  public Content render(
      Request request,
      ImmutableSet<String> programNames,
      Optional<DynamicForm> dynamicForm,
      Optional<ToastMessage> toastMessage) {
    String title = "Create a new API key";

    FormTag formTag =
        form()
            .withMethod("POST")
            .with(
                makeCsrfTokenInputTag(request),
                h2("Name"),
                p("A human-readable name for identifying this API key in the UI."),
                setStateIfPresent(
                        FieldWithLabel.input()
                            .setFieldName(ApiKeyService.FORM_FIELD_NAME_KEY_NAME)
                            .setId(ApiKeyService.FORM_FIELD_NAME_KEY_NAME)
                            .setLabelText("API key name"),
                        dynamicForm,
                        ApiKeyService.FORM_FIELD_NAME_KEY_NAME)
                    .getInputTag(),
                h2("Expiration date"),
                p(EXPIRATION_DESCRIPTION),
                setStateIfPresent(
                        FieldWithLabel.date()
                            .setFieldName(ApiKeyService.FORM_FIELD_NAME_EXPIRATION)
                            .setId(ApiKeyService.FORM_FIELD_NAME_EXPIRATION)
                            .setLabelText("Expiration date"),
                        dynamicForm,
                        ApiKeyService.FORM_FIELD_NAME_EXPIRATION)
                    .getDateTag(),
                h2("Allowed IP addresses"),
                p(SUBNET_DESCRIPTION),
                setStateIfPresent(
                        FieldWithLabel.input()
                            .setFieldName(ApiKeyService.FORM_FIELD_NAME_SUBNET)
                            .setId(ApiKeyService.FORM_FIELD_NAME_SUBNET)
                            .setLabelText("API key subnet"),
                        dynamicForm,
                        ApiKeyService.FORM_FIELD_NAME_SUBNET)
                    .getInputTag());

    formTag.with(h2("Allowed programs"), p("Select the programs this key grants read access to."));

    for (String name :
        programNames.stream()
            .sorted(String::compareToIgnoreCase)
            .collect(ImmutableList.toImmutableList())) {
      formTag.with(
          FieldWithLabel.checkbox()
              .setFieldName(programReadGrantFieldName(name))
              .setLabelText(name)
              .setId(MainModule.SLUGIFIER.slugify(name))
              .setValue("true")
              .setChecked(
                  dynamicForm
                      .map(form -> form.value(programReadGrantFieldName(name)).isPresent())
                      .orElse(false))
              .getCheckboxTag());
    }

    DivTag contentDiv =
        div()
            .withClasses("px-20")
            .with(
                h1(title).withClasses("my-4"),
                formTag
                    .with(
                        submitButton("Save")
                            .withClasses(ButtonStyles.SOLID_BLUE)
                            .withId("apikey-submit-button"))
                    .withAction(routes.AdminApiKeysController.create().url()));

    HtmlBundle htmlBundle = layout.getBundle(request).setTitle(title).addMainContent(contentDiv);

    toastMessage.ifPresent(htmlBundle::addToastMessages);

    return layout.renderCentered(htmlBundle);
  }

  private String programReadGrantFieldName(String name) {
    return "grant-program-read[" + MainModule.SLUGIFIER.slugify(name) + "]";
  }

  private FieldWithLabel setStateIfPresent(
      FieldWithLabel field, Optional<DynamicForm> maybeForm, String key) {
    if (maybeForm.isEmpty()) {
      return field;
    }

    DynamicForm form = maybeForm.get();
    field.setValue(form.value(key).map(String::valueOf));

    if (form.error(key).isPresent()) {
      field.setFieldErrors(enUsMessages, form.error(key).get());
    }

    return field;
  }
}
