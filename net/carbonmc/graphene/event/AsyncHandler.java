package net.carbonmc.graphene.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.METHOD})
public @interface AsyncHandler {
    int priority() default Thread.NORM_PRIORITY;
    String threadPool() default "default";
    boolean fallbackToSync() default true;
}