package views.components;

import static j2html.TagCreator.div;
import static org.assertj.core.api.Assertions.assertThat;

import j2html.tags.ContainerTag;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import views.style.ReferenceClasses;

public class AccordionFactoryTest {
  DivTag accordion;

  @Before
  public void setUp() {
    Optional<String> title = Optional.of("Test accordion title");
    Optional<ContainerTag> content = Optional.of(div("Some content text"));
    accordion = AccordionFactory.buildAccordion(title, content);
  }

  @Test
  public void buildAccordion_includesAccordionHeader() {
    String header = String.format("<div class=\"%s", ReferenceClasses.ACCORDION_HEADER);
    assertThat(accordion.render()).contains(header);
  }

  @Test
  public void buildAccordion_includesButtonInHeader() {
    String button =
        String.format("<button type=\"button\" class=\"%s", ReferenceClasses.ACCORDION_BUTTON);
    assertThat(accordion.render()).contains(button);
  }

  @Test
  public void buildAccordion_includesAccordionContainerDiv() {
    String containerDiv = String.format("<div class=\"%s", ReferenceClasses.ACCORDION);
    assertThat(accordion.render()).contains(containerDiv);
  }

  @Test
  public void buildAccordion_includesContentDiv() {
    String contentDiv =
        String.format("<div id=\"%s\" class=\"%s", "a1", ReferenceClasses.ACCORDION_CONTENT);
    assertThat(accordion.render()).contains(contentDiv);
  }

  @Test
  public void buildAccordion_hasRightNumberOfChildren() {
    assertThat(accordion.getNumChildren()).isEqualTo(2);
  }
}
