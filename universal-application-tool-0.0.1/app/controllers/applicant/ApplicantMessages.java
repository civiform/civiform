package controllers.applicant;

import auth.ProfileUtils;
import auth.UatProfile;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http.Request;

public class ApplicantMessages {
  private static final String ENGLISH = "en";

  private final MessagesApi messagesApi;
  private final ProfileUtils profileUtils;

  @Inject
  public ApplicantMessages(MessagesApi messagesApi, ProfileUtils profileUtils) {
    this.messagesApi = messagesApi;
    this.profileUtils = profileUtils;
  }

  /**
   * Get the {@link Messages} for this applicant's preferred locale. If we cannot find the current
   * user profile, use the default English messages.
   *
   * @param request the current {@link Request}
   * @return all {@link Messages} in the applicant's preferred locale; otherwise defaults to English
   */
  public CompletionStage<Messages> getMessagesForCurrentApplicant(Request request) {
    Optional<UatProfile> profile = profileUtils.currentUserProfile(request);
    if (profile.isPresent()) {
      return profile
          .get()
          .getApplicant()
          .thenApplyAsync(
              applicant ->
                  messagesApi.preferred(
                      ImmutableList.of(new Lang(applicant.getApplicantData().preferredLocale()))));
    }

    return CompletableFuture.completedFuture(
        messagesApi.preferred(ImmutableList.of(Lang.forCode(ENGLISH))));
  }
}
