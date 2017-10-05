package com.aitusoftware.transport.messaging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Topic
{
    int ANY_PORT = -1;
    String ANY_ADDR = "0.0.0.0";

    int port() default ANY_PORT;
    String listenAddress() default ANY_ADDR;
    Storage storage() default Storage.SHARED;
}