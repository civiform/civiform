package controllers;

import static play.inject.Bindings.bind;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import play.api.i18n.DefaultLangs;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.MessagesApi;
import play.inject.guice.GuiceApplicationBuilder;
import support.TestConstants;

/**
 * Contains localization helper methods for use in controllers. To use, first create a fake {@link
 * MessagesApi} with provided translations, then use this to create an instance of the controller.
 * When making a request, use {@code fakeRequest().langCookie(Lang.defaultLang(), messagesApi)}.
 */
public class LocalizationHelper {

  /**
   * Create a {@link MessagesApi} with fake translations. Used in controllers for requests setup and
   * requests.
   */
  public static MessagesApi fakeMessagesApi(ImmutableMap<String, String> translations) {
    Langs langs = new Langs(new DefaultLangs());
    Map<String, Map<String, String>> messageMap =
        ImmutableMap.of(Lang.defaultLang().code(), translations);
    return stubMessagesApi(messageMap, langs);
  }

  /**
   * Override the {@link MessagesApi} with a different instance, such as one created by {@code
   * fakeMessagesApi}.
   */
  public static <T> T overrideMessagesApi(MessagesApi messagesApi, Class<T> instanceToBuild) {
    return new GuiceApplicationBuilder()
        .configure(TestConstants.TEST_DATABASE_CONFIG)
        .overrides(bind(MessagesApi.class).toInstance(messagesApi))
        .build()
        .injector()
        .instanceOf(instanceToBuild);
  }
}
