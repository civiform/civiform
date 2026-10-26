package views.admin.programs;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** ViewModel for the manage program admins page (Thymeleaf). */
@Data
@Builder
public final class ManageProgramAdminsPageViewModel implements BaseViewModel {

  // Admin name of the program whose admins are being managed; the template
  // renders it after the "Manage admins for program: " heading prefix.
  private final String programAdminName;

  // Target of the "Back" link (the programs index page)
  private final String backUrl;

  // Target of the add-admin form
  private final String addAdminUrl;

  // Target of every row's delete form
  private final String deleteAdminUrl;

  // Email addresses of the program's current admins, in repository order
  private final ImmutableList<String> existingAdminEmails;

  // Error toast text with the legacy "Error: " prefix already applied; null when
  // there is no error to show.
  private final String errorMessage;

  // Random id for the error toast, null unless errorMessage is set.
  private final String errorToastId;
}
