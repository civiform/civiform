import play.sbt.PlayRunHook
import sbt.File

import scala.sys.process.Process

/** Bundles js/css assets (not used in prod builds). When running locally in dev
  * mode will watch for file changes and automatically rebuild. When running in
  * CI will only run and build once.
  */
object BundledAssetBuilder {
  def apply(base: File): PlayRunHook = {
    object BundledAssetBuilderHook extends PlayRunHook {
      var watchProcess: Option[Process] = None

      override def beforeStarted(): Unit = {
        println("Running BundledAssetBuilder build...")

        val ci = sys.env.get("CI").map(_.toLowerCase)
        val isWatch = ci match {
          case Some("false") => true
          case _             => false
        }

        val buildCommand =
          if (isWatch) "npm run build:watch" else "npm run build"

        println(s"CI=${ci.getOrElse("not set")} - Running: $buildCommand")

        if (isWatch) {
          // Start watch process in background
          watchProcess = Some(Process(buildCommand, base).run())
          println("BundledAssetBuilder watch process started")
        } else {
          // Run build synchronously
          val exitCode = Process(buildCommand, base).!
          if (exitCode != 0) {
            throw new Exception(
              s"BundledAssetBuilder failed with exit code $exitCode"
            )
          }
          println("BundledAssetBuilder build completed successfully")
        }
      }

      override def afterStopped(): Unit = {
        watchProcess.foreach { process =>
          println("Stopping BundledAssetBuilder watch process...")
          process.destroy()
          watchProcess = None
        }
      }
    }

    BundledAssetBuilderHook
  }
}
