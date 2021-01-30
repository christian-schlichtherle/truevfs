/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides a key manager which promps users for
 * {@linkplain net.java.truecommons.key.spec.prompting.PromptingKey keys}.
 * The manager adapts to different user interface technologies by using a
 * {@linkplain net.java.truecommons.key.spec.prompting.PromptingKey.View view}.
 * Different modules are provided to adapt to concrete user interface
 * technologies, e.g. Swing or the Console.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@javax.annotation.Nonnull @javax.annotation.ParametersAreNonnullByDefault
package net.java.truecommons.key.spec.prompting;
