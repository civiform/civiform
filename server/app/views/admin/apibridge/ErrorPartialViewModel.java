package views.admin.apibridge;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import views.admin.BaseViewModel;

/** Contains all the properties for rendering the DiscoveryDetailsPartial.html */
@Builder
public record ErrorPartialViewModel(ImmutableList<String> errors) implements BaseViewModel {}
