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

/** Renders a page that displays an API key's crentials after it's created. */
public final class ApiKeyAfterCreateView extends BaseHtmlView {
  private final AdminLayout layout;
  private final Slugify slugifier = new Slugify();

  @Inject
  public ApiKeyAfterCreateView(AdminLayout layout) {
    this.layout = checkNotNull(layout);
  }


}
