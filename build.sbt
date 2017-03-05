lazy val commonSettings = Seq(
    scalaVersion := "2.11.8",
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
  ) ++
    addCommandAlias("validate", ";scalafmtTest; coverage; test; coverageReport") ++
    addCommandAlias("validateAggregate", ";coverageAggregate")

lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/dhpcs/play-json-rpc/")),
  startYear := Some(2015),
  description := "A Scala library providing implicit play-json Formats for JSON-RPC 2.0 messages",
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
      browseUrl = url("https://github.com/dhpcs/play-json-rpc/"),
      connection = "scm:git:https://github.com/dhpcs/play-json-rpc.git",
      devConnection = Some("scm:git:git@github.com:dhpcs/play-json-rpc.git")
    )),
  bintrayOrganization := Some("dhpcs"),
  bintrayPackageLabels := Seq("scala", "json-rpc")
)

lazy val noopPublishSettings = Seq(
  publish := {},
  publishLocal := {}
)

lazy val playJson = "com.typesafe.play" %% "play-json" % "2.5.12"

lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1"

lazy val playJsonTestkit = project
  .in(file("testkit"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "play-json-testkit"
  )
  .settings(
    libraryDependencies ++= Seq(
      playJson,
      scalaTest
    ))
  .settings(
    coverageEnabled := false
  )

lazy val playJsonRpc = project
  .in(file("rpc"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "play-json-rpc"
  )
  .settings(libraryDependencies ++= Seq(
    playJson
  ))
  .dependsOn(playJsonTestkit % Test)
  .settings(libraryDependencies ++= Seq(
    scalaTest % Test
  ))

lazy val playJsonRpcMarshalling = project
  .in(file("marshalling"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "play-json-rpc-marshalling"
  )
  .dependsOn(playJsonRpc)
  .settings(
    coverageEnabled := false
  )

lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .settings(noopPublishSettings)
  .settings(
    name := "play-json-rpc-root"
  )
  .aggregate(
    playJsonTestkit,
    playJsonRpc,
    playJsonRpcMarshalling
  )
