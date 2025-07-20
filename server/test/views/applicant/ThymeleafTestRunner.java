package views.applicant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.testing.templateengine.context.IProcessingContextBuilder;
import org.thymeleaf.testing.templateengine.engine.TestExecutor;
import org.thymeleaf.testing.templateengine.report.ConsoleTestReporter;
import org.thymeleaf.testing.templateengine.testable.ITest;

public class ThymeleafTestRunner {
  public static class CustomProcessingContextBuilder implements IProcessingContextBuilder {
    @Override
    public IContext build(ITest test) {
      Context context = new Context();

      test.getContext()
          .getVariables()
          .forEach(
              (key, value) -> {
                context.setVariable(key, value.evaluate(Map.of(), Locale.getDefault()));
              });

      return context;
    }
  }

  private TemplateEngine templateEngine;
  private TestExecutor executor;

  @Before
  public void setup() {
    // Setup template resolver
    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    //    templateResolver.setPrefix("templates/");
    //    templateResolver.setPrefix("views/");
    templateResolver.setSuffix(".html");
    templateResolver.setCharacterEncoding("UTF-8");

    // Configure template engine
    templateEngine = new TemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);

    // Configure test executor
    executor = new TestExecutor("context", new CustomProcessingContextBuilder());
    executor.setReporter(new ConsoleTestReporter());
  }

  @Test
  public void run_sample1() {
    boolean success = runTestFile("test/views/applicant/sample1.thtest");
    assertTrue("test failed", success);
  }

  @Test
  public void run_sample2() {
    boolean success = runTestFile("test/views/applicant/sample2.thtest");
    assertTrue("test failed", success);
  }

  @Test
  public void run_sample3() {
    boolean success = runTestFile("test/views/applicant/sample3.thtest");
    assertTrue("test failed", success);
  }

  //  @Test
  public void run_all() {
    executor.execute("file:test/views");

    // So far don't like this because you have to dig more into the results
    // Explicity single file tests like above make everything align more with junit IMO
    assertThat(executor.isAllOK()).isTrue();
  }

  private boolean runTestFile(String testFilePath) {
    try {
      executor.execute(String.format("file:%s", testFilePath));
      return executor.isAllOK();
    } catch (Exception e) {
      System.err.println("Error executing test file " + testFilePath + ": " + e.getMessage());
      return false;
    }
  }
}
