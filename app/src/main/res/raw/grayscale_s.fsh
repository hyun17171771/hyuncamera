#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform samplerExternalOES s_texture;
varying vec2 v_texCoord;

void transformImage( out vec4 fragColor, in vec2 fragCoord )
  {
    vec4 maskColor = texture2D(s_texture, fragCoord);
    float average = 0.2126 * maskColor.r + 0.7152 * maskColor.g + 0.0722 * maskColor.b;
    maskColor = vec4(average, average, average,1.0);
    fragColor = maskColor;
   }

void main() {
 	transformImage(gl_FragColor, v_texCoord);
 }