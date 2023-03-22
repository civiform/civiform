package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.label;

import com.google.common.collect.ImmutableList;
import controllers.applicant.routes;
import j2html.TagCreator;
import j2html.tags.specialized.ATag;
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
import views.HtmlBundle;
import views.components.Icons;
import views.components.LinkElement;
import views.style.ApplicantStyles;
import views.style.StyleUtils;

/**
 * Renders a page asking the applicant to confirm their address from a list of corrected addresses.
 */
public final class AddressCorrectionBlockView extends ApplicationBaseView {
  private static final String BLOCK_FORM_ID = "cf-block-form";
  private static final int MAX_SUGGESTIONS_TO_DISPLAY = 3;
  public static final String USER_KEEPING_ADDRESS_VALUE = "USER_KEEPING_ADDRESS_VALUE";
  public static final String SELECTED_ADDRESS_NAME = "selectedAddress";
  private final ApplicantLayout layout;

  @Inject
  AddressCorrectionBlockView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(
      Params params,
      Messages messages,
      AddressSuggestionGroup addressSuggestionGroup,
      Boolean isEligibilityEnabled) {
    Address addressAsEntered = addressSuggestionGroup.getOriginalAddress();
    ImmutableList<services.geo.AddressSuggestion> suggestions =
        addressSuggestionGroup.getAddressSuggestions();

    DivTag content =
        div()
            .withClass("my-8 m-auto")
            .with(
                renderForm(params, messages, addressAsEntered, suggestions, isEligibilityEnabled));

    HtmlBundle bundle =
        layout
            .getBundle()
            .setTitle(
                layout.renderPageTitleWithBlockProgress(
                    params.programTitle(), params.blockIndex(), params.totalBlockCount()))
            .addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION)
            .addMainContent(
                layout.renderProgramApplicationTitleAndProgressIndicator(
                    params.programTitle(), params.blockIndex(), params.totalBlockCount(), false),
                content);

    return layout.renderWithNav(params.request(), params.applicantName(), messages, bundle);
  }

  private FormTag renderForm(
      Params params,
      Messages messages,
      Address addressAsEntered,
      ImmutableList<AddressSuggestion> suggestions,
      Boolean isEligibilityEnabled) {
    String formAction =
        routes.ApplicantProgramBlocksController.confirmAddress(
                params.applicantId(), params.programId(), params.block().getId(), params.inReview())
            .url();

    FormTag form =
        form()
            .withId(BLOCK_FORM_ID)
            .withAction(formAction)
            .withMethod(Http.HttpVerbs.POST)
            .with(makeCsrfTokenInputTag(params.request()));

    form.with(
            h2(messages.at(MessageKey.ADDRESS_CORRECTION_HEADING.getKeyName()))
                .withClass("font-bold mb-2"))
        .with(
            div(messages.at(MessageKey.ADDRESS_CORRECTION_PAGE_INSTRUCTIONS.getKeyName()))
                .withClass("mb-8"));

    if (!isEligibilityEnabled) {
      form.with(
          div()
              .withClasses("mb-8")
              .with(
                  renderAsEnteredHeading(
                      params.applicantId(), params.programId(), params.block().getId(), messages),
                  renderAddress(addressAsEntered, false, Optional.empty())));
    }

    MessageKey suggestionsHeadingMessageKey =
        suggestions.size() <= 1
            ? MessageKey.ADDRESS_CORRECTION_SUGGESTED_ADDRESS_HEADING
            : MessageKey.ADDRESS_CORRECTION_SUGGESTED_ADDRESSES_HEADING;

    form.with(
        h3(messages.at(suggestionsHeadingMessageKey.getKeyName())).withClass("font-bold mb-2"),
        div()
            .withClasses("mb-8")
            .with(renderSuggestedAddresses(suggestions))
            .with(renderBottomNavButtons(params)));

    return form;
  }

  private DivTag renderAsEnteredHeading(
      long applicantId, long programId, String blockId, Messages messages) {
    DivTag containerDiv = div().withClass("flex flex-nowrap mb-2");

    ATag editElement =
        new LinkElement()
            .setStyles(
                "bottom-0", "right-0", "text-blue-600", StyleUtils.hover("text-blue-700"), "mb-2")
            .setHref(
                routes.ApplicantProgramBlocksController.review(applicantId, programId, blockId)
                    .url())
            .setText(messages.at(MessageKey.LINK_EDIT.getKeyName()))
            .setIcon(Icons.EDIT, LinkElement.IconPosition.START)
            .asAnchorText();

    containerDiv.with(
        h3(messages.at(MessageKey.ADDRESS_CORRECTION_AS_ENTERED_HEADING.getKeyName()))
            .withClass("font-bold mb-2 w-full"),
        editElement);

    return containerDiv;
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
              Optional.ofNullable(suggestion.getSingleLineAddress())));
      selected = false;
    }

    return ImmutableList.copyOf(addressLabels);
  }

  private LabelTag renderAddress(
      Address address, boolean selected, Optional<String> singleLineAddress) {
    LabelTag containerDiv = label().withClass("flex flex-nowrap mb-2");

    InputTag input =
        TagCreator.input()
            .withType("radio")
            .withName(SELECTED_ADDRESS_NAME)
            .withClass("cf-radio-input h-4 w-4 mr-4 align-middle");

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

  private DivTag renderBottomNavButtons(Params params) {
    return div()
        .withClasses(ApplicantStyles.APPLICATION_NAV_BAR)
        // An empty div to take up the space to the left of the buttons.
        .with(div().withClasses("flex-grow"))
        .with(renderReviewButton(params))
        .with(renderPreviousButton(params))
        .with(renderNextButton(params));
  }

  private ButtonTag renderNextButton(Params params) {
    return submitButton(params.messages().at(MessageKey.BUTTON_NEXT_SCREEN.getKeyName()))
        .withClasses(ApplicantStyles.BUTTON_BLOCK_NEXT)
        .withId("cf-block-submit");
  }

  @Override
  protected ATag renderPreviousButton(Params params) {
    String redirectUrl =
        routes.ApplicantProgramBlocksController.previous(
                params.applicantId(), params.programId(), params.blockIndex(), params.inReview())
            .url();

    return a().withHref(redirectUrl)
        .withText(params.messages().at(MessageKey.BUTTON_PREVIOUS_SCREEN.getKeyName()))
        .withClasses(ApplicantStyles.BUTTON_BLOCK_PREVIOUS)
        .withId("cf-block-previous");
  }
}
