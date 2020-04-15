#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform samplerExternalOES s_texture;
varying vec2 v_texCoord;



void main() {
    gl_FragColor = vec4(vec3(1.0 - texture2D(s_texture, v_texCoord)), 1.0);
}