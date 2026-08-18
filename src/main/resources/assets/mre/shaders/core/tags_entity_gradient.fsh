#version 150

uniform vec2 resolution;
uniform float time;
uniform int oct;
uniform float factor;
uniform vec4 primaryColor;
uniform vec4 secondaryColor;
uniform float alpha;

in vec3 vPos;
out vec4 OutColor;

float random(vec2 pos) {
    return fract(sin(dot(pos.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

float noise(vec2 pos) {
    vec2 i = floor(pos);
    vec2 f = fract(pos);
    float a = random(i + vec2(0.0, 0.0));
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 pos) {
    float v = 0.0;
    float a = 0.5;
    mat2 rot = mat2(cos(0.15), sin(0.45), -sin(0.45), cos(0.45));
    for (int i = 0; i < 32; i++) {
        if (i >= oct) break;
        v += a * noise(pos);
        pos = rot * pos * 2.0 + vec2(100.0);
        a *= 0.5;
    }
    return v;
}

vec3 gradientColor() {
    vec2 local = vPos.xz * 1.8 + vec2(vPos.y * 0.6, vPos.y * 0.35);
    float t = time * 0.85;

    vec2 q = vec2(0.0);
    q.x = fbm(local + vec2(0.0, 0.0) + t * 0.06);
    q.y = fbm(local + vec2(1.0, 1.0) - t * 0.04);

    float n1 = noise(local + vec2(1.0));
    float n2 = noise(local + factor * q + vec2(1.7, 9.2) + 0.11 * t);
    float n3 = noise(local + factor * q + vec2(8.3, 2.8) + 0.09 * t);
    float mixFactor = clamp((n1 + n2 + n3) / 3.0, 0.0, 1.0);

    return mix(primaryColor.rgb, secondaryColor.rgb, mixFactor);
}

void main() {
    vec3 color = gradientColor();
    OutColor = vec4(color, alpha);
}
