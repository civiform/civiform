package support;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Retries a test a given number of times before allowing it to fail. Useful for tests that flap
 * occasionally for baffling reasons.
 */
public final class TestRetry implements TestRule {
  private int retryCount;

  public TestRetry(int retryCount) {
    this.retryCount = retryCount;
  }

  @Override
  public Statement apply(Statement statement, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        Throwable failureCause = null;

        for (int i = 0; i < retryCount; i++) {
          try {
            statement.evaluate();
            return;
          } catch (Throwable throwable) {
            failureCause = throwable;
            System.err.println(
                description.getDisplayName()
                    + ": attempt "
                    + (i + 1)
                    + "/"
                    + retryCount
                    + " failed due to: "
                    + failureCause);
          }
        }

        System.err.println(description.getDisplayName() + ": failed " + retryCount + " times.");
        throw failureCause;
      }
    };
  }
}
