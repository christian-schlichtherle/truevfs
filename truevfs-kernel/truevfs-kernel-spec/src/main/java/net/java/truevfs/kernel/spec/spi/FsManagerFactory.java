/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.spi;

import net.java.truecommons.annotations.ServiceSpecification;
import net.java.truecommons.services.LocatableFactory;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.sl.FsManagerLocator;

/**
 * An abstract service for creating file system managers.
 * Factory services are subject to service location by the
 * {@link FsManagerLocator#SINGLETON}.
 * <p>
 * If multiple factory services are locatable on the class path at run time,
 * the service with the greatest {@linkplain #getPriority() priority} gets
 * selected.
 *
 * @author Christian Schlichtherle
 */
@ServiceSpecification
public abstract class FsManagerFactory
extends LocatableFactory<FsManager> { }
