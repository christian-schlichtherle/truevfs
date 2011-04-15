/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.file;

import java.io.File;
import java.io.FileNotFoundException;
import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate that two paths are referring to the same file or contain
 * each other.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class TContainsFileException extends FileNotFoundException {
    private static final long serialVersionUID = 1237683475282761476L;

    private final File ancestor, descendant;

    public TContainsFileException(
            final File ancestor,
            final File descendant) {
        super("Paths refer to the same file or contain each other!");
        if (null == ancestor || null == descendant)
            throw new NullPointerException();
        this.ancestor = ancestor;
        this.descendant = descendant;
    }

    public final File getAncestor() {
        return ancestor;
    }

    public final File getDescendant() {
        return descendant;
    }
}
