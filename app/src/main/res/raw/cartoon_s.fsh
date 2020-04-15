#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform samplerExternalOES s_texture;
varying vec2 v_texCoord;

void transformImage( out vec4 fragColor, in vec2 fragCoord )
  {
    vec4 texture = texture2D(s_texture, fragCoord);
    float r = abs(texture.r + texture.g * 2.0 - texture.b) * texture.r;
    float g = abs(texture.r + texture.b * 2.0 - texture.g) * texture.r;
    float b = abs(texture.r + texture.b * 2.0 - texture.g) * texture.g;
	vec4 color = vec4(r, g, b, 1.0);
	fragColor = color;
   }

void main() {
 	transformImage(gl_FragColor, v_texCoord);
 }