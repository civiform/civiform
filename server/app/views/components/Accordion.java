package views.components;

import static j2html.TagCreator.div;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.SvgTag;

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

  public DivTag getContainer() {
    DivTag accordion =
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

    DivTag titleContainer =
        div().withClasses(ReferenceClasses.ACCORDION_HEADER, Styles.RELATIVE);
    DivTag titleDiv = div(this.title).withClasses(Styles.TEXT_XL, Styles.FONT_LIGHT);

    SvgTag accordionSvg =
        Icons.svg(Icons.ACCORDION_BUTTON_PATH, 24)
            .withClasses(Styles.H_6, Styles.W_6)
            .attr("fill", "none")
            .attr("stroke-width", "2")
            .attr("stroke-linecap", "round")
            .attr("stroke-linejoin", "round");
    DivTag accordionButton =
        div(accordionSvg)
            .withClasses(
                ReferenceClasses.ACCORDION_BUTTON,
                Styles.TRANSITION_ALL,
                Styles.DURATION_300,
                Styles.ABSOLUTE,
                Styles.TOP_1,
                Styles.RIGHT_2,
                Styles.TRANSFORM);
    titleContainer.with(titleDiv, accordionButton);

    DivTag contentContainer =
        div()
            .with(this.content)
            .withClasses(ReferenceClasses.ACCORDION_CONTENT, Styles.H_0, Styles.OVERFLOW_HIDDEN);
    accordion.with(titleContainer, contentContainer);

    return accordion;
  }
}
