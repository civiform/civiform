package views.components;

import static j2html.TagCreator.div;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.Tag;
import java.util.Arrays;
import java.util.ArrayList;

public class Accordion {
  protected String title = "";
  protected ArrayList<DomContent> content = new ArrayList<>();

  public Accordion setTitle(String title) {
    this.title = title;
    return this;
  }

  public Accordion addContent(DomContent... content) {
    this.content.addAll(Arrays.asList(content));
    return this;
  }

  public ContainerTag getContainer() {
    ContainerTag accordion =
        div()
            .withClasses(
                "cf-accordion bg-white my-4 p-4 rounded-lg shadow-md border border-gray-300");

    ContainerTag titleContainer = div().withClasses("relative");
    ContainerTag titleDiv = div(this.title).withClasses("text-xl font-light");

    ContainerTag accordionSvg =
        Icons.svg(Icons.ACCORDION_BUTTON_PATH, 24)
            .withClasses("h-6 w-6")
            .attr("fill", "none")
            .attr("stroke-width", "2")
            .attr("stroke-linecap", "round")
            .attr("stroke-linejoin", "round");
    ContainerTag accordionButton =
        div(accordionSvg)
            .withClasses(
                "cf-accordion-button transition-all duration-300 absolute top-1 right-2 transform");
    titleContainer.with(titleDiv, accordionButton);

    ContainerTag contentContainer =
        div().with(this.content).withClasses("cf-accordion-content h-0 overflow-hidden");
    accordion.with(titleContainer, contentContainer);

    return accordion;
  }
}
