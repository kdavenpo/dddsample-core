package se.citerus.dddsample.split;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Creates a bean only in the service that owns the given class: a queue listener or the upload
 * scanner runs in one service, not in each. Without a split, the bean is created as before; in the
 * seed step, never (it only loads the sample data).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OwnerCondition.class)
public @interface ConditionalOnOwner {

    /** The fully qualified name of the class whose owner runs the bean. */
    String value();
}
