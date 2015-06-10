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
extends JmxManager(mediator, manager) {

  override def activate() {
    super.activate()
    mediator activateAllStats this
  }

  override def accept[X <: Exception](filter: Filter[_ >: FsController], visitor: Visitor[_ >: FsController, X]) {
    var allUnmounted = true
    val start = System.nanoTime
    manager accept (
      new Filter[FsController] {
        override def accept(controller: FsController) = {
          val accepted = filter accept controller
          if (!accepted)
            allUnmounted = false
          accepted
        }
      },
      new Visitor[FsController, X] {
        override def visit(controller: FsController) {
          try {
            visitor visit controller
          } finally {
            if (controller.getModel.isMounted)
              allUnmounted = false
          }
        }
      }
    )
    if (allUnmounted) {
      mediator logSync (System.nanoTime - start)
      mediator rotateAllStats this
    }
  }
}
