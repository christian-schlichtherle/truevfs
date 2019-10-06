/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext

import net.java.truecommons.shed.{BitField, Filter, Visitor}
import net.java.truevfs.kernel.spec.{FsAccessOption, FsController, FsSyncException}

/** Constrains the number of concurrently mounted archive file systems in order to save some heap space.
  * This package provides a JMX interface for monitoring and management.
  *
  * @author Christian Schlichtherle
  */
package object pacemaker {

  type AccessOptions = BitField[FsAccessOption]
  type ControllerFilter = Filter[_ >: FsController]
  type ControllerVisitor = Visitor[_ >: FsController, FsSyncException]

  /** The name of the system property which determines the initial maximum
    * number of mounted archive file systems.
    */
  val maximumFileSystemsMountedPropertyKey: String = {
    classOf[PaceManagerMXBean].getPackage.getName + ".maximumFileSystemsMounted"
  }

  /** The minimum value for the maximum number of mounted archive file systems.
    * This value must not be less than two or otherwise you couldn't even copy
    * entries from one archive file to another.
    * Well, actually you could because the pace manager doesn't unmount archive
    * file systems with open streams or channels, but let's play it safe and
    * pretend it would.
    */
  val maximumFileSystemsMountedMinimumValue: Int = 2

  /** The default value of the system property which determines the initial
    * maximum number of mounted archive file systems.
    * for the maximum number of mounted file systems.
    * The value of this constant will be set to
    * `maximumFileSystemsMountedMinimumValue` unless a system
    * property with the key string
    * `maximumFileSystemsMountedPropertyKey`
    * is set to a value which is greater than
    * `maximumFileSystemsMountedMinimumValue`.
    */
  val maximumFileSystemsMountedDefaultValue: Int = {
    math.max(
      maximumFileSystemsMountedMinimumValue,
      Integer.getInteger(maximumFileSystemsMountedPropertyKey, maximumFileSystemsMountedMinimumValue)
    )
  }
}
