#version 150

uniform vec2 resolution;
uniform float time;
uniform int oct;
uniform vec4 color;
uniform vec4 primaryColor;
uniform vec4 secondaryColor;

in vec4 vColor;
out vec4 OutColor;

float random (in vec2 _st) {
    return fract(sin(dot(_st.xy, vec2(12.9898,78.233)))*43758.5453123);
}

float noise (in vec2 _st) {
    vec2 i = floor(_st);
    vec2 f = fract(_st);
    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a)* u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm( in vec2 _st) {
    float v = 0.0;
    float a = 0.5;
    vec2 shift = vec2(100.0);
    mat2 rot = mat2(cos(0.5), sin(0.5),
    -sin(0.5), cos(0.50));
    for (int i = 0; i < 32; ++i) {
        if (i >= oct) break;
        v += a * noise(_st);
        _st = rot * _st * 2.0 + shift;
        a *= 0.5;
    }
    return v;
}

vec3 getFillColor() {
    vec2 st = gl_FragCoord.xy / resolution.xy*3.;
    vec3 c = vec3(0.0);
    vec2 q = vec2(0.);
    q.x = fbm(st);
    q.y = fbm( st + vec2(1.0));
    vec2 r = vec2(0.);
    r.x = fbm( st + 1.0*q + vec2(1.7,9.2)+ 0.15*time );
    r.y = fbm( st + 1.0*q + vec2(8.3,2.8)+ 0.126*time);
    float f = fbm(st+r);
    float gradientMix = clamp(0.5 + 0.5 * sin(time * 1.25 + st.x * 5.0 - st.y * 3.5 + f * 3.0), 0.0, 1.0);
    vec3 gradientColor = mix(primaryColor.rgb, secondaryColor.rgb, gradientMix);
    
    c = gradientColor * clamp((f*f)*4.0,0.0,1.0);
    c = mix(c, gradientColor * 0.8, clamp(length(q),0.0,1.0));
    c = mix(c, gradientColor * 0.5, clamp(length(r.x),0.0,1.0));
    return (f*f*f+.6*f*f+.5*f)*c;
}

void main() {
    float outAlpha = max(color.a, max(primaryColor.a, secondaryColor.a));
    OutColor = vec4(getFillColor(), outAlpha);
}
