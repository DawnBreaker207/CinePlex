package com.dawn.common.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    String action();

    String entity();

    String entityId() default "";

    Class<?> entityClass() default void.class;

    String fromState() default "";

    String toState() default "";

    String metadata() default "";
}
