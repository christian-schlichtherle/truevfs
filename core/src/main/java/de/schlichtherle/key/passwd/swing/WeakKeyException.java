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

import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Thrown to indicate that a password or key file is considered weak.
 *
 * @author Christian Schlichtherle
 * @version $Revision$
 * @since TrueZIP 6.1
 */
public class WeakKeyException extends GeneralSecurityException {

    // TODO: Remove this.
    /**
     * @deprecated You should not use this constructor.
     *             It will vanish in the next major version.
     * @param key The resource bundle key for the localized message.
     * @see #getLocalizedMessage()
     */
    public WeakKeyException(ResourceBundle resources, String key) {
        super(CreateKeyPanel.localizedMessage(resources, key, null));
    }

    // TODO: Remove this.
    /**
     * @deprecated You should not use this constructor.
     *             It will vanish in the next major version.
     * @param key The resource bundle key for the localized message.
     * @see #getLocalizedMessage()
     */
    public WeakKeyException(ResourceBundle resources, String key, Object param) {
        super(CreateKeyPanel.localizedMessage(resources, key, param));
    }

    /**
     * Creates a new <code>WeakKeyException</code> with the given localized
     * message.
     */
    public WeakKeyException(String localizedMessage) {
        super(localizedMessage);
    }
}
