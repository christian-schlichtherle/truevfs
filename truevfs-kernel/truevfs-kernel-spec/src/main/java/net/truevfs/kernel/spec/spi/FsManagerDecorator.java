/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.spi;

import javax.annotation.concurrent.ThreadSafe;
import net.truevfs.kernel.spec.FsManager;
import net.truevfs.kernel.spec.sl.FsManagerLocator;

/**
 * An abstract locatable service for decorating file system managers.
 * Implementations of this abstract class are subject to service location
 * by the class {@link FsManagerLocator}.
 * <p>
 * Implementations should be thread-safe.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class FsManagerDecorator extends ServiceProvider {

    /**
     * Decorates the given file system manager.
     * 
     * @param  manager the file system manager to decorate.
     * @return The decorated file system manager.
     */
    public abstract FsManager decorate(FsManager manager);

    /**
     * Returns a priority to help service locators to prioritize the services
     * provided by this object.
     * The decorators will be applied in ascending order of priority so that
     * the decorator with the greatest number becomes the head of the decorator
     * chain.
     * <p>
     * The implementation in the class {@link FsManagerDecorator} returns
     * zero.
     * 
     * @return A priority to help service locators to prioritize the services
     *         provided by this object.
     */
    @Override
    public int getPriority() {
        return 0;
    }
}
