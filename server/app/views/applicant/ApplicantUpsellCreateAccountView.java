package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.section;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import j2html.tags.DomContent;
import java.util.Locale;
import java.util.Optional;
import models.Account;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.LocalizedStrings;
import services.MessageKey;
import views.components.Modal;
import views.components.ToastMessage;
import views.style.ReferenceClasses;

/** Renders a confirmation page after application submission. */
public final class ApplicantUpsellCreateAccountView extends ApplicantUpsellView {

  private final ApplicantLayout layout;
  private final String authProviderName;

  @Inject
  public ApplicantUpsellCreateAccountView(ApplicantLayout layout, Config config) {
    this.layout = checkNotNull(layout);
    this.authProviderName = config.getString("auth.applicant_auth_provider_name");
  }

  /** Renders a sign-up page with a baked-in redirect. */
  public Content render(
      Http.Request request,
      String redirectTo,
      Account account,
      Locale locale,
      String programTitle,
      LocalizedStrings customConfirmationMessage,
      Optional<String> applicantName,
      Long applicantId,
      Long applicationId,
      Messages messages,
      Optional<ToastMessage> bannerMessage) {
    boolean shouldUpsell = shouldUpsell(account);

    Modal loginPromptModal =
        createLoginPromptModal(messages, redirectTo, MessageKey.LINK_APPLY_TO_ANOTHER_PROGRAM);

    ImmutableList<DomContent> actionButtons =
        shouldUpsell
            ? ImmutableList.of(
                loginPromptModal.getButton(), // Apply to another program
                createLoginButton("sign-in", messages, redirectTo),
                createNewAccountButton("sign-up", messages))
            : ImmutableList.of(
                createApplyToProgramsButton(
                    "another-program",
                    messages.at(MessageKey.LINK_APPLY_TO_ANOTHER_PROGRAM.getKeyName()),
                    applicantId));

    String title = messages.at(MessageKey.TITLE_APPLICATION_CONFIRMATION.getKeyName());
    var content =
        createMainContent(
            title,
            section(
                div(messages.at(
                        MessageKey.CONTENT_CONFIRMED.getKeyName(), programTitle, applicationId))
                    .withClasses(ReferenceClasses.BT_APPLICATION_ID, "mb-4"),
                div(customConfirmationMessage.getOrDefault(locale)).withClasses("mb-4")),
            shouldUpsell,
            messages,
            authProviderName,
            actionButtons);
    return layout.renderWithNav(
        request,
        applicantName,
        messages,
        createHtmlBundle(layout, title, bannerMessage, loginPromptModal, content));
  }
}
