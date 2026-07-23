package support.thymeleaf;

import static org.assertj.core.api.Assertions.fail;

import java.util.stream.Collectors;
import org.thymeleaf.testing.templateengine.engine.TestExecutor;
import org.thymeleaf.testing.templateengine.report.ConsoleTestReporter;
import org.thymeleaf.testing.templateengine.testable.ITestResult;

/**
 * Runs a thymeleaf-testing {@code .thtest} file (or a folder of them) and fails the JUnit test if
 * any of the contained tests did not match its expected output.
 *
 * <p>Test files live under {@code test/resources/thymeleaf/}. Paths are relative to the {@code
 * server/} directory, which is the working directory of the forked test JVM.
 *
 * <p>Production templates referenced from a test's {@code %INPUT} markup (e.g. {@code
 * ~{admin/shared/LegacySvgFragments :: iconDelete(...)}}) are resolved automatically against {@code
 * app/views/} by {@link AppViewsTestableResolver} — no {@code %INPUT[name]} declaration is needed
 * unless the test wants to substitute its own markup for a template.
 */
public final class ThymeleafFragmentTester {

  private static final String TEST_RESOURCE_ROOT = "test/resources/thymeleaf/";

  private ThymeleafFragmentTester() {}

  /**
   * Executes the given test file or folder.
   *
   * @param path path under {@code test/resources/thymeleaf/}, e.g. {@code
   *     admin/questions/legacyFieldFragments/toggle.thtest}
   */
  public static void run(String path) {
    ConsoleTestReporter reporter = new ConsoleTestReporter();

    // The single-argument TestExecutor constructor passes a null name to its own null-check.
    TestExecutor executor =
        new TestExecutor("ThymeleafFragmentTester", new NonWebProcessingContextBuilder());
    executor.setTestableResolver(new AppViewsTestableResolver());
    executor.setReporter(reporter);
    executor.execute("file:" + TEST_RESOURCE_ROOT + path);

    if (reporter.isAllOK()) {
      return;
    }

    String failures =
        reporter.getAllTestNames().stream()
            .filter(testName -> !reporter.getResultByTestName(testName).isOK())
            .map(testName -> describe(testName, reporter.getResultByTestName(testName)))
            .collect(Collectors.joining("\n\n"));

    fail("Thymeleaf test(s) failed for %s:\n\n%s", path, failures);
  }

  private static String describe(String testName, ITestResult result) {
    StringBuilder description = new StringBuilder(testName);
    if (result.hasMessage()) {
      description.append('\n').append(result.getMessage());
    }
    if (result.hasThrowable()) {
      description.append('\n').append(result.getThrowable());
    }
    return description.toString();
  }
}
