/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import javax.annotation.concurrent._
import javax.management._

/**
 * A view for [[net.java.truevfs.ext.insight.stats.SyncStatistics]].
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
private final class I5tSyncStatisticsView(controller: I5tStatistics)
extends I5tStatisticsView(classOf[I5tSyncStatisticsMXBean], true)
with I5tSyncStatisticsMXBean {

  assert(null ne controller)

  protected override def getDescription(info: MBeanInfo) = "A log of sync statistics."

  override def stats = controller.stats
  override def getSubject = controller.subject
  override def rotate { controller.rotate }
}
