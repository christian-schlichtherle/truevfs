/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
