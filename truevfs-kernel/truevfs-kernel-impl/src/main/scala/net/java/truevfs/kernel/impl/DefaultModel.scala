/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import javax.annotation.concurrent.ThreadSafe

import net.java.truevfs.kernel.spec._

@ThreadSafe
private final class DefaultModel(mountPoint: FsMountPoint, parent: FsModel)
extends FsAbstractModel(mountPoint, parent) {

  @volatile private var mounted: Boolean = _

  override def isMounted = mounted
  override def setMounted(mounted: Boolean) { this.mounted = mounted }
}
