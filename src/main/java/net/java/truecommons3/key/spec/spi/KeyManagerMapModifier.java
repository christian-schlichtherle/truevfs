/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec.spi;

import java.util.Map;
import net.java.truecommons3.annotations.ServiceSpecification;
import net.java.truecommons3.key.spec.KeyManager;
import net.java.truecommons3.key.spec.sl.KeyManagerMapLocator;
import net.java.truecommons3.services.LocatableModifier;

/**
 * An abstract service for modifying maps of classes to key managers.
 * Modifier services are subject to service location by the
 * {@link KeyManagerMapLocator#SINGLETON}.
 * If multiple modifier services are locatable on the class path at run
 * time, they are applied in ascending order of their
 * {@linkplain #getPriority() priority} so that the result of the modifier
 * service with the greatest number becomes the result of the entire
 * modifier chain.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@ServiceSpecification
public abstract class KeyManagerMapModifier
extends LocatableModifier<Map<Class<?>, KeyManager<?>>> { }
