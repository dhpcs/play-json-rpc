import bintray.BintrayPlugin.autoImport.{bintrayOrganization, bintrayPackageLabels}
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin.autoImport.releaseCrossBuild

object CommonProjectSettingsPlugin extends AutoPlugin {

  private lazy val scalaSettings = Seq(
    scalaVersion := "2.12.2",
    crossScalaVersions := Seq("2.11.11", "2.12.2"),
    // See https://tpolecat.github.io/2017/04/25/scalac-flags.html for explanations. 2.11 doesn't support all of these,
    // so we simply don't set any of them when building for 2.11. The 2.12 build will pick up any issues anyway.
    scalacOptions in Compile ++= (CrossVersion.binaryScalaVersion(scalaVersion.value) match {
      case "2.12" =>
        Seq(
          "-deprecation",
          "-encoding",
          "utf-8",
          "-explaintypes",
          "-feature",
          "-unchecked",
          "-Xfatal-warnings",
          "-Xlint:adapted-args",
          "-Xlint:by-name-right-associative",
          "-Xlint:constant",
          "-Xlint:delayedinit-select",
          "-Xlint:doc-detached",
          "-Xlint:inaccessible",
          "-Xlint:infer-any",
          "-Xlint:missing-interpolator",
          "-Xlint:nullary-override",
          "-Xlint:nullary-unit",
          "-Xlint:option-implicit",
          "-Xlint:package-object-classes",
          "-Xlint:poly-implicit-overload",
          "-Xlint:private-shadow",
          "-Xlint:stars-align",
          "-Xlint:type-parameter-shadow",
          "-Xlint:unsound-match",
          "-Yno-adapted-args",
          "-Ywarn-dead-code",
          "-Ywarn-extra-implicit",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit",
          "-Ywarn-numeric-widen",
          "-Ywarn-unused:implicits",
          "-Ywarn-unused:imports",
          "-Ywarn-unused:locals",
          "-Ywarn-unused:params",
          "-Ywarn-unused:patvars",
          "-Ywarn-unused:privates",
          "-Ywarn-value-discard"
        )
      case "2.11" => Seq("-encoding", "utf-8")
    })
  )

  private lazy val resolverSettings = Seq(
    conflictManager := ConflictManager.strict
  )

  private lazy val publishSettings = Seq(
    homepage := Some(url("https://github.com/dhpcs/scala-json-rpc/")),
    startYear := Some(2015),
    description := "A Scala library providing types and JSON format typeclass instances for JSON-RPC 2.0 messages along with support for marshalling application level commands, responses and notifications via JSON-RPC 2.0.",
    licenses += "Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"),
    organization := "com.dhpcs",
    organizationHomepage := Some(url("https://www.dhpcs.com/")),
    organizationName := "dhpcs",
    developers := List(
      Developer(
        id = "dhpiggott",
        name = "David Piggott",
        email = "david@piggott.me.uk",
        url = url("https://dhpiggott.net/")
      )),
    scmInfo := Some(
      ScmInfo(
        browseUrl = url("https://github.com/dhpcs/scala-json-rpc/"),
        connection = "scm:git:https://github.com/dhpcs/scala-json-rpc.git",
        devConnection = Some("scm:git:git@github.com:dhpcs/scala-json-rpc.git")
      )),
    bintrayOrganization := Some("dhpcs"),
    bintrayPackageLabels := Seq("scala", "json-rpc"),
    releaseCrossBuild := true
  )

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Setting[_]] =
    scalaSettings ++
      resolverSettings ++
      addCommandAlias("validate", ";scalafmtTest; test") ++
      publishSettings

}
