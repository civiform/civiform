package support.thymeleaf;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.thymeleaf.testing.templateengine.resolver.ITestableResolver;
import org.thymeleaf.testing.templateengine.resource.ITestResource;
import org.thymeleaf.testing.templateengine.resource.ITestResourceItem;
import org.thymeleaf.testing.templateengine.standard.resolver.StandardTestableResolver;
import org.thymeleaf.testing.templateengine.testable.AbstractTest;
import org.thymeleaf.testing.templateengine.testable.ITestIterator;
import org.thymeleaf.testing.templateengine.testable.ITestParallelizer;
import org.thymeleaf.testing.templateengine.testable.ITestSequence;
import org.thymeleaf.testing.templateengine.testable.ITestable;

/**
 * Resolves {@code .thtest} files like the standard thymeleaf-testing resolver, then automatically
 * registers the production template under {@code app/views/} for every {@code ~{template ::
 * fragment}} (or {@code ~{template}}) reference in each test's input.
 *
 * <p>This removes the need for boilerplate like:
 *
 * <pre>
 * %INPUT[admin/shared/LegacySvgFragments] (file:app/views/admin/shared/LegacySvgFragments.html)
 * </pre>
 *
 * in every test file: referencing a template in the {@code %INPUT} markup is enough. References are
 * also collected transitively, so a production fragment that itself inserts another template works
 * without extra declarations. An explicit {@code %INPUT[name]} in the test file always wins over
 * auto-registration, so a test can still substitute a fake for a referenced template.
 */
final class AppViewsTestableResolver implements ITestableResolver {

  private static final String APP_VIEWS_ROOT = "app/views/";

  /**
   * Matches {@code ~{name ...}} template references and captures the template name. Self-references
   * ({@code ~{:: frag}}) and dynamic names ({@code ~{${...}}}) intentionally do not match.
   */
  private static final Pattern TEMPLATE_REFERENCE =
      Pattern.compile("~\\{\\s*([A-Za-z0-9_][A-Za-z0-9_/.\\-]*)\\s*(?:}|::)");

  private final StandardTestableResolver delegate = new StandardTestableResolver();

  @Override
  public ITestable resolve(String executionId, String testableName) {
    ITestable testable = delegate.resolve(executionId, testableName);
    if (testable != null) {
      registerProductionTemplates(testable);
    }
    return testable;
  }

  private void registerProductionTemplates(ITestable testable) {
    if (testable instanceof ITestSequence sequence) {
      sequence.getElements().forEach(this::registerProductionTemplates);
    } else if (testable instanceof ITestIterator iterator) {
      registerProductionTemplates(iterator.getIteratedElement());
    } else if (testable instanceof ITestParallelizer parallelizer) {
      registerProductionTemplates(parallelizer.getParallelizedElement());
    } else if (testable instanceof AbstractTest test) {
      registerReferencedTemplates(test, test.getInput());
    }
  }

  private void registerReferencedTemplates(AbstractTest test, ITestResource resource) {
    if (!(resource instanceof ITestResourceItem item)) {
      return;
    }
    Matcher matcher = TEMPLATE_REFERENCE.matcher(item.readAsText());
    while (matcher.find()) {
      String templateName = matcher.group(1);
      if (test.getAdditionalInputs().containsKey(templateName)) {
        continue;
      }
      String path = APP_VIEWS_ROOT + templateName + ".html";
      if (!new File(path).isFile()) {
        throw new IllegalStateException(
            String.format(
                "Test '%s' references template '%s' but %s does not exist. Either fix the"
                    + " reference or declare the template explicitly with %%INPUT[%s].",
                test.getName(), templateName, path, templateName));
      }
      ITestResource template = delegate.getTestResourceResolver().resolve("file:" + path);
      test.setAdditionalInput(templateName, template);
      registerReferencedTemplates(test, template);
    }
  }
}
