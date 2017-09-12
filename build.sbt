lazy val scalaJsonRpc = project
  .in(file("scala-json-rpc"))
  .settings(
    name := "scala-json-rpc"
  )
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.6.3",
      "org.scalatest"     %% "scalatest" % "3.0.4" % Test
    ))

lazy val playJsonRpc = project
  .in(file("play-json-rpc"))
  .settings(
    name := "play-json-rpc"
  )
  .dependsOn(scalaJsonRpc)

lazy val root = project
  .in(file("."))
  .settings(publish := {})
  .settings(
    name := "scala-json-rpc-root"
  )
  .aggregate(
    scalaJsonRpc,
    playJsonRpc
  )
