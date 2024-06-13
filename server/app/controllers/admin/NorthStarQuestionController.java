package controllers.admin;

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
  public Result sampleQuestion(Request request) {
    System.out.println("ssandbekkhaug sample question controller");

    // ThymeleafModule.PlayThymeleafContext context =
    //     createThymeleafContext(
    //         request,
    //         0l,
    //         null,
    //         null,
    //         null);
    // templateEngine.process("applicant/ApplicantUpsellTemplate", context);
    System.out.println(playThymeleafContextFactory.hashCode()); // silence errorprone
    System.out.println(templateEngine.hashCode()); // silence errorprone

    String html = "<p>ssandbekkhaug sample question content</p>";

    return ok(html).as(Http.MimeTypes.HTML);
  }
}
