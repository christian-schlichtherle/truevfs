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

import de.schlichtherle.truezip.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @param   <CE> The type of the {@link #getLocalTarget() local target}.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class ProxyOutputSocket<CE extends CommonEntry>
extends FilterOutputSocket<CE> {
    public ProxyOutputSocket(OutputSocket<? extends CE> output) {
        super(output);
    }

    @Override
    public OutputStream newOutputStream() throws IOException {
        class ProxyOutputStream extends FilterOutputStream {
            ProxyOutputStream() {
                super(null);
            }

            OutputStream getOutputStream() throws IOException {
                return null != out ? out : (out = getOutputSocket().newOutputStream());
            }

            @Override
            public void write(int b) throws IOException {
                getOutputStream().write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                getOutputStream().write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                getOutputStream().flush();
            }

            @Override
            public void close() throws IOException {
                if (null == out)
                    return;
                out.close();
            }
        }
        return new ProxyOutputStream();
    }
}
