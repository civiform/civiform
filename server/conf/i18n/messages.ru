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
#  This appears before applicants select their preferred language.
label.selectLanguage=Выберите язык.
button.untranslatedSubmit=Отправить
# Text on a link that gives more information about CiviForm. "CiviForm" is the app name and should
# not be translated.
footer.aboutCiviform=О сервисе
# Message displayed before the support email address in the page footer for applicants.
footer.supportLinkDescription=Служба технической поддержки:
# Text in the footer telling users that this is an official website of the specified Civic entity.
footer.officialWebsiteOf={0} – официальный сайт
# Text on a link that will scroll to the top of the page
footer.returnToTop=Вернуться к началу страницы
# A message in the footer directing users to technical support. The placeholder is a link to
# send an email to CiviForm technical support.
footer.technicalSupport=Служба технической поддержки: {0}
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
heading.informationAriaLabelPrefix=For your information: {0}
# Message for screen readers as part of the aria label to let user know they completed an action successfully.
heading.successAriaLabelPrefix=Success: {0}
# Toast message that tells the user their session has ended, to help indicate that they are no longer logged in to an account (but still as a guest).
toast.sessionEnded=Сеанс завершен.
# Message for guest users, to avoid showing "Logged in as Guest" when they are really not logged in.
header.guestIndicator=Вы используете сайт в качестве гостя.
# Message with user's name to show who you are logged in as.
header.userName=Вы вошли в аккаунт: {0}
# Error message to answer a required question
validation.isRequired=Ответ на этот вопрос обязателен.
# Validation error that is shown when the user input couldn't be converted for any reason.
validation.invalidInput=Введите действительные данные.
# Error message announced to screen reader when there are errors on the current page.
validation.errorAnnouncementSr=В форме есть ошибки. Чтобы продолжить, исправьте их.
# Message displayed at the top of a question page denoting fields with a * are required.
content.requiredFieldsAnnotation=Примечание. Поля, отмеченные знаком "*", должны быть заполнены.
content.requiredFieldsNote=Примечание. Поля, отмеченные знаком "{0}", должны быть заполнены.
# Disclaimer at top of form explaining that asterisks mark required questions. The placeholder is the red, bold asterisk used throughout the page.
content.requiredFieldsNoteNorthStar=Обязательные поля отмечены звездочкой ({0}).
content.optional=(необязательно)
# Message displayed below "Choose File" button to say that on the phone users can use their camera.
content.mobileFileUploadHelp=Если вы используете телефон, кнопка "Выбрать файл" также позволяет загружать документы с помощью камеры устройства.
toast.errorMessageOutline=Ошибка: {0}.
# Description for an "X" button that will close a dialog, modal, or page.
button.close=Закрыть
# Button text that will navigate the applicant to a page where they can review previous answers
button.review=Проверить ответы
# Indicator for screen readers that a link will open in a new tab. Meant to be used with aria label text eg. "Program details, opens in a new tab"
link.opensNewTabSr=страница откроется в новой вкладке
# Aria-label for the primary navigation
label.primaryNavigation=Основное меню навигации
# Aria-label for agency identifier
label.agencyIdentifier=Идентификатор агентства
# Aria-label for guest session alert
label.guestSessionAlert=Предупреждение о том, что запущен гостевой сеанс

#-------------------------------------------------------------#
# LOGIN - contains text that for login page.                  #
#-------------------------------------------------------------#

# This is a title for the login page
title.login=Вход
# Prompt for applicant to log in, input is the full civic entity name.
content.loginPrompt=Войдите в свой аккаунт, зарегистрированный на портале "{0}"
# The text on the button an applicant clicks to create an account.
button.createAnAccount=Создать аккаунт
# The text on the button an applicant clicks to log in to their session.
button.login=Войти
# Prompt for applicant to create a new account or become a guest
content.alternativeLoginPrompt=Нет аккаунта?
# The text between creating a new account, and becoming a guest
content.or=или
# The text on the button for applicants to create a new account.
button.createAccount=Создать аккаунт
# The text on the button for guests to log in to their session.
button.guestLogin=Продолжить в качестве гостя
# Prompt for whem applicant account login is disabled. Replaces loginPrompt and alternativeLoginPrompt.
content.loginDisabledPrompt=Вход в аккаунт сейчас недоступен
# The words leading up to the admin login anchor
content.adminLoginPrompt=Не нашли подходящий вариант?
# Asking whether the user is an administrator for the program
content.adminFooterPrompt=Вы администратор?
# The text on the anchor for admins to log in to their session.
link.adminLogin=Войти как администратор

#--------------------------------------------------------------------------#
# PROGRAM FORM - contains text shown when filling out an application form. #
#--------------------------------------------------------------------------#

# The text displayed when a file has already been uploaded and the name is being displayed.
input.fileAlreadyUploaded=Загружен файл "{0}".
# Hint placed above file input for questions which accept a single file.
input.singleFileUploadHint=Выберите файл
# Hint placed above file input for questions which accept multiple, but not unlimited, files. {0} contains a number,
# 2 or greater, representing the maximum number of files.
input.multipleFileUploadHint=Выберите один или несколько файлов (не более {0})
# Hint placed above file inputs which accept any number of files.
input.unlimitedFileUploadHint=Выберите один или несколько файлов
# The text on the button an applicant clicks to delete an uploaded file.
button.deleteFile=Удалить
# The text on the button an applicant clicks to skip uploading a new file while there is already an uploaded file.
button.keepFile=Продолжить
# The button text for saving the current applicant-entered data and continuing to the next screen in the form.
button.nextScreen=Сохранить и продолжить
# The button text for navigating to the previous screen in the form.
button.previousScreen=Перейти к предыдущему экрану
# The label on a button that will navigate the user to the previous section of the form.
button.back=Назад
# The label on a button that will save user answers and navigate to the summary of the application.
button.reviewAndExit=Проверить и закрыть форму заявки
# The text on the button an applicant clicks to skip uploading a file.
button.skipFileUpload=Пропустить
# The current screen the user is on ({0}) out of the number of total screens in the application.
content.blockProgress={0} из {1}
# A message on a section indicating how many questions of the total have been answered by the applicant.
content.blockProgressFull=Дано ответов: {0} из {1}
# An aria-label on a progress bar showing user how much of the application they have completed.
content.blockProgressLabel=Прогресс заполнения заявки
# Heading on the application review page. The page shows all the answers the user inputted.
heading.reviewAndSubmit=Проверка и отправка заявки
# Label on a section of a progress bar. The first placeholder is the section number and second is section name (eg: "2. Contact info")
label.blockIndexLabel={0}. {1}
# A toast message that displays when a program is not fully localized to the applicant's preferred locale.
toast.localeNotSupported=К сожалению, эта программа не полностью переведена на выбранный вами язык.
# A link that logs out a guest user after they submit an application.
link.allDone=Завершить сеанс
# A link that appears next to the create account button that offers applicants the option to not create an account right now.
link.applyToAnotherProgram=Подать заявку на участие в другой программе
# A link that offers applicants the option to create an account.
link.createAccountOrSignIn=Создать аккаунт или войти
# Anchor which when clicked removes a file the user has previously uploaded.
link.removeFile=Удалить файл
# The title of a pop-up informing the user that they tried to go to the review page but there were errors in the information that they inputted
modal.errorSaving.review.title=На этой странице заполнены не все поля. Вы все равно хотите покинуть ее и проверить свои ответы?
# The title of a pop-up informing the user that they tried to go to the previous page but there were errors in the information that they inputted
modal.errorSaving.previous.title=На этой странице заполнены не все поля. Вы все равно хотите покинуть ее и открыть предыдущую страницу?
# A message informing the user that there were errors in the information that they inputted. This message also asks the user if they want to (1) stay and fix their answers or (2) discard the information they've inputted and continue to the application review page.
modal.errorSaving.review.content=В информации есть ошибки. Хотите их исправить или перейти на страницу проверки, не сохраняя ответы?
# A message informing the user that there were errors in the information that they inputted. This message also asks the user if they want to (1) stay and fix their answers or (2) discard the information they've inputted and continue to the previous page of the application.
modal.errorSaving.previous.content=В информации есть ошибки. Хотите их исправить или перейти на предыдущую страницу, не сохраняя ответы?
# Text for a button. When the button is clicked, then all the information the user has inputted will be discarded and they will be taken to the application review page.
modal.errorSaving.review.noSaveButton=Не сохранять и перейти на страницу проверки
# Text for a button. When the button is clicked, then all the information the user has inputted will be discarded and they will be taken to the previous page in the application.
modal.errorSaving.previous.noSaveButton=Не сохранять и перейти на предыдущую страницу формы
# Text for a button. When the button is clicked, the user will be shown the information that they previously inputted and will be asked to fix the errors with it.
modal.errorSaving.stayAndFixButton=Остаться и исправить

#----------------------------------------------------------------------------#
# APPLICANT HOME PAGE - contains text specific to the applicant's home page. #
#----------------------------------------------------------------------------#

# The text on the button an applicant clicks to apply to a specific program.
button.apply=Подать заявку
# The text read for screen readers.
button.applySr=Подать заявку на участие в программе "{0}"
# The text on the button an applicant clicks to edit the application for a specific program.
button.edit=Изменить
# The screen reader text for a button allowing an applicant to edit a submitted application for a given program.
button.editSr=Изменить отправленную заявку на участие в программе "{0}"
# The screen reader text for a button allowing an applicant to continue editing an in-progress application for a given program.
button.continueSr=Продолжить заполнять заявку на участие в программе "{0}"
# The text on the button an applicant clicks to start filling out a pre-screener form.
button.startHere=Начать
# The text on a button to view and apply to a program. Clicking the button leads to the program overview page.
button.viewAndApply=Узнать больше и подать заявку
# The screen reader text on a button to view and apply to a program. The variable represents the program name.
button.viewAndApplySr=Узнать больше и подать заявку на участие в программе "{0}"
# The text for the button that allows a guest to bypass the login prompt modal.
button.continueToApplication=Заполнить заявку
# The screen reader text for a button an applicant clicks to start filling out a pre-screener form.
button.startHereCommonIntakeSr=Заполнить форму "{0}"
# The screen reader text for a button an applicant clicks to edit their responses to a pre-screener form.
button.editCommonIntakeSr=Изменить сведения, указанные в форме "{0}"
# The screen reader text for a button an applicant clicks to continue filling out a pre-screener form.
button.continueCommonIntakeSr=Продолжить заполнять форму "{0}"
# Text describing the date the application was last submitted.
content.submittedDate=Вы отправили заявку {0}
# Text for applicants to understand the section is for finding programs
content.findPrograms=Находите программы
# Long form description of the CiviForm site shown to the applicant.
content.findProgramsDescription=CiviForm позволяет находить программы, которые могут быть доступны для вас в этом регионе ({0}). Чтобы приступить к заполнению данных, выберите форму или программу ниже.
# Title for programs page when applicant is not logged in
content.saveTimeServices=Экономьте время на подаче заявок для получения услуг и участия в программах
# Main home page heading
heading.homepage=Подавайте заявки на участие в программах поддержки
# Long form description of the site shown to the applicant when they are not logged in.
# {0} represents the authentication provider's name
content.guestDescription=Войдите в свой аккаунт, зарегистрированный в сервисе "{0}", чтобы подавать заявки на участие в программах и не вводить свои данные повторно. Вы также сможете изменять заполненные заявки и проверять их статус в любое время. Если у вас нет аккаунта, вы можете его создать.
# Main home page intro text
content.homepageIntro=Получайте помощь с оплатой расходов на уход за детьми, питание, транспорт, коммунальные услуги и многое другое.
# The label for the program filter buttons
label.programFilters=Фильтры по категориям программ
# The label for the program filter checkboxes
label.programFilters.v2=Фильтр по категориям
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
# The title for the section in the list of programs that contains all regular, non-pre-screener programs.
title.allProgramsSection=Все программы ({0})
# For the badge on the program list index denoting the current status of an application
title.status=Статус
# Subtitle for the list of programs with draft applications
title.inProgressProgramsUpdated=Вы начали заполнять заявку
# Subtitle for the list of programs for which the applicant has no draft applications
title.activeProgramsUpdated=Вы не заполняли заявку
# Title of the section on the home page with the benefits finder form.
title.benefitsFinderSection.v2=Находите услуги
# Title of the section on the home page that shows programs for which the applicant has already started or submitted applications.
title.myApplicationsSection=Мои заявки ({0})
# Title of the section on the home page that shows programs for which the applicant has already started or submitted applications.
title.myApplicationsSection.v2=Мои заявки
# Title of the section on the home page that shows any programs that don't match the selected filters, with the number of programs in parentheses.
title.otherProgramsSection.v2=Другие программы и услуги ({0})
# Title of the section on the home page that shows all programs when no filter is selected, with the number of programs in parentheses.
title.programsSection.v2=Программы и услуги ({0})
# Title of the section on the home page that shows any available programs that have not yet been applied to.
title.availableProgramsSection=Доступные программы и услуги
# Title of the section on the home page that shows programs that match any of the selected filters.
title.recommendedSection.v2=Программы по фильтрам ({0})
# A label on the summary of a section of the application indicating to the applicant that all required questions have been filled out.
title.programSectionCompleted=Вы заполнили этот раздел
# Subtitle for the list of programs for which the applicant has already submitted an application
title.submittedPrograms=Вы отправили заявку
# Alert banner when an application was successfully saved, with the ID of the application.
toast.applicationSaved=Заявка с идентификатором {0} сохранена.
# Alert banner when an application was already completed
toast.programCompleted=Заявка уже заполнена.
# Informational tag on an in-progress application card
label.inProgress=Заявка не подана
# Informational tag on a submitted application card. Used when the date of submission is unknown.
label.submitted=Заявка подана
# Informational tag on a submitted application card. The parameter is the date of the submission.
label.submittedOn=Заявка подана {0}
# Informational tag on a submitted application card. The first paramater is the status applied to an application. The second parameter is the date the status was applied.
label.statusOn={0} {1}

#------------------------------------------------------------------------------------------------------#
# TRUSTED INTERMEDIARY DASHBOARD PAGE - text when adding, editing, deleting, or searching for a client #
#------------------------------------------------------------------------------------------------------#

# Message that appears when a user attempts to delete an account and it fails.
banner.acctDeleteError=Нельзя удалить этот аккаунт
# Reason included with the account delete error, which explains the account can't be deleted due to applications the client has open
banner.acctDeleteErrorReason=В нем есть открытые заявки.
# Message when information about a client, like their name or contact information is updated.
banner.clientInfoUpdated=Информация клиента обновлена.
# Message when a client account for a user is successfully deleted, with the name of the client substituted in
banner.clientAcctDeleted=Аккаунт клиента {0} удален.
# Message when a user successfully creates a client account.
banner.newClientCreated=Аккаунт клиента создан
# Banner at the top of the page with the name of the client substituted in when viewing an application.
banner.viewApplication=Вы подаете заявку от имени клиента {0}. Хотите выбрать другого клиента?


# Button that allows a user to add a client to the CiviForm system.
button.addNewClient=Добавить клиента
# Button that brings the user back to the list of their clients.
button.backToClientList=Вернуться к списку клиентов
# Button that allows the user to navigate back to the edit page after they click to delete a client and get an error.
button.backToEditing=Вернуться на страницу изменения сведений
# Button that allows the user to cancel the progress they make on adding or editing a client.
button.cancel=Отмена
# Button that allows the user to clear any search parameters they have already entered.
button.clearSearch=Очистить параметры поиска
# Button for a user to delete their client's account
button.deleteAcct=Удалить аккаунт
# Button option when asked if the user is sure they want to delete an account.
button.keepAcct=Оставить аккаунт
# Button to navigate to the next page.
button.nextPage=Далее
# Button to save information that has been entered.
button.save=Сохранить
# Button to execute a search to filter a client list.
button.search=Искать
# Button to select a client.
button.select=Выбрать
# Button to start an application on behalf of a client.
button.startApp=Заполнить заявку
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
# Some explanation text on the confirmation modal when deleting a client.
content.warning=Вы не отправляли заявок от этого клиента, поэтому ничего не будет утеряно.
# Shows there is no email address connected to the account.
content.noEmailAddress=(электронный адрес не указан)
# The status of an organization member when they have been enrolled but have never logged into CiviForm.
content.notLoggedIn=Пользователь не входил в аккаунт
# The status of an organization member when they have logged in but haven't applied on behalf of a client.
content.noApplications=Пользователь входил в аккаунт, но не подавал заявок
# The status of an organization member showing the date of the last application that they submitted for a client.
content.lastApplicationDate=В последний раз пользователь подал заявку {0}
# Confirmation modal text when the user tries to delete a client account.
dialog.deleteConfirmation=Вы действительно хотите удалить аккаунт этого клиента?

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
# Label showing the user when there is no name
label.unnamedUser=Пользователь без имени

link.edit=Изменить
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

# Help text at the top of the review page instructing applicants with how to start applying for programs.
content.reviewPageIntro=Чтобы приступить к подаче заявки, выберите шаг ниже. Вы можете сохранять свои ответы и возвращаться к заполнению в любое время.
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
title.commonIntakeSummary=Сводные данные об этой форме
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
# Title on the page after it has been determined that the applicant is not eligible for a program. This text includes the program name.
title.applicantNotEligible=Судя по ответам на следующие вопросы, вы не подходите под требования программы "{0}"
# Title on the page after it has been determined that the client is not eligible for a program, when someone else is filling out the application on a client''s behalf. This text includes the program name.
title.applicantNotEligibleTi=Судя по ответам на следующие вопросы, ваш клиент не подходит под требования программы "{0}"
# Section heading
heading.eligibilityCriteria=Подробнее о критериях допуска
# Text shown that allows the users to click on a program details link to find out more about the eligibility criteria for the program.
content.eligibilityCriteria=Чтобы ознакомиться с требованиями, перейдите на страницу {0}
# Describes how to learn more about eligibility criteria for a program. The variable text is "program details", which will become a hyperlink to another webpage.
content.eligibilityCriteria.v2=Чтобы узнать больше о критериях допуска к этой программе или связаться с ее кураторами, перейдите на страницу {0}.
# Text shown to explain what the user can do since they are not eligible for the program with their current answers.
content.changeAnswersForEligibility=Вы можете вернуться на предыдущую страницу, чтобы изменить предоставленные сведения, или подать заявку на участие в другой программе.
# Text shown on a webpage when the applicant is ineligible for a program.
content.changeAnswersForEligibility.v2=Если вы считаете, что произошла ошибка, вернитесь на предыдущую страницу и измените предоставленные сведения. Вы также можете перейти на главную страницу, чтобы подать заявку на участие в другой программе.
# Button for the applicant to go back and edit their responses.
button.goBackAndEdit=Вернуться и изменить сведения
# Clicking this button returns the user to the program summary page, where they can edit their responses.
button.editMyResponses=Изменить мои ответы
# Toast message that shows that a client is likely eligible for a program, when someone else is filling out the application on a client''s behalf.
toast.mayQualifyTi=Судя по ответам, ваш клиент подходит под требования программы "{0}". Чтобы продолжить, ответьте на остальные вопросы в заявке.
# Toast message that shows that an applicant is likely eligible for a program, based on their responses.
toast.mayQualify=Судя по ответам, вы подходите под требования программы "{0}". Чтобы продолжить, ответьте на остальные вопросы в заявке.
# Tag on the top of a program card, that lets the applicant know they may qualify for the program, based on their responses in other programs.
tag.mayQualify=Похоже, вы соответствуете условиям
# Tag on the top of a program card, that lets the person know their client may qualify for the program, based on their responses in other programs. This is in the case when someone is filling out applications on their client''s behalf.
tag.mayQualifyTi=Похоже, ваш клиент соответствует условиям
# Tag on the top of a program card, that lets the applicant know they are likely not eligible for the program, based on their responses in other programs.
tag.mayNotQualify=Похоже, вы не соответствуете условиям
# Tag on the top of a program card, that lets the person know their client is likely not eligible for the program, based on their responses in other programs. This is in the case when someone is filling out applications on their client''s behalf.
tag.mayNotQualifyTi=Похоже, ваш клиент не соответствует условиям
# Toast message that shows that an applicant may not be eligible for a program, based on their responses.
toast.mayNotQualify=Судя по ответам, вы не подходите под требования программы "{0}". Если сведения изменились, обновите форму и продолжите подачу заявки.
# Toast message that shows that a client may not be eligible for a program, when someone else is filling out the application on a client''s behalf.
toast.mayNotQualifyTi=Судя по ответам, ваш клиент не подходит под требования программы "{0}". Если его сведения изменились, обновите форму и продолжите подачу заявки.

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

# Shown to applicants to encourage login; {0} represents the authentication provider's name
content.pleaseCreateAccount=Чтобы вся указанная вами информация сохранилась и вы могли в любое время использовать ее для подачи будущих заявок, войдите в ваш аккаунт, зарегистрированный в сервисе "{0}". Если у вас нет аккаунта, вы можете создать его на странице входа.
# A message to show on the login prompt modal that encourages users to log in before applying to other programs.
content.generalLoginModalPrompt=Вы не вошли в аккаунт. Без него вы не сможете изменять созданные заявки и проверять их статус, а также быстро подавать новые для участия в других программах.
# A message to show on the login prompt modal that encourages users to log in before applying to a program from the programs index page.
content.initialLoginModalPrompt=Прежде чем продолжить, войдите в свой аккаунт, зарегистрированный в сервисе "{0}". Тогда вам не придется повторно вводить свою информацию при подаче нескольких заявок. Вы также сможете изменять заполненные заявки и проверять их статус. Если у вас нет аккаунта, вы можете его создать.
# A button for continuing to apply to other programs without an account.
button.continueWithoutAnAccount=Продолжить без входа в аккаунт
title.commonIntakeConfirmation=Программы, которые могут быть вам доступны
# Title on the page after a trusted intermediary has successfully filled out the common intake form.
title.commonIntakeConfirmationTi=Программы, которые могут быть доступны вашему клиенту
# A message explaining that the applicant may be eligible for the following list of programs, and that they need to apply to them.
content.commonIntakeConfirmation=Возможно, вы соответствуете критериям допуска к этим программам. Чтобы подать заявки, нажмите "Зарегистрироваться в программах" и заполните онлайн-формы.
# A message explaining that the applicant may be eligible for the following list of programs.
content.commonIntakeConfirmation.v2=На основе ваших ответов мы подобрали программы, которые могут быть вам доступны:
# A message explaining that the trusted intermediary's client may be eligible for the following list of programs, and that they need to apply to them.
content.commonIntakeConfirmationTi=Возможно, ваш клиент соответствует критериям допуска к этим программам. Чтобы подать заявки, нажмите "Зарегистрироваться в программах" и заполните онлайн-формы.
# A message explaining that the trusted intermediary's client may be eligible for the following list of programs.
content.commonIntakeConfirmationTi.v2=На основе ваших ответов мы подобрали программы, которые могут быть доступны вашему клиенту:
# A message explaining that there were no programs the applicant is currently eligible for. The {0} parameter is a link to another website, where the text is the name of that site. It may read "Access Arkansas", for example.
content.commonIntakeNoMatchingPrograms=Функция предварительного подбора не обнаружила программ, которые сейчас могут быть вам доступны. Однако вы в любое время можете подавать заявки, нажав "Зарегистрироваться в программах". Чтобы посмотреть дополнительные программы, перейдите на сайт {0}.
# A message explaining that there were no programs the trusted intermediary's client is currently eligible for. The {0} parameter is a link to another website, where the text is the name of that site. It may read "Access Arkansas", for example.
content.commonIntakeNoMatchingProgramsTi=Функция предварительного подбора не обнаружила программ, которые сейчас могут быть доступны вашему клиенту. Однако вы в любое время можете подавать заявки, нажав "Зарегистрироваться в программах". Чтобы посмотреть дополнительные программы, перейдите на сайт {0}.
# A message explaining a second option when there are no eligible programs, which is to edit your responses.
content.commonIntakeNoMatchingProgramsNextStep=Вы также можете вернуться на предыдущую страницу и изменить свои ответы.
# A header above a list of other programs the applicant might be interested in applying to.
content.otherProgramsToApplyFor=Другие программы, которые могут быть вам интересны
# Button on the "Application Submitted" page. Clicking it downloads the user's application.
button.downloadApplication=Скачать заявку
button.downloadPdf=Скачать в формате PDF
# A button prompting users to apply to programs.
button.applyToPrograms=Зарегистрироваться в программах
# Heading above a section showing the user's name, confirmation number, and date
heading.yourSubmissionInformation=Сведения о поданной вами заявке
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
# Informational "alert" title to encourage the user to create an account
alert.createAccount=Создайте аккаунт, чтобы сохранить сведения о заявке и свои данные
# Description text in an alert that explains how creating an account can help the user
alert.createAccountDescription=Зарегистрировавшись, вы сможете проверять статус отправленной заявки и быстрее заполнять формы для участия в других программах.
# Hyperlink to log in to an existing account
content.loginToExistingAccount=Войти в существующий аккаунт

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

label.countryCode=Страна
label.phoneNumber=Введите номер телефона
validation.phoneNumberRequired=Укажите номер телефона.
validation.phoneCountryCodeRequired=Выберите страну.
validation.phoneNumberMustContainNumbersOnly=Номер телефона должен состоять только из цифр.
validation.invalidPhoneNumberProvided=Недействительный номер телефона.
validation.phoneMustBeLocalToCountry=Указанный номер телефона не относится к выбранной стране.

#----------------------------------------------------------------------------------------------------------#
# DATE QUESTION - text shown when answering a question where a user must select date. #
#----------------------------------------------------------------------------------------------------------#

validation.invalidDateFormat=Укажите дату в правильном формате.
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
button.chooseFile=Выбрать файл
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
validation.numberRequired=Допускаются только цифры.

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

# Placeholder text - this is shown inside the input box, before a user enters an answer.
placeholder.firstName=Имя
placeholder.lastName=Фамилия
placeholder.middleName=Отчество/второе имя
# Placeholder for name suffix field in name question
placeholder.nameSuffix=Суффикс

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
error.internalServerDescription=Свяжитесь со службой технической поддержки по адресу %s и сообщите этот идентификатор ошибки: {0}.
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
# Text on a banner at the top of the page telling users this is an official government website
banner.northStarTitle=Официальный сайт правительства США.
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
banner.guestBannerText=Вы подаете заявку в качестве гостя. После того как вы ее отправите, {0}, чтобы защитить свои данные.
# A call to action within another message that will be a link to end your guest session.
banner.endYourSession=завершите сеанс

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

# A tag used to filter the list of programs down to those that are related to training.
category.tag.training=Обучение

# A tag used to filter the list of programs down to those that are related to transportation.
category.tag.transportation=Транспорт

# A tag used to filter the list of programs down to those that are related to utilities.
category.tag.utilities=Коммунальные услуги
