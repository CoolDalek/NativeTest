#!/usr/local/bin/amm

import java.time.Instant

import ammonite.ops._
import ammonite.ops.ImplicitWd._

import scala.util.Try

val build = pwd/"build.sbt"

val plugins = pwd/"project"/"plugins.sbt"

@main(
  doc = "Compiles project via Scala Native"
)
def native(): Unit = {
  val toBuild =
    """
      |
      |import scala.scalanative.build._
      |
      |enablePlugins(ScalaNativePlugin)
      |
      |nativeConfig ~= {
      |  _.withMode(Mode.releaseFull)
      |}
      |""".stripMargin
  val toPlugins =
    """
      |
      |addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.0")
      |""".stripMargin
  withBackup { backup =>
    backup(build)
    backup(plugins)
    write.append(build, toBuild)
    write.append(plugins, toPlugins)
    buildModified()
    %sbt "clean; nativeLink"
    successfullyCompiled("ScalaNative")
  }
}

@main(
  doc = "Compiles project via GraalVM Native Image"
)
def graal(): Unit = {
  val toPlugins =
    """
      |
      |addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")
      |""".stripMargin
  withBackup { backup =>
    backup(plugins)
    write.append(plugins, toPlugins)
    buildModified()
    %sbt "clean; assembly"
    println("Successfully created fat jar.")
    %.applyDynamic("native-image")(
      "-H:CompilerBackend=llvm",
      "-H:Optimize=2",
      "-H:Features=org.graalvm.home.HomeFinderFeature",
      "-H:+SpawnIsolates",
      "--no-fallback",
      "-jar",
      "target/scala-2.13/NativeTest-assembly-0.1.jar",
      "target/scala-2.13/graal-compiled"
    )
    successfullyCompiled("GraalVM Native Image")
  }
}

@main(
  doc = "Print help."
)
def help(): Unit =
  println(
    """Usage: "./compile.sc <arg>", where <arg> is "native" or "graal" or "help".
      | "native" - compile project via Scala Native;
      | "graal" - compile project via GraalVM Native Image;
      | "help" - prints this help.
      |""".stripMargin)

def withBackup(func: (Path => Path) => Unit): Unit = {
  import scala.collection.mutable
  val backups = mutable.HashMap.empty[Path, Path]

  def makeBackup(path: Path): Path = {
    println(s"Create backup for $path")
    if(!exists(path)) {
      write(path, "")
    }
    val backup = Path(s"${path.wrapped.getParent}/${path.baseName}-backup.${path.ext}")
    cp(path, backup)
    backups.addOne(path, backup)
    path
  }

  def applyBackup(backupInfo: (Path, Path)): Unit = {
    val (origin, backup) = backupInfo
    println(s"Apply backup for $origin")
    rm! origin
    mv(backup, origin)
    rm! backup
  }

  Try(func(makeBackup)).recover {
    case e =>
      println("Something goes wrong, make log file with stacktrace.")
      write(
        pwd/s"err-${Instant.now()}.log",
        s"$e\n${e.getStackTrace.mkString("\n")}"
      )
  }

  backups.foreach(applyBackup)
}

def successfullyCompiled(via: String): Unit =
  println(s"Successfully compiled project into binary via $via.")

def buildModified(): Unit = println("Build modified successfully.")