/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.shed;

/**
 * @author Christian Schlichtherle
 */
@SuppressWarnings("serial")
final class WarningException extends TestException {

    WarningException(int id, TestException... suppressed) {
        super(id, suppressed);
    }

    @Override
    int getPriority() {
        return -1;
    }
}
