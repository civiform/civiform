package modules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import controllers.AssetsFinder;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.AbstractContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.engine.AttributeDefinition;
import org.thymeleaf.engine.AttributeDefinitions;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.engine.IAttributeDefinitionsAware;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.messageresolver.IMessageResolver;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.processor.AbstractStandardExpressionAttributeTagProcessor;
import org.thymeleaf.standard.util.StandardProcessorUtils;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.unbescape.html.HtmlEscape;
import play.Application;
import play.Environment;
import play.api.i18n.Lang;
import play.i18n.MessagesApi;
import play.mvc.Http;
import views.tags.CiviFormProcessorDialect;

public final class ThymeleafModule extends AbstractModule {

  @Provides
  public TemplateEngine provideTemplateEngine(
      FileTemplateResolver fileTemplateResolver,
      MessagesApi messagesApi,
      AssetsFinder assetsFinder,
      Environment environment) {
    TemplateEngine templateEngine = new TemplateEngine();

    templateEngine.setTemplateResolver(fileTemplateResolver);
    templateEngine.setMessageResolver(new PlayMessageResolver(messagesApi));
    templateEngine.addDialect(new HtmxDialect(new ObjectMapper()));
    templateEngine.addDialect(new CiviFormProcessorDialect(assetsFinder, environment));

    return templateEngine;
  }

  @Provides
  public FileTemplateResolver provideFileTemplateResolver(
      Application application, Environment environment) {
    FileTemplateResolver fileTemplateResolver = new FileTemplateResolver();

    fileTemplateResolver.setTemplateMode(TemplateMode.HTML);
    // Template file paths are all relative to server/app/views/
    fileTemplateResolver.setPrefix(application.path().getAbsolutePath() + "/app/views/");
    fileTemplateResolver.setSuffix(".html");
    fileTemplateResolver.setCacheable(environment.isProd());

    return fileTemplateResolver;
  }

  // Allow using Message Keys in Thymeleaf HTML and resolving them into the appropriate Strings
  // so they can be rendered in the user's locale.
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

  public static final class PlayThymeleafContextFactory {

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

  public static final class PlayThymeleafContext extends AbstractContext {

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

  public static final class HtmxDialect extends AbstractProcessorDialect {

    private final ObjectMapper mapper;

    public HtmxDialect(ObjectMapper mapper) {
      super("Htmx", "hx", 1000);
      this.mapper = mapper;
    }

    @Override
    public ImmutableSet<IProcessor> getProcessors(String dialectPrefix) {
      ImmutableSet.Builder<IProcessor> htmxProcessors = ImmutableSet.builder();

      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "boost", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "confirm", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "delete", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "disable", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "disinherit", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "encoding", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "ext", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "get", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "headers", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "history-elt", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "include", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "indicator", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "params", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "patch", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "post", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "preserve", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "prompt", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "put", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "push-url", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "request", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "select", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "swap", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "swap-oob", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "sync", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "target", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "trigger", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "validate", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "vals", mapper));
      htmxProcessors.add(new HtmxAttributeProcessor(dialectPrefix, "vars", mapper));

      return htmxProcessors.build();
    }
  }

  public static class HtmxAttributeProcessor extends AbstractStandardExpressionAttributeTagProcessor
      implements IAttributeDefinitionsAware {

    public static final int ATTRIBUTE_PRECEDENCE = 1000;
    private final String htmxAttributeSuffix;
    private final ObjectMapper mapper;

    private static final TemplateMode TEMPLATE_MODE = TemplateMode.HTML;

    private AttributeDefinition targetAttributeDefinition;

    public HtmxAttributeProcessor(
        String dialectPrefix, String htmxAttributeSuffix, ObjectMapper mapper) {
      super(TEMPLATE_MODE, dialectPrefix, htmxAttributeSuffix, ATTRIBUTE_PRECEDENCE, false, false);
      this.htmxAttributeSuffix = htmxAttributeSuffix;
      this.mapper = mapper;
    }

    @Override
    public void setAttributeDefinitions(AttributeDefinitions attributeDefinitions) {
      Preconditions.checkNotNull(attributeDefinitions);
      // This allows for a performance optimization when used with
      // StandardProcessorUtils.replaceAttribute below
      // See
      // https://github.com/thymeleaf/thymeleaf/blob/db314973254ca8d9ee8661cf13c680b93349ee59/lib/thymeleaf/src/main/java/org/thymeleaf/engine/ElementTagStructureHandler.java#L252-L254
      this.targetAttributeDefinition =
          attributeDefinitions.forName(TEMPLATE_MODE, htmxAttributeSuffix);
    }

    @Override
    protected final void doProcess(
        ITemplateContext context,
        IProcessableElementTag tag,
        AttributeName attributeName,
        String attributeValue,
        Object expressionResult,
        IElementTagStructureHandler structureHandler) {
      if (expressionResult == null) {
        structureHandler.removeAttribute(attributeName);
        return;
      }

      String expressionResultString;
      if (expressionResult instanceof Map) {
        try {
          expressionResultString = mapper.writeValueAsString(expressionResult);
        } catch (JsonProcessingException e) {
          throw new TemplateProcessingException(
              "Unable to write JSON", tag.getTemplateName(), tag.getLine(), tag.getLine(), e);
        }
      } else {
        expressionResultString = expressionResult.toString();
      }

      Optional<String> newAttributeValue =
          Optional.ofNullable(HtmlEscape.escapeHtml4Xml(expressionResultString));
      StandardProcessorUtils.replaceAttribute(
          structureHandler,
          attributeName,
          targetAttributeDefinition,
          "hx-" + htmxAttributeSuffix,
          newAttributeValue.orElse(""));
    }
  }
}
