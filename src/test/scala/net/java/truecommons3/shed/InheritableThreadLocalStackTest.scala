/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed

import java.util._
import java.util.concurrent._

import org.junit.runner._
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.prop.PropertyChecks._

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class InheritableThreadLocalStackTest extends WordSpec {

  private def create = new InheritableThreadLocalStack[String]

  private def inNewChild[V](operation: => V) {
    var ex: Throwable = null
    val r = new Runnable() {
      def run() {
        try { operation }
        catch { case ex2: Throwable => ex = ex2 }
      }
    }
    val t = new Thread(r)
    t start ()
    t join ()
    if (null != ex) throw new ExecutionException(ex)
  }

  "An inheritable thread local stack" when {
    "ever created" should {
      "be empty" in {
        val stack = create
        stack.isEmpty should be (true)
        inNewChild { stack.isEmpty should be (true) }
      }

      "return null when peeked" in {
        val stack = create
        stack.peek should be (null)
        inNewChild { stack.peek should be (null) }
      }

      "return the given default element when peeked" in {
        val stack = create
        forAll("given") { given: String =>
          stack peekOrElse given should be theSameInstanceAs (given)
        }
        inNewChild {
          forAll("given") { given: String =>
            stack peekOrElse given should be theSameInstanceAs (given)
          }
        }
      }

      "return the given element when pushed" in {
        val stack = create
        forAll("given") { given: String =>
          stack push given should be theSameInstanceAs (given)
        }
        inNewChild {
          forAll("given") { given: String =>
            stack push given should be theSameInstanceAs (given)
          }
        }
      }

      "throw a NoSuchElementException when popped" in {
        val stack = create
        intercept[NoSuchElementException] { stack pop () }
        inNewChild { intercept[NoSuchElementException] { stack pop () } }
      }

      "throw an IllegalStateException when conditionally popped with a given element" in {
        val stack = create
        forAll("given") { given: String =>
          intercept[IllegalStateException] { stack popIf given }
        }
        inNewChild {
          forAll("given") { given: String =>
            intercept[IllegalStateException] { stack popIf given }
          }
        }
      }
    }

    "used appropriately" should {
      "work like a charm" in {
        val stack = create

        val push = Table(("push"), ("foo"), ("bar"), ("baz"))
        val pop  = Table(("pop"),  ("baz"), ("bar"), ("foo"))

        forAll(push) { given: String =>
          stack push given should be theSameInstanceAs (given)
          stack.isEmpty should be (false)
          stack.peek should be theSameInstanceAs (given)
          stack peekOrElse "other" should be theSameInstanceAs (given)
          inNewChild {
            intercept[NoSuchElementException] { stack pop () }
            stack.isEmpty should be (false)
            stack.peek should be theSameInstanceAs (given)
            stack peekOrElse "other" should be theSameInstanceAs (given)
            stack push "other" should be theSameInstanceAs ("other")
            stack.isEmpty should be (false)
            stack.peek should be theSameInstanceAs ("other")
            stack peekOrElse "else" should be theSameInstanceAs ("other")
            stack pop () should be theSameInstanceAs ("other")
            stack.isEmpty should be (false)
            stack.peek should be theSameInstanceAs (given)
            stack peekOrElse "other" should be theSameInstanceAs (given)
          }
        }

        forAll(pop) { given: String =>
          intercept[IllegalStateException] { stack popIf "other" }
          stack pop () should be theSameInstanceAs (given)
          stack push given should be theSameInstanceAs (given)
          stack popIf given
        }

        stack.isEmpty should be (true)
      }

      "leak no memory" in {
        pending
      }
    }
  }
}
