# ------------------------------------------------------------------------------- #
# Tagalog
#
# CAUTION: Apostrophes are used as escape characters
# To type an apostrophe in a message, please type two. Example: can't -> can''t
# https://www.playframework.com/documentation/2.8.x/JavaI18N#Notes-on-apostrophes
# ------------------------------------------------------------------------------- #

# NO TRANSLATION NEEDED - this appears before applicants select their preferred language, so we always use the default.
label.selectLanguage=Please select your preferred language.

# NO TRANSLATION NEEDED - this appears before applicants select their preferred language, so we always use the default.
button.untranslatedSubmit=Submit

# The text on the button an applicant clicks to log out of their session.
button.logout=Mag-logout

# Message displayed before the support email address in the page footer for applicants.
footer.supportLinkDescription=Para sa suporta na pang teknikal, tawagan

# Placeholder message when an applicant clicked the "Continue as Guest".
# This should be consistent with button.guestLogin.
# TODO(#1918): Translate.
guest=Guest

# Label read by screen readers for the language dropdown shown in the page header.
label.languageSr=Pumili ng wika

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

# Message displayed below "Choose File" button to say that on the phone users can use their camera.
content.mobileFileUploadHelp=Ginagamit ang iyong telepono? Binibigyang-daan ka rin ng “Pumili ng File” na gamitin ang camera ng iyong telepono para mag-upload ng mga dokumento.
#-------------------------------------------------------------#
# LOGIN - contains text that for login page.                  #
#-------------------------------------------------------------#

# Prompt for applicant to log in, input is the full civic entity name.
content.loginPrompt=Maari lamang maglog-in gamit ang inyong account sa Lungsod ng {0}

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

# The text on the anchor for admins to log in to their session.
link.adminLogin=Admin login

#--------------------------------------------------------------------------#
# PROGRAM FORM - contains text shown when filling out an application form. #
#--------------------------------------------------------------------------#

# The text displayed when a file has already been uploaded and the name is being displayed.
input.fileAlreadyUploaded=Na-upload na ang file: {0}

# The text on the button an applicant clicks to delete an uploaded file.
button.deleteFile=Burahin

# The text on the button an applicant clicks to skip uploading a new file while there is already an uploaded file.
button.keepFile=Ipagpatuloy

# The button text for saving the current applicant-entered data and continuing to the next screen in the form.
button.nextScreen=Susunod

# The button text for navigating to the previous screen in the form.
button.previousScreen=Nakaraang

# The text on the button an applicant clicks to jump to the review page.
button.review=Pagsusuri

# The text on the button an applicant clicks to skip uploading a file.
button.skipFileUpload=Laktawan

# A toast message that displays when a program is not fully localized to the applicant's preferred locale.
toast.localeNotSupported=Ikinalulungkot namin, Ang programa na ito ay hindi lubos na naisalin sa inyong ninanais na wika.

# A link that logs out a guest user after they submit an application.
link.allDone=Ako ay tapos na

# A link that appears next to the create account button that offers applicants the option to not create an account right now.
link.applyToAnotherProgram=Mag-aplay sa ibang programa

# A link that offers applicants the option to create an account.
link.createAccountOrSignIn=Gumawa ng account o magsign-in

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

# Text describing the date the application was last submitted.
content.submittedDate=Isinumite noong {0}

# "Get benefits" floating text
content.benefits=Kumuha ng mga benepisyo

# Long form description of the CiviForm site shown to the applicant. Line 1.
content.description1=Pinahihintulutan ng CiviForm na makapag-aplay kayo para sa maraming mga benepisyo ng minsanan lamang sa pamamagitan ng paggamit muli ng impormasyon.

# Long form description of the CiviForm site shown to the applicant. Line 2.
content.description2=Magsimula sa pamamagitan ng pagpili ng aplikasyon sa ibaba.

# Link text to read more about a program.
link.programDetails=Mga detalya ng programa
# The same text, read for screen readers.
link.programDetailsSr=Mga detalya ng programa para sa {0}

# The title of the page for the list of programs.
title.programs=Mga programa at mga serbisyo

# Subtitle for the list of programs with draft applications
title.inProgressProgramsUpdated=Kasalukuyang ginagawa

# Subtitle for the list of programs for which the applicant has no draft applications
title.activeProgramsUpdated=Hindi sinimulan

# Subtitle for the list of programs for which the applicant has already submitted an application
title.submittedPrograms=Isinumite

# A toast message that displays when an applicant submits an application.
toast.applicationSaved=Successfully saved application: Matagumpay na nai-save ang aplikasyon: ID ng aplikasyon {0}

# A toast message that displays when an applicant tries to apply to a previously completed program.
toast.programCompleted=Nakumpleto na ang aplikasyon.

#------------------------------------------------------------------------#
# APPLICANT APPLICATION REVIEW PAGE - text when reviewing an application #
#------------------------------------------------------------------------#

# Submit application button
button.submit=Isumite

# Continue application button
button.continue=Magsimula dito

# A toast message shown after submitting an application when it's been determined the application is incomplete typically due to external changes.
toast.applicationOutOfDate=Nagkaroon ng update sa application, pakisuri at sagutan ang mga sagot para magpatuloy

# Text shown next to a question indicating that due to it, the application is not eligible to be submitted.
content.notEligible=Hindi Kwalipikado

# Text shown next to a question indicating that due to it, the application does not qualify or is not eligible.
content.doesNotQualify=Hindi kwalipikado

# The text that appears next to an answered question that the applicant can click on to modify the response.
link.edit=I-edit
link.answer=Sagot

# The text that appears next to a question that was answered in another program to let the user know the date it was previously answered on.
content.previouslyAnsweredOn=Sinagutan noong {0}

# An aria-label for screen readers that helps provide context for the associated edit link. The edit link is for a specific
# question, so this might say "Edit What is your name?". The {0} variable is the question text.
ariaLabel.edit=I-edit {0}

# An aria-label for screen readers that helps provide context for the associated edit link. The edit link is for a specific
# question, so this might say "Edit What is your name?". The {0} variable is the question text.
ariaLabel.answer=Sagot {0}

# Program Summary Title
title.programSummary=Buod ng application para sa programa

#------------------------------------------------------------------------#
# APPLICANT ELIGIBILITY - text related to applicant eligibility #
#------------------------------------------------------------------------#

# Title on the page after it has been determined that the applicant is not eligible for a program. This text includes the program name.
title.applicantNotEligible=Batay sa iyong mga tugon, posibleng hindi ka kwalipikado para sa {0}.

# Title on the page after it has been determined that the client is not eligible for a program, when someone else is filling out the application on a client's behalf. This text includes the program name.
title.applicantNotEligibleTi=Batay sa iyong mga tugon, posibleng hindi kwalipikado ang kliyente mo para sa {0}.

# A message that explains the applicant must meet certain program requirements, followed by a list of questions that determine eligibility.
content.mustMeetRequirements=Dapat mong matugunan ang mga kinakailangang ito ng programa:

# A message that explains the client must meet certain program requirements, when someone else is filling out the application on a client's behalf. This is followed by a list of questions that determine eligibility.
content.mustMeetRequirementsTi=Dapat matugunan ng iyong kliyente ang mga kinakailangang ito ng programa:

# Text shown that allows the users to click on a program details link to find out more about the eligibility criteria for the program.
content.eligibilityCriteria=Para sa mga pamantayan sa pagiging kwalipikado, sumangguni sa {0}

# Text shown to explain what the user can do since they are not eligible for the program with their current answers.
content.changeAnswersForEligibility=Puwede kang bumalik sa nakaraang page para i-edit ang iyong mga sagot. O mag-apply sa isa pang programa.

# Button for the applicant to go back and edit their responses.
button.goBackAndEdit=Bumalik at i-edit

# Toast message that shows that a client is likely eligible for a program, when someone else is filling out the application on a client's behalf.
toast.mayQualifyTi=Batay sa iyong mga tugon, posibleng kwalipikado ang iyong kliyente para sa {0}. Para magpatuloy, patuloy na punan ang application.

# Toast message that shows that an applicant is likely eligible for a program, based on their responses.
toast.mayQualify=Batay sa iyong mga tugon, posibleng kwalipikado ka para sa {0}. Para magpatuloy, patuloy na punan ang application.

# Tag on the top of a program card, that lets the applicant know they may qualify for the program, based on their responses in other programs.
tag.mayQualify=Posibleng kwalipikado ka

# Tag on the top of a program card, that lets the person know their client may qualify for the program, based on their responses in other programs. This is in the case when someone is filling out applications on their client's behalf.
tag.mayQualifyTi=Posibleng kwalipikado ang iyong kliyente

# Tag on the top of a program card, that lets the applicant know they are likely not eligible for the program, based on their responses in other programs.
tag.mayNotQualify=Posibleng hindi ka kwalipikado

# Tag on the top of a program card, that lets the person know their client is likely not eligible for the program, based on their responses in other programs. This is in the case when someone is filling out applications on their client's behalf.
tag.mayNotQualifyTi=Posibleng hindi kwalipikado ang iyong kliyente

# Title of page when there is a system outage and eligibility can't be determined for the applicant.
title.cannotCheckEligibility=Sa ngayon, hindi namin masuri ang pagiging kwalipikado mo para sa {0}.

# Title of page when there is a system outage and eligibility can't be determined for the client, when someone is filling out an application on a client's behalf.
title.cannotCheckEligibilityTi=Sa ngayon, hindi namin masuri ang pagiging kwalipikado ng iyong kliyente para sa {0}.

# Content of page when there is a system outage and eligibility can't be determined for the applicant.
content.cannotCheckEligibility=Nagsisikap kami para mabilis na maayos ang isyung ito. Subukan ulit sa ibang pagkakataon.

# Button that allows an applicant to view all programs.
button.viewAllPrograms=Tingnan ang lahat ng programa

# Toast message that shows that an applicant may not be eligible for a program, based on their responses.
toast.mayNotQualify=Batay sa iyong mga tugon, posibleng hindi ka kwalipikado para sa {0}. Kung nagbago ang iyong impormasyon, puwede mong i-edit ang iyong mga sagot at pagkatapos ay magpatuloy sa application.

# Toast message that shows that a client may not be eligible for a program, when someone else is filling out the application on a client's behalf.
toast.mayNotQualifyTi=Batay sa iyong mga tugon, posibleng hindi kwalipikado ang kliyente mo para sa {0}. Kung nagbago ang iyong impormasyon ng kliyente, puwede mong i-edit ang iyong mga sagot at pagkatapos ay magpatuloy sa application.

# Error when there was an exception while submitting the application
banner.errorSavingApplication=Nagka-error sa pag-save ng application

#---------------------------------------------------------------------------------------------#
# APPLICANT APPLICATION CONFIRMATION PAGE - text for a page confirming application submission #
#---------------------------------------------------------------------------------------------#

# Title on the page after the applicant has successfully submitted an application.
title.applicationConfirmation=Pagkumpirma ng aplikasyon

# A confirmation message that shows after an applicant has submitted their application.
content.confirmed=Salamat sa pag-aplay sa {0}.  Ang inyong aplikasyon ay natanggap na, at ang ID ng application ninyo ay {1}.

# Title (not a main page title) on section prompting an applicant to create an account or sign in to save their data.
title.createAnAccount=Gumawa ng account o magsign-in

# A message urging users to create an account to save their data.
content.pleaseCreateAccount=Kung kayo ay magsign-in ngayon sa isang account ng lungsod, ang inyong impormasyon ay maitatabi at maaring balikan anumang oras upang makumpleto ang mga aplikasyon sa hinaharap. Kung kayo ay wala pang account, pahihintulutan kayo ng pahina ng log-in upang makapagrehistro. Hindi kayo mawawalan ng anumang data na inyo nang nailagay.

#----------------------------------------------------------#
# ADDRESS QUESTION - text when viewing an address question #
#----------------------------------------------------------#

# Address question labels - these are shown as the field label before an input box.
label.addressLine2=Unit/Numero ng Apartment
label.city=Lungsod
label.state=Estado
label.selectState=Pumili ng Estado
label.street=Linya ng adres 1
label.zipcode=Zip code

# Validation errors that are shown when a user enters an invalid address.
validation.streetRequired=Maaari lamang na ilagay ang wasto na pangalan ng kalye, at ang numero.
validation.cityRequired=Maaari lamang na ilagay ang Lungsod.
validation.currencyMisformatted=Ang currency ay dapat nasa isa sa mga sumusunod na format: 1000 1,000 1000.30 1,000.30
validation.stateRequired=Maaari lamang na ilagay ang Estado.
validation.invalidZipcode=Maaari lamang na ilagay ang 5-bilang na ZIP code.
validation.noPoBox=Maaari lamang na ilagay ang wasto na adres. Kami ay hindi natanggap ng mga PO Box.

# Title on address correction dialog asking the user to verify their address.
title.verifyAddress=I-verify ang address

# Content on address correction dialog that asks the user to check a similar address.
content.foundSimilarAddress=Hindi namin ma-verify ang ibinigay mong address, pero may nakita kaming katulad nito.

# Text on address correction dialog that shows what address the user had entered.
content.addressEntered=Inilagay na address:

# Text on address correction dialog that shows what address the user had entered, with the address question name.
content.addressEnteredWithQuestionName=Inilagay na {0}:

# Text on dialog that shows a suggestion for the user's correct address when entered incorrectly.
content.suggestedAddress=Iminumungkahing address:

# Text on dialog that shows a suggestions for the user's correct address when entered incorrectly (and there is more than one).
content.suggestedAddresses=Mga iminumungkahing address:

# Button to close the address correction dialog and allow applicant to edit address manually.
button.editAddress=I-edit ang address

# Button for applicant to save and confirm the address suggested in the dialog.
button.saveAndConfirm=I-save at kumpirmahin

# Title on address correction dialog when no address is found.
title.noValidAddress=Walang nakitang valid na address

# Content on address correction dialog when no address is found.
content.noValidAddress=Hindi namin ma-verify ang ibinigay mong address. Paki-edit ang address mo para magpatuloy.

#----------------------------------------------------------------------------------------------------------#
# DROPDOWN QUESTION - text shown when answering a question where a user must select an option from a list. #
#----------------------------------------------------------------------------------------------------------#

# Placeholder text shown in a dropdown selector before the user has made a selection.
placeholder.noDropdownSelection=Pumili ng opsyon:


#----------------------------------------------------------------------------------------------------------#
# DATE QUESTION - text shown when answering a question where a user must select date. #
#----------------------------------------------------------------------------------------------------------#
validation.dateMisformatted=Please enter a date in the format YYYY/MM/DD.

#--------------------------------------------------------------------------------------------------------#
# ENUMERATION QUESTION - text when viewing a question where a user must name entities, such as children. #
#--------------------------------------------------------------------------------------------------------#

# Text on a button an applicant would click to add another entity
button.addEntity=Idagdag {0}

# Text on a button an applicant would click to delete an entity
button.removeEntity=Alisin {0}

# The confirmation dialog when an applicant deletes an enumerator entry.
dialog.confirmDelete=Kayo ba ay sigurado sa nais na alisin {0}? Ang pagbabago na ito ay maitatala matapos mong i-click ang "Next" at lahat ng mga data na nauugnay dito ay mawawala.

# Validation error that shows when an applicant does not enter a value.
validation.entityNameRequired=Maaari lamang ay maglagay ng halaga para sa bawat linya.

# Validation error that shows when an applicant uses a duplicate entity name
validation.duplicateEntityName=Maaari lamang ay maglagay ng natatangi na halaga para sa bawat linya.


# The placeholder text for enumerator fields
placeholder.entityName={0} Palayaw

#---------------------------------------------------------------------------------#
# FILEUPLOAD QUESTION - text when viewing a question where a user uploads a file. #
#---------------------------------------------------------------------------------#

# Validation error that shows when an applicant does not select a file to upload
validation.fileRequired=Maaari lamang ay pumili ng file.

# Button text that prompts the user to select a file to upload.
button.chooseFile=Pumili ng File

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
validation.tooManySelections=Maaari lamang na pumili ng mas-kaunti sa {0}.

#---------------------------------------------------#
# NAME QUESTION - text when viewing a name question #
#---------------------------------------------------#

# Name question labels - these are shown as the field label before an input box.
label.firstName=Pangalan
label.lastName=Apelyido
label.middleName=Gitnang pangalan

# Placeholder text - this is shown inside the input box, before a user enters an answer.
placeholder.firstName=Pangalan
placeholder.lastName=Apelyido
placeholder.middleName=Gitnang pangalan

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

#-------------------------------------------------------#
# 404 PAGE ERROR - error messages displayed on 404 page #
#-------------------------------------------------------#
error.notFoundTitle=Hindi namin nahanap ang page na sinubukan mong puntahan
error.notFoundDescription='Bumalik o pumunta sa '
error.notFoundDescriptionLink=homepage
