/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.schlichtherle.key.passwd.swing;

import de.schlichtherle.key.*;

import java.util.logging.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.1
 */
public class AesKeyMgmtLifeCycle extends KeyMgmtLifeCycle {

    private static final Logger logger
            = Logger.getLogger(AesKeyMgmtLifeCycle.class.getName());

    private int keyStrength;

    /**
     * @param id The identifier of the protected resource.
     */
    public AesKeyMgmtLifeCycle(final String id) {
        super(id);
    }

    protected KeyProvider getKeyProvider(KeyManager manager, String id) {
        return manager.getKeyProvider(id, AesKeyProvider.class);
    }

    protected void createResourceHook(KeyProvider provider) {
        // In a real world application, you should never blindly cast
        // a key provider.
        this.keyStrength = ((AesKeyProvider) provider).getKeyStrength();

        printKeyStrength(provider);
    }

    protected void openResourceHook(KeyProvider provider) {
        // In a real world application, you should never blindly cast
        // a key provider.
        ((AesKeyProvider) provider).setKeyStrength(keyStrength);

        printKeyStrength(provider);
    }

    private void printKeyStrength(KeyProvider provider) {
        String msg = id + ": key strength is ";
        switch(keyStrength) {
            case AesKeyProvider.KEY_STRENGTH_128:

                msg += "128";

                break;
            case AesKeyProvider.KEY_STRENGTH_192:

                msg += "192";

                break;
            case AesKeyProvider.KEY_STRENGTH_256:

                msg += "256";

                break;
            default:

                throw new AssertionError("Illegal key strength!");
        }
        msg += " bits.";
        logger.fine(msg);
    }
}