/*
 * Copyright (C) 2004-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.fs;

import java.io.IOException;

/**
 * Indicates that a file system is a false positive file system.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
public final class FsFalsePositiveException extends FsException {

    public FsFalsePositiveException(FsModel model, IOException cause) {
        super(model, cause);
        assert !(cause instanceof FsException);
    }
}
