/*
 * Copyright 2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.socket;

/**
 * Creates input and output streams for reading and writing bytes from and to
 * its target.
 *
 * @param   <T> The type of the {@link #getTarget() target} of this instance.
 * @param   <O> The minimum required type of the <i>peer</i> targets for
 *          reading and writing from and to the target of this instance.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface IOStreamSocket<T, O>
extends InputStreamSocket<T, O>, OutputStreamSocket<O, T> {
}
