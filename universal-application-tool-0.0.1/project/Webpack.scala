import play.sbt.PlayRunHook
import sbt.File

import scala.sys.process.Process

object Webpack {
  def apply(base: File): PlayRunHook = {
    object WebpackHook extends PlayRunHook {
      var process: Option[Process] = None

      override def beforeStarted() = {
        process = Option(
          Process("npx webpack", base).run()
        )
      }

      override def afterStarted() = {
        process = Option(
          Process("npx webpack --watch", base).run()
        )
      }

      override def afterStopped() = {
        process.foreach(_.destroy())
        process = None
      }
    }

    WebpackHook
  }
}
