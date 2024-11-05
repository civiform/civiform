package services.email;

import com.google.common.collect.ImmutableList;

/** Interface for sending emails from CiviForm. */
public interface EmailSendClient {

  /**
   * Sends an email from CiviForm with one recipient.
   *
   * @param toAddress A single address of where the email should be sent.
   * @param subject The email subject.
   * @param bodyText The email body.
   */
  void send(String toAddress, String subject, String bodyText);

  /**
   * Sends an email from CiviForm with multiple recipients.
   *
   * @param toAddresses A list of email addresses of where the email should be sent.
   * @param subject The email subject.
   * @param bodyText The email body.
   */
  void send(ImmutableList<String> toAddresses, String subject, String bodyText);
}
