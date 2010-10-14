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
package de.schlichtherle.truezip.io.socket;

/**
 * @see     TargetOutputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class TargetInputSocket<CE extends CommonEntry>
extends FilterInputSocket<CE> {

    private final CE target;

    public TargetInputSocket(   final CE target,
                                final InputSocket<? extends CE> input) {
        super(input);
        if (null == target)
            throw new NullPointerException();
        this.target = target;
    }

    @Override
    public final CE getLocalTarget() {
        return target;
    }
}
