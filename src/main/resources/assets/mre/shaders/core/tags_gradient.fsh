#version 150

uniform vec2 resolution;
uniform float time;
uniform int oct;
uniform float factor;
uniform vec4 primaryColor;
uniform vec4 secondaryColor;

in vec4 vColor;
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
    mat2 rot = mat2(cos(0.1), sin(0.5), -sin(0.5), cos(0.5));
    for (int i = 0; i < 32; i++) {
        if (i >= oct) break;
        v += a * noise(pos);
        pos = rot * pos * 2.0 + vec2(100.0);
        a *= 0.5;
    }
    return v;
}

vec3 getColor() {
    vec2 p = ((vec2(2.0, 2.0) * gl_FragCoord.xy) - resolution.xy) / min(resolution.x, resolution.y);
    float time2 = 3.0 * time / 2.0;
    vec2 q = vec2(0.0);
    q.x = fbm(p + 0.00);
    q.y = fbm(p + vec2(1.0));

    // Используем тот же шумовой градиент, который пользователь прислал для fade.
    float noiseValue1 = noise(p + vec2(1.0));
    float noiseValue2 = noise(p + factor * q + vec2(1.7, 9.2) + 0.15 * time2);
    float noiseValue3 = noise(p + factor * q + vec2(8.3, 2.8) + 0.126 * time2);
    float mixFactor = (noiseValue1 + noiseValue2 + noiseValue3) / 3.0;

    return mix(primaryColor.rgb, secondaryColor.rgb, mixFactor);
}

void main() {
    vec3 color = getColor();
    float alpha = max(primaryColor.a, secondaryColor.a) * vColor.a;
    OutColor = vec4(color, alpha);
}
