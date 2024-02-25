package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;

import auth.Authorizers.Labels;
import play.mvc.Controller;
import services.ai.OpenAIClient;
import org.pac4j.play.java.Secure;
import play.mvc.Result;
import play.mvc.Http.Request;
import views.components.FieldWithLabel;
import j2html.tags.specialized.DivTag;

public class AIResponseController extends Controller {

  private OpenAIClient client;

  @Inject
  public AIResponseController(
    OpenAIClient client
  ) {
    this.client = checkNotNull(client);
  }

  @Secure(authorizers = Labels.CIVIFORM_ADMIN)
  public Result getResponse(Request request) {
    String text = request.body().asFormUrlEncoded().get("questionText")[0];
    String result = client.getRewordedQuestion(request, text);
    //result = result.equals("NO_CHANGE") ? text : result;
    DivTag newElement = FieldWithLabel.textArea()
      .setId("question-text-textarea")
      .setFieldName("questionText")
      .setLabelText("Question text displayed to the applicant")
      .setRequired(true)
      .setDisabled(false)
      .setValue(result)
      .getTextareaTag().withId("question-text-container");
    return ok(newElement.render());
  }
  
}
