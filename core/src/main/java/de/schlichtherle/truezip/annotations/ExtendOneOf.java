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
package de.schlichtherle.truezip.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated interface should not be implemented by client
 * applications without directly or indirectly extending one of the classes
 * provided as the annotation value in order to enable future changes to the
 * annotated interface without breaking binary compatibility to existing
 * implementations.
 * <p>
 * Consider the following example code in version 1.0 of an API:
 * {@code
 *     <AT>ExtendOneOf({ One.class, Two.class })
 *     public interface Specification {
 *         void foo();
 *         void bar();
 *     }
 *
 *     public abstract class One implements Specification {
 *     }
 *
 *     public abstract class Two implements Specification {
 *         public void foo() {
 *         }
 *     }
 * }
 * <p>
 * The annotation in this example indicates that whenever a client application
 * wants to implement the interface {@code Specification}, it should directly
 * or indirectly extend from the eligible base classes {@code One} or
 * {@code Two}, too.
 * <p>
 * Now in version 2.0 of your API things could be changed like follows without
 * breaking binary compatibility to client applications written for version
 * 1.0 of the API:
 * {@code
 *     <AT>ExtendOneOf({ Two.class, Three.class }) // CHANGED!
 *     public interface Specification {
 *         void foo();
 *         void bar();
 *         void baz(); // NEW!
 *     }
 *
 *     <AT>Deprecated // CHANGED!
 *     public abstract class One implements Specification {
 *         public void baz() {
 *             throw new UnsupportedOperationException();
 *         }
 *     }
 *
 *     public abstract class Two implements Specification {
 *         public void baz() {
 *             throw new UnsupportedOperationException();
 *         }
 *
 *         public void foo() {
 *         }
 *     }
 *
 *     public abstract class Three implements Specification {
 *         // ...
 *     }
 * }
 * <p>
 * Notice the advent of the method {@code baz()} in the interface
 * {@code Specification}. Because client applications of version 1.0 of the
 * API could not foresee this, the API has to provide a reasonable default
 * implementation in the classes {@code One} and {@code Two}.
 * <p>
 * Notice also that the declaration of the list of eligible base classes has
 * been changed from { One.class, Two.class } to { Two.class, Three.class } in
 * order to deprecate the class {@code One} and introduce the class
 * {@code Three}. This serves as an example how an API might perform class life
 * cycle management.
 * <p>
 * <strong>FIXME:</strong> Currently, there is no processor available for
 * this annotation type, so there is no means to assert the compliance of a
 * client application.
 * <p>
 * In addition to the constraint explained above, an annotation processor
 * should also assert that the annotation is solely used on interfaces instead
 * of classes and enums and mind the rules of interface inheritance.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Documented
@Target(ElementType.TYPE)
// @Inherited // Useless for interfaces
// TODO: Check if this is still required.
public @interface ExtendOneOf {

    /**
     * Returns a list of eligible base classes for implementing the annotated
     * interface.
     *
     * @return A list of eligible base classes for implementing the annotated
     *         interface.
     */
    Class<?>[] value();
}
