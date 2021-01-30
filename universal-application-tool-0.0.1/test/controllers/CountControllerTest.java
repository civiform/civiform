package controllers;

import org.junit.Test;
import play.mvc.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.contentAsString;

public class CountControllerTest {

  @Test
  public void testCount() {
    final CountController controller = new CountController(() -> 49);
    Result result = controller.count();
    assertThat(contentAsString(result)).isEqualTo("49");
  }
}
