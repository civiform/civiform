package mapping.ti;

import com.google.common.collect.ImmutableList;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationModel;
import models.LifecycleStage;
import models.TrustedIntermediaryGroupModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.SearchParameters;
import services.DateConverter;
import services.PhoneValidationResult;
import services.PhoneValidationUtils;
import services.pagination.PaginationInfo;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.ti.TrustedIntermediaryService;
import views.trustedintermediary.TiDashboardPageViewModel;
import views.trustedintermediary.TiDashboardPageViewModel.ClientRow;

/** Maps TI dashboard data to the TiDashboardPageViewModel. */
public final class TiDashboardPageMapper {

  private static final Logger logger = LoggerFactory.getLogger(TiDashboardPageMapper.class);
  private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

  private final DateConverter dateConverter;
  private final ProgramService programService;

  public TiDashboardPageMapper(DateConverter dateConverter, ProgramService programService) {
    this.dateConverter = dateConverter;
    this.programService = programService;
  }

  public TiDashboardPageViewModel map(
      TrustedIntermediaryGroupModel tiGroup,
      PaginationInfo<AccountModel> pageInfo,
      SearchParameters searchParameters,
      Optional<String> nameQuery,
      Optional<String> dayQuery,
      Optional<String> monthQuery,
      Optional<String> yearQuery) {
    List<ClientRow> clientRows =
        pageInfo.getPageItems().stream()
            .sorted(Comparator.comparing(AccountModel::getApplicantDisplayName))
            .map(this::buildClientRow)
            .collect(Collectors.toList());

    return TiDashboardPageViewModel.builder()
        .tiGroupName(tiGroup.getName())
        .tiGroupId(tiGroup.id)
        .clients(clientRows)
        .page(pageInfo.getPage())
        .totalPageCount(pageInfo.getPageCount())
        .searchNameQuery(nameQuery.orElse(""))
        .searchDayQuery(dayQuery.orElse(""))
        .searchMonthQuery(monthQuery.orElse(""))
        .searchYearQuery(yearQuery.orElse(""))
        .isValidSearch(TrustedIntermediaryService.validateSearch(searchParameters))
        .totalClients(pageInfo.getPageItems().size())
        .build();
  }

  private ClientRow buildClientRow(AccountModel account) {
    Optional<ApplicantModel> newestApplicant = account.representativeApplicant();

    String dateOfBirth = "";
    Optional<String> phoneNumber = Optional.empty();
    Optional<String> email = Optional.empty();
    int applicationCount = 0;
    String programNames = "";
    Optional<Long> applicantId = Optional.empty();

    if (newestApplicant.isPresent()) {
      ApplicantModel applicant = newestApplicant.get();
      dateOfBirth = applicant.getDateOfBirth().map(dateConverter::formatIso8601Date).orElse("");
      phoneNumber = applicant.getPhoneNumber().map(this::formatPhone);
      email = applicant.getEmailAddress();
      applicantId = Optional.of(applicant.id);

      ImmutableList<ApplicationModel> submittedApplications =
          applicant.getApplications().stream()
              .filter(app -> app.getLifecycleStage() == LifecycleStage.ACTIVE)
              .collect(ImmutableList.toImmutableList());
      applicationCount = submittedApplications.size();
      programNames =
          submittedApplications.stream()
              .map(
                  app -> {
                    try {
                      return programService
                          .getFullProgramDefinition(app.getProgram().id)
                          .localizedName()
                          .getDefault();
                    } catch (ProgramNotFoundException e) {
                      logger.error(
                          "Unable to build complete string of programs. At least one program was"
                              + " not found",
                          e);
                      return "<unknown>";
                    }
                  })
              .collect(Collectors.joining(", "));
    }

    return ClientRow.builder()
        .displayName(account.getApplicantDisplayName())
        .accountId(account.id)
        .applicantId(applicantId)
        .dateOfBirth(dateOfBirth)
        .phoneNumber(phoneNumber)
        .email(email)
        .applicationCount(applicationCount)
        .programNames(programNames)
        .tiNote(account.getTiNote())
        .build();
  }

  private String formatPhone(String phone) {
    try {
      PhoneValidationResult phoneValidationResult =
          PhoneValidationUtils.determineCountryCode(Optional.ofNullable(phone));
      Phonenumber.PhoneNumber phoneNumber =
          PHONE_NUMBER_UTIL.parse(phone, phoneValidationResult.getCountryCode().orElse(""));
      return PHONE_NUMBER_UTIL.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
    } catch (NumberParseException e) {
      return "-";
    }
  }
}
