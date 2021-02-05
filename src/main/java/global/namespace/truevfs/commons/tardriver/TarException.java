/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.tardriver;

import java.io.IOException;

/**
 * Indicates that there is an issue when reading or writing a TAR file which
 * is specific to the TAR file format.
 * 
 * @author Christian Schlichtherle
 */
public class TarException extends IOException {

    private static final long serialVersionUID = 0;

    public TarException(String message) {
        super(message);
    }

    public TarException(String message, Throwable cause) {
        super(message, cause);
    }
}
