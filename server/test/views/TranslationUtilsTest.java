package views;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import repository.ResetPostgres;

public class TranslationUtilsTest extends ResetPostgres {

  private Langs langs;

  @Before
  public void setUp() {
    langs = instanceOf(Langs.class);
  }

  @Test
  public void splitTranslatedSingleArgString() {
    String key = "to_translate_key";
    Lang en = Lang.forCode("en-US");
    Lang es = Lang.forCode("es-US");
    MessagesApi messagesApi =
        stubMessagesApi(
            ImmutableMap.of(
                en.code(), ImmutableMap.of(key, "Submitted {0}"),
                es.code(), ImmutableMap.of(key, "{0} submitted")),
            langs);

    Messages enMessages = messagesApi.preferred(ImmutableList.of(en));
    TranslationUtils.TranslatedStringSplitResult enResult =
        TranslationUtils.splitTranslatedSingleArgString(enMessages, key);
    assertThat(enResult.beforeInterpretedContent()).isEqualTo("Submitted ");
    assertThat(enResult.afterInterpretedContent()).isEmpty();

    Messages esMessages = messagesApi.preferred(ImmutableList.of(es));
    TranslationUtils.TranslatedStringSplitResult esResult =
        TranslationUtils.splitTranslatedSingleArgString(esMessages, key);
    assertThat(esResult.beforeInterpretedContent()).isEmpty();
    assertThat(esResult.afterInterpretedContent()).isEqualTo(" submitted");
  }
}
