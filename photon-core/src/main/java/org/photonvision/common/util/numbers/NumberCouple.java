/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.common.util.numbers;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class NumberCouple<T extends Number> {
    protected T first;
    protected T second;

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

    @JsonIgnore
    public boolean isEmpty() {
        return first.intValue() == 0 && second.intValue() == 0;
    }
}
