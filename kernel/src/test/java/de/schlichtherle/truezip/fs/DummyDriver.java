/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.fs;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class DummyDriver extends FsDriver {

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link DummyDriver} always returns
     * {@code false}.
     */
    @Override
    public boolean isFederated() {
        return false;
    }

    @Override
    public FsController<?>
    newController(FsMountPoint mountPoint, FsController<?> parent) {
        assert null == mountPoint.getParent()
                ? null == parent
                : mountPoint.getParent().equals(parent.getModel().getMountPoint());
        return new DummyController<FsModel>(
                new FsModel(mountPoint, null == parent ? null : parent.getModel()), parent);
    }
}
