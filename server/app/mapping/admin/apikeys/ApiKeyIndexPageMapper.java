package mapping.admin.apikeys;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import models.ApiKeyModel;
import services.DateConverter;
import views.admin.apikeys.ApiKeyIndexPageViewModel;

/** Maps data to the ApiKeyIndexPageViewModel. */
public final class ApiKeyIndexPageMapper {

  public ApiKeyIndexPageViewModel map(
      String selectedStatus,
      ImmutableList<ApiKeyModel> apiKeys,
      ImmutableSet<String> allProgramNames,
      DateConverter dateConverter) {
    ImmutableMap<String, String> programSlugToName =
        ApiKeyIndexPageViewModel.buildProgramSlugToName(allProgramNames);

    ImmutableList<ApiKeyIndexPageViewModel.ApiKeyData> apiKeyDataList =
        apiKeys.stream()
            .map(
                apiKey ->
                    ApiKeyIndexPageViewModel.buildApiKeyData(
                        apiKey, programSlugToName, dateConverter))
            .collect(ImmutableList.toImmutableList());

    return ApiKeyIndexPageViewModel.builder()
        .selectedStatus(selectedStatus)
        .apiKeys(apiKeyDataList)
        .build();
  }
}
