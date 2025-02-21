package services;

import static org.junit.Assert.assertEquals;

import org.junit.Before;

import com.google.common.collect.ImmutableSet;
import junitparams.JUnitParamsRunner;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import repository.ResetPostgres;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class AlertSettingsTest  extends ResetPostgres {
  private Messages messages;

  @Before 
  public void setUp() {
     messages = instanceOf(MessagesApi.class).preferred(ImmutableSet.of(Lang.defaultLang()));
  }

  @Test
  public void getTitleHelpText_success() {
    String helpText = AlertSettings.getTitleHelpText(messages, AlertType.SUCCESS, "title");
    assertEquals(helpText, "Success: title");
  }

  @Test 
  public void getTitleHelpText_info() {
    String helpText = AlertSettings.getTitleHelpText(messages, AlertType.INFO, "title");
    assertEquals(helpText, "For your information: title");
  }

  @Test 
  public void getTitleHelpText_emergency() {
    String helpText = AlertSettings.getTitleHelpText(messages, AlertType.EMERGENCY, "title");
    assertEquals(helpText, "title");
  }
}
