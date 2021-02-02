#!/usr/local/bin/amm
import ammonite.ops._
import ammonite.ops.ImplicitWd._

import scala.util.Try

val build = pwd/"build.sbt"

val plugins = pwd/"project"/"plugins.sbt"

@arg(doc = "Compiles project via Scala Native")
@main
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

@arg(doc = "Compiles project via GraalVM Native Image")
@main
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
      "--no-fallback",
      "-jar",
      "target/scala-2.13/NativeTest-assembly-0.1.jar",
      "target/scala-2.13/graal-compiled"
    )
    successfullyCompiled("GraalVM Native Image")
  }
}

@arg(doc = "Print help")
@main
def help(): Unit =
  println(
    """Usage: "./compile.sc <arg>", where <arg> is "native" or "graal" or "help".
      | "native" - compile project via Scala Native;
      | "graal" - compile project via GraalVM Native Image;
      | "help" - prints this help.
      |""".stripMargin)

def withBackup(func: (Path => Unit) => Unit): Unit = {
  import scala.collection.mutable
  val backups = mutable.HashMap.empty[Path, Path]

  def makeBackup(path: Path): Unit = {
    println(s"Create backup for $path")
    if(!exists(path)) {
      write(path, "")
    }
    val backup = Path(s"${path.wrapped.getParent}/${path.baseName}-backup.${path.ext}")
    cp(path, backup)
    backups.addOne(path, backup)
  }

  def applyBackup(backupInfo: (Path, Path)): Unit = {
    val (origin, backup) = backupInfo
    println(s"Apply backup for $origin")
    rm! origin
    mv(backup, origin)
    rm! backup
  }

  Try(func(makeBackup))

  backups.foreach(applyBackup)
}

def successfullyCompiled(via: String): Unit =
  println(s"Successfully compiled project into binary via $via.")

def buildModified(): Unit = println("Build modified successfully.")