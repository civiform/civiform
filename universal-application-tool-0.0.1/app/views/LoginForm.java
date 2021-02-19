package views;

import static j2html.TagCreator.body;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;

import play.mvc.Http;
import play.twirl.api.Content;

public class LoginForm extends BaseHtmlView {

  public Content render(Http.Request request) {
    return htmlContent(
        body(
            h1("Log In"),
            form(
                    makeCsrfTokenInputTag(request),
                    textField("uname", "username", "Username"),
                    passwordField("pwd", "password", "Password"),
                    submitButton("login", "Submit"))
                .withMethod("POST")
                .withAction("/callback?client_name=FormClient")));
  }
}
