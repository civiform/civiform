package views.applicant;

import controllers.AssetsFinder;
import controllers.applicant.ApplicantRoutes;
import modules.ThymeleafModule;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http.Request;
import repository.ResetPostgres;
import services.question.types.QuestionDefinition;

public class NorthStarApplicantProgramBlockEditViewTest extends ResetPostgres {

  private static QuestionDefinition ADDRESS_QD =
      testQuestionBank.applicantAddress().getQuestionDefinition();
  private static ApplicantRoutes applicantRoutes = new ApplicantRoutes();

  private static ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory =
      Mockito.mock(ThymeleafModule.PlayThymeleafContextFactory.class);
  private static ThymeleafModule.PlayThymeleafContext playThymeleafContext =
      Mockito.mock(ThymeleafModule.PlayThymeleafContext.class);
  private static Request mockRequest = Mockito.mock(Request.class);

  private static NorthStarApplicantProgramBlockEditView northStarView;

  @Before
  public void setup() {
    Mockito.when(playThymeleafContextFactory.create(mockRequest)).thenReturn(playThymeleafContext);

    northStarView =
        new NorthStarApplicantProgramBlockEditView(
            Mockito.mock(TemplateEngine.class),
            playThymeleafContextFactory,
            Mockito.mock(AssetsFinder.class),
            applicantRoutes);
  }

  @Test
  public void testPreviousUrl() {
    ApplicationBaseViewParams params = Mockito.mock(ApplicationBaseViewParams.class);
    northStarView.render(mockRequest, params);
    Mockito.verify(playThymeleafContext).setVariable("previousUrl", "abc.com");
  }
}
