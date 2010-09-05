/*
 * Copyright (C) 2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.filesystem;

/**
 * Visits the children of a directory.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface ChildVisitor {

    /**
     * Called to initialize the implementation before any childre are visited.
     * 
     * @param numChildren the number of children which are going to be visited.
     */
    public void init(int numChildren);

    /**
     * Called to visit the child of a directory.
     *
     * @param child the base name of the child.
     */
    public void visit(String child);
}
