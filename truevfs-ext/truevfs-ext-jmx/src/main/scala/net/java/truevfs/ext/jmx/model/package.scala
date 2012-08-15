/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx

/**
  * @author Christian Schlichtherle
  */
package object model {

  private[model] def hash(thread: Thread) = {
    var hash = 17L;
    hash = 31 * hash + System.identityHashCode(thread)
    hash = 31 * hash + thread.getId
    hash
  }
}
