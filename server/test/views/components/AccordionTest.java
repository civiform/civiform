package views.components;

import static j2html.TagCreator.div;
import static org.assertj.core.api.Assertions.assertThat;

import j2html.tags.specialized.DivTag;
import org.junit.Before;
import org.junit.Test;
import views.style.ReferenceClasses;

public class AccordionTest {
  Accordion accordion;
  DivTag accordionContainer;

  @Before
  public void setUp() {
    accordion = new Accordion();
    accordion.setTitle("Test accordion title");
    accordion.addContent(div("Some content text"));
    accordionContainer = accordion.getContainer();
  }

  @Test
  public void getContainer_includesAccordionHeaderButton() {
    String button =
        String.format("<button type=\"button\" class=\"%s", ReferenceClasses.ACCORDION_HEADER);
    assertThat(accordionContainer.render()).contains(button);
  }

  @Test
  public void getContainer_includesContentDiv() {
    String contentDiv =
        String.format(
            "<div id=\"%s\" class=\"%s",
            ReferenceClasses.ACCORDION_CONTENT, ReferenceClasses.ACCORDION_CONTENT);
    assertThat(accordionContainer.render()).contains(contentDiv);
  }

  @Test
  public void getContainer_hasRightNumberOfChildren() {
    assertThat(accordionContainer.getNumChildren()).isEqualTo(2);
  }
}
