/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import javax.annotation.concurrent.Immutable;

/**
 * @deprecated Since TrueZIP 7.5.3, the functionality of this class has moved
 *             to {@link FsDefaultManager}.
 *             It's now nothing but a useless decorator and will be removed in
 *             TrueZIP 8.
 * @author Christian Schlichtherle
 */
@Immutable
@Deprecated
public final class FsFailSafeManager extends FsDecoratingManager<FsManager> {

    public FsFailSafeManager(FsManager manager) {
        super(manager);
    }
}
