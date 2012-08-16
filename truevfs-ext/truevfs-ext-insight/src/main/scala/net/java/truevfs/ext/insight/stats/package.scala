/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

/**
  * Provides file system statistics for the TrueVFS Kernel.
  * 
  * @author Christian Schlichtherle
  */
package object stats {

  private[stats] def hash(thread: Thread) = {
    var hash = 17L;
    hash = 31 * hash + System.identityHashCode(thread)
    hash = 31 * hash + thread.getId
    hash
  }
}
