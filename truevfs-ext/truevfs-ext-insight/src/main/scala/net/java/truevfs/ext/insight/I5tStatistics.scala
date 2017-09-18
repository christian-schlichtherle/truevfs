/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import javax.annotation.concurrent._
import net.java.truevfs.comp.jmx._
import net.java.truevfs.ext.insight.stats._

/**
 * A controller for [[net.java.truevfs.ext.insight.stats.FsStatistics]].
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
private abstract class I5tStatistics(mediator: I5tMediator, offset: Int)
extends JmxComponent {

  assert(0 <= offset)

  def subject: String = mediator.subject
  def stats: FsStatistics = mediator stats offset
  def rotate(): Unit = { mediator rotateStats this }

  private def objectName = mediator
    .nameBuilder(classOf[FsStatistics])
    .put("subject", subject)
    .put("offset", mediator formatOffset offset)
    .get

  def newView: I5tStatisticsView

  override def activate(): Unit = { mediator register (objectName, newView) }

  override def toString: String =
    "%s[subject=%s, offset=%d, mediator=%s]" format (getClass.getName, subject, offset, mediator)
}
