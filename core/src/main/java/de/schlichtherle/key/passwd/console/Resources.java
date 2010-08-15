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

package de.schlichtherle.key.passwd.console;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Convenience class to look up the bundle in a {@link ResourceBundle}.
 * Provided for comfort.
 * 
 * @author Christian Schlichtherle
 */
class Resources {

    /** The resource bundle used. */
    public final ResourceBundle bundle;

    public Resources(String baseName) {
        bundle = ResourceBundle.getBundle(baseName);
    }

    /**
     * Looks up a string resource identified by <code>key</code> in
     * <code>bundle</code>.
     */
    public final String getString(String key) {
        return bundle.getString(key);
    }

    /**
     * Looks up a string resource identified by <code>key</code> in
     * <code>bundle</code> and formats it as a message using
     * <code>MessageFormat.format</code> with the given <code>arguments</code>.
     */
    public final String getString(String key, Object[] arguments) {
        return MessageFormat.format(getString(key), arguments);
    }
    
    /**
     * Looks up a string resource identified by <code>key</code> in
     * <code>bundle</code> and formats it as a message using
     * <code>MessageFormat.format</code> with the given singular <code>argument</code>.
     */
    public final String getString(String key, Object argument) {
        return MessageFormat.format(getString(key), new Object[] { argument });
    }
    
    /**
     * Looks up a string resource identified by <code>key</code> in
     * <code>bundle</code> and formats it as a message using
     * <code>MessageFormat.format</code> with the given singular <code>argument</code>.
     */
    public final String getString(String key, int argument) {
        return MessageFormat.format(getString(key), new Object[] { new Integer(argument) });
    }
}
