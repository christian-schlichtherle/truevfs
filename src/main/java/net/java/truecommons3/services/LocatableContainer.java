/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.services;

import javax.annotation.concurrent.Immutable;

/**
 * A locatable provider which contains a single product.
 * For best results, clients should create another abstract subclass which just
 * specifies the type parameter {@code P}.
 * In the following example the type parameter is specified as
 * {@link String}:
 * <p>
 * <pre>{@code
 * package com.company.spec;
 *
 * import net.java.truecommons3.services.LocatableContainer;
 *
 * public abstract class StringContainer
 * extends LocatableContainer<String> {
 * }
 * }</pre>
 * <p>
 * An implementation could now implement this service as follows:
 * <pre>{@code
 * package com.company.impl;
 *
 * import com.company.spec.StringContainer;
 *
 * public class GreetingContainer extends StringContainer {
 *     \@Override
 *     public String get() {
 *         // Return the same instance on each call!
 *         return "Hello Christian!";
 *     }
 * }
 * }</pre>
 * <p>
 * Next, the implementation needs to advertise its service by providing a file
 * with the name {@code META-INF/services/com.company.spec.StringContainer}
 * on the run time class path with the following single line content:
 * <pre>{@code
 * com.company.impl.GreetingContainer
 * }</pre>
 * <p>
 * If multiple container services are locatable on the class path at run time,
 * the service with the greatest {@linkplain #getPriority() priority} gets
 * selected.
 * <p>
 * Finally, a client could now simply compose a container according to the
 * {@code StringContainer} specification by calling:
 * <pre>{@code
 * package com.company.client;
 *
 * import net.java.truecommons3.services.Locator;
 * import com.company.spec.StringContainer;
 *
 * public class Main {
 *     public static void main(String[] args) {
 *         Locator l = new Locator(Main.class); // specify calling class
 *         Container<String> c = l.container(StringContainer.class);
 *         String s = c.get(); // obtain product
 *         System.out.println(s); // use product
 *     }
 * }
 * }</pre>
 * <p>
 * Note that multiple calls to {@code c.get()} would always return the same
 * product again because {@code c} is a container, not a factory.
 * <p>
 * Implementations should be thread-safe.
 *
 * @see    Locator
 * @param  <P> the type of the product to contain.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class LocatableContainer<P>
extends LocatableProvider<P> implements Container<P> { }
