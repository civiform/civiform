package views.dev;

import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

import lombok.Builder;
import lombok.Data;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
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
}
