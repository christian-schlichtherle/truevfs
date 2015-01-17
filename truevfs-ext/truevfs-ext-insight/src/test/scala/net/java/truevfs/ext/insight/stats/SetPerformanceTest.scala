/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight.stats

import collection._
import java.util.concurrent.atomic._
import util._

/**
 * @author Christian Schlichtherle
 */
object SetPerformanceTest {

  private val iterations = 10
  private val operations = 1000 * 1000
  private val interval = 100

  def main(args: Array[String]) {
    for (i <- 1 to iterations) {
      printf("\nIteration %d:\n", i)
      printf("Immutable Set: %s\n", format(immutableSet))
      printf("Mutable Set  : %s\n", format(mutableSet))
    }
  }

  private def format(operation: () => Unit) =
    "%,12d nanoseconds".format(nanoseconds(operation))

  private def nanoseconds(operation: () => Unit) = {
    val start = System.nanoTime
    operation()
    System.nanoTime - start
  }

  private def immutableSet() {
    var set = new AtomicReference(immutable.Set[Long]())
    for (_ <- 1 to operations) atomic(set)(update)
  }

  private def atomic[V](ref: AtomicReference[V])(next: V => V): V = {
    while (true) {
      val expect = ref.get
      val update = next(expect)
      if (ref.weakCompareAndSet(expect, update)) return update
    }
    throw new AssertionError
  }

  private def update(set: immutable.Set[Long]) = {
    set + (Random nextInt interval)
  }

  private def mutableSet() {
    val set = mutable.Set[Long]()
    for (_ <- 1 to operations) isolated(set)(update)
  }

  private def isolated[V <: AnyRef](obj: V)(operation: V => Unit) {
    obj synchronized { operation(obj) }
  }

  private def update(set: mutable.Set[Long]) {
    set += Random nextInt interval
  }
}
