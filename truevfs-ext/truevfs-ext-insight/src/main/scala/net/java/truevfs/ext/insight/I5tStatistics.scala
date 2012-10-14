/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import javax.annotation.concurrent._
import net.java.truevfs.comp.jmx._
import net.java.truevfs.comp.jmx.JmxUtils._
import net.java.truevfs.ext.insight.stats._

/**
 * A controller for [[net.java.truevfs.ext.insight.stats.FsStatistics]].
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
private abstract class I5tStatistics(mediator: I5tMediator, offset: Int) extends JmxColleague {

  assert(0 <= offset)

  def subject = mediator.subject
  def stats = mediator stats offset
  def rotate { mediator rotateStats this }

  private def objectName = mediator
    .nameBuilder(classOf[FsStatistics])
    .put("subject", subject)
    .put("offset", mediator.formatOffset(offset))
    .get

  def newView: I5tStatisticsView

  override def start() { mediator register (objectName, newView) }

  override def toString =
    "%s[subject=%s, offset=%d, mediator=%s]".format(getClass.getName, subject, offset, mediator)
}
