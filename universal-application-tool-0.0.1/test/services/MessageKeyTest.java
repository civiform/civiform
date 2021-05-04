package services;

import static org.assertj.core.api.Assertions.assertThat;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import repository.WithPostgresContainer;

@RunWith(JUnitParamsRunner.class)
public class MessageKeyTest extends WithPostgresContainer {

  private MessagesApi messagesApi;

  @Before
  public void createMessages() {
    // We need the actual MessagesApi for our application.
    messagesApi = instanceOf(MessagesApi.class);
  }

  @Test
  @Parameters(source = MessageKey.class)
  public void messageKey_isValid(MessageKey messageKey) {
    // If a message key does not exist, it will return the invalid key as the message itself.
    // Therefore, we can verify that all keys provided are valid keys by simply checking
    // whether generating the message returns something other than the key name.
    String keyName = messageKey.getKeyName();
    assertThat(messagesApi.get(Lang.defaultLang(), keyName)).isNotEqualTo(keyName);
  }
}
