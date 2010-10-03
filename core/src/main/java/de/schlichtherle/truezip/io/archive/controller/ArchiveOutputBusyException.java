/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.archive.descriptor.ArchiveDescriptor;

/**
 * Like its super class, but indicates the existance of open output streams.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ArchiveOutputBusyException
extends ArchiveBusyException {

    private static final long serialVersionUID = 4652936465192837172L;

    ArchiveOutputBusyException(ArchiveDescriptor archive, int numStreams) {
        super(archive, numStreams);
    }
}
