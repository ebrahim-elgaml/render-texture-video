package com.example.ebrahim_elgaml.retrieveframes.gl;

import android.annotation.TargetApi;
import android.opengl.EGL14;
import android.os.Build;
import android.util.Log;

public class GLUtil {	
    /**
     * Checks for EGL errors.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static void checkEglError(String msg) {
        boolean failed = false;
        int error;
        while ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            Log.e("TAG", msg + ": EGL error: 0x" + Integer.toHexString(error));
            failed = true;
        }
        if (failed) {
            throw new RuntimeException("EGL error encountered (see log)");
        }
    }	
}
