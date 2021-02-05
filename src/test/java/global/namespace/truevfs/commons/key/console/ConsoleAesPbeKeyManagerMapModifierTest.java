/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.console;

import global.namespace.truevfs.commons.key.api.spi.KeyManagerMapModifier;
import global.namespace.truevfs.commons.key.api.spi.KeyManagerMapModifierTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class ConsoleAesPbeKeyManagerMapModifierTest extends KeyManagerMapModifierTestSuite {

    @Override
    protected KeyManagerMapModifier newModifier() {
        return new ConsoleAesPbeKeyManagerMapModifier();
    }
}
