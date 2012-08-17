/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides the Swing based implementation of the interface
 * {@link net.java.truevfs.key.spec.PromptingKeyProvider.View}.
 * If a key file is used for authentication instead of a password, then the
 * file size must be 512 bytes or more, of which only the first 512 bytes are
 * used.
 */
@javax.annotation.Nonnull @javax.annotation.ParametersAreNonnullByDefault
package net.java.truevfs.key.swing;
