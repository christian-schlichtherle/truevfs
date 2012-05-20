/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import net.truevfs.kernel.FsManager
import net.truevfs.kernel.spi.FsManagerService

/**
 * A service for the file system manager implementation in this package.
 * 
 * @author Christian Schlichtherle
 */
final class ArchiveManagerService extends FsManagerService {
  override lazy val getManager: FsManager = new ArchiveManager
  override def getPriority = -100
}
