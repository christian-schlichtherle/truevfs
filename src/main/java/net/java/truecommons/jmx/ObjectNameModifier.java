/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.jmx;

import javax.annotation.Nullable;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

/**
 * An encoder and decoder for JMX {@linkplain ObjectName object names}.
 * <p>
 * Implementations need to be safe for multi-threading.
 *
 * @since  TrueCommons 2.3
 * @author Christian Schlichtherle
 */
public interface ObjectNameModifier {

    /**
     * Encodes the given object name.
     *
     * @param name the object name to encode.
     * @return The encoded object name.
     */
    @Nullable ObjectName apply(@Nullable ObjectName name);

    /**
     * Decodes the given object name.
     *
     * @param name the object name to decode.
     * @return The decoded object name.
     */
    @Nullable ObjectName unapply(@Nullable ObjectName name);

    /**
     * Encodes the object name in the given object instance.
     *
     * @param instance the object instance with the object name to encode.
     * @return The transformed object instance.
     */
    @Nullable ObjectInstance apply(@Nullable ObjectInstance instance);

    /**
     * Decodes the object name in the given object instance.
     *
     * @param instance the object instance with the object name to decode.
     * @return The transformed object instance.
     */
    @Nullable ObjectInstance unapply(@Nullable ObjectInstance instance);
}
