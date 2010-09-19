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
 * its local target for I/O operations.
 *
 * @param   <LT> The type of the <i>local target</i> for I/O operations.
 *          i.e. the {@link #getTarget() target} of this instance.
 * @param   <PT> The minimum required type of the <i>peer targets</i> for
 *          for I/O operations.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface IOStreamSocket<LT, PT>
extends InputStreamSocket<LT, PT>, OutputStreamSocket<LT, PT> {
}
