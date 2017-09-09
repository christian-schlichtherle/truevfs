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
  .aggregate(access, comp, driver, kernel)
  .settings(releaseSettings)
  .settings(aggregateSettings)

lazy val access: Project = project
  .in(file("truevfs-access"))
  .dependsOn(driverFile % Runtime, kernelSpec)
  .settings(javaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      MockitoCore % Test,
      ScalaCheck % Test,
      ScalaPlus % Test,
      ScalaTest % Test,
    )
  )

lazy val comp: Project = project
  .in(file("truevfs-comp"))
  .aggregate(compZip, compZipdriver)
  .settings(aggregateSettings)

lazy val compZip: Project = project
  .in(file("truevfs-comp/truevfs-comp-zip"))
  .settings(javaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      BCProvJDK15On,
      CommonsCompress,
      FindBugsAnnotations,
      JUnitInterface % Test,
      ScalaTest % Test,
      TrueCommonsIO classifier "",
      TrueCommonsIO % Test classifier "tests",
      TrueCommonsKeySpec,
      TrueCommonsShed,
      TrueCommonsShed % Test classifier "tests"
    ),
    normalizedName := "truevfs-comp-zip"
  )

lazy val compZipdriver: Project = project
  .in(file("truevfs-comp/truevfs-comp-zipdriver"))
  .dependsOn(compZip, kernelSpec)
  .settings(javaLibrarySettings)
  .settings(
    normalizedName := "truevfs-comp-zipdriver"
  )

lazy val driver: Project = project
  .in(file("truevfs-driver"))
  .aggregate(driverFile)
  .settings(aggregateSettings)

lazy val driverFile: Project = project
  .in(file("truevfs-driver/truevfs-driver-file"))
  .dependsOn(kernelSpec)
  .settings(javaLibrarySettings)
  .settings(
    normalizedName := "truevfs-driver-file"
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
      MockitoCore % Test,
      ScalaCheck % Test,
      ScalaTest % Test,
      TrueCommonsShed % Test classifier "" classifier "tests"
    ),
    normalizedName := "truevfs-kernel-impl"
  )

lazy val kernelSpec: Project = project
  .in(file("truevfs-kernel/truevfs-kernel-spec"))
  .settings(javaLibrarySettings)
  .settings(
    libraryDependencies ++= Seq(
      FindBugsAnnotations,
      JUnitInterface % Test,
      TrueCommonsAnnotations,
      TrueCommonsCIO,
      TrueCommonsIO,
      TrueCommonsServices
    ),
    normalizedName := "truevfs-kernel-spec"
  )
