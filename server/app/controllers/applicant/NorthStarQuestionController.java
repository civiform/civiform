package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableSet;
import controllers.CiviFormController;
import java.util.List;
import javax.inject.Inject;
import modules.ThymeleafModule;
import org.pac4j.play.java.Secure;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;

/** Controller for rendering inputs for questions. */
public final class NorthStarQuestionController extends CiviFormController {

  private final TemplateEngine templateEngine;
  private final ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory;

  @Inject
  public NorthStarQuestionController(
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory) {
    super(profileUtils, versionRepository);
    this.templateEngine = checkNotNull(templateEngine);
    this.playThymeleafContextFactory = checkNotNull(playThymeleafContextFactory);
  }

  @Secure
  public Result emailQuestion(
      Request request,
      String id,
      String name,
      String ariaDescribedByIds,
      String ariaLabel,
      String value,
      boolean required,
      boolean autofocus,
      List<String> errors) {
    return question(
        request,
        id,
        name,
        ariaDescribedByIds,
        ariaLabel,
        value,
        required,
        autofocus,
        errors,
        "email-input");
  }

  @Secure
  public Result idQuestion(
      Request request,
      String id,
      String name,
      String ariaDescribedByIds,
      String ariaLabel,
      String value,
      boolean required,
      boolean autofocus,
      List<String> errors) {
    return question(
        request,
        id,
        name,
        ariaDescribedByIds,
        ariaLabel,
        value,
        required,
        autofocus,
        errors,
        "id-input");
  }

  private Result question(
      Request request,
      String id,
      String name,
      String ariaDescribedByIds,
      String ariaLabel,
      String value,
      boolean required,
      boolean autofocus,
      List<String> errors,
      String fragment) {
    ThymeleafModule.PlayThymeleafContext context = playThymeleafContextFactory.create(request);
    context.setVariable("id", id);
    context.setVariable("name", name);
    context.setVariable("ariaDescribedByIds", ariaDescribedByIds);
    context.setVariable("ariaLabel", ariaLabel);
    context.setVariable("value", value);
    context.setVariable("required", required);
    context.setVariable("autofocus", autofocus);
    context.setVariable("errors", errors);
    context.setVariable("fragment", fragment);
    String content =
        templateEngine.process("questiontypes/Question", ImmutableSet.of("question"), context);
    return ok(content).as(Http.MimeTypes.HTML);
  }
}
