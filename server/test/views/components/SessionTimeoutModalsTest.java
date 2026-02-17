package views.components;

import static org.assertj.core.api.Assertions.assertThat;

import j2html.tags.specialized.DivTag;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import repository.ResetPostgres;

public class SessionTimeoutModalsTest extends ResetPostgres {

  private Messages messages;

  @Before
  public void setUp() {
    MessagesApi messagesApi = instanceOf(MessagesApi.class);
    messages = messagesApi.preferred(List.of(Lang.defaultLang()));
  }

  @Test
  public void render_createsCorrectStructure() {
    String csrfToken = "test-csrf-token";
    DivTag result = SessionTimeoutModals.render(messages, csrfToken);
    String html = result.render();
    assertSessionTimeoutModalStructure(html, csrfToken);
  }

  @Test
  public void render_modalsAreHiddenByDefault() {
    DivTag result = SessionTimeoutModals.render(messages, "test-csrf-token");
    String html = result.render();

    assertThat(html).contains("class=\"hidden\"");
    assertThat(html).contains("session-timeout-messages");
    assertThat(html).contains("session-inactivity-warning-modal");
    assertThat(html).contains("session-length-warning-modal");
  }

  public static void assertSessionTimeoutModalStructure(String html, String csrfTokenValue) {
    // verify container structure
    assertThat(html).contains("id=\"session-timeout-container\"");
    assertThat(html).contains("id=\"session-timeout-modals\"");

    // verify inactivity warning modal
    assertThat(html).contains("id=\"session-inactivity-warning-modal\"");
    assertThat(html).contains("Session ending soon");
    assertThat(html).contains("Your session will expire soon due to inactivity");
    assertThat(html).contains("Extend Session");

    // verify session length warning modal
    assertThat(html).contains("id=\"session-length-warning-modal\"");
    assertThat(html).contains("Your session is about to expire");
    assertThat(html).contains("Log in");

    // verify form and CSRF token
    assertThat(html).contains("id=\"extend-session-form\"");
    assertThat(html).contains("name=\"csrfToken\"");
    assertThat(html).contains(String.format("value=\"%s\"", csrfTokenValue));
    assertThat(html).contains("hx-post=\"/extend-session\"");

    // verify localized messages container
    assertThat(html).contains("id=\"session-timeout-messages\"");
    assertThat(html).contains("id=\"session-extended-success-text\"");
    assertThat(html).contains("Session successfully extended");
    assertThat(html).contains("id=\"session-extended-error-text\"");
    assertThat(html).contains("Failed to extend session");

    // verify data attributes for JavaScript interaction
    assertThat(html).contains("data-modal-primary");
    assertThat(html).contains("data-modal-secondary");
    assertThat(html).contains("data-modal-type=\"session-inactivity-warning\"");
    assertThat(html).contains("data-modal-type=\"session-length-warning\"");
  }
}
