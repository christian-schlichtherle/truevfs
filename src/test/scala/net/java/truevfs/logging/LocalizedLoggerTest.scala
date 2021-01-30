/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.logging

import java.util._

import net.java.truecommons3.logging.Disambiguate._
import org.junit.runner._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.mock.MockitoSugar.mock
import org.slf4j._

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class LocalizedLoggerTest extends WordSpec {

  import net.java.truecommons3.logging.LocalizedLoggerTest._

  def create(delegate: Logger) = new LocalizedLogger(delegate, bundle)

  "A localized logger" when {
    val delegate = mock[Logger]
    val logger = create(delegate)

    "not trace-enabled" should {
      "not forward the logging call" in {
        reset(delegate)
        logger trace "0"
        verify(delegate, never()) trace any()
      }
    }

    "trace-enabled" should {
      "resolve a message with zero parameters and forward the logging call" in {
        reset(delegate)
        when(delegate.isTraceEnabled) thenReturn true
        logger trace "0"
        verify(delegate) trace ""
      }

      "resolve a message with one parameter and forward the logging call" in {
        reset(delegate)
        when(delegate.isTraceEnabled) thenReturn true
        logger trace ("1", "one")
        verify(delegate) trace "one"
      }

      "resolve a message with two parameters and forward the logging call" in {
        reset(delegate)
        when(delegate.isTraceEnabled) thenReturn true
        trace2(logger, "2", "one", "two")
        verify(delegate) trace "one two"
      }

      "resolve a message with three parameters and forward the logging call" in {
        reset(delegate)
        when(delegate.isTraceEnabled) thenReturn true
        logger trace ("3", "one", "two", "three")
        verify(delegate) trace "one two three"
      }
    }

    "not debug-enabled" should {
      "not forward the logging call" in {
        reset(delegate)
        logger debug "0"
        verify(delegate, never()) debug any()
      }
    }

    "debug-enabled" should {
      "resolve a message with zero parameters and forward the logging call" in {
        reset(delegate)
        when(delegate.isDebugEnabled) thenReturn true
        logger debug "0"
        verify(delegate) debug ""
      }

      "resolve a message with one parameter and forward the logging call" in {
        reset(delegate)
        when(delegate.isDebugEnabled) thenReturn true
        logger debug ("1", "one")
        verify(delegate) debug "one"
      }

      "resolve a message with two parameters and forward the logging call" in {
        reset(delegate)
        when(delegate.isDebugEnabled) thenReturn true
        debug2(logger, "2", "one", "two")
        verify(delegate) debug "one two"
      }

      "resolve a message with three parameters and forward the logging call" in {
        reset(delegate)
        when(delegate.isDebugEnabled) thenReturn true
        logger debug ("3", "one", "two", "three")
        verify(delegate) debug "one two three"
      }
    }

    "not info-enabled" should {
      "not forward the logging call" in {
        reset(delegate)
        logger info "0"
        verify(delegate, never()) info any()
      }
    }

    "info-enabled" should {
      "resolve a message with zero parameters and forward the logging call" in {
        reset(delegate)
        when(delegate.isInfoEnabled) thenReturn true
        logger info "0"
        verify(delegate) info ""
      }

      "resolve a message with one parameter and forward the logging call" in {
        reset(delegate)
        when(delegate.isInfoEnabled) thenReturn true
        logger info ("1", "one")
        verify(delegate) info "one"
      }

      "resolve a message with two parameters and forward the logging call" in {
        reset(delegate)
        when(delegate.isInfoEnabled) thenReturn true
        info2(logger, "2", "one", "two")
        verify(delegate) info "one two"
      }

      "resolve a message with three parameters and forward the logging call" in {
        reset(delegate)
        when(delegate.isInfoEnabled) thenReturn true
        logger info ("3", "one", "two", "three")
        verify(delegate) info "one two three"
      }
    }

    "not warn-enabled" should {
      "not forward the logging call" in {
        reset(delegate)
        logger warn "0"
        verify(delegate, never()) warn any()
      }
    }

    "warn-enabled" should {
      "resolve a message with zero parameters and forward the logging call" in {
        reset(delegate)
        when(delegate.isWarnEnabled) thenReturn true
        logger warn "0"
        verify(delegate) warn ""
      }

      "resolve a message with one parameter and forward the logging call" in {
        reset(delegate)
        when(delegate.isWarnEnabled) thenReturn true
        logger warn ("1", "one")
        verify(delegate) warn "one"
      }

      "resolve a message with two parameters and forward the logging call" in {
        reset(delegate)
        when(delegate.isWarnEnabled) thenReturn true
        warn2(logger, "2", "one", "two")
        verify(delegate) warn "one two"
      }

      "resolve a message with three parameters and forward the logging call" in {
        reset(delegate)
        when(delegate.isWarnEnabled) thenReturn true
        logger warn ("3", "one", "two", "three")
        verify(delegate) warn "one two three"
      }
    }

    "not error-enabled" should {
      "not forward the logging call" in {
        reset(delegate)
        logger error "0"
        verify(delegate, never()) error any()
      }
    }

    "error-enabled" should {
      "resolve a message with zero parameters and forward the logging call" in {
        reset(delegate)
        when(delegate.isErrorEnabled) thenReturn true
        logger error "0"
        verify(delegate) error ""
      }

      "resolve a message with one parameter and forward the logging call" in {
        reset(delegate)
        when(delegate.isErrorEnabled) thenReturn true
        logger error ("1", "one")
        verify(delegate) error "one"
      }

      "resolve a message with two parameters and forward the logging call" in {
        reset(delegate)
        when(delegate.isErrorEnabled) thenReturn true
        error2(logger, "2", "one", "two")
        verify(delegate) error "one two"
      }

      "resolve a message with three parameters and forward the logging call" in {
        reset(delegate)
        when(delegate.isErrorEnabled) thenReturn true
        logger error ("3", "one", "two", "three")
        verify(delegate) error "one two three"
      }
    }
  }
}

object LocalizedLoggerTest {
  val bundle = new ListResourceBundle {
    override def getContents = Array(
      Array("0", ""),
      Array("1", "%s"),
      Array("2", "%s %s"),
      Array("3", "%s %s %s")
    )
  }
}