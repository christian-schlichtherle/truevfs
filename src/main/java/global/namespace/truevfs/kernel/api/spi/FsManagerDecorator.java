/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api.spi;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.service.wight.annotation.ServiceInterface;
import global.namespace.truevfs.kernel.api.FsManager;
import global.namespace.truevfs.kernel.api.sl.FsManagerLocator;

import java.util.function.UnaryOperator;

/**
 * A service for decorating file system managers.
 * Subclasses annotated with {@link ServiceImplementation} are subject to service location by the
 * {@link FsManagerLocator#SINGLETON}.
 * <p>
 * If multiple decorator services are locatable on the class path at run time, they are applied in ascending order of
 * their {@linkplain ServiceImplementation#priority()} so that the product of the decorator service with the greatest
 * number becomes the head of the resulting decorator chain.
 *
 * @author Christian Schlichtherle
 */
@ServiceInterface
public interface FsManagerDecorator extends UnaryOperator<FsManager> {
}
