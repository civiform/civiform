package views.tags;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Set;
import org.junit.Before;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

public abstract class BaseElementTagModelProcessorTest {
  private TemplateEngine templateEngine;

  @Before
  public void setup() {
    var stringTemplateResolver = new StringTemplateResolver();
    stringTemplateResolver.setTemplateMode(TemplateMode.HTML);
    stringTemplateResolver.setCacheable(false);

    templateEngine = new TemplateEngine();
    templateEngine.addTemplateResolver(stringTemplateResolver);
    templateEngine.addDialect(
        new AbstractProcessorDialect("CiviForm", "cf", 10) {
          @Override
          public Set<IProcessor> getProcessors(String dialectPrefix) {
            return getTestProcessors(getPrefix());
          }
        });
  }

  protected abstract Set<IProcessor> getTestProcessors(String prefix);

  private String normalizeHtml(String html) {
    return html.replaceAll("\\s+", " ") // All whitespace becomes single space
        .replaceAll("\\s+>", ">") // Remove space before >
        .replaceAll(">\\s+<", "><") // Remove space between tags
        .trim();
  }

  protected void assertHtml(String template, String expected) {
    assertHtml(new Context(), template, expected);
  }

  protected void assertHtml(Object model, String template, String expected) {
    var context = new Context();
    context.setVariable("model", model);
    assertHtml(context, template, expected);
  }

  private void assertHtml(Context context, String template, String expected) {
    String actual = templateEngine.process(template, context);
    assertThat(normalizeHtml(actual)).isEqualTo(normalizeHtml(expected));
  }

  protected void assertException(String template, Class<? extends Throwable> expectedClass) {
    assertException(new Context(), template, expectedClass);
  }

  protected void assertException(
      Object model, String template, Class<? extends Throwable> expectedClass) {
    var context = new Context();
    context.setVariable("model", model);
    assertException(context, template, expectedClass);
  }

  private void assertException(
      Context context, String template, Class<? extends Throwable> expectedClass) {
    assertThatExceptionOfType(TemplateInputException.class)
        .isThrownBy(() -> templateEngine.process(template, context))
        .withRootCauseInstanceOf(expectedClass);
  }
}
