package services;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class UrlUtilsTest {
  @Test
  public void testValidURLs() {
    ImmutableList<String> URLs =
        ImmutableList.of(
            "http://civiform.us",
            "https://civiform.us",
            "http://foo.com",
            "https://foo.gov",
            "http://www.foo.co.uk",
            "http://www.foo.nl",
            "https://foo.gov/bar/baz/",
            "https://foo.gov/bar?query=housing",
            "https://foo.gov/bar?q=<+metacharacter&param=1#fragment");

    for (String url : URLs) {
      System.out.println(url);
      assertThat(UrlUtils.isValid(url)).isTrue();
    }
  }

  @Test
  public void testInvalidURLs() {
    ImmutableList<String> URLs =
        ImmutableList.of(
            "www.foo.gov",
            "javascript:alert('XSS')",
            "<script>alert('XSS')</script>",
            "https://foo.gov/bar?q=javascript:alert('XSS')",
            "https://foo.gov/bar?q=<script>alert('XSS')</script>");

    for (String url : URLs) {
      System.out.println(url);
      assertThat(UrlUtils.isValid(url)).isFalse();
    }
  }
}
