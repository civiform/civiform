package views.admin.apibridge.discovery;

import services.apibridge.ApiBridgeServiceDto;

public record DiscoveryDetailsViewModel(
    String hostUri, ApiBridgeServiceDto.DiscoveryResponse list) {}
