package views.admin.apibridge.bridgeconfiguration;

import com.google.common.collect.ImmutableList;
import models.ApiBridgeConfigurationModel;

public record ListViewModel(ImmutableList<ApiBridgeConfigurationModel> list) {}
