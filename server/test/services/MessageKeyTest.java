package services;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.EnumSet;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import repository.ResetPostgres;

// Note: If this test is removed / renamed, the assertion
// in bin/run-test-ci will need to be updated.

@RunWith(JUnitParamsRunner.class)
public class MessageKeyTest extends ResetPostgres {

  private MessagesApi messagesApi;

  @Before
  public void createMessages() {
    // We need the actual MessagesApi for our application.
    messagesApi = instanceOf(MessagesApi.class);
  }

  @Test
  public void allMessageKeys_areValid() {
    var invalidKeys = new ArrayList<String>();

    for (MessageKey messageKey : MessageKey.values()) {
      if (!messagesApi.isDefinedAt(Lang.defaultLang(), messageKey.getKeyName())) {
        invalidKeys.add(messageKey.getKeyName());
      }
    }

    assertThat(invalidKeys).isEmpty();
  }

  @Test
  public void noDuplicateKeys() {
    EnumSet<MessageKey> messageKeys = EnumSet.allOf(MessageKey.class);
    ImmutableSet<String> stringKeys =
        messageKeys.stream().map(MessageKey::getKeyName).collect(toImmutableSet());
    assertThat(messageKeys.size()).isEqualTo(stringKeys.size());
  }
}
