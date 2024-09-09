package controllers.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import controllers.WithMockedProfiles;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;

public class SessionDisplayControllerTest extends WithMockedProfiles {
  private SessionDisplayController controller;

  @Before
  public void setUp() throws Exception {
    controller = instanceOf(SessionDisplayController.class);
  }

  @Test
  public void testIndexWithProfile() {
    Http.Request request = fakeRequestBuilder().session("foo", "bar").build();
    Result result = controller.index(request);
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("\"foo\" : \"bar\"");
  }
}
