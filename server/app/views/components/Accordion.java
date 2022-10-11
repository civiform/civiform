package views.components;

import static j2html.TagCreator.div;

import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import java.util.ArrayList;
import java.util.Arrays;
import views.style.ReferenceClasses;


/**
 * Utility class for rendering an accordion.
 *
 * <p>See {@link TextFormatter}.
 */
public final class Accordion {
  private String title = "";
  private ArrayList<DomContent> content = new ArrayList<>();

  public Accordion setTitle(String title) {
    this.title = title;
    return this;
  }

  public Accordion addContent(DomContent... content) {
    this.content.addAll(Arrays.asList(content));
    return this;
  }

  public DivTag getContainer() {
    DivTag accordion =
        div()
            .withClasses(
                ReferenceClasses.ACCORDION,
                "bg-white",
                "my-4",
                "p-4",
                "rounded-lg",
                "shadow-md",
                "border",
                "border-gray-300");

    DivTag titleContainer = div().withClasses(ReferenceClasses.ACCORDION_HEADER, "relative");
    DivTag titleDiv = div(this.title).withClasses("text-xl", "font-light");

    SvgTag accordionSvg =
        Icons.svg(Icons.ACCORDION_BUTTON)
            .withClasses("h-6", "w-6")
            .attr("fill", "none")
            .attr("stroke-width", "2")
            .attr("stroke-linecap", "round")
            .attr("stroke-linejoin", "round");
    DivTag accordionButton =
        div(accordionSvg)
            .withClasses(
                ReferenceClasses.ACCORDION_BUTTON,
                "transition-all",
                "duration-300",
                "absolute",
                "top-1",
                "right-2",
                "transform");
    titleContainer.with(titleDiv, accordionButton);

    DivTag contentContainer =
        div()
            .with(this.content)
            .withClasses(ReferenceClasses.ACCORDION_CONTENT, "h-0", "overflow-hidden");
    accordion.with(titleContainer, contentContainer);

    return accordion;
  }
}
