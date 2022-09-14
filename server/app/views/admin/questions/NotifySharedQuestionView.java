package views.admin.questions;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.inject.Inject;

import static j2html.TagCreator.div;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.input;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import j2html.tags.specialized.DivTag;

import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.HtmlBundle;
import views.BaseHtmlView;
import play.twirl.api.Content;

public final class NotifySharedQuestionView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public NotifySharedQuestionView(
      AdminLayoutFactory layoutFactory
      ) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.QUESTIONS);
  }

  public Content render() {
    String title = "This question is shared by 2 programs";
    DivTag formContent = div().with(
        h1("This question is shared by 2 programs. If you edit it, it will be updated for both programs."),
        p("Please be aware that this will effect the following programs by either creating a new draft with this change or updating an existing draft:"),
        div().with(
            span("Program name 1"),
            span("Program name 2")
        ));
    HtmlBundle htmlBundle = layout.getBundle().setTitle(title).addMainContent(formContent);

    return layout.render(htmlBundle);
  }
}
