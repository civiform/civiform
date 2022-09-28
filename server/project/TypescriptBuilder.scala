import com.typesafe.sbt.web.Import.WebKeys.webTarget
import com.typesafe.sbt.web.{PathMapping, incremental}
import com.typesafe.sbt.web.pipeline.Pipeline
import sbt.Keys.streams
import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.web.incremental.{
  OpInputHash,
  OpInputHasher,
  OpResult,
  OpSuccess
}
import sbt.internal.util.ManagedLogger
import sbt.{AutoPlugin, File, Setting, sbtUnchecked, taskKey}

import scala.sys.process.{Process, ProcessLogger}

/** Custom plugin to compile TypeScript code. We are not using sbt-typescript
  * https://github.com/joost-de-vries/sbt-typescript plugin because it hasn't
  * been updated since 2018 and uses Typescript 2 while current Typescript
  * version is 4.
  *
  * Our custom plugin invokes `tsc` compiler directly. `tsconfig.json` is setup
  * to compile code from `app/assets/javascripts` directory and outputs result
  * to `target/web/typescript/javascripts` folder.
  *
  * This plugin introduces `compileTypescript` task which will be used as a
  * pipeline stage. Read about Asset Pipeline tasks
  * https://github.com/sbt/sbt-web#asset-pipeline The task implements Asset
  * Pipeline task and not Source File task because the latter doesn't allow us
  * to keep ∂irectory structure: we want that file `assets/javascripts/foo.ts`
  * be compiled to `assets/javascripts/foo.js`. So we are using Pipeline.Stage
  * for that.
  *
  * This plugin is enabled by adding `compileTypescript` as first task in
  * pipelineStages in build.sbt.
  */
object TypescriptBuilder extends AutoPlugin {
  override def trigger = allRequirements

  object autoImport {
    val compileTypescript = taskKey[Pipeline.Stage](
      "Compiles typescript from assets/javascript folder"
    )
  }
  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    compileTypescript := { inputFiles =>
      val streamsVal = (Assets / streams).value: @sbtUnchecked
      // dir for compiled JS files
      val targetDir = new File(webTarget.value, "typescript/javascripts")
      val cacheDir = new File(streamsVal.cacheDirectory, "run")
      recompileTypescriptIfFilesChanged(
        inputFiles,
        targetDir,
        cacheDir,
        streamsVal.log
      )
      // get list of compiled JS files and maps from targetDir folder and return them together with input files.
      val jsFiles: Seq[PathMapping] =
        targetDir.listFiles().map({ f => (f, "javascripts/" + f.getName) })
      inputFiles ++ jsFiles
    }
  )

  def recompileTypescriptIfFilesChanged(
    inputFiles: Seq[(File, String)],
    targetDir: File,
    cacheDir: File,
    log: ManagedLogger
  ) = {
    // function that provides hash for each file. Includes fileName + timestamp.
    val fileHasher: OpInputHasher[File] =
      OpInputHasher[File](f =>
        OpInputHash.hashString(f.getCanonicalPath + f.lastModified)
      )
    // we are interested only in TS files.
    val tsFiles = inputFiles map { p => p._1 } filter { f =>
      f.getName.endsWith(".ts")
    }
    // we are using sbt-web syncIncremental function. It's somewhat complicated. For our
    // use case we just want to see if any of TS files changed and if so - recompile
    // everything.
    incremental.syncIncremental(cacheDir, tsFiles)({ modifiedFiles =>
      if (modifiedFiles.nonEmpty) {
        log.info("Typescript files changed. Recompiling...")
        compileTypescriptInternal(targetDir, log)
      }
      // ignore opResults. We are not using full functionality of syncIncremental
      // so we always return dummy opResults with OpSuccess for each TS file.
      val opResults: Map[File, OpResult] = modifiedFiles
        .map(f => f -> OpSuccess(Set.empty[File], Set.empty[File]))
        .toMap
      (opResults, ())
    })(fileHasher)
  }

  def compileTypescriptInternal(targetDir: File, log: ManagedLogger) = {
    val compilationCommand =
      "npx tsc --pretty --project tsconfig.json --outDir " + targetDir
    val res = Process(compilationCommand) ! ProcessLogger(
      line => log.error(line),
      line => log.error(line)
    )
    if (res != 0) {
      log.info(
        "To debug TS code compilation can run the following command from 'server' folder:\n    " + compilationCommand
      )
      throw new sbt.MessageOnlyException(
        "TypeScript compilation failed. Check console to see compilation errors."
      )
    }
  }
}
