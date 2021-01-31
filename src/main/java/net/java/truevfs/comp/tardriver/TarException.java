/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.tardriver;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates that there is an issue when reading or writing a TAR file which
 * is specific to the TAR file format.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class TarException extends IOException {

    private static final long serialVersionUID = 0;

    public TarException(String message) {
        super(message);
    }

    public TarException(String message, Throwable cause) {
        super(message, cause);
    }
}
