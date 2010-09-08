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
 * Visits the members of a directory.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface MemberVisitor {

    /**
     * Called to initialize the implementation before any members are visited.
     * Note that this method is called exactly once if and only if the
     * directory is accessible.
     * 
     * @param numMembers the number of members which are going to be visited.
     */
    public void init(int numMembers);

    /**
     * Called to visit the member of a directory.
     * Note that this method is called for each member if and only if the
     * directory is accessible.
     *
     * @param member the base name of the directory member.
     */
    public void visit(String member);
}
