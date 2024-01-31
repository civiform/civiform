package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import controllers.CiviFormController;
import javax.inject.Inject;
import modules.ThymeleafModule;
import org.pac4j.play.java.Secure;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import com.google.common.collect.ImmutableSet;

/**
 * Controller for handling methods for an applicant applying to programs. CAUTION: you must
 * explicitly check the current profile so that an unauthorized user cannot access another
 * applicant's data!
 */
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
  public Result emailQuestion(Request request, String id, String name, String ariaDescribedByIds) {
    ThymeleafModule.PlayThymeleafContext context = playThymeleafContextFactory.create(request);
    context.setVariable("id", id);
    context.setVariable("name", name);
    context.setVariable("ariaDescribedByIds", ariaDescribedByIds);
    String content =
        templateEngine.process(
            "questiontypes/EmailQuestion", ImmutableSet.of("email-question"), context);
    return ok(content).as(Http.MimeTypes.HTML);
  }
}
