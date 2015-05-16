/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

import javax.annotation.concurrent.ThreadSafe
import javax.management.MBeanAttributeInfo
import javax.management.MBeanInfo
import net.java.truevfs.comp.jmx.JmxManagerView

/**
 * A view for a [[PaceManager]].
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
private final class PaceManagerView(manager: PaceManager)
extends JmxManagerView[PaceManager](classOf[PaceManagerMXBean], manager)
with PaceManagerMXBean {

  protected override def getDescription(info: MBeanInfo) = "A pace maker for the file system manager."

  protected override def getDescription(info: MBeanAttributeInfo) = {
    info.getName match {
      case "MaximumFileSystemsMounted" => "The maximum number of mounted file systems."
      case _                           => super.getDescription(info)
    }
  }

  override def getMaximumFileSystemsMounted = manager.max
  override def setMaximumFileSystemsMounted(max: Int) { manager.max = max }
}
