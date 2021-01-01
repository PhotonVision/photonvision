package org.photonvision.common.dataflow;

public interface Triconsumer<T, U, V> {
    public void accept(T t, U u, V v);
}
