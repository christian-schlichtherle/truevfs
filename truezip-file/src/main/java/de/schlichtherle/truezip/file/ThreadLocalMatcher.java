/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.jcip.annotations.ThreadSafe;

/**
 * A thread local {@link Matcher}.
 * This class is intended to be used in multithreaded environments for high
 * performance pattern matching.
 *
 * @see #reset(CharSequence)
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
final class ThreadLocalMatcher extends ThreadLocal<Matcher> {
    private final Pattern pattern;

    /**
     * Constructs a new thread local matcher by using the given pattern.
     *
     * @param  pattern the pattern to be used.
     */
    public ThreadLocalMatcher(Pattern pattern) {
        if (null == pattern)
            throw new NullPointerException();
        this.pattern = pattern;
    }

    @Override
    protected final Matcher initialValue() {
        return pattern.matcher(""); // NOI18N
    }

    /**
     * Resets the thread local matcher with the given character sequence and
     * returns it.
     */
    public final Matcher reset(CharSequence input) {
        return get().reset(input);
    }
}