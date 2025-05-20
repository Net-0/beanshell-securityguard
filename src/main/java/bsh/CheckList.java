package bsh;

import java.util.Objects;

class CheckList<T> {
    private static final Object CHECKED = new Object();
    private Object[] values;

    @SuppressWarnings("unchecked")
    CheckList(T ...valuesToCheck) {
        this.values = new Object[valuesToCheck.length];
        System.arraycopy(valuesToCheck, 0, this.values, 0, valuesToCheck.length);
    }

    boolean check(T value) {
        for (int i = 0; i < this.values.length; i++)
            if (this.values[i] != CHECKED && Objects.equals(this.values[i], value)) {
                this.values[i] = CHECKED;
                return true;
            }
        return false;
    }

}
