/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides the Swing based implementation of the interface
 * {@link de.truezip.key.PromptingKeyProvider.View}.
 * If a key file is used for authentication instead of a password, then the
 * file size must be 512 bytes or more, of which only the first 512 bytes are
 * used.
 */
@edu.umd.cs.findbugs.annotations.DefaultAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class)
package de.truezip.key.impl.pbe.swing;