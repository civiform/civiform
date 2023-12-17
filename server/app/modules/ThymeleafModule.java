package modules;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.AbstractContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.messageresolver.IMessageResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import play.Application;
import play.Environment;
import play.api.i18n.Lang;
import play.i18n.MessagesApi;
import play.mvc.Http;

public final class ThymeleafModule extends AbstractModule {

  @Provides
  public TemplateEngine provideTemplateEngine(
      FileTemplateResolver fileTemplateResolver, MessagesApi messagesApi) {
    TemplateEngine templateEngine = new TemplateEngine();

    templateEngine.setTemplateResolver(fileTemplateResolver);
    templateEngine.setMessageResolver(new PlayMessageResolver(messagesApi));

    return templateEngine;
  }

  @Provides
  public FileTemplateResolver provideFileTemplateResolver(
      Application application, Environment environment) {
    FileTemplateResolver fileTemplateResolver = new FileTemplateResolver();

    fileTemplateResolver.setTemplateMode(TemplateMode.HTML);
    fileTemplateResolver.setPrefix(application.path().getAbsolutePath() + "/app/views/");
    fileTemplateResolver.setSuffix(".html");
    fileTemplateResolver.setCacheable(environment.isProd());

    return fileTemplateResolver;
  }

  static class PlayMessageResolver implements IMessageResolver {

    private final MessagesApi messagesApi;

    PlayMessageResolver(MessagesApi messagesApi) {
      this.messagesApi = Preconditions.checkNotNull(messagesApi);
    }

    @Override
    public String getName() {
      return "PLAY_MESSAGE_RESOLVER";
    }

    @Override
    public Integer getOrder() {
      return 0;
    }

    @Override
    public String resolveMessage(
        ITemplateContext context, Class<?> origin, String key, Object[] messageParameters) {
      Lang lang = Lang.apply(context.getLocale());

      if (messagesApi.isDefinedAt(lang, key)) {
        return messagesApi.get(lang, key, messageParameters);
      }

      if (messagesApi.isDefinedAt(Lang.defaultLang(), key)) {
        return messagesApi.get(Lang.defaultLang(), key, messageParameters);
      }

      return null;
    }

    @Override
    public String createAbsentMessageRepresentation(
        ITemplateContext context, Class<?> origin, String key, Object[] messageParameters) {
      return key
          + "("
          + Arrays.stream(messageParameters).map(String::valueOf).collect(Collectors.joining(", "))
          + ")";
    }
  }

  public static class PlayThymeleafContextFactory {

    private final MessagesApi messagesApi;

    @Inject
    public PlayThymeleafContextFactory(MessagesApi messagesApi) {
      this.messagesApi = Preconditions.checkNotNull(messagesApi);
    }

    public PlayThymeleafContext create(Http.RequestHeader requestHeader) {
      Locale locale = Locale.forLanguageTag(messagesApi.preferred(requestHeader).lang().code());
      return new PlayThymeleafContext(locale);
    }
  }

  public static class PlayThymeleafContext extends AbstractContext {

    public PlayThymeleafContext() {
      super();
    }

    public PlayThymeleafContext(final Locale locale) {
      super(locale);
    }

    public PlayThymeleafContext(final Locale locale, final Map<String, Object> variables) {
      super(locale, variables);
    }
  }
}
