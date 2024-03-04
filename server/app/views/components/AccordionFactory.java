package views.components;

import static j2html.TagCreator.button;
import static j2html.TagCreator.div;

import j2html.tags.ContainerTag;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import views.style.ReferenceClasses;

/**
 * Utility class for building an accordion. This is a modified version of the USWDS accordion
 * component here: https://designsystem.digital.gov/components/accordion/.
 */
public final class AccordionFactory {

  public static DivTag buildAccordion(Optional<String> title, Optional<ContainerTag> content) {
    DivTag accordion =
        div()
            .withClasses(
                ReferenceClasses.ACCORDION,
                "usa-accordion",
                "my-4",
                "rounded-lg",
                "shadow-md",
                "border",
                "border-gray-300");

    ContainerTag headerDiv =
        div()
            .withClasses(ReferenceClasses.ACCORDION_HEADER, "usa-accordion__heading", "text-xl")
            .with(
                button(title.orElseGet(() -> ""))
                    .withType("button")
                    .withClasses(
                        ReferenceClasses.ACCORDION_BUTTON,
                        "usa-accordion__button",
                        // Negating all the inherited button styles from styles.css
                        "bg-white",
                        "text-black",
                        "px-5",
                        "py-4",
                        "rounded-lg",
                        "border-0",
                        "hover:bg-gray-100")
                    .attr("aria-expanded", "false")
                    .attr("aria-controls", "a1"));

    ContainerTag contentDiv =
        div()
            .withId("a1")
            .withClasses(
                ReferenceClasses.ACCORDION_CONTENT,
                "usa-accordion__content",
                "usa-prose",
                "rounded-lg")
            .with(content.orElseGet(() -> new DivTag()));

    return accordion.with(headerDiv, contentDiv);
  }
}
