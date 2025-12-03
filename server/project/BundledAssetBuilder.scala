import play.sbt.PlayRunHook
import sbt.File

import scala.sys.process.Process

// Bundles js/css assets (not used in prod builds)
object BundledAssetBuilder {
  def apply(base: File): PlayRunHook = {
    object BundledAssetBuilderHook extends PlayRunHook {
      override def beforeStarted() = {
        println("Running BundledAssetBuilder build...")
        val exitCode = Process("npm run build", base).!

        if (exitCode != 0) {
          throw new Exception(
            s"BundledAssetBuilder failed with exit code $exitCode"
          )
        }
        println("BundledAssetBuilder build completed successfully")
      }
    }

    BundledAssetBuilderHook
  }
}
