# Please see the README.md in the same directory for more information on localization.
#
# ALWAYS add context strings to the Transifex dashboard immediately after
# merging changes to this file. See
# https://docs.civiform.us/contributor-guide/developer-guide/internationalization-i18n#internationalization-for-application-strings

#-------------------------------------------------------------#
# GENERAL - contains text that corresponds to multiple views. #
#-------------------------------------------------------------#

button.logout=Mag-logout
label.selectLanguage=Pakipili ang iyong gustong wika.
button.untranslatedSubmit=Isumite
footer.supportLinkDescription=Para sa suporta na pang teknikal, tawagan
guest=Bisita
label.languageSr=Pumili ng wika
header.endSession=Tapusin ang session
toast.sessionEnded=Natapos na ang iyong session.
header.guestIndicator=Bisitang user ka
header.userName=Nakalog-in bilang {0}
validation.isRequired=Ang tanong na ito ay kinakailangan.
validation.invalidInput=Maaari lamang ay maglagay ng halaga para sa bawat linya.
validation.errorAnnouncementSr=May mga error sa form. Ayusin ang mga iyon bago magpatuloy.
content.requiredFieldsAnnotation=Tandaan: Kinakailangan ang mga field na may *.
content.requiredFieldsNote=Tandaan: Nire-require ang mga field na may markang {0}.
content.optional=(opsyonal)
content.mobileFileUploadHelp=Ginagamit ang iyong telepono? Binibigyang-daan ka rin ng "Pumili ng File" na gamitin ang camera ng iyong telepono para mag-upload ng mga dokumento.
toast.errorMessageOutline=Error: {0}
# Description for an "X" button that will close a dialog, modal, or page.
button.close=Isara
# Indicator for screen readers that a link will open in a new tab. Meant to be used with aria label text eg. "Program details, opens in a new tab"
link.opensNewTabSr=magbubukas sa bagong tab

#-------------------------------------------------------------#
# LOGIN - contains text that for login page.                  #
#-------------------------------------------------------------#

# This is a title for the login page
title.login=Mag log in
content.loginPrompt=Maari lamang maglog-in gamit ang inyong account sa Lungsod ng {0}
button.createAnAccount=Gumawa ng account
button.login=Maglog-in
content.alternativeLoginPrompt=Walang account?
content.or=o
button.createAccount=Gumawa ng account
button.guestLogin=Magpatuloy bilang panauhin
content.loginDisabledPrompt=Kasalukuyang naka-disable ang pag-log in sa account
content.adminLoginPrompt=Naghahanap ng iba?
content.adminFooterPrompt=Isa ka bang administrator?
link.adminLogin=Admin login

#--------------------------------------------------------------------------#
# PROGRAM FORM - contains text shown when filling out an application form. #
#--------------------------------------------------------------------------#

input.fileAlreadyUploaded=Na-upload na ang file: {0}
button.deleteFile=Burahin
button.keepFile=Ipagpatuloy
button.nextScreen=Susunod
button.previousScreen=Nakaraang
button.review=Pagsusuri
button.skipFileUpload=Laktawan
content.blockProgress={0} sa {1}
toast.localeNotSupported=Pasensya na, hindi ganap na nakasalin ang program na ito sa iyong mas gustong wika.
link.allDone=Ako ay tapos na
link.applyToAnotherProgram=Mag-aplay sa ibang programa
link.createAccountOrSignIn=Gumawa ng account o magsign-in
# The title of a pop-up informing the user that they tried to go to the review page but there were errors in the information that they inputted
modal.errorSaving.review.title=Questions on this page are not complete. Would you still like to leave and begin reviewing?
# The title of a pop-up informing the user that they tried to go to the previous page but there were errors in the information that they inputted
modal.errorSaving.previous.title=Questions on this page are not complete. Would you still like to leave and go to the previous page?
# A message informing the user that there were errors in the information that they inputted. This message also asks the user if they want to (1) go back and fix the errors or (2) discard the information they've inputted and continue to the application review page.
modal.errorSaving.review.content=There are some errors with the information you''ve filled in. Would you like to go back and fix the errors, or go to the review page without saving your answers?
# A message informing the user that there were errors in the information that they inputted. This message also asks the user if they want to (1) go back and fix the errors or (2) discard the information they've inputted and continue to the previous page of the application.
modal.errorSaving.previous.content=There are some errors with the information you''ve filled in. Would you like to go back and fix the errors, or go to the previous question page without saving your answers?
# Text for a button. When the button is clicked, then all the information the user has inputted will be discarded and they will be taken to the application review page.
modal.errorSaving.review.noSaveButton=Go to review page without saving
# Text for a button. When the button is clicked, then all the information the user has inputted will be discarded and they will be taken to the previous page in the application.
modal.errorSaving.previous.noSaveButton=Go to previous page without saving
# Text for a button. When the button is clicked, the user will be shown the information that they previously inputted and will be asked to fix the errors with it.
modal.errorSaving.stayAndFixButton=Stay and fix your answers

#----------------------------------------------------------------------------#
# APPLICANT HOME PAGE - contains text specific to the applicant's home page. #
#----------------------------------------------------------------------------#

button.apply=Mag-aplay
button.applySr=Mag-aplay sa {0}
button.edit=I-edit
button.editSr=I-edit ang isinumiteng aplikasyon sa {0}
button.continueSr=Ipagpatuloy ang aplikasyon sa {0}
button.startHere=Magsimula rito
button.continueToApplication=Magpatuloy sa application
button.startHereCommonIntakeSr=Magsimula rito sa pamamagitan ng pagsasagot sa {0}
button.editCommonIntakeSr=I-edit ang isinumiteng impormasyon para sa {0}
button.continueCommonIntakeSr=Ipagpatuloy ang pagsasagot ng {0}
content.submittedDate=Isinumite noong {0}
content.findPrograms=Maghanap ng mga program
content.findProgramsDescription=Matutulungan ka ng CiviForm na humanap ng mga programa kung saan posibleng kwalipikado ka sa {0}. Magsimula sa pamamagitan ng pagpili ng aplikasyon sa ibaba.
content.saveTimeServices=Makatipid ng oras sa pag-apply sa mga programa at serbisyo
content.guestDescription=Mag-log in sa iyong {0} account o gumawa ng account at mag-apply sa mga programa nang hindi kinakailangang ilagay ulit ang parehong impormasyon. Puwede ka ring mag-edit ng mga aplikasyon anumang oras at tumingin sa mga status ng iyong aplikasyon.
link.programDetails=Mga detalya ng programa
link.programDetailsSr=Mga detalye ng programa para sa {0}, bubukas sa bagong tab
title.programs=Mga programa at mga serbisyo
title.getStartedSection=Magsimula
title.allProgramsSection=Lahat ng programa ({0})
title.status=Status
title.inProgressProgramsUpdated=Kasalukuyang ginagawa
title.activeProgramsUpdated=Hindi sinimulan
title.submittedPrograms=Isinumite
toast.applicationSaved=Successfully saved application: Matagumpay na nai-save ang aplikasyon: ID ng aplikasyon {0}
toast.programCompleted=Nakumpleto na ang aplikasyon.

#------------------------------------------------------------------------------------------------------#
# TRUSTED INTERMEDIARY DASHBOARD PAGE - text when adding, editing, deleting, or searching for a client #
#------------------------------------------------------------------------------------------------------#

# Message that appears when a user attempts to delete an account and it fails.
banner.acctDeleteError=This account cannot be deleted.
# Reason included with the account delete error, which explains the account can't be deleted due to applications the client has open
banner.acctDeleteErrorReason=This account cannot be deleted since it has open applications.
# Message when information about a client, like their name or contact information is updated.
banner.clientInfoUpdated=Client info has been successfully updated.
# Message when a client account for a user is successfully deleted, with the name of the client substituted in
banner.clientAcctDeleted=The client account for "{0}" has been successfully deleted.
# Message when a user successfully creates a client account.
banner.newClientCreated=New client successfully created.

# Button that allows a user to add a client to the CiviForm system.
button.addNewClient=Add new client
# Button that brings the user back to the list of their clients.
button.backToClientList=Back to client list
# Button that allows the user to navigate back to the edit page after they click to delete a client and get an error.
button.backToEditing=Back to editing
# Button that allows the user to cancel the progress they make on adding or editing a client.
button.cancel=Cancel
# Button that allows the user to clear any search parameters they have already entered.
button.clearSearch=Clear search
# Button for a user to delete their client's account
button.deleteAcct=Delete account
# Button option when asked if the user is sure they want to delete an account.
button.keepAcct=Keep account
# Button to navigate to the next page.
button.nextPage=Next
# Button to save information that has been entered.
button.save=Save
# Button to execute a search to filter a client list.
button.search=Search
# Button to select a client.
button.select=Select
# Button to start an application on behalf of a client.
button.startApp=Start an application
# Button to view the applications belonging to an applicant.
button.viewApplications=View applications

# Part of the confirmation banner which appears after a new client has been added to a community-based organization.
content.clientCreated=Your new client {0} has been added to your client list. You can now begin submitting applications for this client.
# For each client on the client list, shows the number of applications that have been submitted for them.
content.numberOfAppSubmitted={0} applications submitted
# Some explanation text on the confirmation modal when deleting a client.
content.warning=There are no applications submitted for this client, so no applications will be lost.
# Confirmation modal text when the user tries to delete a client account.
dialog.deleteConfirmation=Are you sure you want to delete this client''s account?

# Header for the page with the Organization Members table.
header.acctSettings=Account settings
# Tab label for getting to the client list page
header.clientList=Client list

# A column header in the Organization Members table. Entries may be "Not yet logged in" or "Last login XX-XX-XXXX".
label.acctStatus=Account Status
# Label above the phone number for each client on the client list.
label.contactInfo=Contact Information
# The label above the input field for the day of the client birth date. i.e. 12 for November 12
label.day=Day
# Label above the birth date input field when creating or editing a client
label.dob=Date of birth
# Example above the 3 birth date input fields (Month, Day, Year).
label.dobExample=For example: January 28 1986
# Label above the "Email" column of the community-based organization members table.
label.email=Email
# The label above the dropdown select field for the month of the client birth date.
label.month=Month
# Label above the "Name" column of the community-based organization members table. Each entry will be listed as "Last name, First name".
label.name=Name
# Example above the name input field for the client search.
label.nameExample=For example: Gu or Darren or Darren Gu
# Label above the text input field for notes about a client.
label.notes=Notes
# Label above the phone number input field when creating or editing a client.
label.phoneNum=Phone Number
# The label above the Date of Birth search input for the client search.
label.searchByDob=Search by Date of Birth
# The label above the name search field of the client search.
label.searchByName=Search by name(s)
# The label above the input field for the year of the client birth date.
label.year=Year

link.edit=I-edit

# This is the header above the list of clients and the client search.
title.allClients=All Clients
# This is the heading above a new client creation form.
title.createClient=Create a new client
# This is the heading above a filtered list of clients when the filter has narrowed the search down to a certain number of clients.
title.displayingMultiClients=Displaying {0} clients
# This is the heading above a filtered list of clients when the filter has narrowed the search down to a single client.
title.displayingOneClient=Displaying 1 client
# This is the heading above a table that contains the members of the current community-based organization.
title.orgMembers=Organization members

#------------------------------------------------------------------------#
# APPLICANT APPLICATION REVIEW PAGE - text when reviewing an application #
#------------------------------------------------------------------------#

button.submit=Isumite
button.continue=Magsimula dito
toast.applicationOutOfDate=Nagkaroon ng update sa application, pakisuri at sagutan ang mga sagot para magpatuloy

content.doesNotQualify=Hindi kwalipikado
link.edit=I-edit
link.answer=Sagot
content.previouslyAnsweredOn=Sinagutan noong {0}
ariaLabel.answer=Sagot {0}

ariaLabel.edit=I-edit {0}
title.programSummary=Buod ng application para sa programa
title.commonIntakeSummary=Buod ng pre-screener para sa mga benepisyo

#------------------------------------------------------------------------#
# APPLICANT ELIGIBILITY - text related to applicant eligibility #
#------------------------------------------------------------------------#

title.applicantNotEligible=Batay sa iyong mga sagot sa mga sumusunod na tanong, posibleng hindi ka kwalipikado para sa {0}.
title.applicantNotEligibleTi=Batay sa iyong mga sagot sa mga sumusunod na tanong, posibleng hindi kwalipikado ang client mo para sa {0}.
content.eligibilityCriteria=Para sa mga pamantayan sa pagiging kwalipikado, sumangguni sa {0}
content.changeAnswersForEligibility=Puwede kang bumalik sa nakaraang page para i-edit ang iyong mga sagot. O mag-apply sa isa pang programa.
button.goBackAndEdit=Bumalik at i-edit
toast.mayQualifyTi=Batay sa iyong mga tugon, posibleng kwalipikado ang iyong kliyente para sa {0}. Para magpatuloy, patuloy na punan ang application.
toast.mayQualify=Batay sa iyong mga tugon, posibleng kwalipikado ka para sa {0}. Para magpatuloy, patuloy na punan ang application.
tag.mayQualify=Posibleng kwalipikado ka
tag.mayQualifyTi=Posibleng kwalipikado ang iyong kliyente
tag.mayNotQualify=Posibleng hindi ka kwalipikado
tag.mayNotQualifyTi=Posibleng hindi kwalipikado ang iyong kliyente

toast.mayNotQualify=Batay sa iyong mga tugon, posibleng hindi ka kwalipikado para sa {0}. Kung nagbago ang iyong impormasyon, puwede mong i-edit ang iyong mga sagot at pagkatapos ay magpatuloy sa application.

toast.mayNotQualifyTi=Batay sa iyong mga tugon, posibleng hindi kwalipikado ang kliyente mo para sa {0}. Kung nagbago ang iyong impormasyon ng kliyente, puwede mong i-edit ang iyong mga sagot at pagkatapos ay magpatuloy sa application.
banner.errorSavingApplication=Nagka-error sa pag-save ng application

#---------------------------------------------------------------------------------------------#
# APPLICANT APPLICATION CONFIRMATION PAGE - text for a page confirming application submission #
#---------------------------------------------------------------------------------------------#

title.applicationConfirmation=Pagkumpirma ng aplikasyon
content.confirmed=Salamat sa pag-aplay sa {0}.  Ang inyong aplikasyon ay natanggap na, at ang ID ng application ninyo ay {1}.
title.createAnAccount=Gumawa ng account o magsign-in
content.pleaseCreateAccount=Kung kayo ay magsign-in ngayon sa isang account ng lungsod, ang inyong impormasyon ay maitatabi at maaring balikan anumang oras upang makumpleto ang mga aplikasyon sa hinaharap. Kung kayo ay wala pang account, pahihintulutan kayo ng pahina ng log-in upang makapagrehistro. Hindi kayo mawawalan ng anumang data na inyo nang nailagay.
content.generalLoginModalPrompt=Hindi ka naka-log in sa isang account. Hindi mo magagawang mabilis na mag-edit ng aplikasyon, tingnan ang status ng iyong aplikasyon, o mag-apply para sa iba pang programa nang walang account.
content.initialLoginModalPrompt=Bago ka magpatuloy, mag-log in sa iyong {0} account o gumawa ng account. Makakapag-apply ka para sa maraming programa nang hindi kinakailangang ilagay ulit ang parehong impormasyon. Puwede ka ring mag-edit ng mga aplikasyon at tumingin ng mga status ng iyong aplikasyon.
button.continueWithoutAnAccount=Magpatuloy nang walang account
title.commonIntakeConfirmation=Mga programa kung saan puwede kang maging kwalipikado
title.commonIntakeConfirmationTi=Mga programa kung saan puwedeng maging kwalipikado ang kliyente mo
content.commonIntakeConfirmation=Posibleng makapag-enroll ka sa mga programang ito kung mag-a-apply ka sa pamamagitan ng online na aplikasyon sa pamamagitan ng pag-click sa "Mag-apply sa mga programa."
content.commonIntakeConfirmationTi=Posibleng makapag-enroll ang kliyente mo sa mga programang ito kung mag-a-apply ka sa pamamagitan ng online na aplikasyon sa pamamagitan ng pag-click sa "Mag-apply sa mga programa."
content.commonIntakeNoMatchingPrograms=Hindi makahanap ang pre-screener ng mga programa kung saan puwede kang maging kwalipikado sa panahong ito. Pero puwede kang mag-apply sa mga programa anumang oras sa pamamagitan ng pag-click sa "Mag-apply sa mga programa." O para tumingin ng mga karagdagang programa, puwede kang pumunta sa {0}
content.commonIntakeNoMatchingProgramsTi=Hindi makahanap ang pre-screener ng mga programa kung saan puwedeng maging kwalipikado ang kliyente mo sa panahong ito. Pero puwede kang mag-apply sa mga programa anumang oras sa pamamagitan ng pag-click sa "Mag-apply sa mga programa." O para tumingin ng mga karagdagang programa, puwede kang pumunta sa {0}
content.commonIntakeNoMatchingProgramsNextStep=Puwede ka ring bumalik sa nakaraang page para i-edit ang iyong mga sagot.
button.downloadPdf=I-download ang PDF
button.applyToPrograms=Mag-apply sa mga programa

#-------------------------------------------------------------------------------------------------------------------------#
# APPLICANT DUPLICATE SUBMISSION PAGE - text for a page informing the applicant of a duplicate submission and redirecting #
#-------------------------------------------------------------------------------------------------------------------------#

title.noChangesToSave=Walang pagbabagong ise-save para sa {0}
content.noChanges=Wala ka pang ginagawang anumang pagbabago sa aplikasyon. Gusto mo bang magpatuloy sa pag-edit?
button.continueEditing=Magpatuloy sa pag-edit
button.exitApplication=Lumabas sa aplikasyon

#----------------------------------------------------------#
# ADDRESS QUESTION - text when viewing an address question #
#----------------------------------------------------------#

label.addressLine2=Apartment, suite, atbp. (opsyonal)
label.city=Lungsod
label.state=Estado
label.selectState=Pumili ng Estado
label.street=Address
label.zipcode=Zip code

validation.streetRequired=Maaari lamang na ilagay ang wasto na pangalan ng kalye, at ang numero.
validation.cityRequired=Maaari lamang na ilagay ang Lungsod.
validation.currencyMisformatted=Ang currency ay dapat nasa isa sa mga sumusunod na format: 1000 1,000 1000.30 1,000.30
validation.stateRequired=Maaari lamang na ilagay ang Estado.
validation.invalidZipcode=Maaari lamang na ilagay ang 5-bilang na ZIP code.
validation.noPoBox=Maaari lamang na ilagay ang wasto na adres. Kami ay hindi natanggap ng mga PO Box.

title.verifyAddress=I-verify ang address

content.foundSimilarAddress=Hindi namin ma-verify ang ibinigay mong address, pero may nakita kaming katulad nito.
content.addressEntered=Inilagay na address:

# Text on dialog that shows a suggestion for the user's correct address when entered incorrectly.
content.suggestedAddress=Iminumungkahing address:
content.suggestedAddresses=Mga iminumungkahing address:

# Title on address correction dialog when no address is found.
title.noValidAddress=Walang nakitang valid na address
content.noValidAddress=Hindi namin ma-verify ang ibinigay mong address. Paki-edit ang address mo para magpatuloy.

#----------------------------------------------------------------------------------------------------------#
# DROPDOWN QUESTION - text shown when answering a question where a user must select an option from a list. #
#----------------------------------------------------------------------------------------------------------#

placeholder.noDropdownSelection=Pumili ng opsyon:

#------------------------------------------------------------------------------------------------------------------------------#
# PHONE QUESTION - text shown when answering a question where a user must select an option for country and enter phone number. #
#------------------------------------------------------------------------------------------------------------------------------#

label.countryCode=Bansa
label.phoneNumber=Ilagay ang Numero ng telepono
validation.phoneNumberRequired=Kinakailangan ang numero ng telepono
validation.phoneCountryCodeRequired=Dapat piliin ang bansa
validation.phoneNumberMustContainNumbersOnly=Hindi puwedeng maglaman ng mga character na hindi numero ang numero ng telepono
validation.invalidPhoneNumberProvided=Invalid ang numero ng telepono na ito
validation.phoneMustBeLocalToCountry=Hindi nabibilang sa bansa ang ibinigay mong numero ng telepono

#----------------------------------------------------------------------------------------------------------#
# DATE QUESTION - text shown when answering a question where a user must select date. #
#----------------------------------------------------------------------------------------------------------#

validation.dateMisformatted=Please enter a date in the format YYYY/MM/DD.
validation.invalidDateFormat=Maglagay ng petsa sa tamang format

#--------------------------------------------------------------------------------------------------------#
# ENUMERATION QUESTION - text when viewing a question where a user must name entities, such as children. #
#--------------------------------------------------------------------------------------------------------#

button.addEntity=Idagdag {0}
button.removeEntity=Alisin {0}
dialog.confirmDelete=Kayo ba ay sigurado sa nais na alisin {0}? Ang pagbabago na ito ay maitatala matapos mong i-click ang "Next" at lahat ng mga data na nauugnay dito ay mawawala.
validation.entityNameRequired=Maaari lamang ay maglagay ng halaga para sa bawat linya.
validation.duplicateEntityName=Maaari lamang ay maglagay ng natatangi na halaga para sa bawat linya.
placeholder.entityName={0} Palayaw

#---------------------------------------------------------------------------------#
# FILEUPLOAD QUESTION - text when viewing a question where a user uploads a file. #
#---------------------------------------------------------------------------------#

validation.fileRequired=Maaari lamang ay pumili ng file.
button.chooseFile=Pumili ng File

#---------------------------------------------------------------------#
# ID QUESTION - id specific to filling out a question. #
#---------------------------------------------------------------------#

validation.idTooLong=Dapat ay naglalaman ng hindi hihigit sa {0} na mga character.
validation.idTooShort=Dapat ay naglalaman ng hindi bababa sa {0} na mga character.
validation.numberRequired=Dapat ay naglalaman lamang ng mga numero.

#----------------------------------------------------------------------------------------------------------#
# MULTI-SELECT QUESTION - text shown when filling out a question with multiple answers, such as a checkbox #
#----------------------------------------------------------------------------------------------------------#

validation.tooFewSelections=Maaari lamang na pumili ng hindi bababa sa {0}.
validation.tooManySelections=Maaari lamang na pumili ng mas-kaunti sa {0}.

#---------------------------------------------------#
# NAME QUESTION - text when viewing a name question #
#---------------------------------------------------#

label.firstName=Pangalan
label.lastName=Apelyido
label.middleName=Gitnang pangalan

placeholder.firstName=Pangalan
placeholder.lastName=Apelyido
placeholder.middleName=Gitnang pangalan

validation.firstNameRequired=Maaari lamang na ilagay ang inyong pangalan.
validation.lastNameRequired=Maaari lamang na ilagay ang inyong apelyido.

#------------------------------------------------------------------------#
# NUMBER QUESTION - text specific to answering a question with a number. #
#------------------------------------------------------------------------#

validation.numberTooBig=Dapat ay hindi hihigit sa {0}.
validation.numberTooSmall=Dapat ay hindi bababa sa {0}.
validation.numberNonInteger=Dapat ay naglalaman lamang ng mga numero.

#---------------------------------------------------------------------#
# TEXT QUESTION - text specific to filling out a question with words. #
#---------------------------------------------------------------------#

validation.textTooLong=Dapat ay naglalaman ng hindi hihigit sa {0} na mga character.
validation.textTooShort=Dapat ay naglalaman ng hindi bababa sa {0} na mga character.

#---------------------------------------------------------------------#
# MULTI OPTION QUESTION ADMIN EDIT - text specific when creating/editing a multi option question. #
#---------------------------------------------------------------------#

adminValidation.multiOptionEmpty=Ang mga katanungan na maraming pagpipiliang opsyon ay hindi maaaring magkaroon ng mga blanko na mga opsyon.
adminValidation.multiOptionAdminError=Mga titik, numero, underscore, at gitling lang ang puwedeng gamitin para sa mga pangalan ng admin.

#----------------------------------#
# ERRORS - various error messages  #
#----------------------------------#

error.notFoundTitle=Hindi namin nahanap ang page na sinubukan mong puntahan
error.notFoundDescription=Bumalik o pumunta ulit sa {0}
error.notFoundDescriptionLink=homepage
error.internalServerTitle=Nagkaroon ng error
error.internalServerDescription=Makipag-ugnayan sa technical support para sa tulong sa %s at isama itong error ID na {0}

#-------------------------------------------------------------#
# EMAILS - boilerplate text contained in emails sent to users #
#-------------------------------------------------------------#

email.loginToCiviform=Mag-log in sa CiviForm sa {0}
email.applicationReceivedSubject=Natanggap na ang iyong aplikasyon sa programang {0}
email.applicationReceivedBody=Natanggap na ang aplikasyon mo sa programang {0}. {1} ang applicant ID mo at {2} ang application ID
email.tiApplicationSubmittedSubject=Nagsumite ka ng aplikasyon para sa programang {0} sa ngalan ng aplikanteng si {1}
email.tiApplicationSubmittedBody=Natanggap na ang aplikasyon sa programang {0} bilang aplikanteng si {1}, at {2} ang application ID.
email.tiManageYourClients=Pamahalaan ang iyong mga client sa {0}.
email.applicationUpdateSubject=Update sa iyong aplikasyong {0}
email.tiApplicationUpdateSubject=Update sa aplikasyon para sa programang {0} sa ngalan ng aplikanteng si {1}
email.tiApplicationUpdateBody=Ang status para sa aplikanteng si {0} sa programang {1} ay naging {2}.

#------------------------------------------------------------------------------#
# GOVERNMENT BANNER - text explaining that CF is an official government site #
#------------------------------------------------------------------------------#

banner.title=Isa itong opisyal na website ng pamahalaan.
banner.link=Ganito mo malalaman
banner.govWebsiteSectionHeader=Gumagamit ang mga opisyal na website ng .gov
banner.govWebsiteSectionContent=Ang mga address ng website na nagtatapos sa .gov ay pagmamay-ari ng mga opisyal na organisasyon ng pamahalaan sa United States.
banner.httpsSectionHeader=Gumagamit ng HTTPS ang mga secure na .gov na website
banner.httpsSectionContent=Ang lock na ( {0} ) o https:// ay nangangahulugang ligtas kang nakakonekta sa .gov na website. Magbahagi lang ng sensitibong impormasyon sa mga opisyal at secure na website.
