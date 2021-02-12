package repository;

import static play.test.Helpers.fakeApplication;

import com.google.common.collect.ImmutableMap;
import play.Application;
import play.test.WithApplication;

public class WithPostgresContainer extends WithApplication {
  protected Application provideApplication() {
    return fakeApplication();
  }
}
