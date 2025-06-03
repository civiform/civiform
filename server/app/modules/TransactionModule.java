package modules;

import annotations.BindingAnnotations;
import com.google.inject.AbstractModule;

import com.google.inject.matcher.Matchers;
import io.ebean.DB;
import io.ebean.Transaction;
import org.aopalliance.intercept.MethodInterceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionModule extends AbstractModule {

  private static final Logger logger =
    LoggerFactory.getLogger(TransactionModule.class);
  @Override
  protected void configure() {
    // This is logged
    logger.error("In configure");
    // This seemingly doesn't actually bind to the annotated method
    bindInterceptor(
      Matchers.any(), // can be refined to specific packages or classes
      Matchers.annotatedWith(BindingAnnotations.RequiresTransaction.class),
      new RequiresTransactionInterceptor()
    );
  }
  public static class RequiresTransactionInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
      Transaction currentTx = DB.currentTransaction();
      // This is NOT logged
      logger.error("In invoke");
      if (currentTx == null) {
        throw new IllegalStateException("Method " + invocation.getMethod().getName() +
          " requires an active Ebean Transaction, but none was found.");
      }
      return invocation.proceed();
    }
  }
}
