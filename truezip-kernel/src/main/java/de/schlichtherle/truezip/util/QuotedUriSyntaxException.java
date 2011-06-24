/*
 * Copyright (C) 2011 Schlichtherle IT Services
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

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URISyntaxException;

/**
 * Quotes the input string before passing it to the super class constructor.
 * 
 * @version $Id$
 * @author  Christian Schlichtherle
 */
@DefaultAnnotation(NonNull.class)
public class QuotedUriSyntaxException extends URISyntaxException {
    private static final long serialVersionUID = 2452323414521345231L;

    /**
     * Constructs a new quoted input URI syntax exception.
     * 
     * @param input the object with the
     *        {@link Object#toString() string representation} to put in quotes.
     * @param reason a string explaining why the input could not be parsed.
     */
    public QuotedUriSyntaxException(Object input, String reason) {
        this(input, reason, -1);
    }

    /**
     * Constructs a new quoted input URI syntax exception.
     * 
     * @param input the object with the
     *        {@link Object#toString() string representation} to put in quotes.
     * @param reason a string explaining why the input could not be parsed.
     * @param index the index at which the parse error occurred,
     *        or {@code -1} if unknown.
     */
    public QuotedUriSyntaxException(Object input, String reason, int index) {
        super("\"" + input + "\"", reason, index);
    }
}
