import play.sbt.PlayRunHook
import sbt.File

import scala.sys.process.Process

object ViteDevServer {
  def apply(base: File): PlayRunHook = {
    object ViteDevServerHook extends PlayRunHook {
      var process: Option[Process] = None

      override def beforeStarted(): Unit = {
        println("Starting Vite dev server...")
        process = Some(Process("npm run dev", base).run())
      }

      override def afterStopped(): Unit = {
        println("Stopping Vite dev server...")
        process.foreach(_.destroy())
        process = None
      }
    }

    ViteDevServerHook
  }
}
