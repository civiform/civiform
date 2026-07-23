package support.thymeleaf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.testing.templateengine.context.IProcessingContextBuilder;
import org.thymeleaf.testing.templateengine.context.ITestContext;
import org.thymeleaf.testing.templateengine.context.ITestContextExpression;
import org.thymeleaf.testing.templateengine.exception.TestEngineExecutionException;
import org.thymeleaf.testing.templateengine.testable.ITest;

/**
 * Builds the Thymeleaf context for a {@code .thtest} file without any servlet/web dependencies.
 *
 * <p>thymeleaf-testing only ships a web context builder, which requires a mocked servlet API that
 * CiviForm doesn't use. Variables declared in the {@code %CONTEXT} section are OGNL expressions and
 * are evaluated here the same way the web builder evaluates them.
 */
public final class NonWebProcessingContextBuilder implements IProcessingContextBuilder {

  private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

  @Override
  public IContext build(ITest test) {
    if (test == null) {
      return null;
    }

    ITestContext testContext = test.getContext();
    Locale locale = resolveLocale(testContext);

    Map<String, Object> variables = new HashMap<>();
    for (Map.Entry<String, ITestContextExpression> entry : testContext.getVariables().entrySet()) {
      variables.put(entry.getKey(), evaluate(entry.getKey(), entry.getValue(), variables, locale));
    }

    return new Context(locale, variables);
  }

  private static Locale resolveLocale(ITestContext testContext) {
    ITestContextExpression localeExpression = testContext.getLocale();
    if (localeExpression == null) {
      return DEFAULT_LOCALE;
    }
    Object result = localeExpression.evaluate(Collections.emptyMap(), DEFAULT_LOCALE);
    if (result == null) {
      return DEFAULT_LOCALE;
    }
    return Locale.forLanguageTag(result.toString().replace('_', '-'));
  }

  private static Object evaluate(
      String name,
      ITestContextExpression expression,
      Map<String, Object> variables,
      Locale locale) {
    try {
      return expression.evaluate(variables, locale);
    } catch (Throwable t) {
      throw new TestEngineExecutionException(
          String.format("Error evaluating context variable \"%s\"", name), t);
    }
  }
}
