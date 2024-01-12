inThisBuild(
  List(
    organization := "app.broad",
    homepage     := Some(url("https://juliano-alves.com/")),
    developers := List(
      Developer("juliano", "Juliano Alves", "von.juliano@gmail.com", url("https://juliano-alves.com/"))
    ),
    scalafmtCheck    := true,
    scalafmtSbtCheck := true
  )
)

lazy val root = project
  .in(file("."))
  .settings(
    name                       := "rinha-scala",
    version                    := "0.1.0",
    scalaVersion               := "3.3.1",
    assembly / assemblyJarName := "rinha.jar",
    libraryDependencies ++= Seq(
      "dev.zio"       %% "zio"            % "2.0.17",
      "dev.zio"       %% "zio-json"       % "0.6.2",
      "dev.zio"       %% "zio-http"       % "3.0.0-RC2",
      "dev.zio"       %% "zio-prelude"    % "1.0.0-RC20",
      "io.getquill"   %% "quill-jdbc-zio" % "4.6.0.1",
      "org.postgresql" % "postgresql"     % "42.6.0",
      "dev.zio"       %% "zio-redis"      % "0.2.0"
    ),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) =>
        xs match {
          case "MANIFEST.MF" :: Nil => MergeStrategy.discard
          case _                    => MergeStrategy.first
        }
      case x => MergeStrategy.first
    }
  )
