package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.u;
import static j2html.TagCreator.ul;

import com.google.common.collect.ImmutableList;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.FlashKey;
import controllers.ti.routes;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.H3Tag;
import j2html.tags.specialized.LiTag;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationModel;
import models.LifecycleStage;
import models.TrustedIntermediaryGroupModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import repository.SearchParameters;
import services.DateConverter;
import services.MessageKey;
import services.PhoneValidationResult;
import services.PhoneValidationUtils;
import services.applicant.ApplicantData;
import services.applicant.ApplicantPersonalInfo;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.ti.TrustedIntermediaryService;
import views.HtmlBundle;
import views.ViewUtils;
import views.admin.ti.TrustedIntermediaryGroupListView;
import views.components.Icons;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.ReferenceClasses;

/** Renders a page for a trusted intermediary to manage their clients. */
public class TrustedIntermediaryClientListView extends TrustedIntermediaryDashboardView {
  private static final Logger logger =
      LoggerFactory.getLogger(TrustedIntermediaryClientListView.class);
  private final DateConverter dateConverter;
  private final ProgramService programService;
  private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

  @Inject
  public TrustedIntermediaryClientListView(
      ApplicantLayout layout,
      DateConverter dateConverter,
      ProgramService programService,
      Config configuration) {
    super(configuration, layout);
    this.dateConverter = checkNotNull(dateConverter);
    this.programService = checkNotNull(programService);
  }

  public Content render(
      TrustedIntermediaryGroupModel tiGroup,
      ApplicantPersonalInfo personalInfo,
      ImmutableList<AccountModel> managedAccounts,
      int totalPageCount,
      int page,
      SearchParameters searchParameters,
      Http.Request request,
      Messages messages,
      Long currentTisApplicantId) {
    HtmlBundle bundle =
        layout
            .getBundle(request)
            .setTitle(messages.at(MessageKey.TITLE_TI_DASHBOARD.getKeyName()))
            .addMainContent(
                h1(tiGroup.getName()).withClasses(BaseStyles.TI_HEADER_BAND_H1),
                renderTabButtons(messages, TabType.CLIENT_LIST),
                div(
                        renderSubHeader(messages.at(MessageKey.TITLE_ALL_CLIENTS.getKeyName()))
                            .withClass("mb-0"),
                        renderAddNewClientButton(messages, tiGroup.id))
                    .withClasses(BaseStyles.TI_HEADER_BAND_H2, "justify-between"),
                div(
                        h4(messages.at(MessageKey.HEADER_SEARCH.getKeyName())),
                        renderSearchForm(
                            request, searchParameters, messages, managedAccounts.size()),
                        renderClientList(
                            managedAccounts, searchParameters, page, totalPageCount, messages))
                    .withClasses("px-20"))
            .addMainStyles("bg-white");

    Http.Flash flash = request.flash();
    if (flash.get(FlashKey.ERROR).isPresent()) {
      LoggerFactory.getLogger(TrustedIntermediaryGroupListView.class)
          .info(request.flash().get(FlashKey.ERROR).get());
      bundle.addToastMessages(
          ToastMessage.errorNonLocalized(flash.get(FlashKey.ERROR).get()).setDuration(-1));
    }
    return layout.renderWithNav(request, personalInfo, messages, bundle, currentTisApplicantId);
  }

  private ATag renderAddNewClientButton(Messages messages, Long tiGroupId) {
    String redirectUrl = routes.TrustedIntermediaryController.showAddClientForm(tiGroupId).url();
    return new ATag()
        .withText(messages.at(MessageKey.BUTTON_ADD_NEW_CLIENT.getKeyName()))
        .withId("add-new-client")
        .withClasses("usa-button")
        .withHref(redirectUrl);
  }

  private DivTag renderSearchForm(
      Http.Request request,
      SearchParameters searchParameters,
      Messages messages,
      int totalClients) {
    boolean isValidSearch = TrustedIntermediaryService.validateSearch(searchParameters);
    return div()
        .with(
            form()
                .withId("ti-search-form")
                .withClasses("mb-6")
                .withMethod("GET")
                .withAction(
                    routes.TrustedIntermediaryController.dashboard(
                            /* paramName=  nameQuery */ Optional.empty(),
                            /* paramName=  dayQuery */ Optional.empty(),
                            /* paramName=  monthQuery */ Optional.empty(),
                            /* paramName=  yearQuery */ Optional.empty(),
                            /* paramName=  page */ Optional.empty())
                        .url())
                .with(
                    div(
                            div(
                                label(messages.at(MessageKey.SEARCH_BY_NAME.getKeyName()))
                                    .withClass("usa-label")
                                    .withId("name-search")
                                    .withFor("name-query"),
                                span(messages.at(MessageKey.NAME_EXAMPLE.getKeyName()))
                                    .withClass("usa-hint")),
                            input()
                                .withClasses("usa-input", "mt-12")
                                .withId("name-query")
                                .withName("nameQuery")
                                .withValue(searchParameters.nameQuery().orElse("")))
                        .withClasses("flex", "flex-col", "justify-between"),
                    ViewUtils.makeMemorableDate(
                            searchParameters.dayQuery().orElse(""),
                            searchParameters.monthQuery().orElse(""),
                            searchParameters.yearQuery().orElse(""),
                            messages.at(MessageKey.SEARCH_BY_DOB.getKeyName()),
                            !isValidSearch,
                            Optional.of(messages))
                        .withClass("ml-6"),
                    makeCsrfTokenInputTag(request),
                    div(submitButton(messages.at(MessageKey.BUTTON_SEARCH.getKeyName()))
                            .withClasses("usa-button usa-button--outline", "ml-6", "h-11"))
                        .withClasses("flex", "flex-col", "justify-end"))
                .withClasses("flex", "my-6"))
        .with(
            div()
                .condWith(
                    isValidSearch,
                    renderDisplayingNumberOfClients(totalClients, searchParameters, messages)
                        .attr("data-testid", "displaying-clients")
                        .withClasses("mr-6"))
                .with(u(renderClearSearchLink(messages)).withClasses("text-sm"))
                .withClasses("flex", "items-center", "mb-4"));
  }

  private H3Tag renderDisplayingNumberOfClients(
      int totalClients, SearchParameters searchParameters, Messages messages) {
    boolean noSearchTerms =
        TrustedIntermediaryService.findMissingSearchParams(searchParameters).size() == 4;
    if (noSearchTerms) {
      return h3(messages.at(MessageKey.TITLE_DISPLAY_ALL_CLIENTS.getKeyName()));
    }
    if (totalClients == 1) {
      return h3(messages.at(MessageKey.TITLE_DISPLAY_ONE_CLIENT.getKeyName()));
    }
    return h3(messages.at(MessageKey.TITLE_DISPLAY_MULTI_CLIENTS.getKeyName(), totalClients));
  }

  private ATag renderClearSearchLink(Messages messages) {
    return new LinkElement()
        .setId(ReferenceClasses.TI_CLEAR_SEARCH)
        .setText(messages.at(MessageKey.BUTTON_CLEAR_SEARCH.getKeyName()))
        .asAnchorText();
  }

  private DivTag renderClientList(
      ImmutableList<AccountModel> managedAccounts,
      SearchParameters searchParameters,
      int page,
      int totalPageCount,
      Messages messages) {
    DivTag clientsList =
        div()
            .with(
                ul().withClass("usa-card-group")
                    .with(
                        each(
                            managedAccounts.stream()
                                .sorted(Comparator.comparing(AccountModel::getApplicantDisplayName))
                                .collect(Collectors.toList()),
                            account -> renderClientCard(account, messages))));

    return clientsList.condWith(
        managedAccounts.size() > 0,
        renderPagination(
            page,
            totalPageCount,
            pageNumber ->
                routes.TrustedIntermediaryController.dashboard(
                    searchParameters.nameQuery(),
                    searchParameters.dayQuery(),
                    searchParameters.monthQuery(),
                    searchParameters.yearQuery(),
                    Optional.of(pageNumber)),
            Optional.of(messages)));
  }

  private LiTag renderClientCard(AccountModel account, Messages messages) {
    return li().withClass("usa-card grid-col-12")
        .with(
            div()
                .withClass("usa-card__container")
                .with(
                    div()
                        .withClasses("usa-card__header", "flex justify-between")
                        .with(
                            div(
                                    div(
                                            renderSubHeader(account.getApplicantDisplayName())
                                                .withClass("usa-card__heading"),
                                            u(renderEditClientLink(account.id, messages))
                                                .withClass("ml-2"))
                                        .withClass("flex"),
                                    renderClientDateOfBirth(account))
                                .withClass("flex-col"),
                            renderIndexPageLink(account, messages)),
                    div()
                        .withClasses("usa-card__body", "flex")
                        .with(
                            renderCardContactInfo(account, messages).withClasses("w-2/5"),
                            renderCardApplications(account, messages).withClasses("ml-10 w-2/5"),
                            renderCardNotes(account.getTiNote(), messages)
                                .withClasses("ml-10 w-3/5"))));
  }

  private DivTag renderCardContactInfo(AccountModel account, Messages messages) {
    Optional<ApplicantModel> newestApplicant = account.newestApplicant();
    if (newestApplicant.isEmpty()) {
      return div();
    }
    ApplicantData applicantData = newestApplicant.get().getApplicantData();
    Optional<String> optionalPhoneNumber = applicantData.getPhoneNumber();
    Optional<String> optionalEmail = applicantData.getApplicantEmail();

    return div(
        label(messages.at(MessageKey.CONTACT_INFO_LABEL.getKeyName()))
            .withFor("card_contact_info")
            .withClass("whitespace-nowrap"),
        div()
            .condWith(
                optionalPhoneNumber.isPresent(),
                div(
                        Icons.svg(Icons.PHONE).withClasses("h-3", "w-3", "mr-1"),
                        p(formatPhone(optionalPhoneNumber.orElse(""))))
                    .withClass("flex items-center"))
            .condWith(
                optionalEmail.isPresent(),
                div(
                        Icons.svg(Icons.EMAIL).withClasses("h-3", "w-3", "mr-1"),
                        p(optionalEmail.orElse("")))
                    .withClass("flex items-center"))
            .withClass("text-xs")
            .withId("card_contact_info"));
  }

  private String formatPhone(String phone) {
    try {
      PhoneValidationResult phoneValidationResults =
          PhoneValidationUtils.determineCountryCode(Optional.ofNullable(phone));

      Phonenumber.PhoneNumber phoneNumber =
          PHONE_NUMBER_UTIL.parse(phone, phoneValidationResults.getCountryCode().orElse(""));
      return PHONE_NUMBER_UTIL.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
    } catch (NumberParseException e) {
      return "-";
    }
  }

  private DivTag renderCardApplications(AccountModel account, Messages messages) {
    Optional<ApplicantModel> newestApplicant = account.newestApplicant();
    if (newestApplicant.isEmpty()) {
      return div();
    }

    ImmutableList<ApplicationModel> submittedApplications =
        newestApplicant.get().getApplications().stream()
            .filter(application -> application.getLifecycleStage() == LifecycleStage.ACTIVE)
            .collect(ImmutableList.toImmutableList());
    int applicationCount = submittedApplications.size();
    String programs =
        submittedApplications.stream()
            .map(
                application -> {
                  try {
                    return programService
                        .getFullProgramDefinition(application.getProgram().id)
                        .localizedName()
                        .getDefault();
                  } catch (ProgramNotFoundException e) {
                    // Since this is just trying to get a csv representation of the programs
                    // put a placeholder string if this exception occurs. Realistically at
                    // this area of CiviForm it "shouldn't" occur, but why leave it up to
                    // fate to raise an exception.
                    logger.error(
                        "Unable to build complete string of programs. At least one program was not"
                            + " found",
                        e);
                    return "<unknown>";
                  }
                })
            .collect(Collectors.joining(", "));

    return div(
        label(createApplicationsSubmittedText(applicationCount, messages))
            .withFor("card_applications")
            .withClass("whitespace-nowrap"),
        p(programs).withClass("text-xs").withId("card_applications"));
  }

  private String createApplicationsSubmittedText(int applicationCount, Messages messages) {
    if (applicationCount == 1) {
      return messages.at(MessageKey.CONTENT_ONE_APP_SUBMITTED.getKeyName());
    }
    return messages.at(MessageKey.CONTENT_NUMBER_OF_APP_SUBMITTED.getKeyName(), applicationCount);
  }

  private DivTag renderCardNotes(String notes, Messages messages) {
    return div(
        label(messages.at(MessageKey.NOTES_LABEL.getKeyName())).withFor("card_notes"),
        p(notes).withClass("text-xs").withId("card_notes"));
  }

  private ATag renderEditClientLink(Long accountId, Messages messages) {
    return new LinkElement()
        .setId("edit-client")
        .setText(messages.at(MessageKey.LINK_EDIT.getKeyName()))
        .setHref(
            controllers.ti.routes.TrustedIntermediaryController.showEditClientForm(accountId).url())
        .asAnchorText();
  }

  private DivTag renderClientDateOfBirth(AccountModel account) {
    Optional<ApplicantModel> newestApplicant = account.newestApplicant();
    if (newestApplicant.isEmpty()) {
      return div();
    }
    String currentDob =
        newestApplicant
            .get()
            .getApplicantData()
            .getDateOfBirth()
            .map(this.dateConverter::formatIso8601Date)
            .orElse("");
    return div()
        .withClasses("flex", "text-xs")
        .with(
            Icons.svg(Icons.CAKE).withClasses("h-3", "w-3", "mr-1"), p(String.format(currentDob)));
  }

  private DivTag renderIndexPageLink(AccountModel applicant, Messages messages) {
    Optional<ApplicantModel> newestApplicant = applicant.newestApplicant();
    if (newestApplicant.isEmpty()) {
      return div();
    }
    return div()
        .with(
            new ATag()
                .withClasses("usa-button usa-button--outline")
                .withId(String.format("act-as-%d-button", newestApplicant.get().id))
                .withText(messages.at(MessageKey.BUTTON_VIEW_APPLICATIONS.getKeyName()))
                .withHref(
                    controllers.applicant.routes.ApplicantProgramsController.indexWithApplicantId(
                            newestApplicant.get().id, /* categories */ ImmutableList.of())
                        .url()));
  }
}
