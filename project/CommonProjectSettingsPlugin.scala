import bintray.BintrayPlugin.autoImport.{bintrayOrganization, bintrayPackageLabels}
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin.autoImport.releaseCrossBuild

object CommonProjectSettingsPlugin extends AutoPlugin {

  private[this] val scalaSettings = Seq(
    scalaVersion := "2.12.2",
    crossScalaVersions := Seq("2.11.11", "2.12.2"),
    scalacOptions in Compile ++= Seq(
      "-target:jvm-1.8",
      "-encoding",
      "UTF-8",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xfuture",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-inaccessible",
      "-Ywarn-infer-any",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused",
      "-Ywarn-unused-import"
    )
  )

  private[this] val publishSettings = Seq(
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

  override lazy val projectSettings: Seq[Setting[_]] =
    scalaSettings ++
      addCommandAlias("validate", ";scalafmtTest; coverage; test; coverageReport") ++
      addCommandAlias("validateAggregate", ";coverageAggregate") ++
      publishSettings

}
