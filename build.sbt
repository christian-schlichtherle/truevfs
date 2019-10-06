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
  .settings(name := "TrueVFS")

lazy val access: Project = project
  .in(file("truevfs-access"))
  .dependsOn(driverFile % Runtime, kernelSpec)
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides convenient access to the (federated virtual) file system space for TrueVFS client applications.
        |Features simple, uniform, transparent, thread-safe, read/write access to archive files as if they were virtual directories in a file system path.""".stripMargin,
    libraryDependencies ++= Seq(
      JunitInterface % Test,
      MockitoCore % Test,
      Scalacheck % Test,
      Scalatest % Test
    ),
    name := "TrueVFS Access",
    normalizedName := "truevfs-access"
  )

lazy val accessSwing: Project = project
  .in(file("truevfs-access-swing"))
  .dependsOn(access % "compile;runtime->runtime")
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """This module provides Swing GUI classes for viewing file trees and choosing entries in archive files.""".stripMargin,
    name := "TrueVFS Access Swing",
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
  )
  .settings(aggregateSettings)
  .settings(name := "TrueVFS Component")

lazy val compIbm437: Project = project
  .in(file("truevfs-comp/truevfs-comp-ibm437"))
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides the IBM437 character set alias CP437 for use with genuine ZIP files.
        |Add the JAR artifact of this module to the run time class path to make its file system drivers available for service location in the client API modules.""".stripMargin,
    libraryDependencies ++= Seq(
      JunitInterface % Test,
      TruecommonsAnnotations
    ),
    name := "TrueVFS Component IBM437",
    normalizedName := "truevfs-comp-ibm437"
  )

lazy val compInst: Project = project
  .in(file("truevfs-comp/truevfs-comp-inst"))
  .dependsOn(kernelSpec)
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides basic functionality for instrumenting the TrueVFS Kernel.""".stripMargin,
    name := "TrueVFS Component Instrumentation",
    normalizedName := "truevfs-comp-inst"
  )

lazy val compJmx: Project = project
  .in(file("truevfs-comp/truevfs-comp-jmx"))
  .dependsOn(compInst)
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides basic functionality for instrumenting the TrueVFS Kernel with JMX.""".stripMargin,
    libraryDependencies ++= Seq(
      JunitInterface % Test,
      Scalatest % Test,
      TruecommonsJMX
    ),
    name := "TrueVFS Component JMX",
    normalizedName := "truevfs-comp-jmx"
  )

lazy val compTarDriver: Project = project
  .in(file("truevfs-comp/truevfs-comp-tardriver"))
  .dependsOn(kernelSpec)
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides basic drivers for reading and writing TAR files.""".stripMargin,
    libraryDependencies ++= Seq(
      CommonsCompress
    ),
    name := "TrueVFS Component TarDriver",
    normalizedName := "truevfs-comp-tardriver"
  )

lazy val compZip: Project = project
  .in(file("truevfs-comp/truevfs-comp-zip"))
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides basic functionality for reading and writing ZIP files.""".stripMargin,
    libraryDependencies ++= Seq(
      BcprovJdk15on,
      CommonsCompress,
      FindbugsAnnotations,
      JunitInterface % Test,
      Scalatest % Test,
      TruecommonsIO,
      TruecommonsIO % Test classifier "tests",
      TruecommonsKeySpec,
      TruecommonsShed,
      TruecommonsShed % Test classifier "tests"
    ),
    name := "TrueVFS Component ZIP",
    normalizedName := "truevfs-comp-zip"
  )

lazy val compZipDriver: Project = project
  .in(file("truevfs-comp/truevfs-comp-zipdriver"))
  .dependsOn(compZip, kernelSpec)
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides basic drivers for reading and writing ZIP files.""".stripMargin,
    name := "TrueVFS Component ZipDriver",
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
  )
  .settings(aggregateSettings)
  .settings(name := "TrueVFS Driver")

lazy val driverFile: Project = project
  .in(file("truevfs-driver/truevfs-driver-file"))
  .dependsOn(kernelSpec)
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides a file system driver for accessing the platform file system.
        |Add the JAR artifact of this module to the run time class path to make its file system drivers available for service location in the client API modules.""".stripMargin,
    libraryDependencies ++= Seq(
      JunitInterface % Test
    ),
    name := "TrueVFS Driver FILE",
    normalizedName := "truevfs-driver-file"
  )

lazy val driverHttp: Project = project
  .in(file("truevfs-driver/truevfs-driver-http"))
  .dependsOn(kernelSpec)
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides a file system driver for read-only access to the web.
        |Add the JAR artifact of this module to the run time class path to make its file system drivers available for service location in the client API modules.""".stripMargin,
    libraryDependencies ++= Seq(
      Httpclient,
      JclOverSlf4j % Runtime
    ),
    name := "TrueVFS Driver HTTP(S)",
    normalizedName := "truevfs-driver-http"
  )

lazy val driverJar: Project = project
  .in(file("truevfs-driver/truevfs-driver-jar"))
  .dependsOn(compZipDriver)
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides a file system driver for accessing the JAR file format.
        |Add the JAR artifact of this module to the run time class path to make its file system drivers available for service location in the client API modules.""".stripMargin,
    name := "TrueVFS Driver JAR",
    normalizedName := "truevfs-driver-jar"
  )

lazy val driverOdf: Project = project
  .in(file("truevfs-driver/truevfs-driver-odf"))
  .dependsOn(compZipDriver)
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides a file system driver for accessing the Open Document File format.
        |Add the JAR artifact of this module to the run time class path to make its file system drivers available for service location in the client API modules.""".stripMargin,
    name := "TrueVFS Driver ODF",
    normalizedName := "truevfs-driver-odf"
  )

lazy val driverSfx: Project = project
  .in(file("truevfs-driver/truevfs-driver-sfx"))
  .dependsOn(compZipDriver)
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides a file system driver for read-only access to the SelF eXtracting ZIP file format, alias SFX.
        |Add the JAR artifact of this module to the run time class path to make its file system drivers available for service location in the client API modules.""".stripMargin,
    name := "TrueVFS Driver SFX",
    normalizedName := "truevfs-driver-sfx"
  )

lazy val driverTar: Project = project
  .in(file("truevfs-driver/truevfs-driver-tar"))
  .dependsOn(compTarDriver)
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides a file system driver for accessing the TAR file format.
        |Add the JAR artifact of this module to the run time class path to make its file system drivers available for service location in the client API modules.""".stripMargin,
    name := "TrueVFS Driver TAR",
    normalizedName := "truevfs-driver-tar"
  )

lazy val driverTarBzip2: Project = project
  .in(file("truevfs-driver/truevfs-driver-tar-bzip2"))
  .dependsOn(compTarDriver)
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides a file system driver for accessing the BZIP2 compressed TAR file format.
        |Add the JAR artifact of this module to the run time class path to make its file system drivers available for service location in the client API modules.""".stripMargin,
    name := "TrueVFS Driver TAR.BZIP2",
    normalizedName := "truevfs-driver-tar-bzip2"
  )

lazy val driverTarGzip: Project = project
  .in(file("truevfs-driver/truevfs-driver-tar-gzip"))
  .dependsOn(compTarDriver)
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides a file system driver for accessing the GZIP compressed TAR file format.
        |Add the JAR artifact of this module to the run time class path to make its file system drivers available for service location in the client API modules.""".stripMargin,
    name := "TrueVFS Driver TAR.GZIP",
    normalizedName := "truevfs-driver-tar-gzip"
  )

lazy val driverTarXz: Project = project
  .in(file("truevfs-driver/truevfs-driver-tar-xz"))
  .dependsOn(compTarDriver)
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides a file system driver for accessing the XZ compressed TAR file format.
        |Add the JAR artifact of this module to the run time class path to make its file system drivers available for service location in the client API modules.""".stripMargin,
    libraryDependencies ++= Seq(
      Xz
    ),
    name := "TrueVFS Driver TAR.XZ",
    normalizedName := "truevfs-driver-tar-xz"
  )

lazy val driverZip: Project = project
  .in(file("truevfs-driver/truevfs-driver-zip"))
  .dependsOn(compIbm437 % Runtime, compZipDriver)
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides a file system driver for accessing the ZIP file format.
        |Add the JAR artifact of this module to the run time class path to make its file system drivers available for service location in the client API modules.""".stripMargin,
    name := "TrueVFS Driver ZIP",
    normalizedName := "truevfs-driver-zip"
  )

lazy val driverZipRaes: Project = project
  .in(file("truevfs-driver/truevfs-driver-zip-raes"))
  .dependsOn(compZipDriver)
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Provides a file system driver for accessing the RAES encrypted ZIP file format, alias ZIP.RAES or TZP.
        |Add the JAR artifact of this module to the run time class path to make its file system drivers available for service location in the client API modules.""".stripMargin,
    name := "TrueVFS Driver ZIP.RAES",
    normalizedName := "truevfs-driver-zip-raes"
  )

lazy val ext: Project = project
  .in(file("truevfs-ext"))
  .aggregate(extInsight, extLogging, extPacemaker)
  .settings(aggregateSettings)
  .settings(name := "TrueVFS Extension")

lazy val extInsight: Project = project
  .in(file("truevfs-ext/truevfs-ext-insight"))
  .dependsOn(compJmx)
  .settings(scalaLibrarySettings)
  .settings(
    description :=
      """Instruments the TrueVFS Kernel for statistics monitoring via JMX.
        |Add the JAR artifact of this module to the run time class path to make its services available for service location in the client API modules.""".stripMargin,
    libraryDependencies ++= Seq(
      FunIoBIOS % Test,
      JunitInterface % Test,
      Scalatest % Test
    ),
    name := "TrueVFS Extension Insight",
    normalizedName := "truevfs-ext-insight"
  )

lazy val extLogging: Project = project
  .in(file("truevfs-ext/truevfs-ext-logging"))
  .dependsOn(compInst)
  .settings(scalaLibrarySettings)
  .settings(
    description :=
      """Instruments the TrueVFS Kernel for logging via SLF4J.
        |Add the JAR artifact of this module to the run time class path to make its services available for service location in the client API modules.""".stripMargin,
    name := "TrueVFS Extension Logging",
    normalizedName := "truevfs-ext-logging"
  )

lazy val extPacemaker: Project = project
  .in(file("truevfs-ext/truevfs-ext-pacemaker"))
  .dependsOn(compJmx)
  .settings(scalaLibrarySettings)
  .settings(
    description :=
      """Constraints the number of concurrently mounted archive file systems in order to save some heap space.
        |Provides a JMX interface for monitoring and management.
        |Add the JAR artifact of this module to the run time class path to make its services available for service location in the client API modules.""".stripMargin,
    libraryDependencies ++= Seq(
      JunitInterface % Test,
      MockitoCore % Test,
      Scalacheck % Test,
      Scalatest % Test,
      Slf4jSimple % Test
    ),
    name := "TrueVFS Extension Pacemaker",
    normalizedName := "truevfs-ext-pacemaker"
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
    description :=
      """Provides integration tests for TrueVFS.""".stripMargin,
    libraryDependencies ++= Seq(
      FunIoBIOS % Test,
      FunIoScalaApi % Test,
      JunitInterface % Test,
      MockitoCore % Test,
      Scalacheck % Test,
      Scalatest % Test,
      Slf4jSimple % Test,
      TruecommonsIO % Test classifier "" classifier "tests",
      TruecommonsKeyDefault % Test,
      TruecommonsKeySpec % Test classifier "" classifier "tests",
      TruecommonsShed % Test classifier "" classifier "tests"
    ),
    name := "TrueVFS Integration Tests",
    normalizedName := "truevfs-it",
    publishArtifact := false
  )

lazy val kernel: Project = project
  .in(file("truevfs-kernel"))
  .aggregate(kernelImpl, kernelSpec)
  .settings(aggregateSettings)
  .settings(name := "TrueVFS Kernel")

lazy val kernelImpl: Project = project
  .in(file("truevfs-kernel/truevfs-kernel-impl"))
  .dependsOn(kernelSpec)
  .settings(scalaLibrarySettings)
  .settings(
    description :=
      """Implements the API for accessing the federated virtual file system space.
        |You can override it by providing another file system manager factory implementation with a higher priority on the class path.""".stripMargin,
    libraryDependencies ++= Seq(
      JunitInterface % Test,
      MockitoCore % Test,
      Scalatest % Test,
      Slf4jSimple % Test,
      TruecommonsShed % Test classifier "" classifier "tests"
    ),
    name := "TrueVFS Kernel Implementation",
    normalizedName := "truevfs-kernel-impl"
  )

lazy val kernelSpec: Project = project
  .in(file("truevfs-kernel/truevfs-kernel-spec"))
  .settings(javaLibrarySettings)
  .settings(
    description :=
      """Specifies the API for accessing the federated virtual file system space.
        |Provides a service provider API for a singleton file system manager, an I/O buffer pool and a file system driver map.""".stripMargin,
    libraryDependencies ++= Seq(
      JunitInterface % Test,
      TruecommonsAnnotations,
      TruecommonsCIO,
      TruecommonsIO,
      TruecommonsServices
    ),
    name := "TrueVFS Kernel Specification",
    normalizedName := "truevfs-kernel-spec"
  )

lazy val profile: Project = project
  .in(file("truevfs-profile"))
  .aggregate(profileBase, profileDefault, profileFull)
  .settings(aggregateSettings)
  .settings(name := "TrueVFS Profile")

lazy val profileBase: Project = project
  .in(file("truevfs-profile/truevfs-profile-base"))
  .dependsOn(
    accessSwing % "compile;runtime->runtime",
    driverJar % Runtime,
    driverZip % "runtime->runtime",
    kernelImpl % Runtime
  ).settings(scalaLibrarySettings)
  .settings(
    description :=
      """Bundles dependencies to support the most prominent use cases.
        |Provides the file system drivers for JAR and ZIP.""".stripMargin,
    libraryDependencies ++= Seq(
      TruecommonsKeyConsole % Runtime,
      TruecommonsKeyDefault % Runtime,
      TruecommonsKeySwing % Runtime
    ),
    name := "TrueVFS Profile Base",
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
    profileBase % "compile;runtime->runtime"
  ).settings(scalaLibrarySettings)
  .settings(
    description :=
      """Bundles dependencies to support accessing all TrueVFS features without the slight negative performance impact of some excluded modules.
        |Depends on the base configuration profile and adds the file system drivers for HTTP(S), ODF, TAR, TAR.BZIP2, TAR.GZIP, TAR.XZ and ZIP.RAES.""".stripMargin,
    libraryDependencies ++= Seq(
      TruecommonsKeyMacosx % Runtime
    ),
    name := "TrueVFS Profile Default",
    normalizedName := "truevfs-profile-default"
  )

lazy val profileFull: Project = project
  .in(file("truevfs-profile/truevfs-profile-full"))
  .dependsOn(
    driverSfx % Runtime,
    extInsight % Runtime,
    extLogging % Runtime,
    extPacemaker % Runtime,
    profileDefault % "compile;runtime->runtime"
  ).settings(scalaLibrarySettings)
  .settings(
    description :=
      """Bundles dependencies to support all TrueVFS features.
        |Should not be used in production environments because of its slightly negative performance impact.
        |Depends on the default configuration profile and adds the file system driver for SFX and the extensions Insight, Logging and Pacemaker.""".stripMargin,
    libraryDependencies ++= Seq(
      TruecommonsKeyHurlfb % Runtime
    ),
    name := "TrueVFS Profile Full",
    normalizedName := "truevfs-profile-full"
  )

lazy val samples: Project = project
  .in(file("truevfs-samples"))
  .dependsOn(
    access % "compile;runtime->runtime",
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
  ).settings(scalaLibrarySettings)
  .settings(
    description :=
      """Sample applications to demonstrate the usage of TrueVFS modules to support many, even esoteric use cases.""".stripMargin,
    libraryDependencies ++= Seq(
      Slf4jSimple % Runtime,
      TruecommonsKeyConsole % Runtime,
      TruecommonsKeyDefault % Runtime,
      TruecommonsKeyHurlfb % Runtime,
      TruecommonsKeyMacosx % Runtime,
      TruecommonsKeySwing % Runtime
    ),
    name := "TrueVFS Samples",
    normalizedName := "truevfs-samples"
  )
