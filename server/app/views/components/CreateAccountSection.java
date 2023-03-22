package views.components;

import com.google.common.base.Strings;
import controllers.routes;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.SectionTag;
import models.Account;
import play.i18n.Messages;
import services.MessageKey;
import views.style.ApplicantStyles;
import views.style.StyleUtils;

import java.util.Optional;

import static j2html.TagCreator.*;
import static j2html.TagCreator.div;

public class CreateAccountSection {

  String redirectTo;
  Account account;
  Messages messages;

  public CreateAccountSection(
    String redirectTo,
    Account account,
    Messages messages
  ) {
    this.redirectTo = redirectTo;
    this.account = account;
    this.messages = messages;
  }

  public SectionTag getSection() {
    SectionTag upsellSection = section();

    DivTag createAccountSection =
      div()
        .with(
          h2(messages.at(MessageKey.TITLE_CREATE_AN_ACCOUNT.getKeyName()))
            .withClasses("mb-4", "font-bold"))
        .with(
          div(messages.at(MessageKey.CONTENT_PLEASE_CREATE_ACCOUNT.getKeyName()))
            .withClasses("mb-4"))
        .with(
          div()
            .withClasses(
              "flex", "flex-col", "gap-4", StyleUtils.responsiveMedium("flex-row"))
            // Empty div to push buttons to the right on desktop.
            .with(div().withClasses("flex-grow"))
            .with(
              redirectButton(
                "another-program",
                messages.at(MessageKey.LINK_APPLY_TO_ANOTHER_PROGRAM.getKeyName()),
                redirectTo)
                .withClasses(ApplicantStyles.BUTTON_NOT_RIGHT_NOW))
            .with(
              redirectButton(
                "all-done",
                messages.at(MessageKey.LINK_ALL_DONE.getKeyName()),
                org.pac4j.play.routes.LogoutController.logout().url())
                .withClasses(ApplicantStyles.BUTTON_NOT_RIGHT_NOW))
            .with(
              redirectButton(
                "sign-in",
                messages.at(MessageKey.LINK_CREATE_ACCOUNT_OR_SIGN_IN.getKeyName()),
                controllers.routes.LoginController.applicantLogin(Optional.of(redirectTo))
                  .url())
                .withClasses(ApplicantStyles.BUTTON_CREATE_ACCOUNT)));

    // Don't show "create an account" upsell box to TIs, or anyone with an email address already.
    if (Strings.isNullOrEmpty(account.getEmailAddress()) && account.getMemberOfGroup().isEmpty()) {
      upsellSection.with(createAccountSection);
    } else {
      upsellSection.with(
        new LinkElement()
          .setHref(redirectTo)
          .setText(messages.at(MessageKey.LINK_APPLY_TO_ANOTHER_PROGRAM.getKeyName()))
          .asAnchorText());
    }

    return upsellSection;
  }
}
