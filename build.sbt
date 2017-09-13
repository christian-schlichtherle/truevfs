/*
 * Copyright © 2017 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import BuildSettings._
import Dependencies._

lazy val root: Project = project
  .in(file("."))
  .aggregate(access, accessSwing, comp, driver, ext, it, kernel, profile, samples)
  .settings(releaseSettings)
  .settings(aggregateSettings)

lazy val access: Project = project
  .in(file("truevfs-access"))
  .dependsOn(driverFile % Runtime, kernelSpec)
  .settings(javaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      JunitInterface % Test,
      MockitoCore % Test,
      Scalacheck % Test,
      Scalatest % Test
    ),
    normalizedName := "truevfs-access"
  )

lazy val accessSwing: Project = project
  .in(file("truevfs-access-swing"))
  .dependsOn(access % "compile;runtime->runtime")
  .settings(javaLibrarySettings)
  .settings(
    normalizedName := "truevfs-access-swing"
  )

lazy val comp: Project = project
  .in(file("truevfs-comp"))
  .aggregate(
    compIbm437,
    compInst,
    compJmx,
    compTarDriver,
    compZip,
    compZipDriver
  ).settings(aggregateSettings)

lazy val compIbm437: Project = project
  .in(file("truevfs-comp/truevfs-comp-ibm437"))
  .settings(javaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      JunitInterface % Test,
      TruecommonsAnnotations
    ),
    normalizedName := "truevfs-comp-ibm437"
  )

lazy val compInst: Project = project
  .in(file("truevfs-comp/truevfs-comp-inst"))
  .dependsOn(kernelSpec)
  .settings(javaLibrarySettings)
  .settings(
    normalizedName := "truevfs-comp-inst"
  )

lazy val compJmx: Project = project
  .in(file("truevfs-comp/truevfs-comp-jmx"))
  .dependsOn(compInst)
  .settings(javaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      JunitInterface % Test,
      Scalatest % Test,
      TruecommonsJMX
    ),
    normalizedName := "truevfs-comp-jmx"
  )

lazy val compTarDriver: Project = project
  .in(file("truevfs-comp/truevfs-comp-tardriver"))
  .dependsOn(kernelSpec)
  .settings(javaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      CommonsCompress
    ),
    normalizedName := "truevfs-comp-tardriver"
  )

lazy val compZip: Project = project
  .in(file("truevfs-comp/truevfs-comp-zip"))
  .settings(javaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      BcprovJdk15on,
      CommonsCompress,
      FindbugsAnnotations,
      JunitInterface % Test,
      Scalatest % Test,
      TruecommonsIO classifier "",
      TruecommonsIO % Test classifier "tests",
      TruecommonsKeySpec,
      TruecommonsShed,
      TruecommonsShed % Test classifier "tests"
    ),
    normalizedName := "truevfs-comp-zip"
  )

lazy val compZipDriver: Project = project
  .in(file("truevfs-comp/truevfs-comp-zipdriver"))
  .dependsOn(compZip, kernelSpec)
  .settings(javaLibrarySettings)
  .settings(
    normalizedName := "truevfs-comp-zipdriver"
  )

lazy val driver: Project = project
  .in(file("truevfs-driver"))
  .aggregate(
    driverFile,
    driverHttp,
    driverJar,
    driverOdf,
    driverSfx,
    driverTar,
    driverTarBzip2,
    driverTarGzip,
    driverTarXz,
    driverZip,
    driverZipRaes
  ).settings(aggregateSettings)

lazy val driverFile: Project = project
  .in(file("truevfs-driver/truevfs-driver-file"))
  .dependsOn(kernelSpec)
  .settings(javaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      JunitInterface % Test
    ),
    normalizedName := "truevfs-driver-file"
  )

lazy val driverHttp: Project = project
  .in(file("truevfs-driver/truevfs-driver-http"))
  .dependsOn(kernelSpec)
  .settings(javaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      Httpclient,
      JclOverSlf4j % Runtime
    ),
    normalizedName := "truevfs-driver-http"
  )

lazy val driverJar: Project = project
  .in(file("truevfs-driver/truevfs-driver-jar"))
  .dependsOn(compZipDriver)
  .settings(javaLibrarySettings)
  .settings(
    normalizedName := "truevfs-driver-jar"
  )

lazy val driverOdf: Project = project
  .in(file("truevfs-driver/truevfs-driver-odf"))
  .dependsOn(compZipDriver)
  .settings(javaLibrarySettings)
  .settings(
    normalizedName := "truevfs-driver-odf"
  )

lazy val driverSfx: Project = project
  .in(file("truevfs-driver/truevfs-driver-sfx"))
  .dependsOn(compZipDriver)
  .settings(javaLibrarySettings)
  .settings(
    normalizedName := "truevfs-driver-sfx"
  )

lazy val driverTar: Project = project
  .in(file("truevfs-driver/truevfs-driver-tar"))
  .dependsOn(compTarDriver)
  .settings(javaLibrarySettings)
  .settings(
    normalizedName := "truevfs-driver-tar"
  )

lazy val driverTarBzip2: Project = project
  .in(file("truevfs-driver/truevfs-driver-tar-bzip2"))
  .dependsOn(compTarDriver)
  .settings(javaLibrarySettings)
  .settings(
    normalizedName := "truevfs-driver-tar-bzip2"
  )

lazy val driverTarGzip: Project = project
  .in(file("truevfs-driver/truevfs-driver-tar-gzip"))
  .dependsOn(compTarDriver)
  .settings(javaLibrarySettings)
  .settings(
    normalizedName := "truevfs-driver-tar-gzip"
  )

lazy val driverTarXz: Project = project
  .in(file("truevfs-driver/truevfs-driver-tar-xz"))
  .dependsOn(compTarDriver)
  .settings(javaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      Xz
    ),
    normalizedName := "truevfs-driver-tar-xz"
  )

lazy val driverZip: Project = project
  .in(file("truevfs-driver/truevfs-driver-zip"))
  .dependsOn(compIbm437 % Runtime, compZipDriver)
  .settings(javaLibrarySettings)
  .settings(
    normalizedName := "truevfs-driver-zip"
  )

lazy val driverZipRaes: Project = project
  .in(file("truevfs-driver/truevfs-driver-zip-raes"))
  .dependsOn(compZipDriver)
  .settings(javaLibrarySettings)
  .settings(
    normalizedName := "truevfs-driver-zip-raes"
  )

lazy val ext: Project = project
  .in(file("truevfs-ext"))
  .aggregate(extInsight, extLogging, extPacemaker)
  .settings(aggregateSettings)

lazy val extInsight: Project = project
  .in(file("truevfs-ext/truevfs-ext-insight"))
  .dependsOn(compJmx)
  .settings(scalaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      JunitInterface % Test,
      ScalaPlus,
      Scalatest % Test
    ),
    normalizedName := "truevfs-ext-insight"
  )

lazy val extLogging: Project = project
  .in(file("truevfs-ext/truevfs-ext-logging"))
  .dependsOn(compInst)
  .settings(scalaLibrarySettings)
  .settings(
    normalizedName := "truevfs-ext-logging"
  )

lazy val extPacemaker: Project = project
  .in(file("truevfs-ext/truevfs-ext-pacemaker"))
  .dependsOn(compJmx)
  .settings(scalaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      JunitInterface % Test,
      MockitoCore % Test,
      Scalacheck % Test,
      ScalaPlus,
      Scalatest % Test
    ),
    normalizedName := "truevfs-ext-logging"
  )

lazy val it: Project = project
  .in(file("truevfs-it"))
  .dependsOn(
    access % "compile;runtime->runtime",
    driverFile,
    driverHttp,
    driverJar,
    driverOdf,
    driverSfx,
    driverTar,
    driverTarBzip2,
    driverTarGzip,
    driverTarXz,
    driverZip % "compile;runtime->runtime",
    driverZipRaes,
    kernelImpl
  ).settings(scalaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      JunitInterface % Test,
      MockitoCore % Test,
      Scalacheck % Test,
      ScalaPlus,
      Scalatest % Test,
      TruecommonsIO % Test classifier "tests",
      TruecommonsKeyDefault,
      TruecommonsKeySpec % Test classifier "" classifier "tests",
      TruecommonsShed % Test classifier "" classifier "tests"
    ),
    normalizedName := "truevfs-it"
  )

lazy val kernel: Project = project
  .in(file("truevfs-kernel"))
  .aggregate(kernelImpl, kernelSpec)
  .settings(aggregateSettings)

lazy val kernelImpl: Project = project
  .in(file("truevfs-kernel/truevfs-kernel-impl"))
  .dependsOn(kernelSpec)
  .settings(scalaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      JunitInterface % Test,
      MockitoCore % Test,
      Scalacheck % Test,
      Scalatest % Test,
      TruecommonsShed % Test classifier "" classifier "tests"
    ),
    normalizedName := "truevfs-kernel-impl"
  )

lazy val kernelSpec: Project = project
  .in(file("truevfs-kernel/truevfs-kernel-spec"))
  .settings(javaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      FindbugsAnnotations,
      JunitInterface % Test,
      TruecommonsAnnotations,
      TruecommonsCIO,
      TruecommonsIO,
      TruecommonsServices
    ),
    normalizedName := "truevfs-kernel-spec"
  )

lazy val profile: Project = project
  .in(file("truevfs-profile"))
  .aggregate(profileBase, profileDefault, profileFull)
  .settings(aggregateSettings)

lazy val profileBase: Project = project
  .in(file("truevfs-profile/truevfs-profile-base"))
  .dependsOn(
    accessSwing % "runtime->runtime",
    driverJar % Runtime,
    driverZip % "runtime->runtime",
    kernelImpl % Runtime
  ).settings(scalaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      TruecommonsKeyConsole % Runtime,
      TruecommonsKeyDefault % Runtime,
      TruecommonsKeySwing % Runtime
    ),
    normalizedName := "truevfs-profile-base"
  )

lazy val profileDefault: Project = project
  .in(file("truevfs-profile/truevfs-profile-default"))
  .dependsOn(
    driverHttp % Runtime,
    driverOdf % Runtime,
    driverTar % Runtime,
    driverTarBzip2 % Runtime,
    driverTarGzip % Runtime,
    driverTarXz % Runtime,
    driverZipRaes % Runtime,
    profileBase % "runtime->runtime"
  ).settings(scalaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      TruecommonsKeyMacosx % Runtime
    ),
    normalizedName := "truevfs-profile-default"
  )

lazy val profileFull: Project = project
  .in(file("truevfs-profile/truevfs-profile-full"))
  .dependsOn(
    driverSfx % Runtime,
    extInsight % Runtime,
    extLogging % Runtime,
    extPacemaker % Runtime,
    profileDefault % "runtime->runtime"
  ).settings(scalaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      TruecommonsKeyHurlfb % Runtime
    ),
    normalizedName := "truevfs-profile-full"
  )

lazy val samples: Project = project
  .in(file("truevfs-samples"))
  .dependsOn(
    access % "compile;runtime->runtime",
    driverFile % Runtime,
    driverHttp % Runtime,
    driverJar,
    driverOdf % Runtime,
    driverSfx,
    driverTar,
    driverTarBzip2,
    driverTarGzip,
    driverTarXz,
    driverZip % "compile;runtime->runtime",
    driverZipRaes,
    kernelImpl % Runtime
  ).settings(javaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      Slf4jSimple % Runtime,
      TruecommonsKeyConsole % Runtime,
      TruecommonsKeyDefault % Runtime,
      TruecommonsKeyHurlfb % Runtime,
      TruecommonsKeyMacosx % Runtime,
      TruecommonsKeySwing % Runtime
    ),
    normalizedName := "truevfs-samples"
  )
