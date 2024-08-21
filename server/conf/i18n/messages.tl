# Please see the README.md in the same directory for more information on localization.
#
# ALWAYS add context strings to the Transifex dashboard immediately after
# merging changes to this file. See
# https://github.com/civiform/civiform/wiki/Internationalization-(i18n)#internationalization-for-application-strings

#-------------------------------------------------------------#
# GENERAL - contains text that corresponds to multiple views. #
#-------------------------------------------------------------#

button.logout=Mag-logout
label.selectLanguage=Pakipili ang iyong gustong wika.
button.untranslatedSubmit=Isumite
# Text on a link that gives more information about CiviForm. "CiviForm" is the app name and should
# not be translated.
footer.aboutCiviform=Tungkol sa CiviForm
footer.supportLinkDescription=Para sa suporta na pang teknikal, tawagan
# Text in the footer telling users that this is an official website of the specified Civic entity.
footer.officialWebsiteOf=Opisyal na website ng {0}
# Text on a link that will scroll to the top of the page
footer.returnToTop=Bumalik sa itaas
# A message in the footer directing users to technical support. The placeholder is a link to
# send an email to CiviForm technical support.
footer.technicalSupport=Para sa technical support: {0}
guest=Bisita
label.languageSr=Pumili ng wika
# A label on the button that opens the header menu shown on mobile
header.menu=Menu
header.endSession=Tapusin ang session
toast.sessionEnded=Natapos na ang iyong session.
header.guestIndicator=Bisitang user ka
header.userName=Nakalog-in bilang {0}
validation.isRequired=Ang tanong na ito ay kinakailangan.
validation.invalidInput=Maaari lamang ay maglagay ng halaga para sa bawat linya.
validation.errorAnnouncementSr=May mga error sa form. Ayusin ang mga iyon bago magpatuloy.
content.requiredFieldsAnnotation=Tandaan: Kinakailangan ang mga field na may *.
content.requiredFieldsNote=Tandaan: Nire-require ang mga field na may markang {0}.
# Disclaimer at top of form explaining that asterisks mark required questions. The placeholder is the red, bold asterisk used throughout the page.
content.requiredFieldsNoteNorthStar=Minamarkahan ng asterisk ({0}) ang mga nire-require na field.
content.optional=(opsyonal)
content.mobileFileUploadHelp=Ginagamit ang iyong telepono? Binibigyang-daan ka rin ng "Pumili ng File" na gamitin ang camera ng iyong telepono para mag-upload ng mga dokumento.
toast.errorMessageOutline=Error: {0}
# Description for an "X" button that will close a dialog, modal, or page.
button.close=Isara
# Button text that will navigate the applicant to a page where they can review previous answers
button.review=Pagsusuri
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
# The label on a button that will navigate the user to the previous section of the form.
button.back=Bumalik
# The label on a button that will save user answers and navigate to the summary of the application.
button.saveAndExit=Mag-save at tapusin ang application sa ibang pagkakataon
button.skipFileUpload=Laktawan
content.blockProgress={0} sa {1}
# A message on a section indicating how many questions of the total have been answered by the applicant.
content.blockProgressFull={0} sa {1} ang natapos
# An aria-label on a progress bar showing user how much of the application they have completed.
content.blockProgressLabel=Pag-usad ng aplikasyon
# A message on a section indicating how many files have been uploaded in a section consisting of a file upload question
content.fileUploadBlockProgressFull={0} sa {1} ang na-upload
# Label on a section of a progress bar. The first placeholder is the section number and second is section name (eg: "2. Contact info")
label.blockIndexLabel={0}. {1}
toast.localeNotSupported=Pasensya na, hindi ganap na nakasalin ang program na ito sa iyong mas gustong wika.
link.allDone=Ako ay tapos na
link.applyToAnotherProgram=Mag-aplay sa ibang programa
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
# Button on each program card that allows the user to view the details of the program, possibly through an external link.
button.viewDetails=Tingnan ang mga detalye
content.submittedDate=Isinumite noong {0}
content.findPrograms=Maghanap ng mga program
content.findProgramsDescription=Matutulungan ka ng CiviForm na humanap ng mga programa kung saan posibleng kwalipikado ka sa {0}. Magsimula sa pamamagitan ng pagpili ng aplikasyon sa ibaba.
content.saveTimeServices=Makatipid ng oras sa pag-apply sa mga programa at serbisyo
content.guestDescription=Mag-log in sa iyong {0} account o gumawa ng account at mag-apply sa mga programa nang hindi kinakailangang ilagay ulit ang parehong impormasyon. Puwede ka ring mag-edit ng mga aplikasyon anumang oras at tumingin sa mga status ng iyong aplikasyon.
# The label for the program filter buttons
label.programFilters=Program filters
link.programDetails=Mga detalya ng programa
link.programDetailsSr=Mga detalye ng programa para sa {0}, bubukas sa bagong tab
title.programs=Mga programa at mga serbisyo
title.getStartedSection=Magsimula
title.allProgramsSection=Lahat ng programa ({0})
title.status=Status
title.inProgressProgramsUpdated=Kasalukuyang ginagawa
title.activeProgramsUpdated=Hindi sinimulan
# Title of the section on the home page with the benefits finder form.
title.benefitsFinderSection=Maghanap ng mga serbisyong makakatulong sa iyo
# Title of the section on the home page that shows programs for which the applicant has already started or submitted applications.
title.myApplicationsSection=Mga aplikasyon ko ({0})
# Title of the section on the home page that shows any programs that don't match the selected filters.
title.otherProgramsSection=Iba pang program ({0})
# Title of the section on the home page that shows all programs when no filter is selected.
title.programsSection=Mga Program ({0})
# Title of the section on the home page that shows programs that match any of the selected filters.
title.recommendedSection=Inirerekomenda ({0})
# A label on the summary of a section of the application indicating to the applicant that all required questions have been filled out.
title.programSectionCompleted=Natapos na ang seksyon
title.submittedPrograms=Isinumite
toast.applicationSaved=Successfully saved application: Matagumpay na nai-save ang aplikasyon: ID ng aplikasyon {0}
toast.programCompleted=Nakumpleto na ang aplikasyon.

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

button.submit=Isumite
button.continue=Magsimula dito
# Button text on a button that will allow the user to start filling out a section of questions.
button.start=Magsimula
toast.applicationOutOfDate=Nagkaroon ng update sa application, pakisuri at sagutan ang mga sagot para magpatuloy

# Help text at the top of the review page instructing applicants with how to start applying for programs.
content.reviewPageIntro=Pumili ng hakbang sa ibaba para simulan ang iyong aplikasyon. Puwede kang mag-save at bumalik anumang oras.
content.doesNotQualify=Hindi kwalipikado
link.edit=I-edit
link.answer=Sagot
content.previouslyAnsweredOn=Sinagutan noong {0}
ariaLabel.answer=Sagot {0}

ariaLabel.edit=I-edit {0}
title.commonIntakeSummary=Buod ng pre-screener para sa mga benepisyo
# Heading content at the top of the review page, where applicants can start answering questions.
title.getStarted=Magsimula na tayo
title.programSummary=Buod ng application para sa programa

#------------------------------------------------------------------------#
# APPLICANT ELIGIBILITY - text related to applicant eligibility #
#------------------------------------------------------------------------#

title.applicantNotEligible=Batay sa iyong mga sagot sa mga sumusunod na tanong, posibleng hindi ka kwalipikado para sa {0}.
title.applicantNotEligibleTi=Batay sa iyong mga sagot sa mga sumusunod na tanong, posibleng hindi kwalipikado ang client mo para sa {0}.
# Section heading
heading.eligibilityCriteria=More about eligibility requirements
content.eligibilityCriteria=Para sa mga pamantayan sa pagiging kwalipikado, sumangguni sa {0}
# Describes how to learn more about eligibility criteria for a program. The variable text is "program details", which will become a hyperlink to another webpage.
content.eligibilityCriteria.v2=To learn more about this program''s eligibility rules or contact the program team directly, please see {0}.
content.changeAnswersForEligibility=Puwede kang bumalik sa nakaraang page para i-edit ang iyong mga sagot. O mag-apply sa isa pang programa.
# Text shown on a webpage when the applicant is ineligible for a program.
content.changeAnswersForEligibility.v2=If you think there was a mistake, you can return to the previous page to edit your answers. You can also go back to the homepage to apply to a different program.
button.goBackAndEdit=Bumalik at i-edit
# Clicking this button returns the user to the program summary page, where they can edit their responses.
button.editMyResponses=Edit my responses
toast.mayQualifyTi=Batay sa iyong mga tugon, posibleng kwalipikado ang iyong kliyente para sa {0}. Para magpatuloy, patuloy na punan ang application.
toast.mayQualify=Batay sa iyong mga tugon, posibleng kwalipikado ka para sa {0}. Para magpatuloy, patuloy na punan ang application.
tag.mayQualify=Posibleng kwalipikado ka
tag.mayQualifyTi=Posibleng kwalipikado ang iyong kliyente
tag.mayNotQualify=Posibleng hindi ka kwalipikado
tag.mayNotQualifyTi=Posibleng hindi kwalipikado ang iyong kliyente
toast.mayNotQualify=Batay sa iyong mga tugon, posibleng hindi ka kwalipikado para sa {0}. Kung nagbago ang iyong impormasyon, puwede mong i-edit ang iyong mga sagot at pagkatapos ay magpatuloy sa application.
toast.mayNotQualifyTi=Batay sa iyong mga tugon, posibleng hindi kwalipikado ang kliyente mo para sa {0}. Kung nagbago ang iyong impormasyon ng kliyente, puwede mong i-edit ang iyong mga sagot at pagkatapos ay magpatuloy sa application.

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
alert.eligibilityApplicantNotEligibleTextShort=Based on your responses to the following questions, you may not be eligible for this program.

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
alert.eligibilityTiNotEligibleTextShort=Based on your responses to the following questions, your client may not be eligible for this program.

# Alert title when TI client may be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_ti_fastforwarded_eligible_title=Posibleng kwalipikado ang kliyente mo para sa programang ito
# Alert text when TI client may be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_ti_fastforwarded_eligible_text=Posibleng may mga bago o na-update na tanong na nakakaapekto sa mga sagot o pagiging kwalipikado ng kliyente mo. Pakisuri ang lahat ng tanong sa ibaba.

# Alert title when TI client may not be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_ti_fastforwarded_not_eligible_title=Posibleng hindi na kwalipikado ang kliyente mo para sa programang ito
# Alert text when TI client may not be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_ti_fastforwarded_not_eligible_text=Posibleng may mga bago o na-update na tanong na nakakaapekto sa mga sagot o pagiging kwalipikado ng kliyente mo. Pakisuri ang lahat ng tanong sa ibaba.

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
# A header above a list of other programs the applicant might be interested in applying to.
content.otherProgramsToApplyFor=Ibang programang posibleng kainteresan mo
button.downloadPdf=I-download ang PDF
button.applyToPrograms=Mag-apply sa mga programa
# Heading above a section showing the user's name, confirmation number, and date
heading.yourSubmissionInformation=Your submission information
# Text above the name of the person who submitted this application
heading.applicantName=Who submitted this application
# Text above the confirmation number of the application
heading.confirmationNumber=Confirmation number
# Text above the date submitted of the application
heading.dateSubmitted=Date submitted
# Text above a message telling the user they can print this page
heading.forYourRecords=Confirmation for your records
# Text above a button to print this page
content.youCanPrint=You can print this confirmation for your records.
# Button to print this webpage
button.print=Print this page
# Heading above a paragraph explaining the next steps in the application process
heading.nextSteps=What are my next steps?
# Confirmation "alert" message that appears next to a checkmark icon. {0} is the program name, such as "Housing Assistance Program"
alert.submitted=You''ve submitted your {0} application
# Informational "alert" title to encourage the user to create an account
alert.createAccount=Create an account to save your application information
# Description text in an alert that explains how creating an account can help the user
alert.createAccountDescription=Use an account to log back in to check on your application status and save time applying to other programs.
# Hyperlink to log in to an existing account
content.loginToExistingAccount=Login to an existing account

#-------------------------------------------------------------------------------------------------------------------------#
# APPLICANT DUPLICATE SUBMISSION PAGE - text for a page informing the applicant of a duplicate submission and redirecting #
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
content.suggestedAddresses=Mga iminumungkahing address:
# Text on a button to save the address that the user chose (either the original address they entered, or one of the suggested valid addresses)
button.confirmAddress=Kumpirmahin ang address

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

button.addEntity=Idagdag {0}
button.removeEntity=Alisin {0}
# Dialog text shown to the user when they choose to delete an entry that they added previously. The dialog asks them to confirm if they want to delete the entry and all data associated with it. {0} represents the type of entry being deleted. For example, users may be asked to list all cars they own or all children in their household, so {0} could be "car" or "child".
dialog.confirmDeleteAllButtonsSave=Sigurado ka bang gusto mong alisin ang {0} na ito? Ise-save ang pagbabagong ito kapag na-click mo ang "Suriin," "Nakaraan," o "Susunod" at mawawala ang lahat ng data na nauugnay sa {0} na ito.
validation.entityNameRequired=Maaari lamang ay maglagay ng halaga para sa bawat linya.
validation.duplicateEntityName=Maaari lamang ay maglagay ng natatangi na halaga para sa bawat linya.
# Error shown if the enumerator question has a maximum answer limit and the applicant enters too many values
validation.tooManyEntities=Maglagay ng hindi lalampas sa {0} (na) value.
# Error shown if the enumerator question has a minimum answer limit and the applicant enters too few values
validation.tooFewEntities=Maglagay ng hindi bababa sa {0} (na) value.
placeholder.entityName={0} Palayaw

#---------------------------------------------------------------------------------#
# FILEUPLOAD QUESTION - text when viewing a question where a user uploads a file. #
#---------------------------------------------------------------------------------#

validation.fileRequired=Maaari lamang ay pumili ng file.
# An error shown to the user if they upload a file that's too large. The error also asks them to upload a smaller file. {0} will be a number specifying the maximum size the file can be in megabytes. For example, "Please choose a file less than 100 MB".
validation.fileTooLarge=Masyadong malaki ang iyong file. Pumili ng file na wala pang {0} MB.
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
# Error message that indicates the applicant has selected too many choices on the multi-select question
validation.tooManySelections.v2=Pumili ng hindi hihigit sa {0}.

#---------------------------------------------------#
# NAME QUESTION - text when viewing a name question #
#---------------------------------------------------#

label.firstName=Pangalan
label.lastName=Apelyido
label.middleName=Gitnang pangalan
# Label for name suffix field in name question
label.nameSuffix=Suffix

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
# An error messaging saying that the admin name (a unique, admin-facing identifier) that the admin just entered is invalid.
adminValidation.multiOptionAdminError=Maliliit na titik, numero, underscore, at gitling lang ang puwedeng gamitin para sa mga pangalan ng admin.

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
banner.guestBannerText=Nag-a-apply ka bilang bisita. Pagkatapos mong mag-apply, {0} para protektahan ang data mo.
# A call to action within another message that will be a link to end your guest session.
banner.endYourSession=tapusin ang session mo
