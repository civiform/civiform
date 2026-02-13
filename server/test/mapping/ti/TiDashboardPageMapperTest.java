package mapping.ti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import models.AccountModel;
import models.TrustedIntermediaryGroupModel;
import org.junit.Before;
import org.junit.Test;
import repository.SearchParameters;
import services.DateConverter;
import services.pagination.PaginationInfo;
import services.program.ProgramService;
import views.trustedintermediary.TiDashboardPageViewModel;

public final class TiDashboardPageMapperTest {

  private TiDashboardPageMapper mapper;
  private TrustedIntermediaryGroupModel mockTiGroup;
  private DateConverter mockDateConverter;
  private ProgramService mockProgramService;

  @Before
  public void setup() {
    mockDateConverter = mock(DateConverter.class);
    mockProgramService = mock(ProgramService.class);
    mapper = new TiDashboardPageMapper(mockDateConverter, mockProgramService);
    mockTiGroup = mock(TrustedIntermediaryGroupModel.class);
    mockTiGroup.id = 5L;
    when(mockTiGroup.getName()).thenReturn("Test TI Group");
  }

  private SearchParameters emptySearchParams() {
    return SearchParameters.builder()
        .setNameQuery(Optional.empty())
        .setDayQuery(Optional.empty())
        .setMonthQuery(Optional.empty())
        .setYearQuery(Optional.empty())
        .build();
  }

  @Test
  public void map_setsGroupInfo() {
    PaginationInfo<AccountModel> pageInfo = PaginationInfo.paginate(ImmutableList.of(), 10, 1);

    TiDashboardPageViewModel result =
        mapper.map(
            mockTiGroup,
            pageInfo,
            emptySearchParams(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThat(result.getTiGroupName()).isEqualTo("Test TI Group");
    assertThat(result.getTiGroupId()).isEqualTo(5L);
  }

  @Test
  public void map_noClients_returnsEmptyList() {
    PaginationInfo<AccountModel> pageInfo = PaginationInfo.paginate(ImmutableList.of(), 10, 1);

    TiDashboardPageViewModel result =
        mapper.map(
            mockTiGroup,
            pageInfo,
            emptySearchParams(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThat(result.getClients()).isEmpty();
    assertThat(result.getTotalClients()).isEqualTo(0);
  }

  @Test
  public void map_setsPaginationInfo() {
    PaginationInfo<AccountModel> pageInfo = PaginationInfo.paginate(ImmutableList.of(), 10, 1);

    TiDashboardPageViewModel result =
        mapper.map(
            mockTiGroup,
            pageInfo,
            emptySearchParams(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThat(result.getPage()).isEqualTo(1);
    assertThat(result.getTotalPageCount()).isEqualTo(1);
  }

  @Test
  public void map_setsSearchQueries() {
    PaginationInfo<AccountModel> pageInfo = PaginationInfo.paginate(ImmutableList.of(), 10, 1);

    TiDashboardPageViewModel result =
        mapper.map(
            mockTiGroup,
            pageInfo,
            emptySearchParams(),
            Optional.of("John"),
            Optional.of("15"),
            Optional.of("06"),
            Optional.of("1990"));

    assertThat(result.getSearchNameQuery()).isEqualTo("John");
    assertThat(result.getSearchDayQuery()).isEqualTo("15");
    assertThat(result.getSearchMonthQuery()).isEqualTo("06");
    assertThat(result.getSearchYearQuery()).isEqualTo("1990");
  }

  @Test
  public void map_emptySearchQueries_defaultsToEmpty() {
    PaginationInfo<AccountModel> pageInfo = PaginationInfo.paginate(ImmutableList.of(), 10, 1);

    TiDashboardPageViewModel result =
        mapper.map(
            mockTiGroup,
            pageInfo,
            emptySearchParams(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThat(result.getSearchNameQuery()).isEmpty();
    assertThat(result.getSearchDayQuery()).isEmpty();
    assertThat(result.getSearchMonthQuery()).isEmpty();
    assertThat(result.getSearchYearQuery()).isEmpty();
  }

  @Test
  public void map_setsUrls() {
    PaginationInfo<AccountModel> pageInfo = PaginationInfo.paginate(ImmutableList.of(), 10, 1);

    TiDashboardPageViewModel result =
        mapper.map(
            mockTiGroup,
            pageInfo,
            emptySearchParams(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThat(result.getDashboardUrl()).isNotEmpty();
    assertThat(result.getClearSearchUrl()).isNotEmpty();
    assertThat(result.getAccountSettingsUrl()).isNotEmpty();
  }

  @Test
  public void map_emptySearch_isValidSearch() {
    PaginationInfo<AccountModel> pageInfo = PaginationInfo.paginate(ImmutableList.of(), 10, 1);

    TiDashboardPageViewModel result =
        mapper.map(
            mockTiGroup,
            pageInfo,
            emptySearchParams(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThat(result.isValidSearch()).isTrue();
  }
}
