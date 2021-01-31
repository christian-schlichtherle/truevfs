/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.jmx.spi;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.service.wight.annotation.ServiceInterface;
import net.java.truecommons.jmx.sl.MBeanServerLocator;

import javax.management.MBeanServer;
import java.util.function.UnaryOperator;

/**
 * An abstract service for decorating MBean servers.
 * Decorator services are subject to service location by the
 * {@link MBeanServerLocator#SINGLETON}.
 * <p>
 * If multiple decorator services are locatable on the class path at run time, they are applied in ascending order of
 * their {@linkplain ServiceImplementation#priority()} so that the product of the decorator service with the greatest
 * number becomes the head of the resulting product chain.
 *
 * @author Christian Schlichtherle
 * @since TrueCommons 2.3
 */
@ServiceInterface
public interface MBeanServerDecorator extends UnaryOperator<MBeanServer> {
}
