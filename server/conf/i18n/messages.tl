# Please see the README.md in the same directory for more information on localization.
#
# ALWAYS add context strings to the Transifex dashboard immediately after
# merging changes to this file. See
# https://github.com/civiform/civiform/wiki/Internationalization-(i18n)#internationalization-for-application-strings

#-------------------------------------------------------------#
# GENERAL - contains text that corresponds to multiple views. #
#-------------------------------------------------------------#

# The text on the button an applicant clicks to log out of their session.
button.logout=Mag-logout
#  This appears before applicants select their preferred language.
label.selectLanguage=Pakipili ang iyong gustong wika.
button.untranslatedSubmit=Isumite
# Text on a link that gives more information about CiviForm. "CiviForm" is the app name and should
# not be translated.
footer.aboutCiviform=Tungkol sa CiviForm
# Message displayed before the support email address in the page footer for applicants.
footer.supportLinkDescription=Para sa suporta na pang teknikal, tawagan
# Text in the footer telling users that this is an official website of the specified Civic entity.
footer.officialWebsiteOf=Opisyal na website ng {0}
# Text on a link that will scroll to the top of the page
footer.returnToTop=Bumalik sa itaas
# A message in the footer directing users to technical support. The placeholder is a link to
# send an email to CiviForm technical support.
footer.technicalSupport=Para sa technical support: {0}
# Placeholder message when an applicant clicked the "Continue as Guest".
# This should be consistent with button.guestLogin.
guest=Bisita
# Clicking this button shows a dropdown menu to change the display language
button.languages=Pumili ng Wika
# Label read by screen readers for the language dropdown shown in the page header.
label.languageSr=Pumili ng wika
# A label on the button that opens the header menu shown on mobile
header.menu=Menu
# Message for guest users to end their session. Technically this logs out the user from the guest profile, but we use different phrasing in order to not imply that they are currently logged in from a product perspective.
header.endSession=Tapusin ang session
# Message for screen readers as part of aria label to let user know about an informational alert.
heading.informationAriaLabelPrefix=For your information: {0}
# Message for screen readers as part of the aria label to let user know they completed an action successfully.
heading.successAriaLabelPrefix=Success: {0}
# Toast message that tells the user their session has ended, to help indicate that they are no longer logged in to an account (but still as a guest).
toast.sessionEnded=Natapos na ang iyong session.
# Message for guest users, to avoid showing "Logged in as Guest" when they are really not logged in.
header.guestIndicator=Bisitang user ka
# Message with user's name to show who you are logged in as.
header.userName=Nakalog-in bilang {0}
# Error message to answer a required question
validation.isRequired=Ang tanong na ito ay kinakailangan.
# Validation error that is shown when the user input couldn't be converted for any reason.
validation.invalidInput=Maaari lamang ay maglagay ng halaga para sa bawat linya.
# Error message announced to screen reader when there are errors on the current page.
validation.errorAnnouncementSr=May mga error sa form. Ayusin ang mga iyon bago magpatuloy.
# Message displayed at the top of a question page denoting fields with a * are required.
content.requiredFieldsAnnotation=Tandaan: Kinakailangan ang mga field na may *.
content.requiredFieldsNote=Tandaan: Nire-require ang mga field na may markang {0}.
# Disclaimer at top of form explaining that asterisks mark required questions. The placeholder is the red, bold asterisk used throughout the page.
content.requiredFieldsNoteNorthStar=Minamarkahan ng asterisk ({0}) ang mga nire-require na field.
content.optional=(opsyonal)
# Message displayed below "Choose File" button to say that on the phone users can use their camera.
content.mobileFileUploadHelp=Ginagamit ang iyong telepono? Binibigyang-daan ka rin ng "Pumili ng File" na gamitin ang camera ng iyong telepono para mag-upload ng mga dokumento.
toast.errorMessageOutline=Error: {0}
# Description for an "X" button that will close a dialog, modal, or page.
button.close=Isara
# Button text that will navigate the applicant to a page where they can review previous answers
button.review=Pagsusuri
# Indicator for screen readers that a link will open in a new tab. Meant to be used with aria label text eg. "Program details, opens in a new tab"
link.opensNewTabSr=magbubukas sa bagong tab
# Aria-label for the primary navigation
label.primaryNavigation=Pangunahing navigation
# Aria-label for agency identifier
label.agencyIdentifier=Identifier ng ahensya,
# Aria-label for guest session alert
label.guestSessionAlert=Alerto ng impormasyon ng session ng bisita

#-------------------------------------------------------------#
# LOGIN - contains text that for login page.                  #
#-------------------------------------------------------------#

# This is a title for the login page
title.login=Mag log in
# Prompt for applicant to log in, input is the full civic entity name.
content.loginPrompt=Maari lamang maglog-in gamit ang inyong account sa Lungsod ng {0}
# The text on the button an applicant clicks to create an account.
button.createAnAccount=Gumawa ng account
# The text on the button an applicant clicks to log in to their session.
button.login=Maglog-in
# Prompt for applicant to create a new account or become a guest
content.alternativeLoginPrompt=Walang account?
# The text between creating a new account, and becoming a guest
content.or=o
# The text on the button for applicants to create a new account.
button.createAccount=Gumawa ng account
# The text on the button for guests to log in to their session.
button.guestLogin=Magpatuloy bilang panauhin
# Prompt for whem applicant account login is disabled. Replaces loginPrompt and alternativeLoginPrompt.
content.loginDisabledPrompt=Kasalukuyang naka-disable ang pag-log in sa account
# The words leading up to the admin login anchor
content.adminLoginPrompt=Naghahanap ng iba?
# Asking whether the user is an administrator for the program
content.adminFooterPrompt=Isa ka bang administrator?
# The text on the anchor for admins to log in to their session.
link.adminLogin=Admin login

#--------------------------------------------------------------------------#
# PROGRAM FORM - contains text shown when filling out an application form. #
#--------------------------------------------------------------------------#

# The text displayed when a file has already been uploaded and the name is being displayed.
input.fileAlreadyUploaded=Na-upload na ang file: {0}
# Hint placed above file input for questions which accept a single file.
input.singleFileUploadHint=Pumili ng file
# Hint placed above file input for questions which accept multiple, but not unlimited, files. {0} contains a number,
# 2 or greater, representing the maximum number of files.
input.multipleFileUploadHint=Pumili ng isa o higit pang file (maximum na {0})
# Hint placed above file inputs which accept any number of files.
input.unlimitedFileUploadHint=Pumili ng isa o higit pang file
# The text on the button an applicant clicks to delete an uploaded file.
button.deleteFile=Burahin
# The text on the button an applicant clicks to skip uploading a new file while there is already an uploaded file.
button.keepFile=Ipagpatuloy
# The button text for saving the current applicant-entered data and continuing to the next screen in the form.
button.nextScreen=Susunod
# The button text for navigating to the previous screen in the form.
button.previousScreen=Nakaraang
# The label on a button that will navigate the user to the previous section of the form.
button.back=Bumalik
# The label on a button that will save user answers and navigate to the summary of the application.
button.reviewAndExit=Suriin at umalis
# The text on the button an applicant clicks to skip uploading a file.
button.skipFileUpload=Laktawan
# The current screen the user is on ({0}) out of the number of total screens in the application.
content.blockProgress={0} sa {1}
# A message on a section indicating how many questions of the total have been answered by the applicant.
content.blockProgressFull={0} sa {1} ang natapos
# An aria-label on a progress bar showing user how much of the application they have completed.
content.blockProgressLabel=Pag-usad ng aplikasyon
# Heading on the application review page. The page shows all the answers the user inputted.
heading.reviewAndSubmit=Suriin at isumite
# Label on a section of a progress bar. The first placeholder is the section number and second is section name (eg: "2. Contact info")
label.blockIndexLabel={0}. {1}
# A toast message that displays when a program is not fully localized to the applicant's preferred locale.
toast.localeNotSupported=Pasensya na, hindi ganap na nakasalin ang program na ito sa iyong mas gustong wika.
# A link that logs out a guest user after they submit an application.
link.allDone=Ako ay tapos na
# A link that appears next to the create account button that offers applicants the option to not create an account right now.
link.applyToAnotherProgram=Mag-aplay sa ibang programa
# A link that offers applicants the option to create an account.
link.createAccountOrSignIn=Gumawa ng account o magsign-in
# Anchor which when clicked removes a file the user has previously uploaded.
link.removeFile=Alisin ang file
# The title of a pop-up informing the user that they tried to go to the review page but there were errors in the information that they inputted
modal.errorSaving.review.title=Hindi kumpleto ang mga tanong sa page na ito. Gusto mo pa rin bang umalis at simulan ang pagsusuri?
# The title of a pop-up informing the user that they tried to go to the previous page but there were errors in the information that they inputted
modal.errorSaving.previous.title=Hindi kumpleto ang mga tanong sa page na ito. Gusto mo pa rin bang umalis at pumunta sa nakaraang page?
# A message informing the user that there were errors in the information that they inputted. This message also asks the user if they want to (1) stay and fix their answers or (2) discard the information they've inputted and continue to the application review page.
modal.errorSaving.review.content=May ilang error sa impormasyong inilagay mo. Gusto mo bang manatili at ayusin ang iyong mga sagot, o pumunta sa page ng pagsusuri nang hindi sine-save ang mga sagot mo?
# A message informing the user that there were errors in the information that they inputted. This message also asks the user if they want to (1) stay and fix their answers or (2) discard the information they've inputted and continue to the previous page of the application.
modal.errorSaving.previous.content=May ilang error sa impormasyong inilagay mo. Gusto mo bang manatili at ayusin ang mga sagot mo, o pumunta sa nakaraang page ng tanong nang hindi sine-save ang iyong mga sagot?
# Text for a button. When the button is clicked, then all the information the user has inputted will be discarded and they will be taken to the application review page.
modal.errorSaving.review.noSaveButton=Magpatuloy sa page ng pagsusuri nang hindi nagse-save
# Text for a button. When the button is clicked, then all the information the user has inputted will be discarded and they will be taken to the previous page in the application.
modal.errorSaving.previous.noSaveButton=Magpatuloy sa nakaraang mga tanong nang hindi nagse-save
# Text for a button. When the button is clicked, the user will be shown the information that they previously inputted and will be asked to fix the errors with it.
modal.errorSaving.stayAndFixButton=Manatili at ayusin ang iyong mga sagot

#----------------------------------------------------------------------------#
# APPLICANT HOME PAGE - contains text specific to the applicant's home page. #
#----------------------------------------------------------------------------#

# The text on the button an applicant clicks to apply to a specific program.
button.apply=Mag-aplay
# The text read for screen readers.
button.applySr=Mag-aplay sa {0}
# The text on the button an applicant clicks to edit the application for a specific program.
button.edit=I-edit
# The screen reader text for a button allowing an applicant to edit a submitted application for a given program.
button.editSr=I-edit ang isinumiteng aplikasyon sa {0}
# The screen reader text for a button allowing an applicant to continue editing an in-progress application for a given program.
button.continueSr=Ipagpatuloy ang aplikasyon sa {0}
# The text on the button an applicant clicks to start filling out a pre-screener form.
button.startHere=Magsimula rito
# The text on a button to view and apply to a program. Clicking the button leads to the program overview page.
button.viewAndApply=Tingnan at mag-apply
# The screen reader text on a button to view and apply to a program. The variable represents the program name.
button.viewAndApplySr=Tingnan at mag-apply sa {0}
# The text for the button that allows a guest to bypass the login prompt modal.
button.continueToApplication=Magpatuloy sa application
# The screen reader text for a button an applicant clicks to start filling out a pre-screener form.
button.startHereCommonIntakeSr=Magsimula rito sa pamamagitan ng pagsasagot sa {0}
# The screen reader text for a button an applicant clicks to edit their responses to a pre-screener form.
button.editCommonIntakeSr=I-edit ang isinumiteng impormasyon para sa {0}
# The screen reader text for a button an applicant clicks to continue filling out a pre-screener form.
button.continueCommonIntakeSr=Ipagpatuloy ang pagsasagot ng {0}
# Text describing the date the application was last submitted.
content.submittedDate=Isinumite noong {0}
# Text for applicants to understand the section is for finding programs
content.findPrograms=Maghanap ng mga program
# Long form description of the CiviForm site shown to the applicant.
content.findProgramsDescription=Matutulungan ka ng CiviForm na humanap ng mga programa kung saan posibleng kwalipikado ka sa {0}. Magsimula sa pamamagitan ng pagpili ng aplikasyon sa ibaba.
# Title for programs page when applicant is not logged in
content.saveTimeServices=Makatipid ng oras sa pag-apply sa mga programa at serbisyo
# Main home page heading
heading.homepage=Apply for government assistance here
# Long form description of the site shown to the applicant when they are not logged in.
# {0} represents the authentication provider's name
content.guestDescription=Mag-log in sa iyong {0} account o gumawa ng account at mag-apply sa mga programa nang hindi kinakailangang ilagay ulit ang parehong impormasyon. Puwede ka ring mag-edit ng mga aplikasyon anumang oras at tumingin sa mga status ng iyong aplikasyon.
# Main home page intro text
content.homepageIntro=To get help with food, transportation, child care, utilities, and more, click one of the options below. You can apply to one program at a time.
# The label for the program filter buttons
label.programFilters=Mga filter ng programa
# The label for the program filter checkboxes
label.programFilters.v2=What do you want help with?
# The button to apply program filters
button.applySelections=I-apply ang mga napili
# The button to clear program filter selections
button.clearSelections=I-clear ang mga napili
# Link text to read more about a program.
link.programDetails=Mga detalya ng programa
# The same text, read for screen readers.
link.programDetailsSr=Mga detalye ng programa para sa {0}, bubukas sa bagong tab
# The title of the page for the list of programs.
title.programs=Mga programa at mga serbisyo
# The title for the section in the list of programs that contains the pre-screener form and should be started first.
title.getStartedSection=Magsimula
# The title for the section in the list of programs that contains all regular, non-pre-screener programs.
title.allProgramsSection=Lahat ng programa ({0})
# For the badge on the program list index denoting the current status of an application
title.status=Status
# Subtitle for the list of programs with draft applications
title.inProgressProgramsUpdated=Kasalukuyang ginagawa
# Subtitle for the list of programs for which the applicant has no draft applications
title.activeProgramsUpdated=Hindi sinimulan
# Title of the section on the home page with the benefits finder form.
title.benefitsFinderSection.v2=Maghanap ng mga serbisyo
# Title of the section on the home page that shows programs for which the applicant has already started or submitted applications.
title.myApplicationsSection=Mga aplikasyon ko ({0})
# Title of the section on the home page that shows programs for which the applicant has already started or submitted applications.
title.myApplicationsSection.v2=Mga aplikasyon ko
# Title of the section on the home page that shows any programs that don't match the selected filters, with the number of programs in parentheses.
title.otherProgramsSection.v2=Iba pang programa at serbisyo ({0})
# Title of the section on the home page that shows all programs when no filter is selected, with the number of programs in parentheses.
title.programsSection.v2=Mga programa at serbisyo ({0})
# Title of the section on the home page that shows any available programs that have not yet been applied to.
title.availableProgramsSection=Programs and services
# Title of the section on the home page that shows programs that match any of the selected filters.
title.recommendedSection.v2=Mga programa batay sa mga pinili mo ({0})
# A label on the summary of a section of the application indicating to the applicant that all required questions have been filled out.
title.programSectionCompleted=Natapos na ang seksyon
# Subtitle for the list of programs for which the applicant has already submitted an application
title.submittedPrograms=Isinumite
# Alert banner when an application was successfully saved, with the ID of the application.
toast.applicationSaved=Successfully saved application: Matagumpay na nai-save ang aplikasyon: ID ng aplikasyon {0}
# Alert banner when an application was already completed
toast.programCompleted=Nakumpleto na ang aplikasyon.
# Informational tag on an in-progress application card
label.inProgress=Hindi pa naisumite
# Informational tag on a submitted application card. Used when the date of submission is unknown.
label.submitted=Naisumite
# Informational tag on a submitted application card. The parameter is the date of the submission.
label.submittedOn=Naisumite noong {0}
# Informational tag on a submitted application card. The first paramater is the status applied to an application. The second parameter is the date the status was applied.
label.statusOn={0} noong {1}

#------------------------------------------------------------------------------------------------------#
# TRUSTED INTERMEDIARY DASHBOARD PAGE - text when adding, editing, deleting, or searching for a client #
#------------------------------------------------------------------------------------------------------#

# Message that appears when a user attempts to delete an account and it fails.
banner.acctDeleteError=Hindi made-delete ang account na ito.
# Reason included with the account delete error, which explains the account can't be deleted due to applications the client has open
banner.acctDeleteErrorReason=Hindi made-delete ang account na ito dahil mayroon itong mga nakabukas na application.
# Message when information about a client, like their name or contact information is updated.
banner.clientInfoUpdated=Matagumpay na na-update ang impormasyon ng kliyente.
# Message when a client account for a user is successfully deleted, with the name of the client substituted in
banner.clientAcctDeleted=Matagumpay na na-delete ang account ng kliyente para sa "{0}."
# Message when a user successfully creates a client account.
banner.newClientCreated=Matagumpay na nakagawa ng bagong kliyente.
# Banner at the top of the page with the name of the client substituted in when viewing an application.
banner.viewApplication=Nag-a-apply ka para sa {0}. Sinusubukan mo bang mag-apply para sa ibang kliyente?


# Button that allows a user to add a client to the CiviForm system.
button.addNewClient=Magdagdag ng bagong kliyente
# Button that brings the user back to the list of their clients.
button.backToClientList=Bumalik sa listahan ng kliyente
# Button that allows the user to navigate back to the edit page after they click to delete a client and get an error.
button.backToEditing=Bumalik sa pag-edit
# Button that allows the user to cancel the progress they make on adding or editing a client.
button.cancel=Kanselahin
# Button that allows the user to clear any search parameters they have already entered.
button.clearSearch=I-clear ang paghahanap
# Button for a user to delete their client's account
button.deleteAcct=I-delete ang account
# Button option when asked if the user is sure they want to delete an account.
button.keepAcct=Panatilihin ang account
# Button to navigate to the next page.
button.nextPage=Susunod
# Button to save information that has been entered.
button.save=I-save
# Button to execute a search to filter a client list.
button.search=Maghanap
# Button to select a client.
button.select=Piliin
# Button to start an application on behalf of a client.
button.startApp=Magsimula ng application
# Button to view the applications belonging to an applicant.
button.viewApplications=Tingnan ang mga application
# Button to view and add clients
button.viewAndAddClients=Tumingin at magdagdag ng mga kliyente

# Part of the confirmation banner which appears after a new client has been added to a community-based organization.
content.clientCreated=Naidagdag na sa listahan ng kliyente mo ang iyong bagong kliyenteng si {0}. Puwede ka nang magsimulang magsumite ng mga application para sa kliyenteng ito.
# The tooltip of the email input field.
content.emailTooltip=Magdagdag ng email address para awtomatikong makatanggap ang iyong kliyente ng mga update sa status kaugnay ng kanyang aplikasyon. Kapag walang email, ikaw o ang iyong organisasyong nakabatay sa komunidad ang responsable sa pagbibigay-alam ng mga update sa iyong kliyente.
# For each client on the client list, shows the number of applications that have been submitted for them.
content.numberOfAppSubmitted={0} (na) application ang naisumite
# Shows that a single application has been submitted on behalf of the client.
content.oneAppSubmitted=1 aplikasyon ang naisumite
# Some explanation text on the confirmation modal when deleting a client.
content.warning=Walang application na naisumite para sa kliyenteng ito, kaya walang mawawalang application.
# Shows there is no email address connected to the account.
content.noEmailAddress=(walang email address)
# The status of an organization member when they have been enrolled but have never logged into CiviForm.
content.notLoggedIn=Hindi pa naka-log in
# The status of an organization member when they have logged in but haven't applied on behalf of a client.
content.noApplications=Naka-log in, walang isinumiteng aplikasyon
# The status of an organization member showing the date of the last application that they submitted for a client.
content.lastApplicationDate=Huling nagsumite ng aplikasyon noong {0}
# Confirmation modal text when the user tries to delete a client account.
dialog.deleteConfirmation=Sigurado ka bang gusto mong i-delete ang account ng kliyenteng ito?

# Header for the page with the Organization Members table.
header.acctSettings=Mga setting ng account
# Tab label for getting to the client list page
header.clientList=Listahan ng kliyente
# Label for search
header.search=Search

# A column header in the Organization Members table. Entries may be "Not yet logged in" or "Last submitted application on MM-DD-YYYY".
label.acctStatus=Status ng account
# Label above the phone number for each client on the client list.
label.contactInfo=Impormasyon sa Pakikipag-ugnayan
# The label above the input field for the day of the client birth date. i.e. 12 for November 12
label.day=Araw
# Label above the birth date input field when creating or editing a client
label.dob=Petsa ng kapanganakan
# Example above the 3 birth date input fields (Month, Day, Year).
label.dobExample=Halimbawa: Enero 28, 1986
# Label above the "Email" column of the community-based organization members table.
label.email=Email
# Validation error message below date of birth input when the TI clicks on save without filling out the field.
label.errorDOB=Kinakailangan ang petsa ng kapanganakan
# Validation error message below first name input when the TI clicks on save without filling out the field.
label.errorFirstName=Kinakailangan ang pangalan
# Validation error message below last name input when the TI clicks on save without filling out the field.
label.errorLastName=Kinakailangan ang apelyido
# Validation error message below email input when the TI tries to create a client with an email that is already in use.
label.errorEmailInUseForClientCreate=Ginagamit na ang email address. Hindi puwedeng gawin ang aplikante kung mayroon nang account.
# Validation error message below email input when the TI tries to edit a client with an email that is already in use.
label.errorEmailInUseForClientEdit=Ginagamit na ang email address. Hindi puwedeng i-update ang aplikante kung mayroon nang account.
# The label above the dropdown select field for the month of the client birth date.
label.month=Buwan
# Label above the "Name" column of the community-based organization members table. Each entry will be listed as "Last name, First name".
label.name=Pangalan
# Example above the name input field for the client search.
label.nameExample=Halimbawa: Gu o Darren o Darren Gu
# Label above the text input field for notes about a client.
label.notes=Mga Tala
# Label above the phone number input field when creating or editing a client.
label.phoneNum=Numero ng telepono
# The label above the Date of Birth search input for the client search.
label.searchByDob=Maghanap ayon sa petsa ng kapanganakan
# The label above the name search field of the client search.
label.searchByName=Maghanap ayon sa (mga) pangalan
# The label above the input field for the year of the client birth date.
label.year=Taon
# Label showing the user when there is no name
label.unnamedUser=Walang pangalang user

link.edit=I-edit
# Link to change the client that the trusted intermediary is applying for.
link.selectNewClient=Pumili ng bagong kliyente

# This is the header above the list of clients and the client search.
title.allClients=Lahat ng kliyente
# This is the heading above a new client creation form.
title.createClient=Gumawa ng bagong kliyente
# This is the heading above the client editing form.
title.editClient=I-edit ang impormasyon ng kliyente
# This is the heading above a filtered list of clients when the filter has narrowed the search down to a certain number of clients.
title.displayingMultiClients=Nagpapakita ng {0} (na) kliyente
# This is the heading above a filtered list of clients when the filter has narrowed the search down to a single client.
title.displayingOneClient=Nagpapakita ng 1 kliyente
# This is the heading above an unfiltered list of clients.
title.displayingAllClients=Ipinapakita ang lahat ng kliyente
# This is the heading above a table that contains the members of the current community-based organization.
title.orgMembers=Mga miyembro ng organisasyon
# HTML document title for the trusted intermediary dashboard page
title.tiDashboard=Pinagkakatiwalaang Intermediary - Pamahalaan ang Mga Kliyente - CiviForm
# HTML document title for the trusted intermediary account settings page
title.tiAccountSettings=Pinagkakatiwalaang Intermediary - Tingnan ang Mga Setting ng Account - CiviForm

#------------------------------------------------------------------------#
# APPLICANT APPLICATION REVIEW PAGE - text when reviewing an application #
#------------------------------------------------------------------------#

# Submit application button
button.submit=Isumite
# Submit application button
button.submitApplication=Isumite ang application
# Continue application button
button.continue=Magsimula dito
# Button text on a button that will allow the user to start filling out a section of questions.
button.start=Magsimula
# A toast message shown after submitting an application when it's been determined the application is incomplete typically due to external changes.
toast.applicationOutOfDate=Nagkaroon ng update sa application, pakisuri at sagutan ang mga sagot para magpatuloy

# Help text at the top of the review page instructing applicants with how to start applying for programs.
content.reviewPageIntro=Pumili ng hakbang sa ibaba para simulan ang iyong aplikasyon. Puwede kang mag-save at bumalik anumang oras.
# Text shown next to a question indicating that due to it, the application does not qualify or is not eligible.
content.doesNotQualify=Hindi kwalipikado
# The text that appears next to an answered question that the applicant can click on to modify the response.
link.edit=I-edit
link.answer=Sagot
# The text that appears next to a question that was answered in another program to let the user know the date it was previously answered on.
content.previouslyAnsweredOn=Sinagutan noong {0}
# An aria-label for screen readers that helps provide context for the associated edit link. The edit link is for a specific
# question, so this might say "Answer What is your name?". The {0} variable is the question text.
ariaLabel.answer=Sagot {0}

# An aria-label for screen readers that helps provide context for the associated edit link. The edit link is for a specific
# question, so this might say "Edit What is your name?". The {0} variable is the question text.
ariaLabel.edit=I-edit {0}
# Title for the summary of the pre-screener page
title.commonIntakeSummary=Buod ng pre-screener para sa mga benepisyo
# Heading content at the top of the review page, where applicants can start answering questions.
title.getStarted=Magsimula na tayo
title.programSummary=Buod ng application para sa programa

#----------------------------------------------------------------------------#
# PROGRAM OVERVIEW PAGE - contains text specific to the program overview page. #
#----------------------------------------------------------------------------#

# The HTML page title for the program overview page.  The {0} is the program name.
title.programOverview={0} - Pangkalahatang-ideya sa Programa
# Heading for the section of the program overview page that contains the application steps.
heading.applicationSteps=Paano mag-apply
# Top-level heading for the program overview page.  The {0} is the program name.
heading.programOverview=Mag-apply sa programang {0}
# The text on the button that takes users to the account creation form.
link.createAccountFromOverview=Simulan ang pag-apply nang gumagamit ng account
# The link that directs users to the start of the application without creating an account.
link.startAsGuest=Simulan ang pag-apply bilang bisita

#------------------------------------------------------------------------#
# APPLICANT ELIGIBILITY - text related to applicant eligibility #
#------------------------------------------------------------------------#

# Tab title for ineligible page
title.ineligible=Hindi kwalipikado para sa program
# Title on the page after it has been determined that the applicant is not eligible for a program. This text includes the program name.
title.applicantNotEligible=Batay sa iyong mga sagot sa mga sumusunod na tanong, posibleng hindi ka kwalipikado para sa {0}.
# Title on the page after it has been determined that the client is not eligible for a program, when someone else is filling out the application on a client''s behalf. This text includes the program name.
title.applicantNotEligibleTi=Batay sa iyong mga sagot sa mga sumusunod na tanong, posibleng hindi kwalipikado ang client mo para sa {0}.
# Section heading
heading.eligibilityCriteria=Higit pa tungkol sa mga requirement sa pagiging kwalipikado
# Text shown that allows the users to click on a program details link to find out more about the eligibility criteria for the program.
content.eligibilityCriteria=Para sa mga pamantayan sa pagiging kwalipikado, sumangguni sa {0}
# Describes how to learn more about eligibility criteria for a program. The variable text is "program details", which will become a hyperlink to another webpage.
content.eligibilityCriteria.v2=Para matuto pa tungkol sa mga panuntunan sa pagiging kwalipikado ng programang ito o direktang makipag-ugnayan sa programa, pakitingnan ang {0}.
# Text shown to explain what the user can do since they are not eligible for the program with their current answers.
content.changeAnswersForEligibility=Puwede kang bumalik sa nakaraang page para i-edit ang iyong mga sagot. O mag-apply sa isa pang programa.
# Text shown on a webpage when the applicant is ineligible for a program.
content.changeAnswersForEligibility.v2=Kung sa tingin mo ay may mali, puwede kang bumalik sa nakaraang page para i-edit ang iyong mga sagot. Puwede ka ring bumalik sa homepage para mag-apply sa ibang programa.
# Button for the applicant to go back and edit their responses.
button.goBackAndEdit=Bumalik at i-edit
# Clicking this button returns the user to the program summary page, where they can edit their responses.
button.editMyResponses=I-edit ang mga sagot ko
# Toast message that shows that a client is likely eligible for a program, when someone else is filling out the application on a client''s behalf.
toast.mayQualifyTi=Batay sa iyong mga tugon, posibleng kwalipikado ang iyong kliyente para sa {0}. Para magpatuloy, patuloy na punan ang application.
# Toast message that shows that an applicant is likely eligible for a program, based on their responses.
toast.mayQualify=Batay sa iyong mga tugon, posibleng kwalipikado ka para sa {0}. Para magpatuloy, patuloy na punan ang application.
# Tag on the top of a program card, that lets the applicant know they may qualify for the program, based on their responses in other programs.
tag.mayQualify=Posibleng kwalipikado ka
# Tag on the top of a program card, that lets the person know their client may qualify for the program, based on their responses in other programs. This is in the case when someone is filling out applications on their client''s behalf.
tag.mayQualifyTi=Posibleng kwalipikado ang iyong kliyente
# Tag on the top of a program card, that lets the applicant know they are likely not eligible for the program, based on their responses in other programs.
tag.mayNotQualify=Posibleng hindi ka kwalipikado
# Tag on the top of a program card, that lets the person know their client is likely not eligible for the program, based on their responses in other programs. This is in the case when someone is filling out applications on their client''s behalf.
tag.mayNotQualifyTi=Posibleng hindi kwalipikado ang iyong kliyente
# Toast message that shows that an applicant may not be eligible for a program, based on their responses.
toast.mayNotQualify=Batay sa iyong mga tugon, posibleng hindi ka kwalipikado para sa {0}. Kung nagbago ang iyong impormasyon, puwede mong i-edit ang iyong mga sagot at pagkatapos ay magpatuloy sa application.
# Toast message that shows that a client may not be eligible for a program, when someone else is filling out the application on a client''s behalf.
toast.mayNotQualifyTi=Batay sa iyong mga tugon, posibleng hindi kwalipikado ang kliyente mo para sa {0}. Kung nagbago ang iyong impormasyon ng kliyente, puwede mong i-edit ang iyong mga sagot at pagkatapos ay magpatuloy sa application.

# Error when there was an exception while submitting the application
banner.errorSavingApplication=Nagka-error sa pag-save ng application

# Alert title when applicant may be eligible for a program
alert.eligibility_applicant_eligible_title=Posibleng kwalipikado ka para sa programang ito
# Alert text when applicant may be eligible for a program
alert.eligibility_applicant_eligible_text=Batay sa mga sagot mo, malamang na kwalipikado ka para sa programang ito.

# Alert title when applicant may not be eligible for a program
alert.eligibility_applicant_not_eligible_title=Posibleng hindi ka kwalipikado para sa programang ito.
# Alert text when applicant may not be eligible for a program
alert.eligibility_applicant_not_eligible_text=Batay sa mga sagot mo, posibleng hindi ka kwalipikado para sa programang ito. Kung nagbago ang iyong impormasyon, puwede mong i-edit ang sagot mo at ipagpatuloy ang pag-apply.
# Alert text when applicant may not be eligible for a program (new version for North Star)
alert.eligibilityApplicantNotEligibleTextShort=Batay sa iyong mga sagot sa mga sumusunod na tanong, posibleng hindi ka kwalipikado para sa programang ito.

# Alert title when applicant may be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_applicant_fastforwarded_eligible_title=Posibleng kwalipikado ka para sa programang ito
# Alert text when applicant may be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_applicant_fastforwarded_eligible_text=Posibleng may mga bago o na-update na tanong na nakakaapekto sa mga sagot o pagiging kwalipikado mo. Pakisuri ang lahat ng tanong sa ibaba.

# Alert title when applicant may not be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_applicant_fastforwarded_not_eligible_title=Posibleng hindi ka na kwalipikado para sa programang ito
# Alert text when applicant may not be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_applicant_fastforwarded_not_eligible_text=Posibleng may mga bago o na-update na tanong na nakakaapekto sa mga sagot o pagiging kwalipikado mo. Pakisuri ang lahat ng tanong sa ibaba.

# Alert title when TI client may be eligible for a program
alert.eligibility_ti_eligible_title=Posibleng kwalipikado ang kliyente mo para sa programang ito
# Alert text when TI client may be eligible for a program
alert.eligibility_ti_eligible_text=Batay sa mga sagot mo, malamang na kwalipikado ang iyong kliyente para sa programang ito.

# Alert title when TI client may not be eligible for a program
alert.eligibility_ti_not_eligible_title=Posibleng hindi kwalipikado ang kliyente mo para sa programang ito
# Alert text when TI client may not be eligible for a program
alert.eligibility_ti_not_eligible_text=Batay sa mga sagot mo, posibleng hindi kwalipikado ang iyong kliyente para sa programang ito. Kung nagbago ang impormasyon ng iyong kliyente, puwede mong i-edit ang sagot mo at ipagpatuloy ang pag-apply.
# Alert text when TI client may not be eligible for a program (new version for North Star)
alert.eligibilityTiNotEligibleTextShort=Batay sa iyong mga sagot sa mga sumusunod na tanong, posibleng hindi kwalipikado para sa programang ito ang iyong kliyente.

# Alert title when TI client may be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_ti_fastforwarded_eligible_title=Posibleng kwalipikado ang kliyente mo para sa programang ito
# Alert text when TI client may be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_ti_fastforwarded_eligible_text=Posibleng may mga bago o na-update na tanong na nakakaapekto sa mga sagot o pagiging kwalipikado ng kliyente mo. Pakisuri ang lahat ng tanong sa ibaba.

# Alert title when TI client may not be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_ti_fastforwarded_not_eligible_title=Posibleng hindi na kwalipikado ang kliyente mo para sa programang ito
# Alert text when TI client may not be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_ti_fastforwarded_not_eligible_text=Posibleng may mga bago o na-update na tanong na nakakaapekto sa mga sagot o pagiging kwalipikado ng kliyente mo. Pakisuri ang lahat ng tanong sa ibaba.

# Alert on the program overview page letting the user know that they will likely be eligible for the program.
alert.likelyEligible=Malamang na kwalipikado ka sa programang ito batay sa mga sagot sa ibang pag-apply.
# Alert on the program overview page letting the user know that they will likely NOT be eligible for the program.
alert.likelyIneligible=Posibleng hindi ka maging kwalipikado para sa programang ito batay sa mga sagot sa ibang pag-apply.
# Alert on the program overview page letting a trusted intermediary know that their client will likely be eligible for the program.
alert.clientLikelyEligible=Malamang na kwalipikado ang kliyente mo sa programang ito batay sa mga sagot sa ibang pag-apply.
# Alert on the program overview page letting a trusted intermediary know that their client will likely NOT be eligible for the program.
alert.clientLikelyIneligible=Posibleng hindi maging kwalipikado ang kliyente mo para sa programang ito batay sa mga sagot sa ibang pag-apply.

#---------------------------------------------------------------------------------------------#
# APPLICANT APPLICATION CONFIRMATION PAGE - text for a page confirming application submission #
#---------------------------------------------------------------------------------------------#

# Title on the page after the applicant has successfully submitted an application.
title.applicationConfirmation=Pagkumpirma ng aplikasyon
# A confirmation message that shows after an applicant has submitted their application.
content.confirmed=Salamat sa pag-aplay sa {0}.  Ang inyong aplikasyon ay natanggap na, at ang ID ng application ninyo ay {1}.
# Title (not a main page title) on section prompting an applicant to create an account or sign in to save their data.
title.createAnAccount=Gumawa ng account o magsign-in

# Shown to applicants to encourage login; {0} represents the authentication provider's name
content.pleaseCreateAccount=Kung kayo ay magsign-in ngayon sa isang account ng lungsod, ang inyong impormasyon ay maitatabi at maaring balikan anumang oras upang makumpleto ang mga aplikasyon sa hinaharap. Kung kayo ay wala pang account, pahihintulutan kayo ng pahina ng log-in upang makapagrehistro. Hindi kayo mawawalan ng anumang data na inyo nang nailagay.
# A message to show on the login prompt modal that encourages users to log in before applying to other programs.
content.generalLoginModalPrompt=Hindi ka naka-log in sa isang account. Hindi mo magagawang mabilis na mag-edit ng aplikasyon, tingnan ang status ng iyong aplikasyon, o mag-apply para sa iba pang programa nang walang account.
# A message to show on the login prompt modal that encourages users to log in before applying to a program from the programs index page.
content.initialLoginModalPrompt=Bago ka magpatuloy, mag-log in sa iyong {0} account o gumawa ng account. Makakapag-apply ka para sa maraming programa nang hindi kinakailangang ilagay ulit ang parehong impormasyon. Puwede ka ring mag-edit ng mga aplikasyon at tumingin ng mga status ng iyong aplikasyon.
# A button for continuing to apply to other programs without an account.
button.continueWithoutAnAccount=Magpatuloy nang walang account
title.commonIntakeConfirmation=Mga programa kung saan puwede kang maging kwalipikado
# Title on the page after a trusted intermediary has successfully filled out the common intake form.
title.commonIntakeConfirmationTi=Mga programa kung saan puwedeng maging kwalipikado ang kliyente mo
# A message explaining that the applicant may be eligible for the following list of programs, and that they need to apply to them.
content.commonIntakeConfirmation=Posibleng makapag-enroll ka sa mga programang ito kung mag-a-apply ka sa pamamagitan ng online na aplikasyon sa pamamagitan ng pag-click sa "Mag-apply sa mga programa."
# A message explaining that the applicant may be eligible for the following list of programs.
content.commonIntakeConfirmation.v2=Batay sa iyong mga sagot, posibleng kwalipikado ka sa mga sumusunod na programa:
# A message explaining that the trusted intermediary's client may be eligible for the following list of programs, and that they need to apply to them.
content.commonIntakeConfirmationTi=Posibleng makapag-enroll ang kliyente mo sa mga programang ito kung mag-a-apply ka sa pamamagitan ng online na aplikasyon sa pamamagitan ng pag-click sa "Mag-apply sa mga programa."
# A message explaining that the trusted intermediary's client may be eligible for the following list of programs.
content.commonIntakeConfirmationTi.v2=Batay sa iyong mga sagot, posbileng kwalipikado ang kliyente mo sa mga sumusunod na programa:
# A message explaining that there were no programs the applicant is currently eligible for. The {0} parameter is a link to another website, where the text is the name of that site. It may read "Access Arkansas", for example.
content.commonIntakeNoMatchingPrograms=Hindi makahanap ang pre-screener ng mga programa kung saan puwede kang maging kwalipikado sa panahong ito. Pero puwede kang mag-apply sa mga programa anumang oras sa pamamagitan ng pag-click sa "Mag-apply sa mga programa." O para tumingin ng mga karagdagang programa, puwede kang pumunta sa {0}
# A message explaining that there were no programs the trusted intermediary's client is currently eligible for. The {0} parameter is a link to another website, where the text is the name of that site. It may read "Access Arkansas", for example.
content.commonIntakeNoMatchingProgramsTi=Hindi makahanap ang pre-screener ng mga programa kung saan puwedeng maging kwalipikado ang kliyente mo sa panahong ito. Pero puwede kang mag-apply sa mga programa anumang oras sa pamamagitan ng pag-click sa "Mag-apply sa mga programa." O para tumingin ng mga karagdagang programa, puwede kang pumunta sa {0}
# A message explaining a second option when there are no eligible programs, which is to edit your responses.
content.commonIntakeNoMatchingProgramsNextStep=Puwede ka ring bumalik sa nakaraang page para i-edit ang iyong mga sagot.
# A header above a list of other programs the applicant might be interested in applying to.
content.otherProgramsToApplyFor=Ibang programang posibleng kainteresan mo
# Button on the "Application Submitted" page. Clicking it downloads the user's application.
button.downloadApplication=I-download ang iyong application
button.downloadPdf=I-download ang PDF
# A button prompting users to apply to programs.
button.applyToPrograms=Mag-apply sa mga programa
# Heading above a section showing the user's name, confirmation number, and date
heading.yourSubmissionInformation=Impormasyon ng iyong pagsusumite
# Text above the name of the person who submitted this application
heading.applicantName=Sino ang nagsumite ng aplikasyong ito
# Text above the confirmation number of the application
heading.confirmationNumber=Numero ng kumpirmasyon
# Text above the date submitted of the application
heading.dateSubmitted=Petsa ng pagsusumite
# Text above a message telling the user they can print this page
heading.forYourRecords=Kumpirmasyon para sa iyong mga record
# Text above a button to print this page
content.youCanPrint=Puwede mong i-print ang kumpirmasyong ito para sa iyong mga record.
# Heading above a paragraph explaining the next steps in the application process
heading.nextSteps=Ano ang mga susunod na hakbang ko?
# Confirmation "alert" message that appears next to a checkmark icon. {0} is the program name, such as "Housing Assistance Program"
alert.submitted=Isinumite mo ang iyong aplikasyon sa {0}
# Informational "alert" title to encourage the user to create an account
alert.createAccount=Gumawa ng account para i-save ang impormasyon ng iyong aplikasyon
# Description text in an alert that explains how creating an account can help the user
alert.createAccountDescription=Gumamit ng account para mag-log in ulit para tingnan ang status ng iyong aplikasyon at makatipid ng oras sa pag-apply sa iba pang programa.
# Hyperlink to log in to an existing account
content.loginToExistingAccount=Mag-log in sa kasalukuyang account

#-------------------------------------------------------------------------------------------------------------------------#
# APPLICANT DUPLICATE SUBMISSION PAGE/MODAL - text for a page informing the applicant of a duplicate submission and redirecting #
#-------------------------------------------------------------------------------------------------------------------------#

title.noChangesToSave=Walang pagbabagong ise-save para sa {0}
content.noChanges=Wala ka pang ginagawang anumang pagbabago sa aplikasyon. Gusto mo bang magpatuloy sa pag-edit?
button.continueEditing=Magpatuloy sa pag-edit
button.exitApplication=Lumabas sa aplikasyon


#--------------------------------------------------------------------------------------------------------------#
# Disabled program error page - error info page when user tries to access a disabled program via its deep link #
#--------------------------------------------------------------------------------------------------------------#

# Title of the page stating the program is disabled
title.programNotAvailable=Hindi na available ang program na ito
# Info stating program disabled and see home page for other options.
content.disabledProgramInfo=Pasensya na, na-disable ang program na sinusubukan mong i-access. Para makakita ng mga karagdagang program, bumalik sa home page.
# Button text on a button that takes the user back to the home page.
button.homePage=Bumalik sa homepage


#----------------------------------------------------------#
# ADDRESS QUESTION - text when viewing an address question #
#----------------------------------------------------------#

# Address question labels - these are shown as the field label before an input box.
label.addressLine2=Apartment, suite, atbp. (opsyonal)
label.city=Lungsod
label.state=Estado
label.selectState=Pumili ng Estado
label.street=Address
label.zipcode=Zip code

# Validation errors that are shown when a user enters an invalid address.
validation.streetRequired=Maaari lamang na ilagay ang wasto na pangalan ng kalye, at ang numero.
validation.cityRequired=Maaari lamang na ilagay ang Lungsod.
validation.currencyMisformatted=Ang currency ay dapat nasa isa sa mga sumusunod na format: 1000 1,000 1000.30 1,000.30
validation.stateRequired=Maaari lamang na ilagay ang Estado.
validation.invalidZipcode=Maaari lamang na ilagay ang 5-bilang na ZIP code.
validation.noPoBox=Maaari lamang na ilagay ang wasto na adres. Kami ay hindi natanggap ng mga PO Box.

#-------------------------------------------------------------------------#
# ADDRESS CORRECTION - text when viewing the address correction screen    #
#-------------------------------------------------------------------------#

# Title for a page asking the user to check if their address is correct and check whether it matches one of the listed address suggestions
title.confirmAddress=Kumpirmahin ang iyong address
# Message explaining that the address the user provided couldn't be found when comparing it to a list of valid addresses.
content.confirmAddressLine1=Hindi namin nahanap ang iyong address.
# Message asking the user to check if their address is correct, or choose from the provided list of suggested valid addresses.
content.foundSimilarAddressLine2=Pakitingnan kung tama ang spelling ng address mo o pumili ng opsyon sa listahang ibinigay.
# Message asking the user to check if their address is correct and/or edit the address so that it's valid.
content.noValidAddressLine2=Pakitingnan kung tama ang spelling ng address mo at i-edit ito para magpatuloy.
# Heading for showing the user the address they provided earlier
content.addressEntered=Inilagay mong address:
# Text on dialog that shows a suggestion for the user's correct address when entered incorrectly.
content.suggestedAddress=Iminumungkahing address:
# Text on dialog that shows a suggestions for the user's correct address when entered incorrectly (and there is more than one).
content.suggestedAddresses=Mga iminumungkahing address:
# Text on a button to save the address that the user chose (either the original address they entered, or one of the suggested valid addresses)
button.confirmAddress=Kumpirmahin ang address

#----------------------------------------------------------------------------------------------------------#
# DROPDOWN QUESTION - text shown when answering a question where a user must select an option from a list. #
#----------------------------------------------------------------------------------------------------------#

# Placeholder text shown in a dropdown selector before the user has made a selection.
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

validation.invalidDateFormat=Maglagay ng petsa sa tamang format
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.January=01 - Enero
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.February=02 - Pebrero
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.March=03 - Marso
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.April=04 - Abril
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.May=05 - Mayo
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.June=06 - Hunyo
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.July=07 - Hulyo
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.August=08 - Agosto
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.September=09 - Setyembre
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.October=10 - Oktubre
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.November=11 - Nobyembre
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.December=12 - Disyembre
# A placeholder for the month dropdown menu when selecting a specific date.
placeholder.memorableDate=Piliin
# An error message that appears when the user tries to search by date without completing all the fields.
error.incompleteDate=Error: Ilagay ang buwan, petsa, at taon.
# An error message that appears when the user tries to enter a date of birth that is in the future.
validation.dobNotInPast=Kailangang nasa nakaraan ang Petsa ng Kapanganakan.
# An error message that appears when the user tries to enter a date of birth that is too far in the past.
validation.impossibleDob=Kailangang hindi hihigit sa 150 taon ang nakalipas ng Petsa ng Kapanganakan.

#--------------------------------------------------------------------------------------------------------#
# ENUMERATION QUESTION - text when viewing a question where a user must name entities, such as children. #
#--------------------------------------------------------------------------------------------------------#

# Text on a button an applicant would click to add another entity
button.addEntity=Idagdag {0}
# Text on a button an applicant would click to delete an entity
button.removeEntity=Alisin {0}
# Dialog text shown to the user when they choose to delete an entry that they added previously. The dialog asks them to confirm if they want to delete the entry and all data associated with it. {0} represents the type of entry being deleted. For example, users may be asked to list all cars they own or all children in their household, so {0} could be "car" or "child".
dialog.confirmDeleteAllButtonsSave=Sigurado ka bang gusto mong alisin ang {0} na ito? Ise-save ang pagbabagong ito kapag na-click mo ang "Suriin," "Nakaraan," o "Susunod" at mawawala ang lahat ng data na nauugnay sa {0} na ito.
validation.entityNameRequired=Maaari lamang ay maglagay ng halaga para sa bawat linya.
# Validation error that shows when an applicant uses a duplicate entity name
validation.duplicateEntityName=Maaari lamang ay maglagay ng natatangi na halaga para sa bawat linya.
# Error shown if the enumerator question has a maximum answer limit and the applicant enters too many values
validation.tooManyEntities=Maglagay ng hindi lalampas sa {0} (na) value.
# Error shown if the enumerator question has a minimum answer limit and the applicant enters too few values
validation.tooFewEntities=Maglagay ng hindi bababa sa {0} (na) value.
placeholder.entityName={0} Palayaw

#---------------------------------------------------------------------------------#
# FILEUPLOAD QUESTION - text when viewing a question where a user uploads a file. #
#---------------------------------------------------------------------------------#

# Validation error that shows when an applicant does not select a file to upload
validation.fileRequired=Maaari lamang ay pumili ng file.
# An error shown to the user if they upload a file that's too large. The error also asks them to upload a smaller file. {0} will be a number specifying the maximum size the file can be in megabytes. For example, "Please choose a file less than 100 MB".
validation.fileTooLarge=Masyadong malaki ang iyong file. Pumili ng file na wala pang {0} MB.
button.chooseFile=Pumili ng File
# Text shown on a tag while a file is uploading.
label.uploading=Ina-upload
# Label applied to a list of uploaded files
label.uploadedFiles=Mga Na-upload na File

#---------------------------------------------------------------------#
# ID QUESTION - id specific to filling out a question. #
#---------------------------------------------------------------------#

# Validation errors that appear if a user enters an answer that is too long or too short.
validation.idTooLong=Dapat ay naglalaman ng hindi hihigit sa {0} na mga character.
validation.idTooShort=Dapat ay naglalaman ng hindi bababa sa {0} na mga character.
validation.numberRequired=Dapat ay naglalaman lamang ng mga numero.

#----------------------------------------------------------------------------------------------------------#
# MULTI-SELECT QUESTION - text shown when filling out a question with multiple answers, such as a checkbox #
#----------------------------------------------------------------------------------------------------------#

# Validation errors that are shown if a user selects too many or too few answers.
validation.tooFewSelections=Maaari lamang na pumili ng hindi bababa sa {0}.
# Error message that indicates the applicant has selected too many choices on the multi-select question
validation.tooManySelections.v2=Pumili ng hindi hihigit sa {0}.

#---------------------------------------------------#
# NAME QUESTION - text when viewing a name question #
#---------------------------------------------------#

# Name question labels - these are shown as the field label before an input box.
label.firstName=Pangalan
label.lastName=Apelyido
label.middleName=Gitnang pangalan
# Label for name suffix field in name question
label.nameSuffix=Suffix

# Placeholder text - this is shown inside the input box, before a user enters an answer.
placeholder.firstName=Pangalan
placeholder.lastName=Apelyido
placeholder.middleName=Gitnang pangalan
# Placeholder for name suffix field in name question
placeholder.nameSuffix=Suffix

# Option Junior for the name suffix dropdown
option.junior=Jr.
# Option Senior for the name suffix dropdown
option.senior=Sr.
# Option the first for the name suffix dropdown
option.first=I
# Option the second for the name suffix dropdown
option.second=II
# Option the third for the name suffix dropdown
option.third=III
# Option the forth for the name suffix dropdown
option.forth=IV
# Option the fifth for the name suffix dropdown
option.fifth=V

# Validation errors - these are shown if a user enters an invalid name.
validation.firstNameRequired=Maaari lamang na ilagay ang inyong pangalan.
validation.lastNameRequired=Maaari lamang na ilagay ang inyong apelyido.

#------------------------------------------------------------------------#
# NUMBER QUESTION - text specific to answering a question with a number. #
#------------------------------------------------------------------------#

# Validation errors that are shown when a user enters a number that is too big or too small, or a non-integer.
validation.numberTooBig=Dapat ay hindi hihigit sa {0}.
validation.numberTooSmall=Dapat ay hindi bababa sa {0}.
validation.numberNonInteger=Dapat ay naglalaman lamang ng mga numero.

#---------------------------------------------------------------------#
# TEXT QUESTION - text specific to filling out a question with words. #
#---------------------------------------------------------------------#

# Validation errors that appear if a user enters an answer that is too long or too short.
validation.textTooLong=Dapat ay naglalaman ng hindi hihigit sa {0} na mga character.
validation.textTooShort=Dapat ay naglalaman ng hindi bababa sa {0} na mga character.

#---------------------------------------------------------------------#
# MULTI OPTION QUESTION ADMIN EDIT - text specific when creating/editing a multi option question. #
#---------------------------------------------------------------------#

# Validation errors that appear if an admin leaves a multi option question blank.
adminValidation.multiOptionEmpty=Ang mga katanungan na maraming pagpipiliang opsyon ay hindi maaaring magkaroon ng mga blanko na mga opsyon.
# An error messaging saying that the admin name (a unique, admin-facing identifier) that the admin just entered is invalid.
adminValidation.multiOptionAdminError=Maliliit na titik, numero, underscore, at gitling lang ang puwedeng gamitin para sa mga pangalan ng admin.

#----------------------------------#
# ERRORS - various error messages  #
#----------------------------------#

error.notFoundTitle=Hindi namin nahanap ang page na sinubukan mong puntahan
error.notFoundDescription=Bumalik o pumunta ulit sa {0}
error.notFoundDescriptionLink=homepage
# The title on the error page that is show to the user in large font
error.internalServerTitle.v2=Nagkaroon ng error
# The subtitle on the error page that is show to the user in medium font
error.internalServerSubtitle=Pasensya na, nagkaroon ng internal error sa aming system.
# Additional information about the error that is shown to the user in small font
error.internalServerDescription=Makipag-ugnayan sa technical support para sa tulong sa %s at isama itong error ID na {0}
# A button redirecting the user to the homepage
error.internalServerHomeButton=Pumunta sa homepage
# A message to the user indicating what the status code is of the error that was thrown, for example 404 or 500
error.statusCode=Error code: {0}

#-------------------------------------------------------------#
# EMAILS - boilerplate text contained in emails sent to users #
#-------------------------------------------------------------#

# Bottom of the body of the email sent automatically on application submission
email.loginToCiviform=Mag-log in sa CiviForm sa {0}
# Subject of the email sent automatically on application submission
email.applicationReceivedSubject=Natanggap na ang iyong aplikasyon sa programang {0}
# Body of the email sent automatically on application submission
email.applicationReceivedBody=Natanggap na ang aplikasyon mo sa programang {0}. {1} ang applicant ID mo at {2} ang application ID
# Subject of the email sent to the TI on application submission
email.tiApplicationSubmittedSubject=Nagsumite ka ng aplikasyon para sa programang {0} sa ngalan ng aplikanteng si {1}
# Body of the email sent to the TI on application submission
email.tiApplicationSubmittedBody=Natanggap na ang aplikasyon sa programang {0} bilang aplikanteng si {1}, at {2} ang application ID.
# Link at the bottom of the email sent to the TI on application submission
email.tiManageYourClients=Pamahalaan ang iyong mga client sa {0}.
# Subject of the email sent to an applicant when the status of their application has changed
email.applicationUpdateSubject=Update sa iyong aplikasyong {0}
# Subject of the email sent to a TI when they change the status of an application
email.tiApplicationUpdateSubject=Update sa aplikasyon para sa programang {0} sa ngalan ng aplikanteng si {1}
# Body of the email sent to a TI when they change the status of an application
email.tiApplicationUpdateBody=Ang status para sa aplikanteng si {0} sa programang {1} ay naging {2}.

#------------------------------------------------------------------------------#
# GOVERNMENT BANNER - text explaining that CF is an official government site #
#------------------------------------------------------------------------------#

banner.title=Isa itong opisyal na website ng pamahalaan.
# Text on a banner at the top of the page telling users this is an official government website
banner.northStarTitle=Opisyal na website ng pamahalaan ng United States
banner.link=Ganito mo malalaman
banner.govWebsiteSectionHeader=Gumagamit ang mga opisyal na website ng .gov
banner.govWebsiteSectionContent=Ang mga address ng website na nagtatapos sa .gov ay pagmamay-ari ng mga opisyal na organisasyon ng pamahalaan sa United States.
banner.httpsSectionHeader=Gumagamit ng HTTPS ang mga secure na .gov na website
banner.httpsSectionContent=Ang lock na ( {0} ) o https:// ay nangangahulugang ligtas kang nakakonekta sa .gov na website. Magbahagi lang ng sensitibong impormasyon sa mga opisyal at secure na website.

#----------------------------------------------------------------------------------------#
# NOT PRODUCTION BANNER - text explaining that this site is not a production environment #
#----------------------------------------------------------------------------------------#

# A stern message shown to the user to make it clear this is a test site only
banner.notForProductionBannerLine1=Para lang sa layunin ng pagsubok ang site na ito. Huwag maglagay ng personal na impormasyon.
# A friendly message shown to the user with a call to action to go to the production site link provided in {0}
banner.notForProductionBannerLine2=Para mag-apply sa isang program o serbisyo, pumunta sa {0}.

#------------------------------------------------------------------------------#
# GUEST BANNER - alert explaining that user is logged in as a guest #
#------------------------------------------------------------------------------#
# A message in an alert about ending their session when they are finished. The placeholder in {0} will be a link with the text 'end your session'.
banner.guestBannerText=After you apply, {0} to protect your data.
# A call to action within another message that will be a link to end your guest session.
banner.endYourSession=tapusin ang session mo

#------------------------------------------------------------------------------#
# CATEGORIES - tags that admins can choose to specify the type of program #
# Note for developers: these messages aren't used in static text, but instead they #
#  are seeded into the database, since these will be admin defined in the future #
#------------------------------------------------------------------------------#

# A tag used to filter the list of programs down to those that are related to childcare.
category.tag.childcare=Pangangalaga ng Bata

# A tag used to filter the list of programs down to those that are related to economy.
category.tag.economic=Pang-ekonomiya

# A tag used to filter the list of programs down to those that are related to education.
category.tag.education=Edukasyon

# A tag used to filter the list of programs down to those that are related to employment.
category.tag.employment=Pagtatrabaho

# A tag used to filter the list of programs down to those that are related to food.
category.tag.food=Pagkain

# A tag used to filter the list of programs down to those that have a general purpose.
category.tag.general=Pangkalahatan

# A tag used to filter the list of programs down to those that are related to healthcare.
category.tag.healthcare=Pangangalagang Pangkalusugan

# A tag used to filter the list of programs down to those that are related to housing.
category.tag.housing=Pabahay

# A tag used to filter the list of programs down to those that are related to the Internet.
category.tag.internet=Internet

# A tag used to filter the list of programs down to those that are related to training.
category.tag.training=Pagsasanay

# A tag used to filter the list of programs down to those that are related to transportation.
category.tag.transportation=Transportasyon

# A tag used to filter the list of programs down to those that are related to utilities.
category.tag.utilities=Mga Utility
