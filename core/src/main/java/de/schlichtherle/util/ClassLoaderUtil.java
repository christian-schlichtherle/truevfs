/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.util;

/**
 * Provides a static utility method for convenient class loading which is
 * designed to work in both JEE and OSGi environments.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.8
 */
public class ClassLoaderUtil {

    private ClassLoaderUtil() {
    }

    /**
     * Tries to load the class {@code classToLoad} using the class loader of
     * the class {@code loadingClass} first or, failing that, using the
     * current thread's context class loader or, if not available, the system
     * class loader.
     *
     * @param classToLoad The class to load.
     * @param loadingClass The class which wants to load {@code classToLoad}.
     * @return The loaded class.
     * @throws ClassNotFoundException If loading the class failed for some
     *         reason.
     */
    public static Class load(String classToLoad, Class loadingClass)
    throws ClassNotFoundException {
        final ClassLoader l1 = loadingClass.getClassLoader();
        try {
            return l1.loadClass(classToLoad);
        } catch (ClassNotFoundException cnfe) {
            ClassLoader l2 = Thread.currentThread().getContextClassLoader();
            if (l2 == null)
                l2 = ClassLoader.getSystemClassLoader();
            if (l1 == l2)
                throw cnfe; // optimization: there's no point in trying this twice.
            return l2.loadClass(classToLoad);
        }
    }
}
