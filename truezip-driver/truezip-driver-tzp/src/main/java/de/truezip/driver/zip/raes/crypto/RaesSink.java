/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.crypto;

import de.truezip.kernel.io.Sink;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A sink for writing a RAES file.
 * 
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class RaesSink implements Sink {

    private final Sink sink;
    private final RaesParameters param;

    /**
     * Constructs a new RAES sink.
     * 
     * @param sink the sink for writing the RAES file to.
     * @param param The {@link RaesParameters} used to determine and configure
     *        the type of RAES file created.
     *        If the run time class of this parameter matches multiple
     *        parameter interfaces, it is at the discretion of this
     *        implementation which one is picked and hence which type of
     *        RAES file is created.
     *        If you need more control over this, then just implement the
     *        {@link RaesParametersProvider} interface.
     *        Instances of this interface are queried to find RAES parameters
     *        which match a known RAES type.
     *        This algorithm will be recursively applied.
     */
    public RaesSink(final Sink sink, final RaesParameters param) {
        if (null == (this.sink = sink))
            throw new NullPointerException();
        if (null == (this.param = param))
            throw new NullPointerException();
    }

    /** @throws UnsupportedOperationException */
    @Override
    public SeekableByteChannel newSeekableByteChannel() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws RaesParametersException if {@code param} is {@code null} or
     *         no suitable RAES parameters can be found.
     */
    @Override
    public RaesOutputStream newOutputStream() throws IOException {
        RaesParameters param = this.param;
        while (null != param) {
            // HC SUNT DRACONES!
            if (param instanceof Type0RaesParameters) {
                return new Type0RaesOutputStream(sink,
                        (Type0RaesParameters) param);
            } else if (param instanceof RaesParametersProvider) {
                param = ((RaesParametersProvider) param)
                        .get(RaesParameters.class);
            } else {
                break;
            }
        }
        throw new RaesParametersException("No suitable RAES parameters available!");
    }
}
