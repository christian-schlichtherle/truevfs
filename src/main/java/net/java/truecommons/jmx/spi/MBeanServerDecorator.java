/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.jmx.spi;

import javax.management.MBeanServer;
import net.java.truecommons.annotations.ServiceSpecification;
import net.java.truecommons.jmx.sl.MBeanServerLocator;
import net.java.truecommons.services.LocatableDecorator;

/**
 * An abstract service for decorating MBean servers.
 * Decorator services are subject to service location by the
 * {@link MBeanServerLocator#SINGLETON}.
 * <p>
 * If multiple decorator services are locatable on the class path at run time,
 * they are applied in ascending order of their
 * {@linkplain #getPriority() priority} so that the product of the decorator
 * service with the greatest number becomes the head of the resulting product
 * chain.
 *
 * @since  TrueCommons 2.3
 * @author Christian Schlichtherle
 */
@ServiceSpecification
public abstract class MBeanServerDecorator
extends LocatableDecorator<MBeanServer> { }
