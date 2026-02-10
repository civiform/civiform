# Please see the README.md in the same directory for more information on localization.
#
# ALWAYS add context strings to the Transifex dashboard immediately after
# merging changes to this file. See
# https://github.com/civiform/civiform/wiki/Internationalization-(i18n)#internationalization-for-application-strings

#-------------------------------------------------------------#
# GENERAL - contains text that corresponds to multiple views. #
#-------------------------------------------------------------#

# The text on the button an applicant clicks to log out of their session.
button.logout=Выйти
# Message displayed before the support email address in the page footer for applicants.
footer.supportLinkDescription=Служба технической поддержки:
# Text in the footer telling users that this is an official website of the specified Civic entity.
footer.officialWebsiteOf={0} – официальный сайт
# Text on a link that will scroll to the top of the page
footer.returnToTop=Вернуться к началу страницы
# A message in the footer directing users to technical support. The placeholder is a link to
# send an email to CiviForm technical support.
footer.technicalSupport=Служба технической поддержки: {0}
# A message in the footer directing users to technical support. The placeholder is an email.
footer.technicalSupport.v2=Чтобы обратиться в службу технической поддержки, используйте адрес {0}.
# Placeholder message when an applicant clicked the "Continue as Guest".
# This should be consistent with button.guestLogin.
guest=Гость
# Clicking this button shows a dropdown menu to change the display language
button.languages=Выбрать язык
# Label read by screen readers for the language dropdown shown in the page header.
label.languageSr=Выбрать язык
# A label on the button that opens the header menu shown on mobile
header.menu=Меню
# Message for guest users to end their session. Technically this logs out the user from the guest profile, but we use different phrasing in order to not imply that they are currently logged in from a product perspective.
header.endSession=Завершить сеанс
# Message for screen readers as part of aria label to let user know about an informational alert.
heading.informationAriaLabelPrefix=Для справки: {0}
# Message for screen readers as part of the aria label to let user know they completed an action successfully.
heading.successAriaLabelPrefix=Готово: {0}
# Toast message that tells the user their session has ended, to help indicate that they are no longer logged in to an account (but still as a guest).
toast.sessionEnded=Сеанс завершен.
# Message for guest users, to avoid showing "Logged in as Guest" when they are really not logged in.
header.guestIndicator=Вы используете сайт в качестве гостя.
# Message with user's name to show who you are logged in as.
header.userName=Вы вошли в аккаунт: {0}
# Error message to answer a required question
validation.isRequired=Ответ на этот вопрос обязателен.
# Error message to fill a required field
validation.fieldIsRequired=Это поле является обязательным.
# Validation error that is shown when the user input couldn't be converted for any reason.
validation.invalidInput=Введите действительные данные.
content.requiredFieldsNote=Примечание. Поля, отмеченные знаком "{0}", должны быть заполнены.
# Disclaimer at top of form explaining that asterisks mark required questions. The placeholder is the red, bold asterisk used throughout the page.
content.requiredFieldsNoteNorthStar=Обязательные поля отмечены звездочкой ({0}).
content.optional=(необязательно)
toast.errorMessageOutline=Ошибка: {0}.
# Description for an "X" button that will close a dialog, modal, or page.
button.close=Закрыть
# Button text that will navigate the applicant to a page where they can review previous answers
button.review=Проверить ответы
# Button text that will go back to the previous page.
button.goBack=Назад
# Indicator for screen readers that a link will open in a new tab. Meant to be used with aria label text eg. "Program details, opens in a new tab"
link.opensNewTabSr=страница откроется в новой вкладке
# Aria-label for the primary navigation
label.primaryNavigation=Основное меню навигации
# Aria-label for agency identifier
label.agencyIdentifier=Идентификатор агентства
# Aria-label for guest session alert
label.guestSessionAlert=Предупреждение о том, что запущен гостевой сеанс
# Link to skip to the main content of the page
link.skipToMainContent=Перейти к основному контенту

#-------------------------------------------------------------#
# LOGIN - contains text that for login page.                  #
#-------------------------------------------------------------#

# The text on the button an applicant clicks to create an account.
button.createAnAccount=Создать аккаунт
# The text on the button in the login drop down an applicant clicks to log in to their session.
button.applicantLogin=Вход заявителя
# The text on the button in the login drop down an admin clicks to log in to their session.
button.adminLogin=Вход администратора
# The text on the button an applicant clicks to log in to their session.
button.login=Войти
# The text on the button an applicant clicks to sign in to their session.
button.signIn=Войти
# The text between creating a new account, and becoming a guest
content.or=или
# The text on the button for applicants to create a new account.
button.createAccount=Создать аккаунт
# Asking whether the user is an administrator for the program
content.adminFooterPrompt=Вы администратор?
# The text on the anchor for admins to log in to their session.
link.adminLogin=Войти как администратор

#--------------------------------------------------------------------------#
# PROGRAM FORM - contains text shown when filling out an application form. #
#--------------------------------------------------------------------------#

# Hint placed above file input for questions which accept a single file.
input.singleFileUploadHint=Выберите файл
# Hint placed above file input for questions which accept multiple, but not unlimited, files. {0} contains a number,
# 2 or greater, representing the maximum number of files.
input.multipleFileUploadHint=Выберите один или несколько файлов (не более {0})
# Hint placed above file inputs which accept any number of files.
input.unlimitedFileUploadHint=Выберите один или несколько файлов
# The button text for saving the current applicant-entered data and continuing to the next screen in the form.
button.nextScreen=Сохранить и продолжить
# The button text for navigating to the previous screen in the form.
button.previousScreen=Перейти к предыдущему экрану
# The label on a button that will navigate the user to the previous section of the form.
button.back=Назад
# The label on a button that will save user answers and navigate to the summary of the application.
button.reviewAndExit=Проверить и подать заявку
# The current screen the user is on ({0}) out of the number of total screens in the application.
content.blockProgress={0} из {1}
# An aria-label on a progress bar showing user how much of the application they have completed.
content.blockProgressLabel=Прогресс заполнения заявки
# Heading on the application review page. The page shows all the answers the user inputted.
heading.reviewAndSubmit=Проверка и отправка заявки
# A toast message that displays when a program is not fully localized to the applicant's preferred locale.
toast.localeNotSupported=К сожалению, эта программа не полностью переведена на выбранный вами язык.
# A link that appears next to the create account button that offers applicants the option to not create an account right now.
link.applyToAnotherProgram=Подать заявку на участие в другой программе
# Displayed in breadcrumbs to link back to the main entry point of the applicant's portal. In effect a shortening of "Homepage".
link.home=Главная
# Displayed in breadcrumbs to indicate this is an application of the supplied program name.
link.applicationForProgram=Заявка: "{0}"
# Anchor which when clicked removes a file the user has previously uploaded.
link.removeFile=Удалить файл
# Screen reader text for a link which when clicked removes a file the user has previously uploaded. {0} is the file name.
link.removeFileSr=Удалить файл "{0}"
# Title of a pop-up informing the user that there were errors in the information they inputted. (new version for North Star)
modal.errorSaving.title=Исправьте ошибки в ответах на этой странице
# Text of a pop-up informing the user that continuing to the review page will lose the information inputted since it has errors unless they are corrected. (new version for North Star)
modal.errorSaving.content.review=На этой странице есть ошибки в ответах или незаполненные поля. Если вы перейдете к проверке заявки, введенная здесь информация будет потеряна. Чтобы ответы сохранились, исправьте ошибки.
# Text of a pop-up informing the user that continuing to the previous page will lose the information inputted since it has errors unless they are corrected. (new version for North Star)
modal.errorSaving.content.previous=На этой странице есть ошибки в ответах или незаполненные поля. Если вы вернетесь к предыдущему экрану, введенная здесь информация будет потеряна. Чтобы ответы сохранились, исправьте ошибки.
# Text of button that discards all the information the user has inputted and navigates to the application review page. (new version for North Star)
modal.errorSaving.continueButton.review=Не сохранять и перейти к проверке
# Text of button that discards all the information the user has inputted and navigates to the application preview page. (new version for North Star)
modal.errorSaving.continueButton.previous=Не сохранять и вернуться
# Text of button that shows the previous inputted information and asks to fix the errors with it. (new version for North Star)
modal.errorSaving.fixButton=Исправить ошибки на этой странице

#----------------------------------------------------------------------------#
# APPLICANT HOME PAGE - contains text specific to the applicant's home page. #
#----------------------------------------------------------------------------#

# The text on the button an applicant clicks to apply to a specific program.
button.apply=Подать заявку
# The text on the button an applicant clicks to edit the application for a specific program.
button.edit=Изменить
button.startSurvey=Пройти опрос
# The text on a button to view and apply to a program. Clicking the button leads to the program overview page.
button.viewAndApply=Узнать больше и подать заявку
# The screen reader text on a button to view and apply to a program. The variable represents the program name.
button.viewAndApplySr=Узнать больше и подать заявку на участие в программе "{0}"
# The text on a button to view program on a new tab. This is used for external programs.
button.viewInNewTab=Открыть в новой вкладке
# The screen reader text on a button to view program on a new tab. The variable represents the program name. This is used for external programs.
button.viewInNewTabSr=Открыть "{0}" в новой вкладке
# The text for the button that allows a guest to bypass the login prompt modal.
button.continueToApplication=Заполнить заявку
# Text for applicants to understand the section is for finding programs
content.findPrograms=Находите программы
# Main home page heading, updated to be more generic
heading.homepage.v2=Подавайте онлайн-заявки на участие в государственных программах
# Main home page intro text
content.homepageIntro=Если вы нуждаетесь в помощи, например с питанием, передвижением, уходом за детьми или оплатой коммунальных услуг, выберите нужный вариант ниже. За раз можно подать заявку только в одной категории.
# Main home page intro text, updated to be more generic
content.homepageIntro.v2=Узнайте, какие услуги могут быть вам доступны. Вам не нужно каждый раз вводить свои данные.
# The label for the program filter buttons
label.programFilters=Фильтры по категориям программ
# The label for the program filter checkboxes
label.programFilters.v2=С чем вам нужна помощь?
# The button to apply program filters
button.applySelections=Применить фильтры
# The button to clear program filter selections
button.clearSelections=Удалить фильтры
# Link text to read more about a program.
link.programDetails=Сведения о программе
# The same text, read for screen readers.
link.programDetailsSr=Сведения о программе "{0}", страница открывается в новой вкладке
# The title of the page for the list of programs.
title.programs=Программы и услуги
# The title for the section in the list of programs that contains the pre-screener form and should be started first.
title.getStartedSection=С чего начать
# Title of the section on the home page that shows programs for which the applicant has already started or submitted applications.
title.myApplicationsSection=Мои заявки ({0})
# Title of the section on the home page that shows programs for which the applicant has already started or submitted applications.
title.myApplicationsSection.v2=Мои заявки
# Title of the section on the home page that shows any programs that don't match the selected filters, with the number of programs in parentheses.
title.otherProgramsSection.v2=Другие программы и услуги ({0})
# Title of the section on the home page that shows all programs when no filter is selected, with the number of programs in parentheses.
title.programsSection.v2=Программы и услуги ({0})
# Title of the section on the home page that shows any available programs that have not yet been applied to.
title.availableProgramsSection=Программы и услуги
# Title of the section on the home page that shows programs that match any of the selected filters.
title.recommendedSection.v2=Программы по фильтрам ({0})
# Informational tag on an in-progress application card
label.inProgress=Заявка не подана
# Informational tag on a submitted application card. Used when the date of submission is unknown.
label.submitted=Заявка подана
# Informational tag on a submitted application card. The parameter is the date of the submission.
label.submittedOn=Заявка подана {0}
# Informational tag on a submitted application card. The first paramater is the status applied to an application. The second parameter is the date the status was applied.
label.statusOn={0} {1}
# Title of the external program modal that opens when a user clicks on an external program card.
title.externalProgramModal=Страница откроется на другом сайте
# Content of the external program modal that opens when a user clicks on an external program card.
content.externalProgramModal=Чтобы перейти на сайт программы, где вы сможете узнать о ней больше и подать заявку, нажмите "Продолжить".

#------------------------------------------------------------------------------------------------------#
# TRUSTED INTERMEDIARY DASHBOARD PAGE - text when adding, editing, deleting, or searching for a client #
#------------------------------------------------------------------------------------------------------#

# Message when information about a client, like their name or contact information is updated.
banner.clientInfoUpdated=Информация клиента обновлена.
# Message when a user successfully creates a client account.
banner.newClientCreated=Аккаунт клиента создан
# Banner at the top of the page with the name of the client substituted in when viewing an application.
banner.viewApplication=Вы подаете заявку от имени клиента {0}. Хотите выбрать другого клиента?


# Button that allows a user to add a client to the CiviForm system.
button.addNewClient=Добавить клиента
# Button that brings the user back to the list of their clients.
button.backToClientList=Вернуться к списку клиентов
# Button that allows the user to cancel the progress they make on adding or editing a client.
button.cancel=Отмена
# Button that allows the user to clear any search parameters they have already entered.
button.clearSearch=Очистить параметры поиска
# Button to navigate to the next page.
button.nextPage=Далее
# Button to save information that has been entered.
button.save=Сохранить
# Button to execute a search to filter a client list.
button.search=Искать
# Button to start an application on behalf of a client.
button.startApp=Заполнить заявку
# Button to redirect to the login page when the applicant is not logged in for login-only programs.
button.startAppForLoginOnlyProgram=Войдите в систему, чтобы подать заявку
# Button to view the applications belonging to an applicant.
button.viewApplications=Посмотреть заявки
# Button to view and add clients
button.viewAndAddClients=Посмотреть и добавить клиентов

# Part of the confirmation banner which appears after a new client has been added to a community-based organization.
content.clientCreated=Пользователь {0} добавлен как ваш клиент. Теперь вы можете подавать заявки от его имени.
# The tooltip of the email input field.
content.emailTooltip=Укажите электронный адрес, на который клиент будет получать автоматические уведомления об изменении статуса своей заявки. Если не предоставить электронный адрес, вам или вашей общественной организации потребуется сообщать клиенту об обновлениях самостоятельно.
# For each client on the client list, shows the number of applications that have been submitted for them.
content.numberOfAppSubmitted=Отправлено заявок: {0}
# Shows that a single application has been submitted on behalf of the client.
content.oneAppSubmitted=Отправлено заявок: 1

# Header for the page with the Organization Members table.
header.acctSettings=Настройки аккаунта
# Tab label for getting to the client list page
header.clientList=Список клиентов
# Label for search
header.search=Поиск

# A column header in the Organization Members table. Entries may be "Not yet logged in" or "Last submitted application on MM-DD-YYYY".
label.acctStatus=Статус аккаунта
# Label above the phone number for each client on the client list.
label.contactInfo=Контактная информация
# The label above the input field for the day of the client birth date. i.e. 12 for November 12
label.day=День
# Label above the birth date input field when creating or editing a client
label.dob=Дата рождения
# Example above the 3 birth date input fields (Month, Day, Year).
label.dobExample=Например: январь, 28, 1986
# Label above the "Email" column of the community-based organization members table.
label.email=Электронный адрес
# Validation error message below date of birth input when the TI clicks on save without filling out the field.
label.errorDOB=Укажите дату рождения.
# Validation error message below first name input when the TI clicks on save without filling out the field.
label.errorFirstName=Укажите имя.
# Validation error message below last name input when the TI clicks on save without filling out the field.
label.errorLastName=Укажите фамилию.
# Validation error message below email input when the TI tries to create a client with an email that is already in use.
label.errorEmailInUseForClientCreate=Этот электронный адрес уже используется. Нельзя создать заявителя, если его данные зарегистрированы в другом аккаунте.
# Validation error message below email input when the TI tries to edit a client with an email that is already in use.
label.errorEmailInUseForClientEdit=Этот электронный адрес уже используется. Нельзя указывать данные, которые уже зарегистрированы в другом аккаунте.
# The label above the dropdown select field for the month of the client birth date.
label.month=Месяц
# Label above the "Name" column of the community-based organization members table. Each entry will be listed as "Last name, First name".
label.name=Имя
# Example above the name input field for the client search.
label.nameExample=Например: Петров, или Алексей, или Алексей Петров
# Label above the text input field for notes about a client.
label.notes=Примечания
# Label above the phone number input field when creating or editing a client.
label.phoneNum=Номер телефона
# The label above the Date of Birth search input for the client search.
label.searchByDob=Поиск по дате рождения
# The label above the name search field of the client search.
label.searchByName=Поиск по имени
# The label above the input field for the year of the client birth date.
label.year=Год

# Link to change the client that the trusted intermediary is applying for.
link.selectNewClient=Выбрать

# This is the header above the list of clients and the client search.
title.allClients=Все клиенты
# This is the heading above a new client creation form.
title.createClient=Создать клиента
# This is the heading above the client editing form.
title.editClient=Изменить данные клиента
# This is the heading above a filtered list of clients when the filter has narrowed the search down to a certain number of clients.
title.displayingMultiClients=Найдено клиентов: {0}
# This is the heading above a filtered list of clients when the filter has narrowed the search down to a single client.
title.displayingOneClient=Найден 1 клиент
# This is the heading above an unfiltered list of clients.
title.displayingAllClients=Показаны все клиенты
# This is the heading above a table that contains the members of the current community-based organization.
title.orgMembers=Участники организации
# HTML document title for the trusted intermediary dashboard page
title.tiDashboard=Надежный посредник – Управление клиентами – CiviForm
# HTML document title for the trusted intermediary account settings page
title.tiAccountSettings=Надежный посредник – Просмотр настроек аккаунта – CiviForm

#------------------------------------------------------------------------#
# APPLICANT APPLICATION REVIEW PAGE - text when reviewing an application #
#------------------------------------------------------------------------#

# Submit application button
button.submit=Отправить
# Submit application button
button.submitApplication=Подать заявку
# Continue application button
button.continue=Продолжить
# Button text on a button that will allow the user to start filling out a section of questions.
button.start=Начать
# A toast message shown after submitting an application when it's been determined the application is incomplete typically due to external changes.
toast.applicationOutOfDate=Форма заявки изменилась. Чтобы продолжить, проверьте заполненные данные и ответьте на все вопросы.

# Text shown next to a question indicating that due to it, the application does not qualify or is not eligible.
content.doesNotQualify=Указанные данные не отвечают требованиям программы
# The text that appears next to an answered question that the applicant can click on to modify the response.
link.edit=Изменить
link.answer=Ответить
# The text that appears next to a question that was answered in another program to let the user know the date it was previously answered on.
content.previouslyAnsweredOn=Вы отвечали на этот вопрос {0}
# An aria-label for screen readers that helps provide context for the associated edit link. The edit link is for a specific
# question, so this might say "Answer What is your name?". The {0} variable is the question text.
ariaLabel.answer=Ответить на вопрос "{0}"

# An aria-label for screen readers that helps provide context for the associated edit link. The edit link is for a specific
# question, so this might say "Edit What is your name?". The {0} variable is the question text.
ariaLabel.edit=Изменить ответ на вопрос "{0}"
# Title for the summary of the pre-screener page
title.preScreenerSummary=Сводные данные об этой форме
# Heading content at the top of the review page, where applicants can start answering questions.
title.getStarted=Давайте начнем
title.programSummary=Сводные данные о заявке на участие в программе

#----------------------------------------------------------------------------#
# PROGRAM OVERVIEW PAGE - contains text specific to the program overview page. #
#----------------------------------------------------------------------------#

# The HTML page title for the program overview page.  The {0} is the program name.
title.programOverview={0} – Обзор программы
# Heading for the section of the program overview page that contains the application steps.
heading.applicationSteps=Как подать заявку
# Top-level heading for the program overview page.  The {0} is the program name.
heading.programOverview=Как подать заявку на участие в программе "{0}"
# The text on the button that takes users to the account creation form.
link.createAccountFromOverview=Создать аккаунт для подачи заявки
# The link that directs users to the start of the application without creating an account.
link.startAsGuest=Подать заявку в качестве гостя

#------------------------------------------------------------------------#
# APPLICANT ELIGIBILITY - text related to applicant eligibility #
#------------------------------------------------------------------------#

# Tab title for ineligible page
title.ineligible=Вы не подпадаете под условия программы
# Section heading
heading.eligibilityCriteria=Подробнее о критериях допуска
# Text shown that allows the users to click on a program details link to find out more about the eligibility criteria for the program.
content.eligibilityCriteria=Чтобы ознакомиться с требованиями, перейдите на страницу {0}
content.eligibilityCriteria.v3=Чтобы узнать больше о критериях допуска к этой программе или связаться с ее кураторами, перейдите на страницу {0} (откроется в новой вкладке).
# Text shown to explain what the user can do since they are not eligible for the program with their current answers.
content.changeAnswersForEligibility=Вы можете вернуться на предыдущую страницу, чтобы изменить предоставленные сведения, или подать заявку на участие в другой программе.
# Text shown on a webpage when the applicant is ineligible for a program.
content.changeAnswersForEligibility.v2=Если вы считаете, что произошла ошибка, вернитесь на предыдущую страницу и измените предоставленные сведения. Вы также можете перейти на главную страницу, чтобы подать заявку на участие в другой программе.
# Button for the applicant to go back and edit their responses.
button.goBackAndEdit=Вернуться и изменить сведения
# Clicking this button returns the user to the program summary page, where they can edit their responses.
button.editMyResponses=Изменить мои ответы
# Tag on the top of a program card, that lets the applicant know they may qualify for the program, based on their responses in other programs.
tag.mayQualify=Похоже, вы соответствуете условиям
# Tag on the top of a program card, that lets the person know their client may qualify for the program, based on their responses in other programs. This is in the case when someone is filling out applications on their client''s behalf.
tag.mayQualifyTi=Похоже, ваш клиент соответствует условиям
# Tag on the top of a program card, that lets the applicant know they are likely not eligible for the program, based on their responses in other programs.
tag.mayNotQualify=Похоже, вы не соответствуете условиям
# Tag on the top of a program card, that lets the person know their client is likely not eligible for the program, based on their responses in other programs. This is in the case when someone is filling out applications on their client''s behalf.
tag.mayNotQualifyTi=Похоже, ваш клиент не соответствует условиям

# Error when there was an exception while submitting the application
banner.errorSavingApplication=При сохранении заявки возникла ошибка.

# Alert title when applicant may be eligible for a program
alert.eligibility_applicant_eligible_title=Возможно, эта программа вам доступна
# Alert text when applicant may be eligible for a program
alert.eligibility_applicant_eligible_text=Судя по ответам, вы подходите под требования этой программы.

# Alert title when applicant may not be eligible for a program
alert.eligibility_applicant_not_eligible_title=Эта программа вам недоступна
# Alert text when applicant may not be eligible for a program
alert.eligibility_applicant_not_eligible_text=Судя по ответам, вы не подходите под требования этой программы. Если сведения изменились, обновите форму и продолжите подачу заявки.
# Alert text when applicant may not be eligible for a program (new version for North Star)
alert.eligibilityApplicantNotEligibleTextShort=Судя по ответам на следующие вопросы, вы не подходите под требования этой программы.

# Alert title when applicant may be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_applicant_fastforwarded_eligible_title=Возможно, эта программа вам доступна
# Alert text when applicant may be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_applicant_fastforwarded_eligible_text=В форме заявки могут быть новые или измененные вопросы, влияющие на допуск к участию в программе или на ваши ответы. Проверьте всю информацию ниже.

# Alert title when applicant may not be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_applicant_fastforwarded_not_eligible_title=Возможно, эта программа теперь вам недоступна
# Alert text when applicant may not be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_applicant_fastforwarded_not_eligible_text=В форме заявки могут быть новые или измененные вопросы, влияющие на допуск к участию в программе или на ваши ответы. Проверьте всю информацию ниже.

# Alert title when TI client may be eligible for a program
alert.eligibility_ti_eligible_title=Возможно, эта программа доступна вашему клиенту
# Alert text when TI client may be eligible for a program
alert.eligibility_ti_eligible_text=Судя по ответам, ваш клиент подходит под требования этой программы.

# Alert title when TI client may not be eligible for a program
alert.eligibility_ti_not_eligible_title=Эта программа недоступна вашему клиенту
# Alert text when TI client may not be eligible for a program
alert.eligibility_ti_not_eligible_text=Судя по ответам, ваш клиент не подходит под требования этой программы. Если его сведения изменились, обновите форму и продолжите подачу заявки.
# Alert text when TI client may not be eligible for a program (new version for North Star)
alert.eligibilityTiNotEligibleTextShort=Судя по ответам на следующие вопросы, ваш клиент не подходит под требования этой программы.

# Alert title when TI client may be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_ti_fastforwarded_eligible_title=Возможно, эта программа доступна вашему клиенту
# Alert text when TI client may be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_ti_fastforwarded_eligible_text=В форме заявки могут быть новые или измененные вопросы, влияющие на ответы клиента или его допуск к участию в программе. Проверьте всю информацию ниже.

# Alert title when TI client may not be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_ti_fastforwarded_not_eligible_title=Возможно, эта программа теперь недоступна вашему клиенту
# Alert text when TI client may not be eligible for a program after the application gets updated to using a newer program version
alert.eligibility_ti_fastforwarded_not_eligible_text=В форме заявки могут быть новые или измененные вопросы, влияющие на ответы клиента или его допуск к участию в программе. Проверьте всю информацию ниже.

# Alert on the program overview page letting the user know that they will likely be eligible for the program.
alert.likelyEligible=Судя по ответам в другой заявке, вы подходите под требования этой программы.
# Alert on the program overview page letting the user know that they will likely NOT be eligible for the program.
alert.likelyIneligible=Судя по ответам в другой заявке, эта программа может вам не подходить.
# Alert on the program overview page letting a trusted intermediary know that their client will likely be eligible for the program.
alert.clientLikelyEligible=Судя по ответам в другой заявке, эта программа может подходить вашему клиенту.
# Alert on the program overview page letting a trusted intermediary know that their client will likely NOT be eligible for the program.
alert.clientLikelyIneligible=Судя по ответам в другой заявке, эта программа может не подходить вашему клиенту.

#---------------------------------------------------------------------------------------------#
# APPLICANT APPLICATION CONFIRMATION PAGE - text for a page confirming application submission #
#---------------------------------------------------------------------------------------------#

# Title on the page after the applicant has successfully submitted an application.
title.applicationConfirmation=Подтверждение заявки
# A confirmation message that shows after an applicant has submitted their application.
content.confirmed=Благодарим вас! Мы получили вашу заявку на участие в программе "{0}"  и присвоили ей идентификатор {1}.
# Title (not a main page title) on section prompting an applicant to create an account or sign in to save their data.
title.createAnAccount=Зарегистрируйтесь или войдите в аккаунт
# The title of a pop-up informing an applicant to sign in before continuing with the application
title.signInModal=Войдите в аккаунт
# The text of a pop-up informing an applicant to sign in so they can access the application later before continuing with the application
content.signInModal=Создав аккаунт, вы сможете проверять статус заявки, изменять ее и быстро подавать новые.

# A message to show on the login prompt modal that encourages users to log in before applying to a program from the programs index page.
content.initialLoginModalPrompt=Прежде чем продолжить, войдите в свой аккаунт, зарегистрированный в сервисе "{0}". Тогда вам не придется повторно вводить свою информацию при подаче нескольких заявок. Вы также сможете изменять заполненные заявки и проверять их статус. Если у вас нет аккаунта, вы можете его создать.
# A button for continuing to apply to other programs without an account.
button.continueWithoutAnAccount=Продолжить без входа в аккаунт
title.preScreenerConfirmation=Программы, которые могут быть вам доступны
# Title on the page after a trusted intermediary has successfully filled out the pre-screener form.
title.preScreenerConfirmationTi=Программы, которые могут быть доступны вашему клиенту
# A message explaining that the applicant may be eligible for the following list of programs, and that they need to apply to them.
content.preScreenerConfirmation=Возможно, вы соответствуете критериям допуска к этим программам. Чтобы подать заявки, нажмите "Зарегистрироваться в программах" и заполните онлайн-формы.
# A message explaining that the applicant may be eligible for the following list of programs.
content.preScreenerConfirmation.v2=На основе ваших ответов мы подобрали программы, которые могут быть вам доступны:
# A message explaining that the trusted intermediary's client may be eligible for the following list of programs, and that they need to apply to them.
content.preScreenerConfirmationTi=Возможно, ваш клиент соответствует критериям допуска к этим программам. Чтобы подать заявки, нажмите "Зарегистрироваться в программах" и заполните онлайн-формы.
# A message explaining that the trusted intermediary's client may be eligible for the following list of programs.
content.preScreenerConfirmationTi.v2=На основе ваших ответов мы подобрали программы, которые могут быть доступны вашему клиенту:
# A message explaining that there were no programs the applicant is currently eligible for. The {0} parameter is a link to another website, where the text is the name of that site. It may read "Access Arkansas", for example.
content.preScreenerNoMatchingPrograms=Функция предварительного подбора не обнаружила программ, которые сейчас могут быть вам доступны. Однако вы в любое время можете подавать заявки, нажав "Зарегистрироваться в программах". Чтобы посмотреть дополнительные программы, перейдите на сайт {0}.
# A message explaining that there were no programs the trusted intermediary's client is currently eligible for. The {0} parameter is a link to another website, where the text is the name of that site. It may read "Access Arkansas", for example.
content.preScreenerNoMatchingProgramsTi=Функция предварительного подбора не обнаружила программ, которые сейчас могут быть доступны вашему клиенту. Однако вы в любое время можете подавать заявки, нажав "Зарегистрироваться в программах". Чтобы посмотреть дополнительные программы, перейдите на сайт {0}.
# A header above a list of other programs the applicant might be interested in applying to.
content.otherProgramsToApplyFor=Другие программы, которые могут быть вам интересны
# Button on the "Application Submitted" page. Clicking it downloads the user's application.
button.downloadApplication=Скачать заявку
# A button prompting users to apply to programs.
button.applyToPrograms=Зарегистрироваться в программах
# Heading above a section showing the user's name, confirmation number, and date
heading.yourSubmissionInformation=Данные вашей заявки
# Text above the name of the person who submitted this application
heading.applicantName=Отправитель заявки
# Text above the confirmation number of the application
heading.confirmationNumber=Номер подтверждения
# Text above the date submitted of the application
heading.dateSubmitted=Дата отправки
# Text above a message telling the user they can print this page
heading.forYourRecords=Копия подтверждения для личного хранения
# Text above a button to print this page
content.youCanPrint=Вы можете распечатать подтверждение, чтобы оно было у вас под рукой.
# Heading above a paragraph explaining the next steps in the application process
heading.nextSteps=Что мне нужно делать дальше?
# Confirmation "alert" message that appears next to a checkmark icon. {0} is the program name, such as "Housing Assistance Program"
alert.submitted=Вы отправили заявку на участие в программе "{0}"
#  Information "alert"  title for programs that require an account to apply
alert.createAccountForLoginOnly=Чтобы получить доступ к вашей заявке позже, необходимо создать учётную запись
# Informational "alert" title to encourage the user to create an account
alert.createAccount=Чтобы вернуться к заявке позже, создайте аккаунт
# Description text in an alert that explains how creating an account can help the user
alert.createAccountDescription=Сохранив информацию в аккаунте, вы сможете проверять статус заявки и быстро подавать новые.
# Informational alert to let the user know there are no additonal programs for them to start an application for at this time.
alert.noProgramsAvailable=Вы создали или подали заявки на участие во всех программах, доступных в настоящее время.
# Hyperlink to log in to an existing account
content.loginToExistingAccount=Войти с существующим аккаунтом
# Informational "alert" title to push user to login as the program is only for logged in user
alert.loginOnly=Чтобы подать заявку на участие в этой программе, необходимо войти в систему
# Description text in an alert that explains why the user must log in to apply for the program
alert.loginOnlyDescription=Войдите в систему или создайте учетную запись, чтобы продолжить работу с этим приложением.

#-------------------------------------------------------------------------------------------------------------------------#
# APPLICANT DUPLICATE SUBMISSION PAGE/MODAL - text for a page informing the applicant of a duplicate submission and redirecting #
#-------------------------------------------------------------------------------------------------------------------------#

title.noChangesToSave=В заявке для программы "{0}" нет изменений, которые можно сохранить
content.noChanges=Вы уже подали заявку на участие в этой программе. Сейчас в форме нет изменений. Хотите продолжить редактирование?
button.continueEditing=Продолжить
button.exitApplication=Закрыть форму заявки


#--------------------------------------------------------------------------------------------------------------#
# Disabled program error page - error info page when user tries to access a disabled program via its deep link #
#--------------------------------------------------------------------------------------------------------------#

# Title of the page stating the program is disabled
title.programNotAvailable=Эта программа больше не доступна
# Info stating program disabled and see home page for other options.
content.disabledProgramInfo=К сожалению, программа, которую вы пытаетесь открыть, уже не активна. Чтобы найти другие программы, перейдите на главную страницу.
# Button text on a button that takes the user back to the home page.
button.homePage=Вернуться на главную страницу

#--------------------------------------------------------------------------------------------------------------#
# ADMIN PREDICATE EDIT - admin-facing text for editing eligibility / visibility predicates in draft programs #
#--------------------------------------------------------------------------------------------------------------#

# Navigation link to go back to editing the program screen. {0} is the screen title, such as "Your Household"
link.backToEditProgramBlock=Вернуться к редактированию {0}

# Long form description for configuring a visibility condition and how it can be used to show or hide a screen for applicants filling out an application form with multiple screens.
content.predicateVisibilityDescription=Настройте, когда этот экран будет отображаться или скрываться для заявителей на основе ответов на вопросы на предыдущих экранах.
# Long form description for configuring an eligibility condition and how it can be used to determine if an applicant qualifies for the program. This text is followed by a link to edit the program, the full sentence reads "You can change this in the program settings."
content.predicateEligibilityDescription=Добавьте условия участия, чтобы определить, соответствует ли заявитель требованиям программы на основе ответов на вопросы на этом экране. Заявители, не соответствующие минимальным требованиям, не смогут подать заявку. Это можно изменить в
# Long form description for configuring an eligibility condition and how it can be used to determine if an applicant qualitifies for the program.
content.predicateEligibilityDescription.v2=Добавьте условия отбора для определения степени соответствия кандидата программе на базе ответов с этого экрана.
# Text informing admins that ineligible applicants will be blocked from submitting an application. This text is followed by a link to edit the program, the full sentence reads "You can change this in the program settings."
content.blockIneligibleApplicants=Кандидаты, не соответствующие минимальным требованиям, не смогут отправить заявку. Это можно изменить в
# Text informing admins that ineligible applicants will still be able to submit an application. This text is followed by a link to edit the program, the full sentence reads "You can change this in the program settings."
content.allowIneligibleApplicants=Кандидат может подать заявку, даже если не соответствует минимальным требованиям. Это можно изменить в
# Link to edit the program. This text is appended to the long form description for eligibility conditions. The full sentence reads "You can change this in the program settings."
link.programSettings=настройках программы

# Text that precedes select option dropdowns for a form to configure screen visibility conditions. The full sentence reads "This screen is shown if any/all conditions are true:"
content.predicateScreenIs=Экран
# Text that precedes a select option dropdown for a form to configure applicant eligibility conditions. The full sentence reads "Applicant is eligible if any/all conditions are true:"
content.predicateApplicantIsEligible=Заявитель соответствует требованиям, если
# Text that is appended to the end of a header describing a visibility or eligibility condition and before a form to configure those conditions. The full sentence reads "This screen is shown if any/all conditions are true:"
content.predicateConditionsAreTrue=выполняются следующие условия:
# Text that shows on the eligibility predicate screen when no eligibility conditions are set.
content.predicateEligibilityNullState=Подходит любой кандидат.
# Text that shows on the visibility predicate screen when no visibility conditions are set.
content.predicateVisibilityNullState=Этот экран отображается всегда.

# Text that precedes a select option dropdown for a form to configure a single condition within a visibility or eligibility predicate and before a form to configure sub-conditions. The full sentence reads "Condition is true if any/all sub-conditions are true:"
content.predicateConditionIsTrueIf=Условие выполняется, если
# Text that is appended to the end of a header describing a single condition within a visibility or eligibility predicate and before a form to configure sub-conditions. The full sentence reads "Condition is true if any/all sub-conditions are true:"
content.predicateSubconditionsAreTrue=выполняются подусловия:

# Label for select option dropdown to choose a question
label.predicateQuestion=Вопрос
# Label for a select option dropdown to choose a field to use within a question, such as "first name", "email", "date", etc.
label.predicateField=Поле
# Label for a select option dropdown to choose a state for a logical condition, such as "is equal to", "is one of", "is later than", etc.
label.predicateState=Состояние
# Label for an input field to enter a value or multiple values.
label.predicateValue=Значение(я)
# Placeholder text for select option dropdown
option.selectPlaceholder=- Выберите -
# Hint text for providing multiple values in a single input field
content.multipleValuesInputHint=Введите список значений, разделённых запятыми. Например, "item1,item2,item3".
# Text between two input fields that represent a range. For example, "between input1 and input2".
content.and=и

# Button to append form fields for configuring a new condition
button.predicateAddCondition=Добавить условие
# Button to remove a condition from the form
button.predicateDeleteCondition=Удалить условие
# Button to remove all conditions from the form
button.predicateDeleteAllConditions=Удалить все условия
# Link to append form fields for configuring a new sub-condition within a condition
link.predicateAddSubcondition=Добавить подусловие
# Link to remove a sub-condition from the form
link.predicateDeleteSubcondition=Удалить подусловие
# Link to go back to the top of the page
link.backToTop=В начало страницы
# Button to save the visibility or eligibility predicate and return to editing the program.
button.saveAndExit=Сохранить и выйти

# Confirmation dialog shown to admin for deleting all conditions in the predicate edit view
confirm.deleteAllConditions=Вы уверены, что хотите удалить все условия?
# Confirmation dialog shown to admin for deleting a condition in the predicate edit view
confirm.deleteSingleCondition=Вы уверены, что хотите удалить это условие?
# Confirmation dialog shown to admin for leaving predicate edit without saving.
confirm.leaveWithoutSaving=Вы уверены, что хотите выйти?
# Dialog to warn admin that a deletion action cannot be undone
confirm.actionNotReversable=Это действие нельзя отменить.
# Dialog to warn admin that they have unsaved changes in the predicate edit view.
confirm.unsavedChanges=Несохраненные изменения будут потеряны.

# Alert shown to notify admin that a visibility condition cannot be created because there are no available questions to use.
alert.predicateNoAvailableQuestionsVisibility=На предыдущих экранах нет доступных вопросов, с помощью которых можно задать условие видимости для этого экрана.
# Alert shown to notify admin that an eligibility condition cannot be created because there are no available questions to use.
alert.predicateNoAvailableQuestionsEligibility=На этом экране нет доступных вопросов, с помощью которых можно задать условие соответствия требованиям.

# Label for textarea where admins can enter a message to show to applicants who are deemed ineligible based on the eligibility conditions set.
label.predicateEligibilityMessageField=Сообщение, показывающееся неподходящим кандидатам
# Help text for a textarea input explaining that markdown is supported.
content.markdownSupported=Поддерживается Markdown

# Error message for when the admin leaves a multi-select question blank in admin predicate edit.
validation.selectionRequired=Нужно выбрать хотя бы одну опцию.
# Error message for when the admin leaves a question unselected in admin predicate edit.
validation.questionRequired=Требуется выбрать вопрос.

#----------------------------------------------------------#
# ADDRESS QUESTION - text when viewing an address question #
#----------------------------------------------------------#

# Address question labels - these are shown as the field label before an input box.
label.addressLine2=Квартира, апартаменты и т. п. (необязательно)
label.city=Город
label.state=Штат
label.selectState=Выбрать штат
label.street=Адрес
label.zipcode=Почтовый индекс

# Validation errors that are shown when a user enters an invalid address.
validation.streetRequired=Укажите действительное название улицы и номер дома.
validation.cityRequired=Укажите город.
validation.currencyMisformatted=Укажите сумму в одном из следующих форматов (без точки с запятой): 1000; 1,000; 1000.30; 1,000.30.
validation.stateRequired=Укажите штат.
validation.invalidZipcode=Введите действительный 5-значный почтовый индекс.
validation.noPoBox=Введите действительный адрес. Не указывайте номер абонентского ящика.

#-------------------------------------------------------------------------#
# ADDRESS CORRECTION - text when viewing the address correction screen    #
#-------------------------------------------------------------------------#

# Title for a page asking the user to check if their address is correct and check whether it matches one of the listed address suggestions
title.confirmAddress=Подтвердите свой адрес
# Message explaining that the address the user provided couldn't be found when comparing it to a list of valid addresses.
content.confirmAddressLine1=Нам не удалось найти указанный адрес.
# Message asking the user to check if their address is correct, or choose from the provided list of suggested valid addresses.
content.foundSimilarAddressLine2=Проверьте его на наличие ошибок или выберите вариант из списка предложенных.
# Message asking the user to check if their address is correct and/or edit the address so that it's valid.
content.noValidAddressLine2=Проверьте его на наличие ошибок и укажите действительный вариант, чтобы продолжить.
# Heading for showing the user the address they provided earlier
content.addressEntered=Адрес, который вы указывали:
# Text on dialog that shows a suggestion for the user's correct address when entered incorrectly.
content.suggestedAddress=Предлагаемый адрес:
# Text on dialog that shows a suggestions for the user's correct address when entered incorrectly (and there is more than one).
content.suggestedAddresses=Предлагаемые адреса:
# Text on a button to save the address that the user chose (either the original address they entered, or one of the suggested valid addresses)
button.confirmAddress=Подтвердить адрес

#----------------------------------------------------------------------------------------------------------#
# DROPDOWN QUESTION - text shown when answering a question where a user must select an option from a list. #
#----------------------------------------------------------------------------------------------------------#

# Placeholder text shown in a dropdown selector before the user has made a selection.
placeholder.noDropdownSelection=Выбрать вариант

#------------------------------------------------------------------------------------------------------------------------------#
# PHONE QUESTION - text shown when answering a question where a user must select an option for country and enter phone number. #
#------------------------------------------------------------------------------------------------------------------------------#

label.phoneNumber=Введите номер телефона
validation.phoneNumberRequired=Укажите номер телефона.
validation.phoneNumberMustContainNumbersOnly=Номер телефона должен состоять только из цифр.
validation.invalidPhoneNumberProvided=Недействительный номер телефона.
validation.phoneMustBeLocalToCountry=Указанный номер телефона не относится к выбранной стране.

#----------------------------------------------------------------------------------------------------------#
# DATE QUESTION - text shown when answering a question where a user must select date. #
#----------------------------------------------------------------------------------------------------------#

validation.invalidDateFormat=Укажите дату в правильном формате.
# Date cannot be over specified years in past.
validation.dateBeyondAllowablePast=Допустимый период – последние {0} лет.
# Date cannot be over specified years in future.
validation.dateBeyondAllowableFuture=Допустимый период – следующие {0} лет.
# Valdation error shown when the user enters a date in the past but the question requires a date later than the current date.
validation.futureDateRequired=Дата должна быть позже текущей.
# Valdation error shown when the user enters a date that is earlier than the minimum allowed date.
validation.dateTooFarInPast=Дата должна быть позже {0}.
# Validation error shown when the user enters a date in the future but the question requires a date earlier than the current date.
validation.pastDateRequired=Дата должна быть раньше текущей.
# Validation error shown when the user enters a date that is later than the maximum allowed date.
validation.dateTooFarInFuture=Дата должна быть раньше {0}.
# Validation error shown when the user enters a date that is before or after the allowed date range.
validation.dateNotInRange=Дата должна быть между {0} и {1}.
validation.currentDateRequired=Введите сегодняшнюю дату.
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.January=01 – январь
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.February=02 – февраль
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.March=03 – март
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.April=04 – апрель
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.May=05 – май
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.June=06 – июнь
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.July=07 – июль
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.August=08 – август
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.September=09 – сентябрь
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.October=10 – октябрь
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.November=11 – ноябрь
# A month option when selecting a specific date from a dropdown menu.
option.memorableDate.December=12 – декабрь
# A placeholder for the month dropdown menu when selecting a specific date.
placeholder.memorableDate=Выбрать
# An error message that appears when the user tries to search by date without completing all the fields.
error.incompleteDate=Ошибка: укажите месяц, день и год.
# An error message that appears when the user tries to enter a date of birth that is in the future.
validation.dobNotInPast=Нельзя указать дату рождения в будущем.
# An error message that appears when the user tries to enter a date of birth that is too far in the past.
validation.impossibleDob=Нельзя указать дату рождения, с которой прошло 150 или более лет.

#--------------------------------------------------------------------------------------------------------#
# ENUMERATION QUESTION - text when viewing a question where a user must name entities, such as children. #
#--------------------------------------------------------------------------------------------------------#

# Text on a button an applicant would click to add another entity
button.addEntity=Добавить запись: {0}
# Text on a button an applicant would click to delete an entity
button.removeEntity=Удалить запись: {0}
# Dialog text shown to the user when they choose to delete an entry that they added previously. The dialog asks them to confirm if they want to delete the entry and all data associated with it. {0} represents the type of entry being deleted. For example, users may be asked to list all cars they own or all children in their household, so {0} could be "car" or "child".
dialog.confirmDeleteAllButtonsSave=Подтвердите, что вы хотите удалить выбранную запись ({0}). После того как вы нажмете "Проверить ответы", "Перейти к предыдущему экрану" или "Далее", это изменение сохранится и все связанные с записью "{0}" данные будут утеряны.
validation.entityNameRequired=Укажите название для каждой записи.
# Validation error that shows when an applicant uses a duplicate entity name
validation.duplicateEntityName=Названия записей не должны совпадать.
# Error shown if the enumerator question has a maximum answer limit and the applicant enters too many values
validation.tooManyEntities=Максимально допустимое число записей: {0}.
# Error shown if the enumerator question has a minimum answer limit and the applicant enters too few values
validation.tooFewEntities=Минимально допустимое число записей: {0}.
placeholder.entityName=Название записи ({0})

#---------------------------------------------------------------------------------#
# FILEUPLOAD QUESTION - text when viewing a question where a user uploads a file. #
#---------------------------------------------------------------------------------#

# Validation error that shows when an applicant does not select a file to upload
validation.fileRequired=Выберите файл.
# An error shown to the user if they upload a file that's too large. The error also asks them to upload a smaller file. {0} will be a number specifying the maximum size the file can be in megabytes. For example, "Please choose a file less than 100 MB".
validation.fileTooLarge=Слишком большой файл. Загрузите файл размером меньше {0} МБ.
# Text shown on a tag while a file is uploading.
label.uploading=Загрузка
# Label applied to a list of uploaded files
label.uploadedFiles=Загруженные файлы

#---------------------------------------------------------------------#
# ID QUESTION - id specific to filling out a question. #
#---------------------------------------------------------------------#

# Validation errors that appear if a user enters an answer that is too long or too short.
validation.idTooLong=Максимально допустимое число символов: {0}.
validation.idTooShort=Минимально допустимое число символов: {0}.
# Validation error that appears if a user enters an answer that is not a number
validation.numberRequired=Допускаются только цифры.
validation.numberRequired.v2=Должно содержать только цифры от 0 до 9.

#----------------------------------------------------------------------------------------------------------#
# MULTI-SELECT QUESTION - text shown when filling out a question with multiple answers, such as a checkbox #
#----------------------------------------------------------------------------------------------------------#

# Validation errors that are shown if a user selects too many or too few answers.
validation.tooFewSelections=Минимально допустимое число выбранных вариантов: {0}.
# Error message that indicates the applicant has selected too many choices on the multi-select question
validation.tooManySelections.v2=Максимально допустимое число выбранных вариантов: {0}.

#---------------------------------------------------#
# NAME QUESTION - text when viewing a name question #
#---------------------------------------------------#

# Name question labels - these are shown as the field label before an input box.
label.firstName=Имя
label.lastName=Фамилия
label.middleName=Отчество/второе имя
# Label for name suffix field in name question
label.nameSuffix=Суффикс

# Option Junior for the name suffix dropdown
option.junior=мл.
# Option Senior for the name suffix dropdown
option.senior=ст.
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
validation.firstNameRequired=Укажите имя.
validation.lastNameRequired=Укажите фамилию.

#------------------------------------------------------------------------#
# NUMBER QUESTION - text specific to answering a question with a number. #
#------------------------------------------------------------------------#

# Validation errors that are shown when a user enters a number that is too big or too small, or a non-integer.
validation.numberTooBig=Число не должно быть больше {0}.
validation.numberTooSmall=Число не должно быть меньше {0}.
validation.numberNonInteger=Укажите положительное целое число, которое состоит только из цифр от 0 до 9.

#---------------------------------------------------------------------#
# TEXT QUESTION - text specific to filling out a question with words. #
#---------------------------------------------------------------------#

# Validation errors that appear if a user enters an answer that is too long or too short.
validation.textTooLong=Максимально допустимое число символов: {0}.
validation.textTooShort=Минимально допустимое число символов: {0}.

#---------------------------------------------------------------------#
# YES/NO QUESTION - text specific to yes/no questions. #
#---------------------------------------------------------------------#

option.yes=Да
option.no=Нет
option.notSure=Затрудняюсь ответить
option.maybe=Возможно

#-------------------------------------------------------------#
# MAP QUESTION - text specific to map questions.              #
#-------------------------------------------------------------#

# Text for map question apply filters button
map.applyFiltersButtonText=Применить фильтры
# Aria label for next page pagination button
map.ariaLabelNextPage=Перейти на предыдущую страницу с местоположениями на карте
# Aria label for pagination list
map.ariaLabelPaginationList=Нумерация страниц для местоположений на карте
# Aria label for previous page pagination button
map.ariaLabelPreviousPage=Перейти на предыдущую страницу с местоположениями на карте
# Header for the section showing available locations
map.availableLocations=Доступные местоположения
# Legend text for the map filters section
map.filterLegendText=Фильтры
# Aria label for pagination buttons
map.goToPage=Перейти на страницу {0} с метоположениями на карте
# Link text for location details URLs
map.locationLinkText=Подробнее
# The screen reader text on a link to view more details for a location. The variable represents the location name.
map.locationLinkTextSr=Подробнее о {0}
# Text showing the count of displayed locations, with placeholders for current count and total count
map.locationsCount=Показываем места с {0} по {1} из {2}
# Text showing the count of selected locations, with placeholders for current count and maximum allowed selections
map.locationsSelectedCount=Выбрано {0} из максимум {1} местоположений.
# Alt text for map region
map.mapRegionAltText=Интерактивная карта с отображением местоположений
# Text for map question selected button in popups
map.mapSelectedButtonText=Выбрано
# Message displayed when filter results return no locations
map.noResultsFound=Результаты не найдены. Попробуйте настроить фильтры.
# Message displayed when no locations have been selected yet
map.noSelectionsMessage=Места не выбраны.
# Sr only message displayed when page has changed
map.paginationStatus=Сейчас отображается страница {0} из {1}.
# Text for map question reset filters button
map.resetFiltersButtonText=Очистить фильтры
# Heading for the section showing selected locations
map.selectedLocationsHeading=Выбранные места
# Placeholder text for map question select button in popups
map.selectLocationButtonText=Выбрать местоположение
# Text showing the maximum number of locations that can be selected, with a placeholder for that number
map.selectLocations=Выберите до {0} местоположений
# Placeholder text for map question select option dropdown
map.selectOptionPlaceholderText=- Выбрать -
# Button text to switch from list view to map view on mobile
map.switchToMapView=Перейти к просмотру карты
# Button text to switch from map view to list view on mobile
map.switchToListView=Перейти к просмотру списка
# Screen reader text announcing that the view has been switched to map view
map.switchToMapViewSr=Вид переключен на карту
# Screen reader text announcing that the view has been switched to list view
map.switchToListViewSr=Вид переключен на список
# Error message that indicates the applicant has selected too many locations
map.validation.tooManySelections=Выберите не более {0} местоположений.
# Error message that indicates the geojson could not be loaded and displayed to the applicant due to an internal error, with placeholders for html links to the homepage and contact us email
map.geoJsonErrorText = К сожалению, в данный момент мы не можем отобразить этот вопрос. Попробуйте вернуться к этому заявлению позже. А пока вернитесь к {0} или {1}, и мы подскажем вам верное направление.
map.contactUs = связаться с нами
map.homepage = домашняя страница
# screen reader text anouncing that a user cannot select any more locations with a placeholder for the total amount of locations they are allowed to select.
map.maxLocationsSelectedSr = Вы выбрали максимальное количество мест {0}. Чтобы добавить другое, отмените выбор хотя бы одного.

#---------------------------------------------------------------------------#
# MAP QUESTION ADMIN EDIT - text specific to creating/editing map questions. #
#---------------------------------------------------------------------------#

# Button for adding a filter
map.addFilterButton=Добавить фильтр
# Subtitle text for adding filters
map.addFiltersSubtitle=Выберите до шести фильтров, которые будут доступны кандидатам.
# Title for filters section
map.addFiltersTitle=Фильтры
# Button for adding a tag
map.addTagButton=Добавить тег
# Subtitle text for adding a tag
map.addTagSubtitle=Добавьте тег, который будет отображаться в определённых местоположениях, где выбранный ключ соответствует определённому значению. При выборе заявителем хотя бы одного из этих местоположений можно отображать оповещение в области "Выбранные местоположения".
# Title for tag section
map.addTagTitle=Тег
# Label for display name field
map.displayNameLabel=Отображаемое имя
# Label for key field
map.keyLabel=Ключ
# Error message when a configured key is not found in the GeoJSON data
map.keyNotFoundError=Ошибка: ключ не найден. Выберите другой ключ.
# Help text for location address field
map.locationAddressHelpText=Выберите, какой ключ соответствует адресу местоположения.
# Label for location address key field
map.locationAddressLabel=Ключ адреса
# Help text for location details URL field
map.locationDetailsUrlHelpText=Выберите, какой ключ представляет URL-адрес сведений о местоположении.
# Label for location detail URL key field
map.locationDetailsUrlLabel=Подробнее о URL-ключе
# Help text for location name field
map.locationNameHelpText=Выберите ключ, соответствующий названию местоположения.
# Label for location name key field
map.locationNameLabel=Ключ имени
# Label for maximum number of locations field
map.maxLocationSelectionsLabel=Максимальное количество выбранных местоположений
# Label for setting text field
map.settingTextLabel=Текст оповещения
# Label for value field
map.valueLabel=Значение
# Text on a grey square that indicates where a map will appear once a question has been published
map.mapPreviewText=Карта будет отображаться здесь

#---------------------------------------------------------------------#
# MULTI OPTION QUESTION ADMIN EDIT - text specific when creating/editing a multi option question. #
#---------------------------------------------------------------------#

# Validation errors that appear if an admin leaves a multi option question blank.
adminValidation.multiOptionEmpty=На вопрос с множественным выбором должен быть дан ответ.
# An error messaging saying that the admin name (a unique, admin-facing identifier) that the admin just entered is invalid.
adminValidation.multiOptionAdminError=В названиях, предназначенных для администраторов, допускаются только строчные буквы, цифры, символ подчеркивания и дефис.

#----------------------------------#
# ERRORS - various error messages  #
#----------------------------------#

error.notFoundTitle=Нам не удалось найти страницу, которую вы пытались открыть
error.notFoundDescription=Перейдите на {0} или вернитесь на предыдущую.
error.notFoundDescriptionLink=главную страницу
# The title on the error page that is show to the user in large font
error.internalServerTitle.v2=Произошла ошибка
# The subtitle on the error page that is show to the user in medium font
error.internalServerSubtitle=К сожалению, в нашей системе возникла ошибка.
# Additional information about the error that is shown to the user in small font
error.internalServerDescription=Обратитесь за помощью в службу поддержки по адресу {0}, указав ID ошибки {1}.
# A button redirecting the user to the homepage
error.internalServerHomeButton=Перейти на главную страницу
# A message to the user indicating what the status code is of the error that was thrown, for example 404 or 500
error.statusCode=Код ошибки: {0}

#-------------------------------------------------------------#
# EMAILS - boilerplate text contained in emails sent to users #
#-------------------------------------------------------------#

# Bottom of the body of the email sent automatically on application submission
email.loginToCiviform=Чтобы войти в свой аккаунт на сайте Civiform, перейдите на страницу {0}.
# Subject of the email sent automatically on application submission
email.applicationReceivedSubject=Мы получили вашу заявку на участие в программе "{0}"
# Body of the email sent automatically on application submission
email.applicationReceivedBody=Мы получили заявку, которую вы отправили для участия в программе "{0}", и присвоили ей идентификатор {2}. Ваш идентификатор заявителя: {1}.
# Subject of the email sent to the TI on application submission
email.tiApplicationSubmittedSubject=Вы подали заявку на участие в программе "{0}" от имени заявителя {1}
# Body of the email sent to the TI on application submission
email.tiApplicationSubmittedBody=Мы получили заявку, отправленную от имени заявителя {1} для участия в программе "{0}", и присвоили ей идентификатор {2}.
# Link at the bottom of the email sent to the TI on application submission
email.tiManageYourClients=Чтобы добавить, удалить клиентов или изменить информацию о них, перейдите на страницу {0}.
# Subject of the email sent to an applicant when the status of their application has changed
email.applicationUpdateSubject=Статус вашей заявки {0} изменен
# Subject of the email sent to a TI when they change the status of an application
email.tiApplicationUpdateSubject=Изменен статус заявки, отправленной от имени заявителя {1} для участия в программе "{0}"
# Body of the email sent to a TI when they change the status of an application
email.tiApplicationUpdateBody=Статус заявки, отправленной от имени заявителя {0} для участия в программе "{1}", обновлен на следующий: {2}.

#------------------------------------------------------------------------------#
# GOVERNMENT BANNER - text explaining that CF is an official government site #
#------------------------------------------------------------------------------#

banner.title=Официальный правительственный сайт.
banner.link=Вот как это проверить
banner.govWebsiteSectionHeader=Официальные сайты используют домен ".gov"
banner.govWebsiteSectionContent=Адреса сайтов, заканчивающиеся на ".gov", принадлежат правительственным организациям США.
banner.httpsSectionHeader=Защищенные сайты с доменом ".gov" используют протокол HTTPS
banner.httpsSectionContent=Если адрес страницы начинается с "https://" или рядом с ним есть значок замка ({0}), значит вы подключились к сайту с доменом ".gov" безопасным образом. Указывайте конфиденциальную информацию только на официальных и защищенных сайтах.

#----------------------------------------------------------------------------------------#
# NOT PRODUCTION BANNER - text explaining that this site is not a production environment #
#----------------------------------------------------------------------------------------#

# A stern message shown to the user to make it clear this is a test site only
banner.notForProductionBannerLine1=Этот сайт предназначен только для тестирования. Не указывайте свои личные данные.
# A friendly message shown to the user with a call to action to go to the production site link provided in {0}
banner.notForProductionBannerLine2=Чтобы подать заявку на участие в программе или получение услуги, перейдите на сайт {0}.

#------------------------------------------------------------------------------#
# GUEST BANNER - alert explaining that user is logged in as a guest #
#------------------------------------------------------------------------------#
# A message in an alert about ending their session when they are finished. The placeholder in {0} will be a link with the text 'end your session'.
banner.guestBannerText=После подачи заявки {0}, чтобы защитить данные.
# A call to action within another message that will be a link to end your guest session.
banner.endYourSession=завершите сеанс
# A message letting the user know how long their session will last before they are logged out automatically. The placeholder will be a phrase like "2 hours and 30 minutes" or "30 minutes".
banner.sessionExpiration=Ваш сеанс автоматически завершится через {0} с начала сеанса. Войдите в систему или отправьте заявку до этого времени, чтобы не потерять свой прогресс.
# Phrases that combine hours and minutes for the session expiration message.
banner.hourAndMinutes=1 час и {0} мин.
banner.hourAndMinute=1 час и 1 мин.
banner.hoursAndMinutes={0} час. и {1} мин.
banner.hoursAndMinute={0} час. и 1 мин.
# Amount of time in hours or minutes for the session expiration message.
banner.minutes={0} мин.
banner.hours={0} час.
banner.hour=1 час
banner.minute=1 мин.

#------------------------------------------------------------------------------#
# CATEGORIES - tags that admins can choose to specify the type of program #
# Note for developers: these messages aren't used in static text, but instead they #
#  are seeded into the database, since these will be admin defined in the future #
#------------------------------------------------------------------------------#

# A tag used to filter the list of programs down to those that are related to childcare.
category.tag.childcare=Уход за детьми

# A tag used to filter the list of programs down to those that are related to economy.
category.tag.economic=Экономика

# A tag used to filter the list of programs down to those that are related to education.
category.tag.education=Образование

# A tag used to filter the list of programs down to those that are related to employment.
category.tag.employment=Трудоустройство

# A tag used to filter the list of programs down to those that are related to food.
category.tag.food=Питание

# A tag used to filter the list of programs down to those that have a general purpose.
category.tag.general=Общее

# A tag used to filter the list of programs down to those that are related to healthcare.
category.tag.healthcare=Здравоохранение

# A tag used to filter the list of programs down to those that are related to housing.
category.tag.housing=Жилье

# A tag used to filter the list of programs down to those that are related to the Internet.
category.tag.internet=Интернет

# A tag used to filter the list of programs down to those that are related to the military.
category.tag.military=Армия

# A tag used to filter the list of programs down to those that are related to training.
category.tag.training=Обучение

# A tag used to filter the list of programs down to those that are related to transportation.
category.tag.transportation=Транспорт

# A tag used to filter the list of programs down to those that are related to utilities.
category.tag.utilities=Коммунальные услуги

# A tag used to filter the list of programs down to those that are related to military veterans.
category.tag.veteran=Ветеран

# An aria-label for screen readers that helps provide context for the category tags.
ariaLabel.categories=Категории

#------------------------------------------------------------------------------#
#  Session timeout messages                                                    #
#------------------------------------------------------------------------------#

# Title of the warning modal that appears when the user has been inactive for a while
session.inactivity.warning.title=Вы слишком долго были неактивны

# Message shown in a modal asking the user if they want to extend their session due to inactivity
session.inactivity.warning.message=Сеанс скоро закончится. Хотите его продлить?

# Title of a modal dialog displayed when a user's session is about to expire due to session length limit
session.length.warning.title=Слишком долгий сеанс

# Message shown in a modal warning the user their session will end soon due to session length limit
session.length.warning.message=Сеанс скоро закончится. Сохраните изменения и, если нужно, заново войдите в аккаунт.

# Text on a button that allows users to extend their session when shown timeout warnings
session.extend.button=Продлить сеанс

# Success message shown when the user's session is successfully extended after clicking the extend button
session.extended.success=Сеанс продлен.

# Error message shown when there was a problem extending the user's session
session.extended.error=Не удалось продлить сеанс.

#------------------------------------------------------------------------------#
#  ADMIN PROGRAM BLOCK EDIT                                                    #
#------------------------------------------------------------------------------#

# Heading for the repeated set creation method radio buttons
heading.repeatedSet.creationMethod=
# Option for allowing an admin to create a new repeated set
option.repeatedSet.createNew=
# Option for allowing an admin to choose an existing repeated set
option.repeatedSet.chooseExisting=
# Label for the new repeated set form
label.repeatedSet.newSetForm=
# Label for the text input field asking what type of entity users will be listing (e.g., household members)
input.repeatedSet.listedEntity=
# Label for the text input field asking for the admin identifier of the repeated set
input.repeatedSet.adminId=
# A text input field asking admins for the text that will prompt applicants to list out the entity
input.repeatedSet.questionText=
# A text input field for admins to add a hint that will help applicants decide what entities to list out
input.repeatedSet.hintText=
# A form field where admins set the minimum number of entities that applicants must list
input.repeatedSet.minEntities=
# A form field where admins set the maximum number of entities that applicants can list
input.repeatedSet.maxEntities=
# Alert shown to notify admin that creating a new repeated set will add a new question to the question bank
alert.repeatedSet.newQuestion=
# Submission button for creating a new repeated set
button.repeatedSet.submitNew=
# Button for admins to add a new repeated set
button.repeatedSet.addNew=
# Text letting admins know which block represents the group of screens for repeating questions
text.repeatedSet=
# Text letting admins know which block represents the group of screens for nested repeating questions
text.nestedRepeatedSet=
# An uneditable prefix for the enumerator screen name representing the repeated object that will be enumerated
text.repeatedSet.prefix=заголовок родительского элемента
# An uneditable prefix for the enumerator screen name representing the nested repeated object that will be enumerated
text.repeatedSet.nestedPrefix=заголовок дочернего элемента
# Instructions for admins on how to add or change the repeated set question in a program block
text.repeatedSet.questionDescription=Add a repeated set that will ask residents to add objects or individuals to a list. If a repeated set has already been added and you would like to select a different one, delete the question below.
# Heading above the repeated set question in the program block editor
heading.repeatedSet.question=Repeated set question
# Informative text above the listed entity input field to help admins understand what to enter in that field.
description.repeatedSet.listedEntity=
# Informative text above the admin ID input field telling admins that an ID will be auto-generated based on the wording they entered in the preview field.
description.repeatedSet.adminId=
# An informative description above the question text input field to help admins understand what to enter in that field.
description.repeatedSet.questionText=
# An informative description above the hint text input field to help admins understand what to enter in that field.
description.repeatedSet.hintText=

#------------------------------------------------------------------------------#
#  ADMIN REPORTING                                                             #
#------------------------------------------------------------------------------#

# Caption for the table showing submissions by program
caption.submissionsByProgram=Заявки по программам (все программы)
# Column header for program name
content.program=Программа
# Column header for number of submissions
content.submissions=Заявки
# Column header for 25th percentile time to complete
content.timeToCompleteP25=Время завершения (p25)
# Column header for median (50th percentile) time to complete
content.medianTimeToComplete=Медианное время завершения
# Column header for 75th percentile time to complete
content.timeToCompleteP75=Время завершения (p75)
# Column header for 99th percentile time to complete
content.timeToCompleteP99=Время завершения (p99)
# Text on button to download CSV file
button.downloadCsv=Скачать CSV
# Caption for the table showing submissions by month
caption.submissionsByMonth=Заявки по месяцам (все программы)
# Column header for month
content.month=Месяц

#------------------------------------------------------------------------------#
#  ADMIN REPORTING PROGRAM                                                     #
#------------------------------------------------------------------------------#

# Heading for the program reporting page
reportingProgram.heading={0} отчетов
# Info text about data delay
reportingProgram.dataDelay=Данные могут отображаться с задержкой до часа.
# Caption for the table showing submissions by month for a program
reportingProgram.caption.submissionsByMonth=Заявки по месяцам
# Column header for month
reportingProgram.month=Месяц
# Column header for submissions
reportingProgram.submissions=Заявки
# Column header for 25th percentile time to complete
reportingProgram.timeToCompleteP25=Время завершения (p25)
# Column header for median (50th percentile) time to complete
reportingProgram.medianTimeToComplete=Медианное время завершения
# Column header for 75th percentile time to complete
reportingProgram.timeToCompleteP75=Время завершения (p75)
# Column header for 99th percentile time to complete
reportingProgram.timeToCompleteP99=Время завершения (p99)
# Text on button to download CSV file
reportingProgram.downloadCsv=Скачать CSV

#------------------------------------------------------------------------------#
#  DEV TOOLS - text for the developer tools page                               #
#------------------------------------------------------------------------------#
# The text on the button to go to the home page from the dev tools page.
devtools.homepage=Home page

# Heading for the seed section of the dev tools page.
devtools.seed.title=Seed
# Description for the seed section of the dev tools page.
devtools.seed.description=Populate or clear sample data
# Button text to seed sample programs and categories.
devtools.seed.programs=Seed sample programs and categories
# Button text to seed sample questions.
devtools.seed.questions=Seed sample questions
# Button text to clear the entire database (irreversible action).
devtools.seed.clear=Clear entire database (irreversible!)

# Heading for the caching section of the dev tools page.
devtools.cache.title=Caching
# Description for the caching section of the dev tools page.
devtools.cache.description=Manage or clear cache
# Button text to clear the cache.
devtools.cache.clear=Clear cache

# Heading for the durable jobs section of the dev tools page.
devtools.jobs.title=Durable Jobs
# Description for the durable jobs section of the dev tools page.
devtools.jobs.description=Manually run the selected job
# Label for the dropdown to select a durable job to run.
devtools.jobs.select=Choose job to run once
# Button text to run the selected durable job.
devtools.jobs.run=Run job

# Heading for the icons section of the dev tools page.
devtools.icons.title=Icons
# Description for the icons section of the dev tools page.
devtools.icons.description=See all the svg icons in CiviForm
# Button text to view all SVG icons.
devtools.icons.view=View All SVG Icons

# Heading for the Localstack section of the dev tools page.
devtools.localstack.title=Localstack
# Description for the Localstack section of the dev tools page.
devtools.localstack.description=Open S3 or SES endpoints on localstack
# Button text to view SES emails in Localstack.
devtools.localstack.ses=View SES Emails
# Button text to view the S3 private bucket in Localstack.
devtools.localstack.private=S3 Private Bucket
# Button text to view the S3 public bucket in Localstack.
devtools.localstack.public=S3 Public Bucket

# Heading for the address tools section of the dev tools page.
devtools.address.title=Address Tools
# Description for the address tools section of the dev tools page.
devtools.address.description=View address lookup and eligibility results
# Button text to go to the address tools page.
devtools.address.go=Go to address tools

# Heading for the session tools section of the dev tools page.
devtools.session.title=Session Tools
# Description for the session tools section of the dev tools page.
devtools.session.description=Inspect the current session
# Button text to view the current pac4j profile.
devtools.session.pac4j=View current pac4j profile
# Button text to view the current Play session.
devtools.session.play=View current Play session
