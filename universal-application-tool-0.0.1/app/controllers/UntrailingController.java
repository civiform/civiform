package controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class UntrailingController extends Controller {

  public Result untrail(String path) {
    return movedPermanently("/" + path);
  }
}
