package controllers;

import play.mvc.Controller;
import play.mvc.Result;

/** Controller for redirecting requests with unnecessary trailing slashes. */
public class UntrailingController extends Controller {

  public Result untrail(String path) {
    return movedPermanently("/" + path);
  }
}
