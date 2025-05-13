package views.admin.apibridge;

import static autovalue.shaded.com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http;
import views.CspUtil;
import views.html.helper.CSRF;

public abstract class BaseView<T> {
  private final TemplateEngine templateEngine;
  private final ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory;
  private final AssetsFinder assetsFinder;

  @Inject
  public BaseView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder) {
    this.templateEngine = checkNotNull(templateEngine);
    this.playThymeleafContextFactory = checkNotNull(playThymeleafContextFactory);
    this.assetsFinder = checkNotNull(assetsFinder);
  }

  protected String pageTitle() {
    return "";
  }

  protected abstract String thymeleafTemplate();

  protected abstract void customizeContext(ThymeleafModule.PlayThymeleafContext context);

  public String render(Http.Request request, T model) {
    ThymeleafModule.PlayThymeleafContext context = playThymeleafContextFactory.create(request);

    context.setVariable("pageTitle", pageTitle());
    context.setVariable("uswdsStylesheet", assetsFinder.path("dist/uswds.min.css"));
    context.setVariable("uswdsJsInit", assetsFinder.path("javascripts/uswds/uswds-init.min.js"));
    context.setVariable("uswdsJsBundle", assetsFinder.path("dist/uswds.bundle.js"));
    context.setVariable("cspNonce", CspUtil.getNonce(request));
    context.setVariable("csrfToken", CSRF.getToken(request.asScala()).value());
    context.setVariable("presenter", this);
    context.setVariable("model", model);

    customizeContext(context);

    return templateEngine.process(thymeleafTemplate(), context);
  }

  public String prettyPrintJson(String json) {
    try {
      ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
      Object jsonObject = mapper.readValue(json, Object.class);
      return mapper.writeValueAsString(jsonObject);
    } catch (JsonProcessingException e) {
      return json;
    }
  }
}
