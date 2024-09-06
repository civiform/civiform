package views.tags;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import controllers.AssetsFinder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import play.Environment;
import play.Mode;
import repository.ResetPostgres;

public class IconElementTagProcessorTest extends ResetPostgres {
  @Mock private ITemplateContext mockContext;
  @Mock private IProcessableElementTag mockTag;
  @Mock private IElementTagStructureHandler mockStructureHandler;

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void renderTagWithClass() {
    IconElementTagProcessor tagProcessor =
        new IconElementTagProcessor(
            "cf", instanceOf(AssetsFinder.class), instanceOf(Environment.class));
    when(mockTag.getAttributeValue("type")).thenReturn("icon-https");
    when(mockTag.hasAttribute("class")).thenReturn(true);
    when(mockTag.getAttributeValue("class")).thenReturn("aaa bbb ccc");

    tagProcessor.doProcess(mockContext, mockTag, mockStructureHandler);

    String expected =
        """
<img src="/assets/Images/uswds/[a-zA-Z0-9]+-icon-https.svg" aria-hidden="true" role="img" class="aaa bbb ccc" alt="">
""";

    verify(mockStructureHandler).replaceWith(matches(expected.trim()), eq(false));
  }

  @Test
  public void renderTagWithoutClass() {
    IconElementTagProcessor tagProcessor =
        new IconElementTagProcessor(
            "cf", instanceOf(AssetsFinder.class), instanceOf(Environment.class));
    when(mockTag.getAttributeValue("type")).thenReturn("icon-https");

    tagProcessor.doProcess(mockContext, mockTag, mockStructureHandler);

    String expected =
        """
<img src="/assets/Images/uswds/[a-zA-Z0-9]+-icon-https.svg" aria-hidden="true" role="img" alt="">
""";

    verify(mockStructureHandler).replaceWith(matches(expected.trim()), eq(false));
  }

  @Test
  public void throwsWhenIconIsNotFound_NonProdEnvironment() {
    IconElementTagProcessor tagProcessor =
        new IconElementTagProcessor(
            "cf", instanceOf(AssetsFinder.class), instanceOf(Environment.class));
    when(mockTag.getAttributeValue("type")).thenReturn("non-existent");

    assertThatThrownBy(() -> tagProcessor.doProcess(mockContext, mockTag, mockStructureHandler));
  }

  @Test
  public void renderEmptyTagWhenIconIsNotFound_ProdEnvironment() {
    Environment environment = new Environment(Mode.PROD);
    IconElementTagProcessor tagProcessor =
        new IconElementTagProcessor("cf", instanceOf(AssetsFinder.class), environment);
    when(mockTag.getAttributeValue("type")).thenReturn("non-existent");

    tagProcessor.doProcess(mockContext, mockTag, mockStructureHandler);

    verify(mockStructureHandler).replaceWith("", false);
  }
}
