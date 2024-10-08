package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.label;

import com.google.common.collect.ImmutableList;
import controllers.applicant.ApplicantRequestedAction;
import controllers.applicant.ApplicantRoutes;
import j2html.TagCreator;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.LabelTag;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.Address;
import services.MessageKey;
import services.geo.AddressSuggestion;
import services.geo.AddressSuggestionGroup;
import views.ApplicationBaseView;
import views.ApplicationBaseViewParams;
import views.HtmlBundle;
import views.components.ButtonStyles;
import views.style.ApplicantStyles;

/**
 * Renders a page asking the applicant to confirm their address from a list of corrected addresses.
 */
public final class AddressCorrectionBlockView extends ApplicationBaseView {
  private static final String BLOCK_FORM_ID = "cf-block-form";
  private static final int MAX_SUGGESTIONS_TO_DISPLAY = 3;
  public static final String USER_KEEPING_ADDRESS_VALUE = "USER_KEEPING_ADDRESS_VALUE";
  public static final String SELECTED_ADDRESS_NAME = "selectedAddress";
  private final ApplicantLayout layout;
  private final ApplicantRoutes applicantRoutes;

  @Inject
  AddressCorrectionBlockView(ApplicantLayout layout, ApplicantRoutes applicantRoutes) {
    this.layout = checkNotNull(layout);
    this.applicantRoutes = checkNotNull(applicantRoutes);
  }

  public Content render(
      ApplicationBaseViewParams params,
      Messages messages,
      AddressSuggestionGroup addressSuggestionGroup,
      ApplicantRequestedAction applicantRequestedAction,
      Boolean isEligibilityEnabled) {
    Address addressAsEntered = addressSuggestionGroup.getOriginalAddress();
    ImmutableList<services.geo.AddressSuggestion> suggestions =
        addressSuggestionGroup.getAddressSuggestions();

    DivTag content =
        div()
            .withClass("my-8 m-auto")
            .with(
                renderForm(
                    params,
                    messages,
                    addressAsEntered,
                    suggestions,
                    applicantRequestedAction,
                    isEligibilityEnabled));

    HtmlBundle bundle =
        layout
            .getBundle(params.request())
            .setTitle(
                layout.renderPageTitleWithBlockProgress(
                    params.programTitle(), params.blockIndex(), params.totalBlockCount(), messages))
            .addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION)
            .addMainContent(
                layout.renderProgramApplicationTitleAndProgressIndicator(
                    params.programTitle(),
                    params.blockIndex(),
                    params.totalBlockCount(),
                    false,
                    messages),
                content);

    return layout.renderWithNav(
        params.request(),
        params.applicantPersonalInfo(),
        messages,
        bundle,
        Optional.of(params.applicantId()));
  }

  private FormTag renderForm(
      ApplicationBaseViewParams params,
      Messages messages,
      Address addressAsEntered,
      ImmutableList<AddressSuggestion> suggestions,
      ApplicantRequestedAction applicantRequestedAction,
      Boolean isEligibilityEnabled) {
    FormTag form =
        form()
            .withId(BLOCK_FORM_ID)
            .withAction(getFormAction(params, ApplicantRequestedAction.NEXT_BLOCK))
            .withMethod(Http.HttpVerbs.POST)
            .with(makeCsrfTokenInputTag(params.request()));
    MessageKey title = MessageKey.ADDRESS_CORRECTION_TITLE;
    MessageKey instructionsLine1 = MessageKey.ADDRESS_CORRECTION_LINE_1;
    MessageKey instructionsLine2 =
        suggestions.size() > 0
            ? MessageKey.ADDRESS_CORRECTION_FOUND_SIMILAR_LINE_2
            : MessageKey.ADDRESS_CORRECTION_NO_VALID_LINE_2;
    form.with(h2(messages.at(title.getKeyName())).withClass("font-bold mb-2"))
        .with(div(messages.at(instructionsLine1.getKeyName())).withClass("mb-4"))
        .with(div(messages.at(instructionsLine2.getKeyName())).withClass("mb-8"));

    boolean anySuggestions = suggestions.size() > 0;
    // If the address question is part of determining eligibility, then the address *must* get
    // corrected. We need the address's latitude and longitude values from ESRI to determine if
    // the address is eligible, and we only get the lat/long values via address correction. We
    // can't let the user choose their original, non-corrected address because then we won't have
    // the lat/long values to determine eligibility.
    if (!isEligibilityEnabled) {
      form.with(
          div()
              .withClasses("mb-8")
              .with(
                  div()
                      .withClass("flex flex-nowrap mb-2")
                      .with(
                          h3(messages.at(
                                  MessageKey.ADDRESS_CORRECTION_AS_ENTERED_HEADING.getKeyName()))
                              .withClass("font-bold mb-2 w-full")),
                  renderAddress(
                      addressAsEntered,
                      /* selected= */ !anySuggestions,
                      /* hideButton= */ !anySuggestions,
                      /* singleLineAddress= */ Optional.empty())));
    }

    if (anySuggestions) {
      MessageKey suggestionsHeadingMessageKey =
          suggestions.size() <= 1
              ? MessageKey.ADDRESS_CORRECTION_SUGGESTED_ADDRESS_HEADING
              : MessageKey.ADDRESS_CORRECTION_SUGGESTED_ADDRESSES_HEADING;

      form.with(
          h3(messages.at(suggestionsHeadingMessageKey.getKeyName())).withClass("font-bold mb-2"),
          div().withClasses("mb-8").with(renderSuggestedAddresses(suggestions)));
    }
    form.with(renderBottomNavButtons(params, applicantRequestedAction));

    return form;
  }

  private ImmutableList<LabelTag> renderSuggestedAddresses(
      ImmutableList<AddressSuggestion> suggestions) {
    boolean selected = true;
    List<LabelTag> addressLabels = new ArrayList<>();

    int maxSuggestions = Math.min(suggestions.size(), MAX_SUGGESTIONS_TO_DISPLAY);

    for (int i = 0; i < maxSuggestions; i++) {
      AddressSuggestion suggestion = suggestions.get(i);
      addressLabels.add(
          renderAddress(
              suggestion.getAddress(),
              selected,
              /* hideButton= */ false,
              Optional.ofNullable(suggestion.getSingleLineAddress())));
      selected = false;
    }

    return ImmutableList.copyOf(addressLabels);
  }

  private LabelTag renderAddress(
      Address address, boolean selected, boolean hideButton, Optional<String> singleLineAddress) {
    LabelTag containerDiv = label().withClass("flex flex-nowrap mb-2");

    InputTag input =
        TagCreator.input()
            .withType("radio")
            .withName(SELECTED_ADDRESS_NAME)
            .withClass("cf-radio-input h-4 w-4 mr-4 align-middle")
            .withCondHidden(hideButton);

    if (singleLineAddress.isPresent()) {
      input.withValue(singleLineAddress.get());
    } else {
      input.withValue(USER_KEEPING_ADDRESS_VALUE);
    }

    if (selected) {
      input.attr("checked", "checked");
    }

    containerDiv.with(div().with(input));

    DivTag addressDiv = div().with(div(address.getStreet()));

    if (address.hasLine2()) {
      addressDiv.with(div(address.getLine2()));
    }

    addressDiv.with(
        div(String.format("%s, %s %s", address.getCity(), address.getState(), address.getZip())));

    containerDiv.with(addressDiv);

    return containerDiv;
  }

  private DivTag renderBottomNavButtons(
      ApplicationBaseViewParams params, ApplicantRequestedAction applicantRequestedAction) {
    DivTag bottomNavButtonsContainer = div().withClasses(ApplicantStyles.APPLICATION_NAV_BAR);

    // The address correction screen is an intermediate step that only provides two actions:
    //   1. "Confirm address": Save the address the user selected and take them to wherever they had
    //      requested to go previously: "Review" -> review page, "Save & next" -> block *after* the
    //      block with the address question, "Previous" -> block *before* the block with the address
    //      question.
    //   2. "Go back and edit": Go back to the block with the address question to edit the address
    //      they'd entered.
    ButtonTag confirmAddressButton =
        submitButton(
                params.messages().at(MessageKey.ADDRESS_CORRECTION_CONFIRM_BUTTON.getKeyName()))
            .withClass(ButtonStyles.SOLID_BLUE)
            .withFormaction(getFormAction(params, applicantRequestedAction));
    ButtonTag goBackAndEditButton =
        redirectButton(
                params.messages().at(MessageKey.BUTTON_GO_BACK_AND_EDIT.getKeyName()),
                applicantRoutes
                    .blockEditOrBlockReview(
                        params.profile(),
                        params.applicantId(),
                        params.programId(),
                        params.block().getId(),
                        params.inReview())
                    .url())
            // Set this as "button" type so that it doesn't submit the form.
            .withType("button")
            .withClasses(ButtonStyles.OUTLINED_TRANSPARENT);
    return bottomNavButtonsContainer.with(goBackAndEditButton, confirmAddressButton);
  }

  private String getFormAction(
      ApplicationBaseViewParams params, ApplicantRequestedAction applicantRequestedAction) {
    return applicantRoutes
        .confirmAddress(
            params.profile(),
            params.applicantId(),
            params.programId(),
            params.block().getId(),
            params.inReview(),
            applicantRequestedAction)
        .url();
  }
}
