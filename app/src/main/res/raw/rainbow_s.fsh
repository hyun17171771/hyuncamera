#extension GL_OES_EGL_image_external : require
precision mediump float;


uniform samplerExternalOES s_texture;
uniform float u_time;
varying vec2 v_texCoord;

void transformImage( out vec4 fragColor, in vec2 fragCoord )
   {

       vec4 texture = texture2D(s_texture, fragCoord);
       vec2 uv = fragCoord.xy;
         uv.x *= fragCoord.x/fragCoord.y;

         vec3 col = vec3(0.);
         col = vec3(uv.x,uv.y,abs(sin(u_time)));
         vec4 color = vec4(col, 1.0);

         fragColor = texture * color;

   }

void main() {
    transformImage(gl_FragColor, v_texCoord);
}