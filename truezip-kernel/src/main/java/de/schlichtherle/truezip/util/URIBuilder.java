/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
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

package de.schlichtherle.truezip.util;

import edu.umd.cs.findbugs.annotations.*;

import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

import net.jcip.annotations.*;

import truecollections.*;
import truecollections.CacheMap.*;
import static truecollections.CacheStrategy.*;

/**
 * A mutable JavaBean for building URIs according to RFC&nbsp;2396.
 * This class complements the immutable {@link URI} class:
 * It enables clients to build a URI step-by-step by setting its components
 * as <em>independent</em> properties of a URI builder instance.
 * <p>
 * Each URI is composed of the properties {@link #getScheme scheme},
 * {@link #getUserInfo userInfo}, {@link #getHost host},
 * {@link #getPort port}, {@link #getPath path}, {@link #getQuery query}
 * and {@link #getFragment fragment}.
 * Note that unlike the {@code URI} class, this class does <em>not</em>
 * support IPv6 addresses in the host component.
 * <p>
 * The {@code path} and {@code query} components are further divided into
 * elements which can be accessed via the mutable views returned by
 * {@link #getPathSegments}, {@link #getParameters}, {@link #getParameterNames}
 * and {@link #getParameterValues} respectively.
 * <p>
 * The composed URI can be obtained by calling {@link #toString} or
 * {@link #getURI}.
 * While {@link #toString} does not validate the composed string against the
 * URI syntax defined in RFC&nbsp;2396, {@link #getURI} throws a
 * {@link URISyntaxException} if it's invalid.
 * Note that the returned URI may still be nonsensical.
 * It's the client's responsibility to set the properties of the URI builder
 * instance to meaningful values before calling {@code getURI()} in order
 * to obtain a meaningful URI.
 * <p>
 * Unless otherwise noted, all methods accept and may return {@code null}.
 * <p>
 * This class does <em>not</em> encode/decode illegal characters in a URI
 * (see {@link URICodec}).
 * <p>
 * This class is <em>not</em> thread-safe.
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @see <a target="_blank" href="http://www.ietf.org/rfc/rfc2396.txt">
 *      <i>RFC&nbsp;2396: Uniform Resource Identifiers (URI): Generic Syntax</i></a>
 */
@DefaultAnnotation({ CheckReturnValue.class, CheckForNull.class })
@NotThreadSafe
public final class URIBuilder {

    //
    // Derived from RFC 2396, Appendix A & B.
    //

    // ParameterComponents: These may be prefixed and/or postfixed and consist of a
    // single sequence of characters as the first capturing group.
    private static final String SCHEME_REGEX = "([^:/?#]+):";
    private static final String USER_INFO_REGEX = "([^/?#]*)@";
    private static final String HOST_REGEX = "([^/?#]*)";
    private static final String PORT_REGEX = ":([^/?#]*)";
    private static final String PATH_REGEX = "([^?#]*)";
    private static final String QUERY_REGEX = "\\?([^#]*)";
    private static final String FRAGMENT_REGEX = "#(.*)";

    // Composites: These are composed of one or more components.
    // ParameterComponents may be optional.
    // Composites do not add capturing groups of their own.
    private static final String AUTHORITY_REGEX = "(?:" + USER_INFO_REGEX + ")?" + HOST_REGEX + "(?:" + PORT_REGEX + ")?";
    private static final String SCHEME_SPECIFIC_PART_REGEX = "(?://" + AUTHORITY_REGEX + ")?" + PATH_REGEX + "(?:" + QUERY_REGEX + ")?";
    private static final String URI_REGEX = "^(?:" + SCHEME_REGEX + ")?(?:" + SCHEME_SPECIFIC_PART_REGEX + ")(?:" + FRAGMENT_REGEX + ")?$";

    // The composites as patterns.
    //private static final Pattern AUTHORITY_PATTERN = Pattern.compile(AUTHORITY_REGEX);
    //private static final Pattern SCHEME_SPECIFIC_PART_PATTERN = Pattern.compile(SCHEME_SPECIFIC_PART_REGEX);
    private static final Pattern URI_PATTERN = Pattern.compile(URI_REGEX);

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private String scheme;
    private String userInfo;
    private String host;
    private int port = -1;
    private final List<String> path = new LinkedList<String>();
    private final Query query = new Query();
    private String fragment;

    @CheckForNull
    private ParametersView parametersView;

    @CheckForNull
    private ParameterNamesView parameterNamesView;

    public URIBuilder() {
    }

    public URIBuilder(final URI uri) {
        setURI(uri); // class is final!
    }

    public URIBuilder(final String uri) {
        setURI(uri); // class is final!
    }

    /**
     * Resets the URI from the given URI.
     * This method initializes all properties of this builder to reflect the
     * given URI.
     * In particular, the {@link #setParameterSorting querySorting} property is
     * reset to {@code false}.
     */
    public final void setURI(final URI uri) {
        setScheme(uri);
        setUserInfo(uri);
        setHost(uri);
        setPort(uri);
        setPath(uri == null
                ? null
                : uri.isOpaque() ? uri.getSchemeSpecificPart() : uri.getPath());
        setQuery(uri);
        setParameterSorting(false);
        setFragment(uri);
    }

    /**
     * Resets the URI from the given URI string.
     * This method initializes all properties of this builder to reflect the
     * given URI.
     * In particular, the {@link #setParameterSorting querySorting} property is
     * reset to {@code false}.
     * 
     * @throws NumberFormatException If a port is given which is not a number.
     */
    public void setURI(final String uri) {
        if (uri == null) {
            setURI((URI) null); // don't bother me
            return;
        }

        final Matcher matcher = URI_PATTERN.matcher(uri);
        if (!matcher.matches())
            throw new AssertionError("bug in URI pattern");
        assert matcher.group(5) != null;
        setPort(matcher.group(4)); // may throw NumberFormatException - apply first!
        setScheme(matcher.group(1));
        setUserInfo(matcher.group(2));
        setHost(matcher.group(3));
        setPath(matcher.group(5));
        setQuery(matcher.group(6));
        setParameterSorting(false);
        setFragment(matcher.group(7));
    }

    /**
     * Returns a new validated URI which is composed of the properties of
     * this URI builder.
     * The URI is validated against the syntax defined in RFC&nbsp;2396.
     * <p>
     * Note that although the returned URI is validated against URI syntax,
     * it may still be nonsensical.
     * It's the client's responsibility to set the properties of this
     * builder to meaningful values before calling this method in order
     * to get a meaningful URI.
     *
     * @return The resulting validated, but probably nonsensical URI.
     *         This is never {@code null}.
     * @throws URISyntaxException If the resulting URI would violate the
     *         syntax as defined in RFC&nbsp;2396.
     */
    @NonNull
    public URI getURI() throws URISyntaxException {
        return new URI(toString());
    }

    /**
     * Returns a new prospective URI which is composed of the properties of
     * this URI builder.
     * The URI is <em>not</em> validated against the syntax defined in
     * RFC&nbsp;2396.
     * To validate the prospective URI, use {@link #getURI} instead.
     *
     * @return The resulting invalidated URI string.
     *         This is never {@code null}, but may be empty.
     */
    @NonNull
    @Override
    public String toString() {
        final String scheme   = getScheme();
        final String userInfo = getUserInfo();
        final String host     = getHost();
        final int    port     = getPort();
        final String path     = getPath();
        final String query    = getQuery();
        final String fragment = getFragment();

        final StringBuilder uriBuilder = new StringBuilder();
        if (scheme != null)
            uriBuilder.append(scheme).append(':');
        if (userInfo != null || host != null || port >= 0)
            uriBuilder.append("//");
        if (userInfo != null)
            uriBuilder.append(userInfo).append('@');
        if (host != null)
            uriBuilder.append(host);
        if (port >= 0)
            uriBuilder.append(':').append(port);
        if (path != null)
            uriBuilder.append(path);
        if (query != null)
            uriBuilder.append('?').append(query);
        if (fragment != null)
            uriBuilder.append('#').append(fragment);
        return uriBuilder.toString();
    }

    public final void setScheme(final URI uri) {
        setScheme(uri != null ? uri.getScheme() : null);
    }

    public void setScheme(final String scheme) {
        this.scheme = scheme;
    }

    @CheckForNull
    public String getScheme() {
        return scheme;
    }

    public final void setUserInfo(final URI uri) {
        setUserInfo(uri != null ? uri.getUserInfo() : null);
    }

    public void setUserInfo(final String userInfo) {
        this.userInfo = userInfo != null ? userInfo : null;
    }

    @CheckForNull
    public String getUserInfo() {
        return userInfo;
    }

    public final void setHost(final URI uri) {
        setHost(uri != null ? uri.getHost() : null);
    }

    public void setHost(final String host) {
        this.host = host;
    }

    @CheckForNull
    public String getHost() {
        return host;
    }

    public final void setPort(final URI uri) {
        setPort(uri != null ? uri.getPort() : -1);
    }

    private void setPort(final String port) {
        this.port = port != null && port.length() > 0 ? Integer.parseInt(port) : -1;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    /**
     * Copies the path string from the given URI.
     * The path string is decomposed into its segments as described in
     * {@link #getPathSegments}.
     * 
     * @param uri The URI which's path string is to be copied.
     *        A {@code null} value leaves the path string undefined.
     * @see #getPathSegments
     */
    public final void setPath(final URI uri) {
        setPath(uri != null ? uri.getPath() : null);
    }

    /**
     * Sets the path string.
     * The path string is decomposed into its segments as described in
     * {@link #getPathSegments}.
     * 
     * @param path The path string.
     *        A {@code null} value leaves the path string undefined.
     * @see #getPathSegments
     */
    public void setPath(final String path) {
        this.path.clear();
        if (path != null)
            this.path.addAll(Arrays.asList(path.split("/", -1)));
    }

    /**
     * Returns the path string.
     * The path string is composed from its segments as described in
     * {@link #getPathSegments}.
     * 
     * @return The path string.
     *         A {@code null} value means the path string is undefined.
     * @see #getPathSegments
     */
    public String getPath() {
        if (path.isEmpty())
            return null;

        final StringBuilder pathBuilder = new StringBuilder();
        int numSegments = 0;
        final Iterator<String> i = path.iterator();
        while (i.hasNext()) {
            final String segment = i.next();
            if (segment == null) {
                i.remove(); // condense
                continue;
            }
            if (numSegments++ > 0)
                pathBuilder.append('/');
            pathBuilder.append(segment);
        }
        return pathBuilder.toString();
    }

    /**
     * Returns a mutable view of the segments in the path string.
     * <p>
     * This method decomposes the path string into zero or more segments
     * which are separated by {@code '/'}.
     * Segments may be empty.
     * Note that this definition corresponds to RFC&nbsp;2396.
     * <p>
     * The returned view lists each segment in order.
     * Modifying the list updates the path string and vice versa.
     */
    @NonNull
    public List<String> getPathSegments() {
        return path;
    }

    /**
     * Copies the query string from the given URI.
     * The query string is decomposed into its segments as described in
     * {@link #getParameters}, where consecutive segments are separated
     * by a {@code '&'} character.
     * 
     * @param uri The URI which's query string is to be copied.
     *        A {@code null} value leaves the query string undefined.
     * @see #getQuery
     */
    public final void setQuery(final URI uri) {
        setQuery(uri != null ? uri.getQuery() : null);
    }

    /**
     * Sets the query string.
     * The query string is decomposed into its segments as described in
     * {@link #getParameters}, where consecutive segments are separated
     * by a {@code '&'} character.
     * 
     * @param query The query string.
     *        A {@code null} value leaves the query string undefined.
     * @see #getQuery
     */
    public void setQuery(final String query) {
        this.query.set(query);
    }

    /**
     * Returns the query string.
     * The query string is composed from its segments in order as described
     * in {@link #getParameters}, where consecutive segments are
     * separated by a {@code '&'} character.
     * <p>
     * If and only if the {@link #isParameterSorting querySorting} property of
     * this URI builder is {@code true}, then the query parameters in the
     * returned string are sorted in ascending natural order.
     * The sorting is applied upon each call to this method and does
     * <em>not</em> affect the view collections.
     * 
     * @return The query string.
     *         A {@code null} value means the query string is undefined.
     * @see #setQuery
     * @see #getParameters
     * @see #getParameterNames
     * @see #getParameterValues
     * @see #isParameterSorting
     */
    public String getQuery() {
        return query.toString();
    }

    /**
     * Returns a mutable view of the parameter <i>&lt;name&gt;=&lt;value&gt;</i>
     * pairs in the query string.
     * If no parameters are present in the query string,
     * an empty collection is returned.
     * <p>
     * The returned collection supports element adding and removing:
     * Adding a {@code null} value results in a {@code NullPointerException}.
     * Adding a string with an unescaped {@code '&'} results
     * in an {@link IllegalArgumentException}.
     * Adding any other string results in a segment with the given string in
     * the query string.
     * The string is subject to further parsing as described in
     * {@link #getParameterValues(String)}.
     * Adding the same or equal parameter values results in duplicated elements.
     * <p>
     * The returned collection actually behaves like a list, that is,
     * duplicated elements are possible and their order is stable.
     * <p>
     * TODO: Consider returning a {@link List} instead of a {@link Collection}.
     * 
     * @return A mutable view of the parameter <i>&lt;name&gt;=&lt;value&gt;</i>
     *         pairs in the query string.
     *         {@code null} is never returned.
     */
    @NonNull
    public Collection<String> getParameters() {
        if (parametersView == null)
            parametersView = new ParametersView();
        return parametersView;
    }

    /**
     * Returns a mutable view of the parameter names in the query string.
     * If no parameters are present in the query string,
     * an empty set is returned.
     * <p>
     * The returned set supports element removing, but not adding.
     * Removing a parameter name removes all its
     * <i>&lt;name&gt;=&lt;value&gt;</i> pairs from the query string.
     * 
     * @return A mutable view of the parameter names in the query string.
     *         {@code null} is never returned.
     */
    @NonNull
    public Set<String> getParameterNames() {
        if (parameterNamesView == null)
            parameterNamesView = new ParameterNamesView();
        return parameterNamesView;
    }

    /**
     * Returns a mutable view of the values for the given parameter name in
     * the query string.
     * If no values are present for the given parameter name,
     * an empty collection is returned.
     * <p>
     * The returned collection supports element adding and removing:
     * Adding a {@code null} value results in a parameter segment with just
     * the parameter name in the query string.
     * Adding an empty string results in a parameter segment with just
     * the parameter name, followed by a {@code '='} in the query string.
     * Adding a string with an unescaped {@code '&'} results
     * in an {@link IllegalArgumentException}.
     * Adding any other string results in a new <i>&lt;name&gt;=&lt;value&gt;</i>
     * pair in the query string with the given parameter name and the string
     * as the parameter value.
     * Adding the same or equal strings results in duplicated
     * <i>&lt;name&gt;=&lt;value&gt;</i> pairs.
     * 
     * @param name The query parameter name.
     * @return A mutable view of the values for the given parameter name in
     *         the query string.
     *         {@code null} is never returned.
     * @throws NullPointerException If {@code name} is {@code null}.
     */
    @NonNull
    public Collection<String> getParameterValues(final String name) {
        return new ParameterValuesView(name);
    }

    /**
     * Returns the query parameter sorting property of this URI builder.
     * 
     * @see #setParameterSorting
     * @see #getQuery
     */
    public boolean isParameterSorting() {
        return query.isSorting();
    }

    /**
     * Sets the query parameter sorting property of this URI builder.
     * If and only if this is {@code true}, then the query parameters
     * in the string returned by {@link #getQuery} are sorted in
     * ascending natural order.
     * The sorting is applied upon each call to {@code getQuery()} and
     * does <em>not</em> affect the view collections.
     * 
     * @see #isParameterSorting
     */
    public void setParameterSorting(boolean sorting) {
        query.setSorting(sorting);
    }

    public final void setFragment(final URI uri) {
        setFragment(uri != null ? uri.getFragment() : null);
    }

    public void setFragment(final String fragment) {
        this.fragment = fragment;
    }

    public String getFragment() {
        return fragment;
    }

    //
    // Interfaces and classes.
    //

    /**
     * An abstract representation of a query string in a URI.
     * Subclasses need to implement the different data structure views and
     * ensure their synchronization.
     */
    @DefaultAnnotation({ CheckReturnValue.class, NonNull.class })
    private static class Query {

        private final Parameters parameters = new Parameters();
        private final ParameterComponents components
                = new ParameterComponents();
        private boolean sorting;

        void set(final String query) {
            parameters.clear();
            if (query != null) {
                final String[] segments = query.split("&", -1);
                for (int i = 0; i < segments.length; i++)
                    parameters.add(new Parameter(segments[i]));
            }
        }

        /**
         * Returns the sorting property of this query.
         *
         * @see #setSorting
         */
        boolean isSorting() {
            return sorting;
        }
        
        /**
         * Sets the sorting property of this query.
         * If and only if this is {@code true}, then the query parameters
         * in the string returned by {@link #toString} are sorted in
         * ascending natural order.
         * The sorting is applied upon each call to {@code toString()} and
         * does <em>not</em> affect the view collections.
         *
         * @see #isSorting
         */
        void setSorting(final boolean sorting) {
            this.sorting = sorting;
        }

        /** Returns a mutable view of this query as a collection of parameters. */
        Collection<Parameter> getParameters() {
            return parameters;
        }

        /** Returns a mutable view of this query as a set of parameter names. */
        Set<Parameter.Name> getParameterNames() {
            return components.keySet();
        }

        /**
         * Returns a mutable view of this query as a collection of parameter
         * values for the given name.
         */
        Collection<Parameter.Value> getParameterValues(Parameter.Name name) {
            assert name != null : "ParameterValuesView should have provided a Parameter.Name!";
            return components.get(name);
        }

        /**
         * Returns the string representation of this query.
         * Note that this method may return {@code null}!
         */
        @Override
        @CheckForNull
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_TOSTRING_COULD_RETURN_NULL")
        public String toString() {
            final Parameter[] ps = parameters.toArray(new Parameter[parameters.size()]);
            if (sorting)
                Arrays.sort(ps);
            final StringBuilder queryBuilder = new StringBuilder();
            int i = 0;
            for (; i < ps.length; i++) {
                final Parameter parameter = ps[i];
                if (i > 0)
                    queryBuilder.append('&');
                queryBuilder.append(parameter);
            }
            return i > 0 ? queryBuilder.toString() : null;
        }

        @DefaultAnnotation({ CheckReturnValue.class, NonNull.class })
        private class Parameters extends ObservingCollection<Parameter> {

            private final Collection<Parameter> model
                    = new ArrayList<Parameter>();

            protected Collection<Parameter> getModel() {
                return model;
            }

            public void onAdd(Parameter parameter) {
                sync(true, parameter);
            }

            public void onRemove(Parameter parameter) {
                sync(false, parameter);
            }

            private void sync(
                    final boolean added,
                    final Parameter parameter) {
                final Parameter.Name name = parameter.getName();
                final Parameter.Value value = parameter.getValue();

                // Update model for parameter values.
                // By using the model collection instead of the observable
                // collection, an event storm caused by recursive event
                // triggering is avoided.
                final Collection<Parameter.Value> model
                        = components.get(name).getModel();
                assert added || model.contains(value);
                final boolean synced = added
                        ? model.add(value)
                        : model.remove(value);
                assert synced;
            }
        } // class Parameters

        @DefaultAnnotation({ CheckReturnValue.class, NonNull.class })
        private class ParameterComponents
                extends ObservingMap<Parameter.Name, ParameterValues> {

            private final CacheMap<Parameter.Name, ParameterValues> model
                    = new CacheMap<Parameter.Name, ParameterValues>();

            protected Map<Parameter.Name, ParameterValues> getModel() {
                return model;
            }

            protected void onAdd(Map.Entry newEntry) {
                throw new AssertionError("Elements in the ParameterComponents must not get added - use its model instead!");
            }

            protected void onReplace(Map.Entry oldEntry, Map.Entry newEntry) {
                throw new AssertionError("Elements in the ParameterComponents must not get replaced - use its model instead!");
            }

            protected void onRemove(final Map.Entry<Parameter.Name, ParameterValues> oldEntry) {
                final Collection<Parameter> model = parameters.getModel();
                final Collection<Parameter.Value> values = oldEntry.getValue();
                for (final Parameter.Value value : values) {
                    final Parameter parameter = value.getParameter();
                    final boolean synced = model.remove(parameter);
                    assert synced;
                }
            }

            /**
             * Returns a mutable view of the enclosing query as a collection
             * of parameter values for the given name.
             * If required, the collection is automatically created so that
             * {@code null} is never returned.
             */
            @Override
            public ParameterValues get(Object o) {
                return get((Parameter.Name) o);
            }

            /**
             * Returns a mutable view of the enclosing query as a collection
             * of parameter values for the given name.
             * If required, the collection is automatically created so that
             * {@code null} is never returned.
             */
            ParameterValues get(final Parameter.Name name) {
                assert name != null : "ParameterNamesView should have provided a name!";
                ParameterValues values = super.get(name);
                if (values == null) {
                    values = new ParameterValues();
                    cacheDiscardably(name, values);
                }
                return values;
            }

            void cacheDiscardably(
                    Parameter.Name name,
                    ParameterValues values) {
                // The caching strategy for empty parameter value collections.
                // Using WEAK instead of SOFT increases the chances that the
                // referential integrity of the views is kept.
                // For example, if a parameter value view collection is
                // cleared and no more references are held to it, the size of
                // the parameter name view set is immediately reduced by one.
                // With SOFT, the parameter value view collection could be
                // kept much longer and hence the parameter name view set would
                // still contain the corresponding name although no
                // corresponding name=value pair is present in the query string.
                model.put(STRONG, name, WEAK, values);
            }

            void cachePermanently(
                    Parameter.Name name,
                    ParameterValues values) {
                model.put(STRONG, name, STRONG, values); // that's the default for put, actually.
            }
        } // class ParameterComponents

        @DefaultAnnotation({ CheckReturnValue.class, NonNull.class })
        private class ParameterValues
                extends ObservingCollection<Parameter.Value> {

            private Collection<Parameter.Value> model = new Model();

            protected Collection<Parameter.Value> getModel() {
                return model;
            }

            protected void onAdd(final Parameter.Value value) {
                sync(true, value);
            }

            protected void onRemove(final Parameter.Value value) {
                sync(false, value);
            }

            private void sync(
                    final boolean added,
                    final Parameter.Value value) {
                final Parameter parameter = value.getParameter();

                // Update model for parameter segments.
                // By using the model collection instead of the observable
                // collection, an event storm caused by recursive event
                // triggering is avoided.
                final Collection<Parameter> model = parameters.getModel();
                assert added || model.contains(parameter);
                final boolean synced = added
                        ? model.add(parameter)
                        : model.remove(parameter);
                assert synced;
            }

            @DefaultAnnotation({ CheckReturnValue.class, NonNull.class })
            private class Model extends ObservingCollection<Parameter.Value> {

                private final Collection<Parameter.Value> model
                        = new LinkedList<Parameter.Value>();

                protected Collection<Parameter.Value> getModel() {
                    return model;
                }

                protected void onAdd(final Parameter.Value value) {
                    final Parameter parameter = value.getParameter();
                    final Parameter.Name name = parameter.getName();
                    if (size() <= 1) // first element added?
                        components.cachePermanently(name, ParameterValues.this);
                }

                protected void onRemove(final Parameter.Value value) {
                    final Parameter parameter = value.getParameter();
                    final Parameter.Name name = parameter.getName();
                    if (size() <= 0) // last element removed?
                        components.cacheDiscardably(name, ParameterValues.this);
                }
            } // class Model
        } // class ParameterValues
    } // class Query

    @DefaultAnnotation({ CheckReturnValue.class, NonNull.class })
    private class ParametersView
            extends ViewCollection<Parameter, String> {

        private final Collection<Parameter> model = query.getParameters();

        protected Collection<Parameter> getModel() {
            return model;
        }

        @Override
        protected String map(Parameter parameter) {
            return parameter.toString();
        }

        @Override
        protected Parameter unmap(String parameter) {
            return new Parameter(parameter);
        }
    } // class ParametersView

    @DefaultAnnotation({ CheckReturnValue.class, NonNull.class })
    private class ParameterNamesView
            extends ViewSet<Parameter.Name, String> {

        private final Set<Parameter.Name> model = query.getParameterNames();

        protected Set<Parameter.Name> getModel() {
            return model;
        }

        @Override
        protected String map(Parameter.Name name) {
            return name.toString();
        }

        @Override
        protected Parameter.Name unmap(String name) {
            return new Parameter(name, null).getName();
        }

        @Override
        public boolean add(String name) {
            // The super class would do similar, except that it would throw
            // a misleading NullPointerException for a null name.
            throw new UnsupportedOperationException("Cannot add query parameters via its name set!");
        }
    }

    @DefaultAnnotation({ CheckReturnValue.class, NonNull.class })
    private class ParameterValuesView
            extends ViewCollection<Parameter.Value, String> {

        private final String name;
        private final Collection<Parameter.Value> model;

        ParameterValuesView(final String name) {
            this.name = name;
            this.model = query.getParameterValues(new Parameter(name, null).getName());
        }

        protected Collection<Parameter.Value> getModel() {
            return model;
        }

        @Override
        protected String map(Parameter.Value value) {
            return value.toString();
        }

        @Override
        protected Parameter.Value unmap(String value) {
            return new Parameter(name, value).getValue();
        }
    }

    @DefaultAnnotation({ CheckReturnValue.class, NonNull.class })
    private static class Parameter implements Comparable<Parameter> {
        private static final Pattern NAME_PATTERN = Pattern.compile("^[^&=]*$");
        private static final Pattern VALUE_PATTERN = Pattern.compile("^[^&]*$");

        final Name name;
        final Value value;

        Parameter(final String parameter) {
            final String[] elements = parameter.split("=", 2);
            name = new Name(elements[0]);
            value = new Value(elements.length > 1 ? elements[1] : null);
        }

        public Parameter(final String name, @CheckForNull final String value) {
            this.name = new Name(name);
            this.value = new Value(value);
        }

        public Name getName() {
            return name;
        }

        public Value getValue() {
            return value;
        }

        /** Two parameters compare like their string representation. */
        @Override
        public int compareTo(Parameter p) {
            return toString().compareTo(p.toString());
        }

        /**
         * Two parameters are equal iff their string representations are
         * equal.
         */
        @Override
        public boolean equals(@CheckForNull Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Parameter))
                return false;
            return toString().equals(((Parameter) o).toString());
        }

        /** Returns the hash code of the string representation. */
        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        /** Returns the string representation of the parameter. */
        @Override
        public String toString() {
            final String n = name.toString();
            final String v = value.toString();
            assert n != null;
            return v != null
                    ? new StringBuilder(n.length() + 1 + v.length())
                        .append(n)
                        .append('=')
                        .append(v)
                        .toString()
                    : n;
        }

        @DefaultAnnotation({ CheckReturnValue.class, NonNull.class })
        private class Name implements Comparable<Name> {
            final String name;

            Name(final String name) {
                if (!NAME_PATTERN.matcher(name).matches())
                    throw new IllegalArgumentException(
                            "illegal characters in query parameter name \"" + name + "\"");
                this.name = name;
            }

            public Parameter getParameter() {
                return Parameter.this;
            }

            /**
             * Two parameter names compare like their string
             * representation.
             */
            @Override
            public int compareTo(Name n) {
                return name.compareTo(n.name);
            }

            /**
             * Two parameter names are equal iff their string
             * representations are equal.
             */
            @Override
            public boolean equals(@CheckForNull Object o) {
                if (this == o)
                    return true;
                if (!(o instanceof Name))
                    return false;
                final String n = name;
                final String on = ((Name) o).name;
                return n == on || n.equals(on);
            }

            /** Returns the hash code of the string representation. */
            @Override
            public int hashCode() {
                return name.hashCode();
            }

            /** Returns the string representation of the parameter name. */
            @Override
            public String toString() {
                return name;
            }
        } // class Name

        @DefaultAnnotation({ CheckReturnValue.class, NonNull.class })
        private class Value implements Comparable<Value> {
            @CheckForNull
            final String value;

            Value(@CheckForNull final String value) {
                if (value != null && !VALUE_PATTERN.matcher(value).matches())
                    throw new IllegalArgumentException(
                            "illegal characters in query parameter value \"" + value + "\"");
                this.value = value;
            }

            public Parameter getParameter() {
                return Parameter.this;
            }

            /**
             * Two parameter values compare like their string
             * representation.
             */
            @Override
            public int compareTo(final Value o) {
                final String v = this.value;
                final String ov = o.value;
                return v != null ? (ov != null ? v.compareTo(ov) : 1)
                                 : (ov != null ?              -1 : 0);
            }

            /**
             * Two parameter values are equal iff their string
             * representations are equal.
             */
            @Override
            public boolean equals(@CheckForNull Object o) {
                if (this == o)
                    return true;
                if (!(o instanceof Value))
                    return false;
                final String v = value;
                final String ov = ((Value) o).value;
                return v == ov || v != null && v.equals(ov);
            }

            /** Returns the hash code of the string representation. */
            @Override
            public int hashCode() {
                return value.hashCode();
            }

            /** Returns the string representation of the parameter value. */
            @Override
            @CheckForNull
            @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_TOSTRING_COULD_RETURN_NULL")
            public String toString() {
                return value;
            }
        } // class Value
    } // class Parameter
}
