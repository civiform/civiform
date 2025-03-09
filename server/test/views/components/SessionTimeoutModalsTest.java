package views.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import j2html.tags.specialized.DivTag;
import org.junit.Test;
import play.i18n.Messages;
import repository.ResetPostgres;

public class SessionTimeoutModalsTest extends ResetPostgres {

  @Test
  public void render_createsCorrectStructure() {
    Messages messages = mock(Messages.class);
    when(messages.at("session.inactivity.warning.title")).thenReturn("Inactivity Warning");
    when(messages.at("session.inactivity.warning.message")).thenReturn("You have been inactive");
    when(messages.at("session.length.warning.title")).thenReturn("Session Length Warning");
    when(messages.at("session.length.warning.message")).thenReturn("Session too long");
    when(messages.at("session.extend.button")).thenReturn("Extend Session");
    when(messages.at("button.logout")).thenReturn("Logout");
    when(messages.at("button.cancel")).thenReturn("Cancel");
    when(messages.at("session.extended.success")).thenReturn("Session extended successfully");
    when(messages.at("session.extended.error")).thenReturn("Failed to extend session");

    String csrfToken = "test-csrf-token";
    DivTag result = SessionTimeoutModals.render(messages, csrfToken);
    String html = result.render();

    // verify container structure
    assertThat(html).contains("id=\"session-timeout-container\"");
    assertThat(html).contains("id=\"session-timeout-modals\"");

    // verify inactivity warning modal
    assertThat(html).contains("id=\"session-inactivity-warning-modal\"");
    assertThat(html).contains("Inactivity Warning");
    assertThat(html).contains("You have been inactive");
    assertThat(html).contains("Extend Session");

    // verify session length warning modal
    assertThat(html).contains("id=\"session-length-warning-modal\"");
    assertThat(html).contains("Session Length Warning");
    assertThat(html).contains("Session too long");
    assertThat(html).contains("Logout");

    // verify form and CSRF token
    assertThat(html).contains("id=\"extend-session-form\"");
    assertThat(html).contains("name=\"csrfToken\"");
    assertThat(html).contains("value=\"test-csrf-token\"");
    assertThat(html).contains("hx-post=\"/extend-session\"");

    // verify localized messages container
    assertThat(html).contains("id=\"session-timeout-messages\"");
    assertThat(html).contains("id=\"session-extended-success-text\"");
    assertThat(html).contains("Session extended successfully");
    assertThat(html).contains("id=\"session-extended-error-text\"");
    assertThat(html).contains("Failed to extend session");

    // verify data attributes for JavaScript interaction
    assertThat(html).contains("data-modal-primary");
    assertThat(html).contains("data-modal-secondary");
    assertThat(html).contains("data-modal-type=\"session-inactivity-warning\"");
    assertThat(html).contains("data-modal-type=\"session-length-warning\"");
  }

  @Test
  public void render_modalsAreHiddenByDefault() {
    Messages messages = mock(Messages.class);
    mockMessages(messages);

    DivTag result = SessionTimeoutModals.render(messages, "test-csrf-token");
    String html = result.render();

    assertThat(html).contains("class=\"hidden\"");
    assertThat(html).contains("session-timeout-messages");
    assertThat(html).contains("session-inactivity-warning-modal");
    assertThat(html).contains("session-length-warning-modal");
  }

  private void mockMessages(Messages messages) {
    when(messages.at("session.inactivity.warning.title")).thenReturn("Inactivity Warning");
    when(messages.at("session.inactivity.warning.message")).thenReturn("You have been inactive");
    when(messages.at("session.length.warning.title")).thenReturn("Session Length Warning");
    when(messages.at("session.length.warning.message")).thenReturn("Session too long");
    when(messages.at("session.extend.button")).thenReturn("Extend Session");
    when(messages.at("button.logout")).thenReturn("Logout");
    when(messages.at("button.cancel")).thenReturn("Cancel");
    when(messages.at("session.extended.success")).thenReturn("Session extended successfully");
    when(messages.at("session.extended.error")).thenReturn("Failed to extend session");
  }
}
