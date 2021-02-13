package controllers;

import play.mvc.Result;
import views.html.demo;
import static j2html.TagCreator.*;
import j2html.tags.Tag;
import play.twirl.api.Html;

import static play.mvc.Results.ok;

public class J2HtmlDemoController {

  public Result index() {
    return ok(demo.render(Html.apply(getHtml())));
  }

  private String getHtml() {
    return document(html(body(h1("I'm a header!"), getForm())));
  }

  private Tag getForm() {
    return form(
            label("What is your first name?").attr("for", "nameFirst"),
            input().withType("text").withName("nameFirst"),
            input().withType("submit").withValue("Enter"))
        .withAction("/demo")
        .withId("demo-id");
  }
}
