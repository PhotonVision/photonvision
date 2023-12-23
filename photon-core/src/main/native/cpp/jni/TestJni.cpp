#include <jni.h>

extern "C" {
    JNIEXPORT jint JNICALL
    some_native_function() {
        return 0;
    }
}