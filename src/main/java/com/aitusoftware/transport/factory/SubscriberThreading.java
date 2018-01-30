package com.aitusoftware.transport.factory;

public enum SubscriberThreading
{
    SINGLE_THREADED,
    THREAD_PER_TOPIC,
    THREAD_PER_CONNECTION
}