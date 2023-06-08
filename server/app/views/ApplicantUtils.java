package views;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.Optional;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import services.MessageKey;

// TODO(#4626): remove this class and use ApplicantPersonalInfo as a better abstraction.
public final class ApplicantUtils {

  private final MessagesApi messagesApi;

  @Inject
  public ApplicantUtils(MessagesApi messagesApi) {
    this.messagesApi = checkNotNull(messagesApi);
  }

  /** Get the applicant's name or the GUEST message with en-US lang. */
  public String getApplicantNameEnUs(Optional<String> maybeName) {
    Messages messages = messagesApi.preferred(ImmutableList.of(Lang.forCode("en-US")));
    return ApplicantUtils.getApplicantName(maybeName, messages);
  }

  /** Get the applicant's name or the GUEST message in the provided messages. */
  public static String getApplicantName(Optional<String> maybeName, Messages messages) {
    return maybeName.orElse(messages.at(MessageKey.GUEST.getKeyName()));
  }
}
