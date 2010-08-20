/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.util.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A thread local {@link Matcher}.
 * This class is intended to be used in multithreaded environments for high
 * performance pattern matching.
 *
 * @see #reset(CharSequence)
 * @version $Id$
 * @since TrueZIP 6.5 (refactored from inner class in
 *        {@link de.schlichtherle.truezip.io.DefaultArchiveDetector})
 */
public class ThreadLocalMatcher extends ThreadLocal<Matcher> {
    private final Pattern pattern;

    /**
     * Creates a new thread local matcher by compiling the given regex.
     *
     * @param regex The expression to be compiled.
     * @throws PatternSyntaxException If the expression's syntax is invalid.
     */
    public ThreadLocalMatcher(final String regex)
    throws PatternSyntaxException {
        this.pattern = Pattern.compile(regex);
    }

    /**
     * Creates a new thread local matcher by using the given pattern.
     *
     * @param pattern The pattern to be used.
     * @throws NullPointerException If the parameter is {@code null}.
     */
    public ThreadLocalMatcher(final Pattern pattern) {
        if (pattern == null)
            throw new NullPointerException();
        this.pattern = pattern;
    }

    @Override
    protected Matcher initialValue() {
        return pattern.matcher(""); // NOI18N
    }

    /**
     * Resets the thread local matcher with the given character sequence and
     * returns it.
     */
    public Matcher reset(CharSequence input) {
        return ((Matcher) get()).reset(input);
    }
}