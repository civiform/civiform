package views.dev;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import services.geo.esri.EsriServiceAreaValidationOption;
import views.admin.BaseViewModel;

@Data
@Builder
public class AddressCheckerPageViewModel implements BaseViewModel {
  // Link to go back to dev tools
  private String backLinkUrl;
  // Address Correction feature enabled
  private boolean addressCorrectionEnabled;
  // Address Validation feature enabled
  private boolean addressValidationEnabled;
  // List of ESRI Find Address Candidates URLs
  private Optional<ImmutableList<String>> findAddressCandidatesUrls;
  // List of ESRI Address Service Area Validation URLs
  private Optional<ImmutableList<String>> addressServiceAreaValidationUrls;
  // Map of service area validation config options (id -> option)
  private Map<String, EsriServiceAreaValidationOption> serviceAreaValidationConfigMap;

  // Form action URLs
  private String correctAddressActionUrl;
  private String checkServiceAreaActionUrl;
}
