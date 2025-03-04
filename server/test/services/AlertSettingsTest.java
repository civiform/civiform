package services;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableSet;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import repository.ResetPostgres;

@RunWith(JUnitParamsRunner.class)
public class AlertSettingsTest extends ResetPostgres {
  private Messages messages;

  @Before
  public void setUp() {
    messages = instanceOf(MessagesApi.class).preferred(ImmutableSet.of(Lang.defaultLang()));
  }

  @Test
  public void getTitleAriaLabel_success() {
    String ariaLabel = AlertSettings.getTitleAriaLabel(messages, AlertType.SUCCESS, "title");
    assertEquals(ariaLabel, "Success: title");
  }

  @Test
  public void getTitleAriaLabel_info() {
    String ariaLabel = AlertSettings.getTitleAriaLabel(messages, AlertType.INFO, "title");
    assertEquals(ariaLabel, "For your information: title");
  }

  @Test
  public void getTitleAriaLabel_emergency() {
    String ariaLabel = AlertSettings.getTitleAriaLabel(messages, AlertType.EMERGENCY, "title");
    assertEquals(ariaLabel, "title");
  }
}
