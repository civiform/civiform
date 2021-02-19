package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.body;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;

import com.google.inject.Inject;
import play.mvc.Http;
import play.twirl.api.Content;

public class LoginForm extends BaseHtmlView {
  private final BaseHtmlLayout layout;

  @Inject
  public LoginForm(BaseHtmlLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(Http.Request request) {
    return layout.htmlContent(
        body(
            h1("Error: You are not logged in")
                .withCondHidden(!request.queryString("message").orElse("").equals("login")),
            h1("Log In"),
            form(
                    makeCsrfTokenInputTag(request),
                    textField("uname", "username", "Username"),
                    passwordField("pwd", "password", "Password"),
                    submitButton("login", "Submit"))
                .withMethod("POST")
                .withAction("/callback?client_name=FormClient"),
            h1("Or, continue as guest."),
            button("guest", "continue")
                .attr("onclick", "window.location = '/callback?client_name=GuestClient';")));
  }
}
