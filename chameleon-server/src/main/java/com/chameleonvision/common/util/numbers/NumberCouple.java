package com.chameleonvision.common.util.numbers;

public abstract class NumberCouple<T extends Number> {

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

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NumberCouple)) {
            return false;
        }

        var couple = (NumberCouple) obj;
        if (!couple.first.equals(first)) {
            return false;
        }

        if (!couple.second.equals(second)) {
            return false;
        }

        return true;
    }

    public boolean isEmpty() {
        return first.intValue() == 0 && second.intValue() == 0;
    }
}
