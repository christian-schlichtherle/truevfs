/*
 * Copyright 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.http;

import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.net.URI;
import net.jcip.annotations.Immutable;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * A file system driver for the HTTP(S) schemes.
 * 
 * @see     #newClient()
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public class HttpDriver extends FsDriver {

    private final IOPoolProvider provider;
    private volatile @CheckForNull HttpClient client;

    public HttpDriver(final IOPoolProvider provider) {
        if (null == provider)
            throw new NullPointerException();
        this.provider = provider;
    }

    IOPool<?> getPool() {
        return provider.get();
    }

    /**
     * Returns the cached http client obtained by calling {@link #newClient()}.
     * 
     * @return The cached http client obtained by calling {@link #newClient()}.
     */
    protected final HttpClient getClient() {
        final HttpClient client = this.client;
        return null != client ? client : (this.client = newClient());
    }

    /**
     * Returns a new http client.
     * <p>
     * The implementation in the class {@link HttpDriver} simply returns
     * {@code new DefaultHttpClient()}.
     * If you need special configuration, e.g. for authentication or caching,
     * then you should override this method.
     * 
     * @return A new http client.
     */
    protected HttpClient newClient() {
        return new DefaultHttpClient();
    }

    /**
     * Executes the HEAD request method for the given URI.
     * Equivalent to {@code getClient().execute(new HttpHead(uri))}.
     */
    protected HttpResponse executeHead(URI uri) throws IOException {
        return getClient().execute(new HttpHead(uri));
    }

    /**
     * Executes the GET request method for the given URI.
     * Equivalent to {@code getClient().execute(new HttpGet(uri))}.
     */
    protected HttpResponse executeGet(URI uri) throws IOException {
        return getClient().execute(new HttpGet(uri));
    }

    @Override
    public FsController<?>
    newController(FsModel model, @CheckForNull FsController<?> parent) {
        assert null == model.getParent()
                ? null == parent
                : model.getParent().equals(parent.getModel());
        if (null != parent)
            throw new IllegalArgumentException();
        return new HttpController(this, model);
    }
}
