#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform float               u_time;
uniform samplerExternalOES  s_texture;
varying vec2                v_texCoord;


void transformImage(out vec4 fragColor, in vec2 fragCoord)
{
    float amount = sin(u_time) * 0.1;

    vec2 uv = fragCoord.xy;
    amount *= 0.2;
    vec2 uv1 = vec2(uv.x + amount, uv.y);
    vec2 uv2 = vec2(uv.x, uv.y + amount);


    float r = texture2D(s_texture, uv1).r;
    float g = texture2D(s_texture, uv).g;
    float b = texture2D(s_texture, uv2).b;

    fragColor = vec4(r, g, b, 1.);

}


void main() {
	transformImage(gl_FragColor, v_texCoord);
}