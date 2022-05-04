package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.p;

import com.github.slugify.Slugify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import j2html.tags.ContainerTag;
import play.data.DynamicForm;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.style.Styles;

/** Renders a page for adding a new ApiKey. */
public final class ApiKeyNewOneView extends BaseHtmlView {
  private final AdminLayout layout;
  private final Slugify slugifier = new Slugify();

  @Inject
  public ApiKeyNewOneView(AdminLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(Request request, DynamicForm form, ImmutableSet<String> programNames) {
    String title = "Create a new API key";

    ContainerTag formTag =
        form()
            .withMethod("POST")
            .with(
                makeCsrfTokenInputTag(request),
                FieldWithLabel.input().setFieldName("keyName").setLabelText("Name").getContainer(),
                h2("Expiration date"),
                p(
                    "Specify a date when this API key will no longer be valid. The expiration date"
                        + " may be edited after the API is created. It is strongly recommended to"
                        + " allow API keys to expire and create new ones to ensure that if a key"
                        + " has been stolen, malicious users will eventually lose access. For"
                        + " highly sensitive data, expiring keys once a week or once a month is"
                        + " recommended. Once a year is acceptable in most situations."),
                FieldWithLabel.date()
                    .setFieldName("expiration")
                    .setLabelText("Expiration date")
                    .getContainer(),
                h2("Allowed IP addresses"),
                p(
                    "Specify a subnet using CIDR notation that is allowed to call the API. The"
                        + " subnet may be edited after the API key is created. A single IP address"
                        + " can be specified with a mask of /32. For example, \"8.8.8.8/32\""
                        + " allows only the IP 8.8.8.8 to use the API key. All IP addresses can be"
                        + " allowed, but this is strongly discouraged for security reasons because"
                        + " it would allow any computer connected to the internet to use the key"
                        + " if they obtain it. All IP addresses can be allowed with a value of"
                        + " 0.0.0.0/0."),
                FieldWithLabel.input()
                    .setFieldName("subnet")
                    .setLabelText("Allowed subnet (CIDR notation)")
                    .getContainer());

    formTag.with(h2("Allowed programs"), p("Select the programs this key grants read access to."));

    for (String name : programNames.stream().sorted().collect(ImmutableList.toImmutableList())) {
      formTag.with(
          FieldWithLabel.checkbox()
              .setFieldName(programReadGrantFieldName(name))
              .setLabelText(name)
              .setValue("true")
              .getContainer());
    }

    ContainerTag contentDiv =
        div()
            .withClasses(Styles.PX_20)
            .with(
                h1(title).withClasses(Styles.MY_4),
                formTag
                    .with(submitButton("Save").withId("apikey-submit-button"))
                    .withAction(controllers.admin.routes.AdminApiKeysController.create().url()));

    HtmlBundle htmlBundle = layout.getBundle().setTitle(title).addMainContent(contentDiv);

    return layout.renderCentered(htmlBundle);
  }

  private String programReadGrantFieldName(String name) {
    return "grant-program-read[" + slugifier.slugify(name) + "]";
  }
}
