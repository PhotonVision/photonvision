package com.chameleonvision.common.util.numbers;

public class NumberCouple<T extends Number> {

    private T first;
    private T second;

    public NumberCouple(T first, T second) {
        this.first = first;
        this.second = second;
    }

    public void setFirst(T first) {
        this.first = first;
    }

    public T getFirst() {
        return first;
    }

    public void setSecond(T second) {
        this.second = second;
    }

    public T getSecond() {
        return second;
    }

    public void set(T first, T second) {
        this.first = first;
        this.second = second;
    }
}
