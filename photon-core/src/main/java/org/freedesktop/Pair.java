package org.freedesktop;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/** Just a typed container class */
public final class Pair<A, B> extends Tuple {
    @Position(0)
    public final A a;

    @Position(1)
    public final B b;

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }
}
