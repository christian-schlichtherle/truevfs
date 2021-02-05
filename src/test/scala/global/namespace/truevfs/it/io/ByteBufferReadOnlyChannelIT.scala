/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.io

import global.namespace.truevfs.commons.io.{ByteBufferChannel, ReadOnlyChannel}

import java.nio._
import java.nio.file._

/**
  * @author Christian Schlichtherle
  */
class ByteBufferReadOnlyChannelIT extends ReadOnlyChannelITSuite {

  override def newChannel(path: Path) =
    new ReadOnlyChannel(new ByteBufferChannel(ByteBuffer.wrap(Files.readAllBytes(path))))
}
