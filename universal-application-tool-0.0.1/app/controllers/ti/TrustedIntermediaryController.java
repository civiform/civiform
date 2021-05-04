package controllers.ti;

import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;
import static play.mvc.Results.unauthorized;

import auth.Authorizers;
import auth.ProfileUtils;
import auth.UatProfile;
import com.google.common.base.Preconditions;
import java.util.Optional;
import javax.inject.Inject;
import javax.mail.Message;
import models.TrustedIntermediaryGroup;
import org.pac4j.play.java.Secure;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.mvc.Result;
import repository.UserRepository;
import views.applicant.TrustedIntermediaryDashboardView;

public class TrustedIntermediaryController {

    private final TrustedIntermediaryDashboardView tiDashboardView;
    private final ProfileUtils profileUtils;
    private final UserRepository userRepository;
    private final MessagesApi messagesApi;

    @Inject
    public TrustedIntermediaryController(
            ProfileUtils profileUtils,
            UserRepository userRepository,
            MessagesApi messagesApi,
            TrustedIntermediaryDashboardView trustedIntermediaryDashboardView
    ) {
        this.profileUtils = Preconditions.checkNotNull(profileUtils);
        this.tiDashboardView = Preconditions.checkNotNull(trustedIntermediaryDashboardView);
        this.userRepository = Preconditions.checkNotNull(userRepository);
        this.messagesApi = Preconditions.checkNotNull(messagesApi);
    }

    @Secure(authorizers = Authorizers.Labels.TI)
    public Result dashboard(Http.Request request) {
        Optional<UatProfile> uatProfile = profileUtils.currentUserProfile(request);
        if (uatProfile.isEmpty()) {
            return unauthorized();
        }
        Optional<TrustedIntermediaryGroup> trustedIntermediaryGroup = userRepository.getTrustedIntermediaryGroup(uatProfile.get());
        if (trustedIntermediaryGroup.isEmpty()) {
            return notFound();
        }
        return ok(tiDashboardView.render(trustedIntermediaryGroup.get(), request, messagesApi.preferred(request)));
    }
}
