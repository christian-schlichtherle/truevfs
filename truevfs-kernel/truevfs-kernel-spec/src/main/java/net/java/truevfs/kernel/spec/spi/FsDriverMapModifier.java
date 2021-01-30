/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.spi;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.service.wight.annotation.ServiceInterface;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsScheme;
import net.java.truevfs.kernel.spec.sl.FsDriverMapLocator;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * A service for modifying maps of file system schemes to file system drivers.
 * Subclasses annotated with {@link ServiceImplementation} are subject to service location by the
 * {@link FsDriverMapLocator#SINGLETON}.
 * <p>
 * If multiple modifier services are located on the class path at run time, they are applied in ascending order of
 * their {@linkplain ServiceImplementation#priority()} so that the product of the modifier service with the greatest
 * number becomes the result of the entire modifier chain.
 *
 * @author Christian Schlichtherle
 */
@ServiceInterface
public interface FsDriverMapModifier extends UnaryOperator<Map<FsScheme, FsDriver>> {
}
