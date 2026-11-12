import play.sbt.PlayRunHook
import sbt.File

import scala.sys.process.Process

// Starts bundler dev server (not used in prod builds)
object BundlerDevServer {
  def apply(base: File): PlayRunHook = {
    object DevServerHook extends PlayRunHook {
      var process: Option[Process] = None

      override def beforeStarted(): Unit = {
        println("Starting Bundler dev server...")
        process = Some(Process("npm run dev", base).run())
      }

      override def afterStopped(): Unit = {
        println("Stopping Bundler dev server...")
        stop()
      }

      // Play calls onError, not afterStopped, when the server fails to start.
      override def onError(): Unit = stop()

      private def stop(): Unit = {
        process.foreach(_.destroy())
        process = None
      }
    }

    DevServerHook
  }
}
