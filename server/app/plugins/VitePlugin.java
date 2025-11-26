package plugins;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import play.Environment;
import play.inject.ApplicationLifecycle;

@Slf4j
@Singleton
public class VitePlugin {
  private Process viteProcess;

  @Inject
  public VitePlugin(Config config, Environment environment, ApplicationLifecycle lifecycle) {
    checkNotNull(config);
    checkNotNull(environment);
    checkNotNull(lifecycle);

    boolean isViteEnabled = environment.isDev() && config.getBoolean("vite.enabled");

    if (!isViteEnabled) {
      return;
    }

    startViteDevServer();

    // Register shutdown hook
    lifecycle.addStopHook(
        () -> {
          stopViteDevServer();
          return CompletableFuture.completedFuture(null);
        });
  }

  private void startViteDevServer() {
    try {
      log.info("Starting Vite dev server...");

      ProcessBuilder pb = new ProcessBuilder("npm", "run", "dev");
      pb.directory(new File(System.getProperty("user.dir")));

      // Inherit IO - this will directly show Vite output in Play's console
      pb.inheritIO();

      viteProcess = pb.start();

      // Give Vite a moment to start
      Thread.sleep(2000);

      if (viteProcess.isAlive()) {
        log.info("Vite dev server started successfully");
      } else {
        log.error("Vite process died immediately, exit code: {}", viteProcess.exitValue());
      }

    } catch (Exception e) {
      log.error("Failed to start Vite dev server", e);
    }
  }

  private void stopViteDevServer() {
    if (viteProcess != null && viteProcess.isAlive()) {
      log.info("Stopping Vite dev server...");
      viteProcess.destroy();
      try {
        if (!viteProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
          log.warn("Vite did not stop gracefully, forcing shutdown...");
          viteProcess.destroyForcibly();
        }
        log.info("Vite dev server stopped");
      } catch (InterruptedException e) {
        log.error("Interrupted while stopping Vite", e);
        viteProcess.destroyForcibly();
      }
    }
  }
}
