package controllers;

import org.mockito.ArgumentMatcher;
import play.mvc.Http;

/**
 * ArgumentMatcher that matches an Http.Request containing a specified header whose value is "true".
 */
public final class HasBooleanHeaderArgumentMatcher implements ArgumentMatcher<Http.Request> {
  private final String headerName;

  HasBooleanHeaderArgumentMatcher(String headerName) {
    this.headerName = headerName;
  }

  @Override
  public boolean matches(Http.Request argument) {
    if (argument == null || !argument.hasHeader(headerName)) {
      return false;
    }
    return Boolean.parseBoolean(argument.header(headerName).get());
  }

  @Override
  public String toString() {
    return String.format("[HTTP Request has header %s containing \"true\"]", headerName);
  }
}
