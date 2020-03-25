package org.renderer.gles;

import android.opengl.GLES20;
import android.util.Log;

public class GLESUtils {
    private static final String TAG = "GLESUtils";
    /**
     *
     * @param type
     * @see GLES20#GL_FRAGMENT_SHADER
     * @see GLES20#GL_VERTEX_SHADER
     * @param shaderCode
     * @return
     */
    public static int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        if(shader == 0){
            //todo create shader failed;
        }

        GLES20.glShaderSource(shader,shaderCode);
        GLES20.glCompileShader(shader);
        int[] params = new int[1];
        GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS, params,0);
        if(params[0] == 0){
            // todo could not compile shader source
            String s = GLES20.glGetShaderInfoLog(shader);
            Log.d(TAG, "loadShader: "+s);
            GLES20.glDeleteShader(shader);
        }
        return shader;
    }
}
