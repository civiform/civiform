package services;

import static com.google.common.base.Preconditions.checkNotNull;

import controllers.AssetsFinder;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/** Wrapper around Play's ${@link AssetsFinder} that handles picking paths from the bundler tool. */
@Singleton
@Slf4j
public final class BundledAssetsFinder {
  private final AssetsFinder assetsFinder;

  @Inject
  public BundledAssetsFinder(AssetsFinder assetsFinder) {
    this.assetsFinder = checkNotNull(assetsFinder);
  }

  public String path(String path) {
    return assetsFinder.path(path);
  }
}
