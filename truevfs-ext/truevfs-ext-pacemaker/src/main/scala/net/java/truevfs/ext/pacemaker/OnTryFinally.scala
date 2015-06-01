/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

/** @author Christian Schlichtherle */
private trait OnTryFinally {

  final def onTry[A](tryFun: => A): OnTryStatement[A] = {
    new OnTryStatement[A] {
      override def onFinally(finallyFun: => Any) = {
        var t: Throwable = null
        try {
          tryFun
        } catch {
          case x: Throwable => t = x; throw x
        } finally {
          try {
            finallyFun
          } catch {
            case y: Throwable =>
              if (null == t) throw y
              t addSuppressed y
          }
        }
      }
    }
  }

  trait OnTryStatement[A] {
    def onFinally(finallyFun: => Any): A
  }
}

/** @author Christian Schlichtherle */
private object OnTryFinally extends OnTryFinally
