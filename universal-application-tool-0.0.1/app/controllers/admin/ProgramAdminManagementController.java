package controllers.admin;

import auth.Authorizers;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import repository.UserRepository;

import javax.inject.Inject;

import static play.mvc.Results.ok;

public class ProgramAdminManagementController {

  private final UserRepository userRepository;

  @Inject
  public ProgramAdminManagementController(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Displays the current programs and the admins for those programs.
   */
  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result index() {
    return ok();
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result update() {
    //
    return ok();
  }
}
