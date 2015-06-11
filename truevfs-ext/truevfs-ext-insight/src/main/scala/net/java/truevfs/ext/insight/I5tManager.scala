/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import javax.annotation.concurrent._

import net.java.truecommons.shed._
import net.java.truevfs.comp.jmx._
import net.java.truevfs.kernel.spec._

/** @author Christian Schlichtherle */
@ThreadSafe
private final class I5tManager(mediator: I5tMediator, manager: FsManager)
extends JmxManager(mediator, manager) { self =>

  override def activate() {
    super.activate()
    mediator activateAllStats this
  }

  override def accept[X <: Exception, V <: Visitor[_ >: FsController, X]](filter: Filter[_ >: FsController], visitor: V) = {
    var allUnmounted = true
    val start = System.nanoTime
    manager.accept[X, Visitor[FsController, X]](
      new Filter[FsController] {
        override def accept(controller: FsController) = {
          val accepted = filter accept controller
          allUnmounted &= accepted
          accepted
        }
      },
      new Visitor[FsController, X] {
        override def visit(controller: FsController) {
          try {
            visitor visit controller
          } finally {
            allUnmounted &= !controller.getModel.isMounted
          }
        }
      }
    )
    if (allUnmounted) {
      mediator logSync (System.nanoTime - start)
      mediator rotateAllStats self
    }
    visitor
  }
}
