package views.admin.ti;

import static j2html.TagCreator.button;
import static j2html.TagCreator.div;

import j2html.tags.specialized.DivTag;
import play.mvc.Http;
import play.twirl.api.Html;

public class TrustedIntermediaryTab2View {

  public Html render(Http.Request request) {

    DivTag div =
        div()
            .with(
                div()
                    .withClasses("tab-list")
                    .attr("role", "tablist")
                    .with(
                        button("Tab 1")
                            .attr("hx-get", "/tab1")
                            .attr("role", "tab")
                            .attr("aria-selected", "false")
                            .attr("aria-controls", "tab-content"),
                        button("Tab 2")
                            .attr("hx-get", "/tab2")
                            .withClasses("selected")
                            .attr("role", "tab")
                            .attr("aria-selected", "false")
                            .attr("aria-controls", "tab-content")),
                div("Oh what lovely tab content for tab 2")
                    .withId("tab-content")
                    .attr("role", "tabpanel")
                    .withClasses("tab-content"));

    Html html = new Html(div.render());

    return html;
  }
}
