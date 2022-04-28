package views;

import java.util.Optional;

import play.i18n.Messages;
import services.MessageKey;

public final class ApplicantUtils {
  public static String getApplicantName(Optional<String> maybeName, Messages messages) {
    return maybeName.orElse(
      messages.at(MessageKey.GUEST.getKeyName()));
  }
}

