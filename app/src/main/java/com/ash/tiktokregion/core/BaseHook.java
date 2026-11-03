package com.ash.tiktokregion.core;

/**
 * Standard hook contract for modular feature handlers.
 */
public interface BaseHook {
    void hook(ClassLoader classLoader);
}
