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
package de.schlichtherle.truezip.util;

import java.io.IOException;
import java.util.Enumeration;

/**
 * Provides static utility methods for convenient class and resource loading
 * which is designed to work in both JEE and OSGi environments.
 * <p>
 * <b>Warning:</b> This class is <em>not</em> intended for public use!
 * It's just a workaround which will exist until the introduction of a better,
 * but probably non-backwards-compatible solution in TrueZIP 7.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
// TODO: Exchange this class for a more general solution, e.g. OSGi.
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
    public static Class loadClass(String classToLoad, Class loadingClass)
    throws ClassNotFoundException {
        ClassLoader l1 = loadingClass.getClassLoader();
        if (l1 == null)
            l1 = ClassLoader.getSystemClassLoader(); // just in case somebody adds TrueZIP to the Boot Class Path
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

    /**
     * Concatenates the enumeration of the resource {@code name} on the class
     * path by using the class loader of the class {@code loadingClass} and the
     * current thread's context class loader or, if not available, the system
     * class loader.
     *
     * @param name The resource to enumerate.
     * @param loadingClass The class which wants to enumerate {@code name}.
     * @return A joint enumeration for the resource {@code name} on the class
     *         path.
     * @throws IOException If I/O errors occur.
     */
    public static Enumeration getResources(String name, Class loadingClass)
    throws IOException {
        ClassLoader l1 = loadingClass.getClassLoader();
        if (l1 == null)
            l1 = ClassLoader.getSystemClassLoader(); // just in case somebody adds TrueZIP to the Boot Class Path
        ClassLoader l2 = Thread.currentThread().getContextClassLoader();
        if (l2 == null)
            l2 = ClassLoader.getSystemClassLoader();
        return l1 == l2
                ? l1.getResources(name)
                : new JointEnumeration( l1.getResources(name),
                                        l2.getResources(name));
    }
}
