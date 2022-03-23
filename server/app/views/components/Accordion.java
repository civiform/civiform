package views.components;

import static j2html.TagCreator.div;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import java.util.ArrayList;
import java.util.Arrays;
import views.style.ReferenceClasses;
import views.style.Styles;

/**
 * Utility class for rendering an accordion.
 *
 * <p>See {@link TextFormatter}.
 */
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
                ReferenceClasses.ACCORDION,
                Styles.BG_WHITE,
                Styles.MY_4,
                Styles.P_4,
                Styles.ROUNDED_LG,
                Styles.SHADOW_MD,
                Styles.BORDER,
                Styles.BORDER_GRAY_300);

    ContainerTag titleContainer =
        div().withClasses(ReferenceClasses.ACCORDION_HEADER, Styles.RELATIVE);
    ContainerTag titleDiv = div(this.title).withClasses(Styles.TEXT_XL, Styles.FONT_LIGHT);

    ContainerTag accordionSvg =
        Icons.svg(Icons.ACCORDION_BUTTON_PATH, 24)
            .withClasses(Styles.H_6, Styles.W_6)
            .attr("fill", "none")
            .attr("stroke-width", "2")
            .attr("stroke-linecap", "round")
            .attr("stroke-linejoin", "round");
    ContainerTag accordionButton =
        div(accordionSvg)
            .withClasses(
                ReferenceClasses.ACCORDION_BUTTON,
                Styles.TRANSITION_ALL,
                Styles.DURATION_300,
                Styles.ABSOLUTE,
                Styles.TOP_1,
                Styles.RIGHT_2);
    titleContainer.with(titleDiv, accordionButton);

    ContainerTag contentContainer =
        div()
            .with(this.content)
            .withClasses(ReferenceClasses.ACCORDION_CONTENT, Styles.H_0, Styles.OVERFLOW_HIDDEN);
    accordion.with(titleContainer, contentContainer);

    return accordion;
  }
}
