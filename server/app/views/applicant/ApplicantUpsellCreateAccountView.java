package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.section;
import static views.applicant.AuthenticateUpsellCreator.createLoginButton;
import static views.applicant.AuthenticateUpsellCreator.createLoginPromptModal;
import static views.applicant.AuthenticateUpsellCreator.createNewAccountButton;

import annotations.BindingAnnotations;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import j2html.tags.DomContent;
import java.util.Locale;
import java.util.Optional;
import models.Account;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.LocalizedStrings;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import views.components.ButtonStyles;
import views.components.Modal;
import views.components.ToastMessage;
import views.style.ReferenceClasses;

/** Renders a confirmation page after application submission. */
public final class ApplicantUpsellCreateAccountView extends ApplicantUpsellView {

  private final ApplicantLayout layout;
  private final String authProviderName;

  @Inject
  public ApplicantUpsellCreateAccountView(
      ApplicantLayout layout,
      @BindingAnnotations.ApplicantAuthProviderName String authProviderName) {
    this.layout = checkNotNull(layout);
    this.authProviderName = checkNotNull(authProviderName);
  }

  /** Renders a sign-up page with a baked-in redirect. */
  public Content render(
      Http.Request request,
      String redirectTo,
      Account account,
      Locale locale,
      String programTitle,
      LocalizedStrings customConfirmationMessage,
      ApplicantPersonalInfo personalInfo,
      Long applicantId,
      Long applicationId,
      Messages messages,
      Optional<ToastMessage> bannerMessage) {
    boolean shouldUpsell = shouldUpsell(account);

    Modal loginPromptModal =
        createLoginPromptModal(
                messages,
                redirectTo,
                /* description= */ messages.at(MessageKey.GENERAL_LOGIN_MODAL_PROMPT.getKeyName()),
                /* bypassMessage= */ MessageKey.BUTTON_CONTINUE_WITHOUT_AN_ACCOUNT)
            .build();

    ImmutableList<DomContent> actionButtons =
        shouldUpsell
            ? ImmutableList.of(
                button(messages.at(MessageKey.LINK_APPLY_TO_ANOTHER_PROGRAM.getKeyName()))
                    .withId(loginPromptModal.getTriggerButtonId())
                    .withClasses(ButtonStyles.OUTLINED_TRANSPARENT),
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
        personalInfo,
        messages,
        createHtmlBundle(layout, title, bannerMessage, loginPromptModal, content));
  }
}
