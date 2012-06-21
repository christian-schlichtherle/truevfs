/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.impl

import javax.annotation.concurrent._
import net.truevfs.kernel._
import net.truevfs.kernel.spi._

/** A service for the file system manager implementation in this package.
  * 
  * @author Christian Schlichtherle
  */
@Immutable
final class ArchiveManagerService extends FsManagerService {
  override lazy val getManager: FsManager = new ArchiveManager
  override def getPriority = -100
}
