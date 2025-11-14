package services;

/** Contains keys into the {@code messages} files used for translation. */
public enum MessageKey {
  ACCT_STATUS_LABEL("label.acctStatus"),
  ADDRESS_CORRECTION_AS_ENTERED_HEADING("content.addressEntered"),
  ADDRESS_CORRECTION_SUGGESTED_ADDRESS_HEADING("content.suggestedAddress"),
  ADDRESS_CORRECTION_SUGGESTED_ADDRESSES_HEADING("content.suggestedAddresses"),
  ADDRESS_CORRECTION_TITLE("title.confirmAddress"),
  ADDRESS_CORRECTION_LINE_1("content.confirmAddressLine1"),
  ADDRESS_CORRECTION_FOUND_SIMILAR_LINE_2("content.foundSimilarAddressLine2"),
  ADDRESS_CORRECTION_NO_VALID_LINE_2("content.noValidAddressLine2"),
  ADDRESS_CORRECTION_CONFIRM_BUTTON("button.confirmAddress"),
  ADDRESS_LABEL_CITY("label.city"),
  ADDRESS_LABEL_LINE_2("label.addressLine2"),
  ADDRESS_LABEL_STATE("label.state"),
  ADDRESS_LABEL_STATE_SELECT("label.selectState"),
  ADDRESS_LABEL_STREET("label.street"),
  ADDRESS_LABEL_ZIPCODE("label.zipcode"),
  ADDRESS_VALIDATION_CITY_REQUIRED("validation.cityRequired"),
  ADDRESS_VALIDATION_INVALID_ZIPCODE("validation.invalidZipcode"),
  ADDRESS_VALIDATION_NO_PO_BOX("validation.noPoBox"),
  ADDRESS_VALIDATION_STATE_REQUIRED("validation.stateRequired"),
  ADDRESS_VALIDATION_STREET_REQUIRED("validation.streetRequired"),
  ALERT_CREATE_ACCOUNT("alert.createAccount"), // North Star only
  ALERT_CREATE_ACCOUNT_DESCRIPTION("alert.createAccountDescription"), // North Star only
  ALERT_LOGIN_ONLY("alert.loginOnly"), // North Star only
  ALERT_LOGIN_ONLY_DESCRIPTION("alert.loginOnlyDescription"), // North Star only
  ALERT_LOGIN_ONLY_CREATE_ACCOUNT("alert.createAccountForLoginOnly"), // North Star only
  ALERT_ELIGIBILITY_APPLICANT_ELIGIBLE_TITLE("alert.eligibility_applicant_eligible_title"),
  ALERT_ELIGIBILITY_APPLICANT_ELIGIBLE_TEXT("alert.eligibility_applicant_eligible_text"),
  ALERT_ELIGIBILITY_APPLICANT_NOT_ELIGIBLE_TITLE("alert.eligibility_applicant_not_eligible_title"),
  ALERT_ELIGIBILITY_APPLICANT_NOT_ELIGIBLE_TEXT("alert.eligibility_applicant_not_eligible_text"),
  ALERT_ELIGIBILITY_APPLICANT_NOT_ELIGIBLE_TEXT_SHORT(
      "alert.eligibilityApplicantNotEligibleTextShort"), // North Star only
  ALERT_ELIGIBILITY_APPLICANT_FASTFORWARDED_ELIGIBLE_TITLE(
      "alert.eligibility_applicant_fastforwarded_eligible_title"),
  ALERT_ELIGIBILITY_APPLICANT_FASTFORWARDED_ELIGIBLE_TEXT(
      "alert.eligibility_applicant_fastforwarded_eligible_text"),
  ALERT_ELIGIBILITY_APPLICANT_FASTFORWARDED_NOT_ELIGIBLE_TITLE(
      "alert.eligibility_applicant_fastforwarded_not_eligible_title"),
  ALERT_ELIGIBILITY_APPLICANT_FASTFORWARDED_NOT_ELIGIBLE_TEXT(
      "alert.eligibility_applicant_fastforwarded_not_eligible_text"),
  ALERT_ELIGIBILITY_TI_ELIGIBLE_TITLE("alert.eligibility_ti_eligible_title"),
  ALERT_ELIGIBILITY_TI_ELIGIBLE_TEXT("alert.eligibility_ti_eligible_text"),
  ALERT_ELIGIBILITY_TI_NOT_ELIGIBLE_TITLE("alert.eligibility_ti_not_eligible_title"),
  ALERT_ELIGIBILITY_TI_NOT_ELIGIBLE_TEXT("alert.eligibility_ti_not_eligible_text"),
  ALERT_ELIGIBILITY_TI_NOT_ELIGIBLE_TEXT_SHORT(
      "alert.eligibilityTiNotEligibleTextShort"), // North Star only
  ALERT_ELIGIBILITY_TI_FASTFORWARDED_ELIGIBLE_TITLE(
      "alert.eligibility_ti_fastforwarded_eligible_title"),
  ALERT_ELIGIBILITY_TI_FASTFORWARDED_ELIGIBLE_TEXT(
      "alert.eligibility_ti_fastforwarded_eligible_text"),
  ALERT_ELIGIBILITY_TI_FASTFORWARDED_NOT_ELIGIBLE_TITLE(
      "alert.eligibility_ti_fastforwarded_not_eligible_title"),
  ALERT_ELIGIBILITY_TI_FASTFORWARDED_NOT_ELIGIBLE_TEXT(
      "alert.eligibility_ti_fastforwarded_not_eligible_text"),
  ALERT_LIKELY_ELIGIBLE("alert.likelyEligible"), // North Star only
  ALERT_LIKELY_INELIGIBLE("alert.likelyIneligible"), // North Star only
  ALERT_CLIENT_LIKELY_ELIGIBLE("alert.clientLikelyEligible"), // North Star only
  ALERT_CLIENT_LIKELY_INELIGIBLE("alert.clientLikelyIneligible"), // North Star only
  ALERT_NO_PROGRAMS_AVAILABLE("alert.noProgramsAvailable"), // North Star only
  ALERT_SUBMITTED("alert.submitted"), // North Star only
  ARIA_LABEL_CATEGORIES("ariaLabel.categories"), // North Star only
  ARIA_LABEL_EDIT("ariaLabel.edit"),
  ARIA_LABEL_ANSWER("ariaLabel.answer"),
  BANNER_ERROR_SAVING_APPLICATION("banner.errorSavingApplication"),
  BANNER_CLIENT_INFO_UPDATED("banner.clientInfoUpdated"),
  BANNER_CLIENT_ACCT_DELETED("banner.clientAcctDeleted"),
  BANNER_ACCT_DELETE_ERROR("banner.acctDeleteError"),
  BANNER_ACCT_DELETE_ERROR_REASON("banner.acctDeleteErrorReason"),
  BANNER_NEW_CLIENT_CREATED("banner.newClientCreated"),
  BANNER_GUEST_BANNER_TEXT("banner.guestBannerText"),
  BANNER_GOV_WEBSITE_SECTION_HEADER("banner.govWebsiteSectionHeader"),
  BANNER_GOV_WEBSITE_SECTION_CONTENT("banner.govWebsiteSectionContent"),
  BANNER_HOUR("banner.hour"),
  BANNER_HOUR_AND_MINUTE("banner.hourAndMinute"),
  BANNER_HOUR_AND_MINUTES("banner.hourAndMinutes"),
  BANNER_HOURS("banner.hours"),
  BANNER_HOURS_AND_MINUTE("banner.hoursAndMinute"),
  BANNER_HOURS_AND_MINUTES("banner.hoursAndMinutes"),
  BANNER_HTTPS_SECTION_HEADER("banner.httpsSectionHeader"),
  BANNER_HTTPS_SECTION_CONTENT("banner.httpsSectionContent"),
  BANNER_LINK("banner.link"),
  BANNER_MINUTE("banner.minute"),
  BANNER_MINUTES("banner.minutes"),
  BANNER_SESSION_EXPIRATION("banner.sessionExpiration"),
  BANNER_TITLE("banner.title"),
  BANNER_VIEW_APPLICATION("banner.viewApplication"),
  BLOCK_INDEX_LABEL("label.blockIndexLabel"),
  BUTTON_ADD_NEW_CLIENT("button.addNewClient"),
  BUTTON_ADMIN_LOGIN("button.adminLogin"), // North star only
  BUTTON_APPLICANT_LOGIN("button.applicantLogin"), // North star only
  BUTTON_APPLY("button.apply"),
  BUTTON_APPLY_TO_PROGRAMS("button.applyToPrograms"),
  BUTTON_APPLY_SELECTIONS("button.applySelections"), // North Star only
  BUTTON_APPLY_SR("button.applySr"),
  BUTTON_BACK_TO_CLIENT_LIST("button.backToClientList"),
  BUTTON_BACK_TO_EDITING("button.backToEditing"),
  BUTTON_CANCEL("button.cancel"),
  BUTTON_CHOOSE_FILE("button.chooseFile"),
  BUTTON_CLEAR_SELECTIONS("button.clearSelections"), // North Star only
  BUTTON_CLOSE("button.close"),
  BUTTON_CONTINUE("button.continue"),
  BUTTON_CONTINUE_SR("button.continueSr"),
  BUTTON_CONTINUE_PRE_SCREENER_SR("button.continuePreScreenerSr"),
  BUTTON_CONTINUE_EDITING("button.continueEditing"),
  BUTTON_CONTINUE_WITHOUT_AN_ACCOUNT("button.continueWithoutAnAccount"),
  BUTTON_CREATE_ACCOUNT("button.createAccount"),
  BUTTON_DELETE_ACCT("button.deleteAcct"),
  BUTTON_DOWNLOAD_APPLICATION("button.downloadApplication"),
  BUTTON_DOWNLOAD_PDF("button.downloadPdf"),
  BUTTON_EDIT("button.edit"),
  BUTTON_EDIT_SR("button.editSr"),
  BUTTON_EDIT_PRE_SCREENER_SR("button.editPreScreenerSr"),
  BUTTON_EXIT_APPLICATION("button.exitApplication"),
  BUTTON_GO_BACK("button.goBack"),
  BUTTON_GO_BACK_AND_EDIT("button.goBackAndEdit"),
  BUTTON_LANGUAGES("button.languages"), // North Star only
  BUTTON_LOGIN("button.login"),
  BUTTON_CREATE_AN_ACCOUNT("button.createAnAccount"),
  BUTTON_CLEAR_SEARCH("button.clearSearch"),
  BUTTON_EDIT_MY_RESPONSES("button.editMyResponses"), // North Star only
  BUTTON_LOGIN_GUEST("button.guestLogin"),
  BUTTON_LOGOUT("button.logout"),
  BUTTON_NEXT("button.nextPage"),
  BUTTON_NEXT_SCREEN("button.nextScreen"),
  BUTTON_PREVIOUS_SCREEN("button.previousScreen"),
  BUTTON_BACK("button.back"),
  BUTTON_REVIEW_AND_EXIT("button.reviewAndExit"),
  BUTTON_REVIEW("button.review"),
  BUTTON_DELETE_FILE("button.deleteFile"),
  BUTTON_KEEP_ACCT("button.keepAcct"),
  BUTTON_KEEP_FILE("button.keepFile"),
  BUTTON_SAVE("button.save"),
  BUTTON_SAVE_AND_EXIT("button.saveAndExit"),
  BUTTON_SEARCH("button.search"),
  BUTTON_SELECT("button.select"),
  BUTTON_SIGNIN("button.signIn"),
  BUTTON_SKIP_FILEUPLOAD("button.skipFileUpload"),
  BUTTON_START("button.start"),
  BUTTON_START_HERE("button.startHere"),
  BUTTON_START_SURVEY("button.startSurvey"),
  BUTTON_START_APP("button.startApp"),
  BUTTON_CONTINUE_TO_APPLICATION("button.continueToApplication"),
  BUTTON_START_HERE_PRE_SCREENER_SR("button.startHerePreScreenerSr"),
  BUTTON_SUBMIT("button.submit"),
  BUTTON_SUBMIT_APPLICATION("button.submitApplication"), // North Star only
  BUTTON_UNTRANSLATED_SUBMIT("button.untranslatedSubmit"),
  BUTTON_VIEW_APPLICATIONS("button.viewApplications"),
  BUTTON_VIEW_AND_ADD_CLIENTS("button.viewAndAddClients"),
  BUTTON_VIEW_AND_APPLY("button.viewAndApply"), // North Star only
  BUTTON_VIEW_AND_APPLY_SR("button.viewAndApplySr"), // North Star only
  BUTTON_VIEW_IN_NEW_TAB("button.viewInNewTab"),
  BUTTON_VIEW_IN_NEW_TAB_SR("button.viewInNewTabSr"),
  BUTTON_HOME_PAGE("button.homePage"),
  CURRENCY_VALIDATION_MISFORMATTED("validation.currencyMisformatted"),
  CONTACT_INFO_LABEL("label.contactInfo"),
  CONTENT_ADMIN_LOGIN_PROMPT("content.adminLoginPrompt"),
  CONTENT_ADMIN_FOOTER_PROMPT("content.adminFooterPrompt"),
  CONTENT_AND("content.and"),
  CONTENT_BLOCK_PROGRESS("content.blockProgress"),
  CONTENT_BLOCK_PROGRESS_FULL("content.blockProgressFull"),
  CONTENT_BLOCK_PROGRESS_LABEL("content.blockProgressLabel"),
  CONTENT_SAVE_TIME("content.saveTimeServices"),
  CONTENT_CHANGE_ELIGIBILITY_ANSWERS("content.changeAnswersForEligibility"),
  CONTENT_CHANGE_ELIGIBILITY_ANSWERS_V2(
      "content.changeAnswersForEligibility.v2"), // North Star only
  CONTENT_CIVIFORM_DESCRIPTION("content.findProgramsDescription"),
  CONTENT_CLIENT_CREATED("content.clientCreated"),
  CONTENT_CONFIRMED("content.confirmed"),
  CONTENT_DOES_NOT_QUALIFY("content.doesNotQualify"),
  CONTENT_DISABLED_PROGRAM_INFO("content.disabledProgramInfo"),
  CONTENT_PRE_SCREENER_CONFIRMATION("content.preScreenerConfirmation"),
  CONTENT_PRE_SCREENER_CONFIRMATION_V2("content.preScreenerConfirmation.v2"), // North Star only
  CONTENT_PRE_SCREENER_CONFIRMATION_TI("content.preScreenerConfirmationTi"),
  CONTENT_PRE_SCREENER_CONFIRMATION_TI_V2(
      "content.preScreenerConfirmationTi.v2"), // North Star only
  CONTENT_PRE_SCREENER_NO_MATCHING_PROGRAMS("content.preScreenerNoMatchingPrograms"),
  CONTENT_PRE_SCREENER_NO_MATCHING_PROGRAMS_TI("content.preScreenerNoMatchingProgramsTi"),
  CONTENT_PRE_SCREENER_NO_MATCHING_PROGRAMS_NEXT_STEP(
      "content.preScreenerNoMatchingProgramsNextStep"),
  CONTENT_OTHER_PROGRAMS_TO_APPLY_FOR("content.otherProgramsToApplyFor"),
  CONTENT_ELIGIBILITY_CRITERIA("content.eligibilityCriteria"),
  CONTENT_ELIGIBILITY_CRITERIA_V2("content.eligibilityCriteria.v2"), // North Star only
  CONTENT_ELIGIBILITY_CRITERIA_V3("content.eligibilityCriteria.v3"), // North Star only
  CONTENT_EXTERNAL_PROGRAM_MODAL("content.externalProgramModal"),
  CONTENT_EMAIL_TOOLTIP("content.emailTooltip"),
  CONTENT_FIND_PROGRAMS("content.findPrograms"),
  CONTENT_GUEST_DESCRIPTION("content.guestDescription"),
  CONTENT_HOMEPAGE_INTRO("content.homepageIntro"), // North Star only
  CONTENT_HOMEPAGE_INTRO_V2("content.homepageIntro.v2"), // North Star only
  CONTENT_LAST_APPLICATION_DATE("content.lastApplicationDate"),
  CONTENT_LOGIN_PROMPT("content.loginPrompt"),
  CONTENT_LOGIN_DISABLED_PROMPT("content.loginDisabledPrompt"),
  CONTENT_LOGIN_PROMPT_ALTERNATIVE("content.alternativeLoginPrompt"),
  CONTENT_LOGIN_TO_EXISTING_ACCOUNT("content.loginToExistingAccount"), // North Star only
  CONTENT_MULTIPLE_VALUES_INPUT_HINT("content.multipleValuesInputHint"),
  CONTENT_NO_CHANGES("content.noChanges"),
  CONTENT_NO_EMAIL_ADDRESS("content.noEmailAddress"),
  CONTENT_NO_APPLICATIONS("content.noApplications"),
  CONTENT_NOT_LOGGED_IN("content.notLoggedIn"),
  CONTENT_NUMBER_OF_APP_SUBMITTED("content.numberOfAppSubmitted"),
  CONTENT_ONE_APP_SUBMITTED("content.oneAppSubmitted"),
  CONTENT_OPTIONAL("content.optional"),
  CONTENT_OR("content.or"),
  CONTENT_WARNING("content.warning"),
  CONTENT_PLEASE_CREATE_ACCOUNT("content.pleaseCreateAccount"),
  CONTENT_PREVIOUSLY_ANSWERED_ON("content.previouslyAnsweredOn"),
  CONTENT_SELECT_LANGUAGE("label.selectLanguage"),
  CONTENT_SIGNIN_MODAL("content.signInModal"), // North Star only
  CONTENT_YOU_CAN_PRINT("content.youCanPrint"), // North Star only
  ERROR_ANNOUNCEMENT_SR("validation.errorAnnouncementSr"),
  ERROR_EMAIL_IN_USE_CLIENT_CREATE("label.errorEmailInUseForClientCreate"),
  ERROR_EMAIL_IN_USE_CLIENT_EDIT("label.errorEmailInUseForClientEdit"),
  ERROR_FIRST_NAME("label.errorFirstName"),
  ERROR_LAST_NAME("label.errorLastName"),
  ERROR_INTERNAL_SERVER_TITLE_V2("error.internalServerTitle.v2"),
  ERROR_INTERNAL_SERVER_SUBTITLE("error.internalServerSubtitle"),
  ERROR_INTERNAL_SERVER_DESCRIPTION("error.internalServerDescription"),
  ERROR_INTERNAL_SERVER_HOME_BUTTON("error.internalServerHomeButton"),
  ERROR_STATUS_CODE("error.statusCode"),
  ERROR_NOT_FOUND_TITLE("error.notFoundTitle"),
  ERROR_NOT_FOUND_DESCRIPTION("error.notFoundDescription"),
  ERROR_NOT_FOUND_DESCRIPTION_LINK("error.notFoundDescriptionLink"),
  DATE_VALIDATION_INVALID_DATE_FORMAT("validation.invalidDateFormat"),
  DATE_VALIDATION_DATE_BEYOND_ALLOWABLE_YEARS_IN_PAST("validation.dateBeyondAllowablePast"),
  DATE_VALIDATION_DATE_BEYOND_ALLOWABLE_YEARS_IN_FUTURE("validation.dateBeyondAllowableFuture"),
  DATE_VALIDATION_DOB_NOT_IN_PAST("validation.dobNotInPast"),
  DATE_VALIDATION_IMPOSSIBLE_DOB("validation.impossibleDob"),
  DATE_VALIDATION_FUTURE_DATE_REQUIRED("validation.futureDateRequired"),
  DATE_VALIDATION_DATE_TOO_FAR_IN_PAST("validation.dateTooFarInPast"),
  DATE_VALIDATION_PAST_DATE_REQUIRED("validation.pastDateRequired"),
  DATE_VALIDATION_DATE_TOO_FAR_IN_FUTURE("validation.dateTooFarInFuture"),
  DATE_VALIDATION_DATE_NOT_IN_RANGE("validation.dateNotInRange"),
  DATE_VALIDATION_CURRENT_DATE_REQUIRED("validation.currentDateRequired"),
  DAY_LABEL("label.day"),
  DIALOG_DELETE_CONFIRMATION("dialog.deleteConfirmation"),
  DOB_EXAMPLE("label.dobExample"),
  DOB_ERROR_LABEL("label.errorDOB"),
  DOB_LABEL("label.dob"),
  DROPDOWN_PLACEHOLDER("placeholder.noDropdownSelection"),
  END_SESSION("header.endSession"),
  END_YOUR_SESSION("banner.endYourSession"),
  EMAIL_APPLICATION_RECEIVED_BODY("email.applicationReceivedBody"),
  EMAIL_APPLICATION_RECEIVED_SUBJECT("email.applicationReceivedSubject"),
  EMAIL_APPLICATION_UPDATE_SUBJECT("email.applicationUpdateSubject"),
  EMAIL_LOGIN_TO_CIVIFORM("email.loginToCiviform"),
  EMAIL_TI_APPLICATION_SUBMITTED_BODY("email.tiApplicationSubmittedBody"),
  EMAIL_TI_APPLICATION_SUBMITTED_SUBJECT("email.tiApplicationSubmittedSubject"),
  EMAIL_TI_APPLICATION_UPDATE_SUBJECT("email.tiApplicationUpdateSubject"),
  EMAIL_TI_APPLICATION_UPDATE_BODY("email.tiApplicationUpdateBody"),
  EMAIL_TI_MANAGE_YOUR_CLIENTS("email.tiManageYourClients"),
  EMAIL_LABEL("label.email"),
  ENUMERATOR_BUTTON_ADD_ENTITY("button.addEntity"),
  ENUMERATOR_BUTTON_REMOVE_ENTITY("button.removeEntity"),
  ENUMERATOR_DIALOG_CONFIRM_DELETE_ALL_BUTTONS_SAVE("dialog.confirmDeleteAllButtonsSave"),
  ENUMERATOR_PLACEHOLDER_ENTITY_NAME("placeholder.entityName"),
  ENUMERATOR_VALIDATION_DUPLICATE_ENTITY_NAME("validation.duplicateEntityName"),
  ENUMERATOR_VALIDATION_ENTITY_REQUIRED("validation.entityNameRequired"),
  ENUMERATOR_VALIDATION_TOO_MANY_ENTITIES("validation.tooManyEntities"),
  ENUMERATOR_VALIDATION_TOO_FEW_ENTITIES("validation.tooFewEntities"),
  ERROR_INCOMPLETE_DATE("error.incompleteDate"),
  FILEUPLOAD_VALIDATION_FILE_REQUIRED("validation.fileRequired"),
  FILEUPLOAD_VALIDATION_FILE_TOO_LARGE("validation.fileTooLarge"),
  FOOTER_ABOUT_CIVIFORM("footer.aboutCiviform"),
  FOOTER_OFFICIAL_WEBSITE_OF("footer.officialWebsiteOf"),
  FOOTER_RETURN_TO_TOP("footer.returnToTop"),
  FOOTER_SUPPORT_LINK_DESCRIPTION("footer.supportLinkDescription"),
  FOOTER_TECHNICAL_SUPPORT("footer.technicalSupport"),
  FOOTER_TECHNICAL_SUPPORT_V2("footer.technicalSupport.v2"), // North Star only
  GENERAL_LOGIN_MODAL_PROMPT("content.generalLoginModalPrompt"),
  GUEST("guest"),
  GUEST_INDICATOR("header.guestIndicator"),
  HEADER_ACCT_SETTING("header.acctSettings"),
  HEADER_CLIENT_LIST("header.clientList"),
  HEADER_SEARCH("header.search"),
  HEADING_APPLICANT_NAME("heading.applicantName"), // North Star Only
  HEADING_APPLICATION_STEPS("heading.applicationSteps"), // North Star only
  HEADING_CONFIRMATION_NUMBER("heading.confirmationNumber"), // North Star Only
  HEADING_DATE_SUBMITTED("heading.dateSubmitted"), // NorthStar Only
  HEADING_ELIGIBILITY_CRITERIA("heading.eligibilityCriteria"), // North Star only
  HEADING_FOR_YOUR_RECORDS("heading.forYourRecords"), // North Star only
  HEADING_INFORMATION_ARIA_LABEL_PREFIX("heading.informationAriaLabelPrefix"), // North Star only
  HEADING_HOMEPAGE("heading.homepage"), // North Star only
  HEADING_HOMEPAGE_V2("heading.homepage.v2"), // North Star only
  HEADING_NEXT_STEPS("heading.nextSteps"), // North Star only
  HEADING_PROGRAM_OVERVIEW("heading.programOverview"), // North Star only
  HEADING_REVIEW_AND_SUBMIT("heading.reviewAndSubmit"), // North Star only
  HEADING_SUCCESS_ARIA_LABEL_PREFIX("heading.successAriaLabelPrefix"), // North Star only
  HEADING_YOUR_SUBMISSION_INFORMATION("heading.yourSubmissionInformation"), // North Star only
  ID_VALIDATION_NUMBER_REQUIRED("validation.numberRequired"),
  ID_VALIDATION_NUMBER_REQUIRED_V2("validation.numberRequired.v2"),
  ID_VALIDATION_TOO_LONG("validation.idTooLong"),
  ID_VALIDATION_TOO_SHORT("validation.idTooShort"),
  INITIAL_LOGIN_MODAL_PROMPT("content.initialLoginModalPrompt"),
  INPUT_FILE_ALREADY_UPLOADED("input.fileAlreadyUploaded"),
  INPUT_SINGLE_FILE_UPLOAD_HINT("input.singleFileUploadHint"),
  INPUT_MULTIPLE_FILE_UPLOAD_HINT("input.multipleFileUploadHint"),
  INPUT_UNLIMITED_FILE_UPLOAD_HINT("input.unlimitedFileUploadHint"),
  INVALID_INPUT("validation.invalidInput"),
  LABEL_PROGRAM_FILTERS("label.programFilters"),
  LABEL_PROGRAM_FILTERS_V2("label.programFilters.v2"), // North Star only
  LANGUAGE_LABEL_SR("label.languageSr"),
  LINK_ADMIN_LOGIN("link.adminLogin"),
  LINK_ALL_DONE("link.allDone"),
  LINK_APPLY_TO_ANOTHER_PROGRAM("link.applyToAnotherProgram"),
  LINK_BACK_TO_EDIT_PROGRAM_BLOCK("link.backToEditProgramBlock"),
  LINK_BACK_TO_TOP("link.backToTop"),
  LINK_CREATE_ACCOUNT_OR_SIGN_IN("link.createAccountOrSignIn"),
  LINK_CREATE_ACCOUNT_FROM_OVERVIEW("link.createAccountFromOverview"), // North Star only
  LINK_EDIT("link.edit"),
  LINK_ANSWER("link.answer"),
  LINK_OPENS_NEW_TAB_SR("link.opensNewTabSr"),
  LINK_START_AS_GUEST("link.startAsGuest"), // North Star only
  LABEL_PRIMARY_NAVIGATION("label.primaryNavigation"),
  LABEL_AGENCY_IDENTIFIER("label.agencyIdentifier"),
  LABEL_GUEST_SESSION_ALERT("label.guestSessionAlert"),
  LABEL_IN_PROGRESS("label.inProgress"), // North Star only
  LABEL_SUBMITTED("label.submitted"), // North Star only
  LABEL_SUBMITTED_ON("label.submittedOn"), // North Star only
  LABEL_STATUS_ON("label.statusOn"), // North Star only
  LINK_PROGRAM_DETAILS("link.programDetails"),
  LINK_PROGRAM_DETAILS_SR("link.programDetailsSr"),
  LINK_PROGRAM_SETTINGS("link.programSettings"),
  LINK_REMOVE_FILE("link.removeFile"),
  LINK_REMOVE_FILE_SR("link.removeFileSr"), // North Star only
  LINK_SELECT_NEW_CLIENT("link.selectNewClient"),
  LINK_SKIP_TO_MAIN_CONTENT("link.skipToMainContent"), // North Star only
  LINK_HOME("link.home"), // North Star only
  LINK_APPLICATION_FOR_PROGRAM("link.applicationForProgram"), // North Star
  // Only
  MAP_ADD_FILTER_BUTTON("map.addFilterButton"), // North Star only
  MAP_ADD_FILTERS_SUBTITLE("map.addFiltersSubtitle"), // North Star only
  MAP_ADD_FILTERS_TITLE("map.addFiltersTitle"), // North Star only
  MAP_ADD_TAG_BUTTON("map.addTagButton"), // North star only
  MAP_ADD_TAG_SUBTITLE("map.addTagSubtitle"), // North Star only
  MAP_ADD_TAG_TITLE("map.addTagTitle"), // North Star only
  MAP_APPLY_FILTERS_BUTTON_TEXT("map.applyFiltersButtonText"), // North Star only
  MAP_ARIA_LABEL_NEXT_PAGE("map.ariaLabelNextPage"), // North Star only
  MAP_ARIA_LABEL_PAGINATION_LIST("map.ariaLabelPaginationList"), // North Star only
  MAP_ARIA_LABEL_PREVIOUS_PAGE("map.ariaLabelPreviousPage"), // North Star only
  MAP_AVAILABLE_LOCATIONS("map.availableLocations"), // North Star only
  MAP_DISPLAY_NAME_LABEL("map.displayNameLabel"), // North Star only
  MAP_FILTER_LEGEND_TEXT("map.filterLegendText"), // North Star only
  MAP_GO_TO_PAGE("map.goToPage"), // North Star only
  MAP_KEY_LABEL("map.keyLabel"), // North Star only
  MAP_KEY_NOT_FOUND_ERROR("map.keyNotFoundError"), // North Star only
  MAP_LOCATION_ADDRESS_HELP_TEXT("map.locationAddressHelpText"), // North Star only
  MAP_LOCATION_ADDRESS_LABEL("map.locationAddressLabel"), // North Star only
  MAP_LOCATION_DETAILS_URL_HELP_TEXT("map.locationDetailsUrlHelpText"), // North Star only
  MAP_LOCATION_DETAILS_URL_LABEL("map.locationDetailsUrlLabel"), // North Star only
  MAP_LOCATION_LINK_TEXT("map.locationLinkText"), // North Star only
  MAP_LOCATION_LINK_TEXT_SR("map.locationLinkTextSr"), // North Star only
  MAP_LOCATION_NAME_HELP_TEXT("map.locationNameHelpText"), // North Star only
  MAP_LOCATION_NAME_LABEL("map.locationNameLabel"), // North Star only
  MAP_LOCATIONS_COUNT("map.locationsCount"), // North Star only
  MAP_LOCATIONS_SELECTED_COUNT("map.locationsSelectedCount"), // North Star only
  MAP_MAX_LOCATION_SELECTIONS_LABEL("map.maxLocationSelectionsLabel"), // North Star only
  MAP_NO_RESULTS_FOUND("map.noResultsFound"), // North Star only
  MAP_NO_SELECTIONS_MESSAGE("map.noSelectionsMessage"), // North Star only
  MAP_PAGINATION_STATUS("map.paginationStatus"), // North Star only
  MAP_REGION_ALT_TEXT("map.mapRegionAltText"), // North Star only
  MAP_RESET_FILTERS_BUTTON_TEXT("map.resetFiltersButtonText"), // North Star only
  MAP_SELECTED_BUTTON_TEXT("map.mapSelectedButtonText"), // North Star only
  MAP_SELECTED_LOCATIONS_HEADING("map.selectedLocationsHeading"), // North Star only
  MAP_SELECT_LOCATION_BUTTON_TEXT("map.selectLocationButtonText"), // North Star only
  MAP_SELECT_LOCATIONS("map.selectLocations"), // North Star only
  MAP_SELECT_OPTION_PLACEHOLDER_TEXT("map.selectOptionPlaceholderText"), // North Star only
  MAP_SETTING_TEXT_LABEL("map.settingTextLabel"), // North Star only
  MAP_SWITCH_TO_LIST_VIEW("map.switchToListView"), // North Star only
  MAP_SWITCH_TO_MAP_VIEW("map.switchToMapView"), // North Star only
  MAP_SWITCH_TO_MAP_VIEW_SR("map.switchToMapViewSr"), // North Star only
  MAP_SWITCH_TO_LIST_VIEW_SR("map.switchToListViewSr"), // North Star only
  MAP_VALIDATION_TOO_MANY("map.validation.tooManySelections"),
  MAP_VALUE_LABEL("map.valueLabel"), // North Star only
  MAP_GEO_JSON_ERROR_TEXT("map.geoJsonErrorText"), // North Star only
  MAP_HOMEPAGE("map.homepage"), // North Star only
  MAP_CONTACT_US("map.contactUs"), // North Star only
  MAP_MAP_PREVIEW_TEXT("map.mapPreviewText"), // North Star only
  MEMORABLE_DATE_PLACEHOLDER("placeholder.memorableDate"),
  MENU("header.menu"),
  MOBILE_FILE_UPLOAD_HELP("content.mobileFileUploadHelp"),
  MODAL_ERROR_SAVING_STAY_AND_FIX_BUTTON("modal.errorSaving.stayAndFixButton"),
  MODAL_ERROR_SAVING_PREVIOUS_CONTENT("modal.errorSaving.previous.content"),
  MODAL_ERROR_SAVING_PREVIOUS_NO_SAVE_BUTTON("modal.errorSaving.previous.noSaveButton"),
  MODAL_ERROR_SAVING_PREVIOUS_TITLE("modal.errorSaving.previous.title"),
  MODAL_ERROR_SAVING_REVIEW_CONTENT("modal.errorSaving.review.content"),
  MODAL_ERROR_SAVING_REVIEW_NO_SAVE_BUTTON("modal.errorSaving.review.noSaveButton"),
  MODAL_ERROR_SAVING_REVIEW_TITLE("modal.errorSaving.review.title"),
  MODAL_ERROR_SAVING_TITLE("modal.errorSaving.title"), // North Star only
  MODAL_ERROR_SAVING_CONTENT_REVIEW("modal.errorSaving.content.review"), // North Star only
  MODAL_ERROR_SAVING_CONTENT_PREVIOUS("modal.errorSaving.content.previous"), // North Star only
  MODAL_ERROR_SAVING_CONTINUE_BUTTON_REVIEW(
      "modal.errorSaving.continueButton.review"), // North Star only
  MODAL_ERROR_SAVING_CONTINUE_BUTTON_PREVIOUS(
      "modal.errorSaving.continueButton.previous"), // North Star only
  MODAL_ERROR_SAVING_FIX_BUTTON("modal.errorSaving.fixButton"), // North Star only
  MULTI_OPTION_VALIDATION("adminValidation.multiOptionEmpty"),
  MULTI_OPTION_ADMIN_VALIDATION("adminValidation.multiOptionAdminError"),
  MULTI_SELECT_VALIDATION_TOO_FEW("validation.tooFewSelections"),
  MULTI_SELECT_VALIDATION_TOO_MANY("validation.tooManySelections.v2"),
  NAME_EXAMPLE("label.nameExample"),
  NAME_LABEL_FIRST("label.firstName"),
  NAME_LABEL_LAST("label.lastName"),
  NAME_LABEL_MIDDLE("label.middleName"),
  NAME_LABEL_SUFFIX("label.nameSuffix"),
  NAME_PLACEHOLDER_FIRST("placeholder.firstName"),
  NAME_PLACEHOLDER_LAST("placeholder.lastName"),
  NAME_PLACEHOLDER_MIDDLE("placeholder.middleName"),
  NAME_PLACEHOLDER_SUFFIX("placeholder.nameSuffix"),
  NAME_VALIDATION_FIRST_REQUIRED("validation.firstNameRequired"),
  NAME_VALIDATION_LAST_REQUIRED("validation.lastNameRequired"),
  OPTION_SELECT_PLACEHOLDER("option.selectPlaceholder"),
  OPTION_SUFFIX_JUNIOR("option.junior"),
  OPTION_SUFFIX_SENIOR("option.senior"),
  OPTION_SUFFIX_FIRST("option.first"),
  OPTION_SUFFIX_SECOND("option.second"),
  OPTION_SUFFIX_THIRD("option.third"),
  OPTION_SUFFIX_FORTH("option.forth"),
  OPTION_SUFFIX_FIFTH("option.fifth"),
  OPTION_MEMORABLE_DATE_JANUARY("option.memorableDate.January"),
  OPTION_MEMORABLE_DATE_FEBRUARY("option.memorableDate.February"),
  OPTION_MEMORABLE_DATE_MARCH("option.memorableDate.March"),
  OPTION_MEMORABLE_DATE_APRIL("option.memorableDate.April"),
  OPTION_MEMORABLE_DATE_MAY("option.memorableDate.May"),
  OPTION_MEMORABLE_DATE_JUNE("option.memorableDate.June"),
  OPTION_MEMORABLE_DATE_JULY("option.memorableDate.July"),
  OPTION_MEMORABLE_DATE_AUGUST("option.memorableDate.August"),
  OPTION_MEMORABLE_DATE_SEPTEMBER("option.memorableDate.September"),
  OPTION_MEMORABLE_DATE_OCTOBER("option.memorableDate.October"),
  OPTION_MEMORABLE_DATE_NOVEMBER("option.memorableDate.November"),
  OPTION_MEMORABLE_DATE_DECEMBER("option.memorableDate.December"),
  OPTION_YES("option.yes"),
  OPTION_NO("option.no"),
  OPTION_NOT_SURE("option.notSure"),
  OPTION_MAYBE("option.maybe"),
  PHONE_NUMBER_LABEL("label.phoneNum"),
  PHONE_VALIDATION_NUMBER_REQUIRED("validation.phoneNumberRequired"),
  PHONE_VALIDATION_COUNTRY_CODE_REQUIRED("validation.phoneCountryCodeRequired"),
  PHONE_VALIDATION_NON_NUMBER_VALUE("validation.phoneNumberMustContainNumbersOnly"),
  PHONE_VALIDATION_INVALID_PHONE_NUMBER("validation.invalidPhoneNumberProvided"),
  PHONE_VALIDATION_NUMBER_NOT_IN_COUNTRY("validation.phoneMustBeLocalToCountry"),
  PHONE_LABEL_COUNTRY_CODE("label.countryCode"),
  PHONE_LABEL_PHONE_NUMBER("label.phoneNumber"),
  PREDICATE_ALERT_NO_AVAILABLE_QUESTIONS_ELIGIBILITY(
      "alert.predicateNoAvailableQuestionsEligibility"),
  PREDICATE_ALERT_NO_AVAILABLE_QUESTIONS_VISIBILITY(
      "alert.predicateNoAvailableQuestionsVisibility"),
  PREDICATE_BUTTON_ADD_CONDITION("button.predicateAddCondition"),
  PREDICATE_BUTTON_DELETE_CONDITION("button.predicateDeleteCondition"),
  PREDICATE_CONTENT_APPLICANT_IS_ELIGIBLE("content.predicateApplicantIsEligible"),
  PREDICATE_CONTENT_CONDITIONS_ARE_TRUE("content.predicateConditionsAreTrue"),
  PREDICATE_CONTENT_CONDITION_IS_TRUE_IF("content.predicateConditionIsTrueIf"),
  PREDICATE_CONTENT_ELIGIBILITY_DESCRIPTION("content.predicateEligibilityDescription"),
  PREDICATE_CONTENT_SCREEN_IS("content.predicateScreenIs"),
  PREDICATE_CONTENT_SUBCONDITIONS_ARE_TRUE("content.predicateSubconditionsAreTrue"),
  PREDICATE_CONTENT_VISIBILITY_DESCRIPTION("content.predicateVisibilityDescription"),
  PREDICATE_LABEL_FIELD("label.predicateField"),
  PREDICATE_LABEL_QUESTION("label.predicateQuestion"),
  PREDICATE_LABEL_STATE("label.predicateState"),
  PREDICATE_LABEL_VALUE("label.predicateValue"),
  PREDICATE_LINK_ADD_SUBCONDITION("link.predicateAddSubcondition"),
  PREDICATE_LINK_DELETE_SUBCONDITION("link.predicateDeleteSubcondition"),
  SEARCH_BY_DOB("label.searchByDob"),
  SEARCH_BY_NAME("label.searchByName"),
  MONTH_LABEL("label.month"),
  NAME_LABEL("label.name"),
  NOTES_LABEL("label.notes"),
  NOT_FOR_PRODUCTION_BANNER_LINE_1("banner.notForProductionBannerLine1"),
  NOT_FOR_PRODUCTION_BANNER_LINE_2("banner.notForProductionBannerLine2"),
  NUMBER_VALIDATION_TOO_BIG("validation.numberTooBig"),
  NUMBER_VALIDATION_TOO_SMALL("validation.numberTooSmall"),
  NUMBER_VALIDATION_NON_INTEGER("validation.numberNonInteger"),
  REQUIRED_FIELDS_ANNOTATION("content.requiredFieldsAnnotation"),
  REQUIRED_FIELDS_NOTE("content.requiredFieldsNote"),
  REQUIRED_FIELDS_NOTE_NORTH_STAR("content.requiredFieldsNoteNorthStar"),
  REVIEW_PAGE_INTRO("content.reviewPageIntro"),
  SUBMITTED_DATE("content.submittedDate"),
  TAG_MAY_NOT_QUALIFY("tag.mayNotQualify"),
  TAG_MAY_NOT_QUALIFY_TI("tag.mayNotQualifyTi"),
  TAG_MAY_QUALIFY("tag.mayQualify"),
  TAG_MAY_QUALIFY_TI("tag.mayQualifyTi"),
  TEXT_VALIDATION_TOO_LONG("validation.textTooLong"),
  TEXT_VALIDATION_TOO_SHORT("validation.textTooShort"),
  TITLE_ALL_CLIENTS("title.allClients"),
  TITLE_ALL_PROGRAMS_SECTION("title.allProgramsSection"),
  TITLE_APPLICATION_CONFIRMATION("title.applicationConfirmation"),
  TITLE_PRE_SCREENER_CONFIRMATION("title.preScreenerConfirmation"),
  TITLE_PRE_SCREENER_CONFIRMATION_TI("title.preScreenerConfirmationTi"),
  TITLE_CREATE_CLIENT("title.createClient"),
  TITLE_APPLICATION_NOT_ELIGIBLE("title.applicantNotEligible"),
  TITLE_APPLICATION_NOT_ELIGIBLE_TI("title.applicantNotEligibleTi"),
  TITLE_AVAILABLE_PROGRAMS_SECTION("title.availableProgramsSection"), // North Star only
  TITLE_BENEFITS_FINDER_SECTION_V2("title.benefitsFinderSection.v2"),
  TITLE_PRE_SCREENER_SUMMARY("title.preScreenerSummary"),
  TITLE_CREATE_AN_ACCOUNT("title.createAnAccount"),
  TITLE_DISPLAY_ALL_CLIENTS("title.displayingAllClients"),
  TITLE_DISPLAY_MULTI_CLIENTS("title.displayingMultiClients"),
  TITLE_DISPLAY_ONE_CLIENT("title.displayingOneClient"),
  TITLE_EDIT_CLIENT("title.editClient"),
  TITLE_EXTERNAL_PROGRAM_MODAL("title.externalProgramModal"),
  TITLE_FIND_SERVICES_SECTION("title.getStartedSection"),
  TITLE_GET_STARTED("title.getStarted"),
  TITLE_INELIGIBLE("title.ineligible"),
  TITLE_LOGIN("title.login"),
  TITLE_MY_APPLICATIONS_SECTION("title.myApplicationsSection"),
  TITLE_MY_APPLICATIONS_SECTION_V2("title.myApplicationsSection.v2"), // North Star only
  TITLE_NO_CHANGES_TO_SAVE("title.noChangesToSave"),
  TITLE_ORG_MEMBERS("title.orgMembers"),
  TITLE_OTHER_PROGRAMS_SECTION_V2("title.otherProgramsSection.v2"),
  TITLE_PROGRAM_NOT_AVAILABLE("title.programNotAvailable"),
  TITLE_PROGRAM_OVERVIEW("title.programOverview"),
  TITLE_PROGRAM_SECTION_COMPLETED("title.programSectionCompleted"),
  TITLE_PROGRAMS("title.programs"),
  TITLE_PROGRAMS_SECTION_V2("title.programsSection.v2"),
  TITLE_PROGRAMS_ACTIVE_UPDATED("title.activeProgramsUpdated"),
  TITLE_PROGRAMS_IN_PROGRESS_UPDATED("title.inProgressProgramsUpdated"),
  TITLE_PROGRAM_SUMMARY("title.programSummary"),
  TITLE_PROGRAMS_SUBMITTED("title.submittedPrograms"),
  TITLE_RECOMMENDED_PROGRAMS_SECTION_V2("title.recommendedSection.v2"),
  TITLE_SIGNIN_MODAL("title.signInModal"), // North Star only
  TITLE_STATUS("title.status"),
  TITLE_TI_ACCOUNT_SETTINGS("title.tiAccountSettings"),
  TITLE_TI_DASHBOARD("title.tiDashboard"),
  TOAST_APPLICATION_SAVED("toast.applicationSaved"),
  TOAST_APPLICATION_OUT_OF_DATE("toast.applicationOutOfDate"),
  TOAST_ERROR_MSG_OUTLINE("toast.errorMessageOutline"),
  TOAST_LOCALE_NOT_SUPPORTED("toast.localeNotSupported"),
  TOAST_MAY_NOT_QUALIFY("toast.mayNotQualify"),
  TOAST_MAY_NOT_QUALIFY_TI("toast.mayNotQualifyTi"),
  TOAST_MAY_QUALIFY("toast.mayQualify"),
  TOAST_MAY_QUALIFY_TI("toast.mayQualifyTi"),
  TOAST_PROGRAM_COMPLETED("toast.programCompleted"),
  TOAST_SESSION_ENDED("toast.sessionEnded"),
  UPLOADING("label.uploading"),
  UPLOADED_FILES("label.uploadedFiles"),
  UNNAMED_USER("label.unnamedUser"),
  USER_NAME("header.userName"),
  VALIDATION_REQUIRED("validation.isRequired"),
  YEAR_LABEL("label.year"),

  // Session timeout related messages
  SESSION_INACTIVITY_WARNING_TITLE("session.inactivity.warning.title"),
  SESSION_INACTIVITY_WARNING_MESSAGE("session.inactivity.warning.message"),
  SESSION_LENGTH_WARNING_TITLE("session.length.warning.title"),
  SESSION_LENGTH_WARNING_MESSAGE("session.length.warning.message"),
  SESSION_EXTEND_BUTTON("session.extend.button"),
  SESSION_EXTENDED_SUCCESS("session.extended.success"),
  SESSION_EXTENDED_ERROR("session.extended.error");

  private final String keyName;

  MessageKey(String keyName) {
    this.keyName = keyName;
  }

  public String getKeyName() {
    return this.keyName;
  }
}
