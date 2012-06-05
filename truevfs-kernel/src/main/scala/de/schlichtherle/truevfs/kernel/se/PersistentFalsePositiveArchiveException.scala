/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import java.io._
import javax.annotation.concurrent._
import net.truevfs.kernel.util._

/** Indicates that a file system is a false positive file system and that this
  * exception may get cached until the federated (archive) file system gets
  * `sync`ed again.
  * 
  * This exception type is reserved for non-local control flow in
  * file system controller chains in order to reroute file system operations to
  * the parent file system of a false positive federated (archive) file system.
  * 
  * @author Christian Schlichtherle
  */
@Immutable
private class PersistentFalsePositiveArchiveException(getCause: IOException)
extends FalsePositiveArchiveException(getCause)
