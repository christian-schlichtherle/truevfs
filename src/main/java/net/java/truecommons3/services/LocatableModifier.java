/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.services;

import javax.annotation.concurrent.Immutable;

/**
 * A locatable function for modifying products.
 * For best results, clients should create another abstract subclass which just
 * specifies the type parameter {@code P}.
 * The following example accompanies the example for {@link LocatableFactory},
 * so the type parameter is specified as {@link StringBuilder} again:
 * <p>
 * <pre>{@code
 * package com.company.spec;
 *
 * import net.java.truecommons3.services.LocatableModifier;
 *
 * public abstract class StringBuilderModifier
 * extends LocatableModifier<StringBuilder> {
 * }
 * }</pre>
 * <p>
 * An implementation could now implement this service as follows:
 * <pre>{@code
 * package com.company.impl;
 *
 * import com.company.spec.StringBuilderModifier;
 *
 * public class SmalltalkModifier extends StringBuilderModifier {
 *     \@Override
 *     public StringBuilder get(StringBuilder b) {
 *         // Modify and return the same instance on each call!
 *         return b.append(" How do you do?");
 *     }
 * }
 * }</pre>
 * <p>
 * Next, the implementation needs to advertise its service by providing a file
 * with the name {@code META-INF/services/com.company.spec.StringBuilderModifier}
 * on the run time class path with the following single line content:
 * <pre>{@code
 * com.company.impl.SmalltalkModifier
 * }</pre>
 * <p>
 * If multiple modifier services are locatable on the class path at run
 * time, they are applied in ascending order of their
 * {@linkplain #getPriority() priority} so that the result of the modifier
 * service with the greatest number becomes the result of the entire
 * modifier chain.
 * <p>
 * Finally, a client could now simply compose a factory with some modifiers
 * according to the {@code StringBuilderFactory} and
 * {@code StringBuilderModifier} specification by calling:
 * <pre>{@code
 * package com.company.client;
 *
 * import net.java.truecommons3.services.Locator;
 * import com.company.spec.StringBuilderFactory;
 * import com.company.spec.StringBuilderModifier;
 *
 * public class Main {
 *     public static void main(String[] args) {
 *         Locator l = new Locator(Main.class); // specify calling class
 *         Factory<StringBuilder> f = l.factory(StringBuilderFactory.class,
 *                                              StringBuilderModifier.class);
 *         StringBuilder b = f.get(); // create product
 *         System.out.println(b.toString()); // use product
 *     }
 * }
 * }</pre>
 * <p>
 * Note that multiple calls to {@code f.get()} would always return a new
 * product because {@code f} is a factory, not a container.
 * <p>
 * Implementations should be thread-safe.
 *
 * @see    Locator
 * @param  <P> the type of the products to modify.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class LocatableModifier<P>
extends LocatableFunction<P> implements Modifier<P> { }
