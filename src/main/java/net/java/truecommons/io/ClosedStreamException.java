/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.io;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

/**
 * Indicates that an input or output stream has been closed.
 *
 * @see    ClosedChannelException
 * @author Christian Schlichtherle
 */
public class ClosedStreamException extends IOException {

    private static final long serialVersionUID = 3438364894193766238L;
}
