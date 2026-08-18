#version 150

uniform float time;
uniform float scale;
uniform int mode;
uniform vec4 primaryColor;
uniform vec4 secondaryColor;
uniform vec4 accentColor;

in vec3 skyPosition;
out vec4 fragColor;

float hash3(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 251.9))) * 43758.5453123);
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);

    float n000 = hash3(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash3(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash3(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash3(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash3(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash3(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash3(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash3(i + vec3(1.0, 1.0, 1.0));

    float nx00 = mix(n000, n100, u.x);
    float nx10 = mix(n010, n110, u.x);
    float nx01 = mix(n001, n101, u.x);
    float nx11 = mix(n011, n111, u.x);
    float nxy0 = mix(nx00, nx10, u.y);
    float nxy1 = mix(nx01, nx11, u.y);
    return mix(nxy0, nxy1, u.z);
}

float fbm3(vec3 p) {
    float value = 0.0;
    float amplitude = 0.5;
    mat3 rot = mat3(
        0.00, 0.80, 0.60,
       -0.80, 0.36, -0.48,
       -0.60, -0.48, 0.64
    );
    for (int i = 0; i < 5; i++) {
        value += noise3(p) * amplitude;
        p = rot * p * 1.92 + vec3(8.7, 4.9, 6.3);
        amplitude *= 0.55;
    }
    return value;
}

float starLayer(vec3 direction, float density, float threshold) {
    vec3 scaled = direction * density;
    vec3 cell = floor(scaled);
    vec3 local = fract(scaled) - 0.5;
    float rnd = hash3(cell);
    float sparkle = hash3(cell + 17.0);
    float radius = mix(0.04, 0.11, sparkle * sparkle);
    float star = smoothstep(radius, 0.0, length(local));
    return star * step(threshold, rnd);
}

vec3 renderPlasma(vec3 direction, float t) {
    vec3 samplePos = direction * (2.8 / max(scale, 0.001));
    vec3 flow = vec3(
        fbm3(samplePos * 0.92 + vec3(t * 0.40, -t * 0.24, t * 0.12)),
        fbm3(samplePos * 0.97 + vec3(-t * 0.21, t * 0.30, -t * 0.09)),
        fbm3(samplePos * 0.88 + vec3(t * 0.14, t * 0.08, -t * 0.26))
    );
    vec3 warp = samplePos + (flow - 0.5) * 1.25;

    float waves = sin(warp.x * 2.2 + t * 1.7);
    waves += sin(warp.y * 2.7 - t * 1.4);
    waves += sin(warp.z * 2.0 + t * 0.95);
    waves += sin(length(warp.xy * vec2(1.05, 1.35)) * 3.8 - t * 1.9);
    float plasma = waves * 0.125 + 0.5;

    float filament = pow(clamp((plasma - 0.62) / 0.38, 0.0, 1.0), 3.2);
    float haze = fbm3(warp * 1.35 + vec3(t * 0.18, -t * 0.12, t * 0.07));
    float glow = smoothstep(-0.35, 0.95, direction.y);

    vec3 base = mix(primaryColor.rgb, secondaryColor.rgb, clamp(plasma * 0.72 + haze * 0.28, 0.0, 1.0));
    vec3 color = base * (0.42 + plasma * 0.32);
    color += accentColor.rgb * filament * 0.72;
    color += secondaryColor.rgb * glow * 0.18;
    color += vec3(1.0) * filament * 0.04;
    return color;
}

vec3 renderCosmos(vec3 direction, float t) {
    vec3 samplePos = direction * (2.2 / max(scale, 0.001));
    float nebula = fbm3(samplePos * 0.84 + vec3(t * 0.05, -t * 0.03, t * 0.01));
    float dust = fbm3(samplePos * 1.72 - vec3(t * 0.04, t * 0.02, -t * 0.02));
    float cloud = smoothstep(0.35, 0.95, nebula * 0.7 + dust * 0.3);

    vec3 color = mix(primaryColor.rgb, secondaryColor.rgb, clamp(nebula * 0.85 + dust * 0.15, 0.0, 1.0));
    color *= 0.32 + cloud * 0.50;
    color += accentColor.rgb * cloud * 0.16;

    float stars = 0.0;
    vec3 starPos = direction + vec3(t * 0.0007, -t * 0.0005, t * 0.0004);
    stars += starLayer(starPos, 22.0, 0.985) * 0.75;
    stars += starLayer(starPos * 1.4 + vec3(4.1, 9.7, 2.3), 34.0, 0.991) * 0.85;
    stars += starLayer(starPos * 1.9 + vec3(12.4, 2.8, 7.6), 48.0, 0.996) * 1.10;
    color += mix(accentColor.rgb, vec3(1.0), 0.55) * stars;

    color += secondaryColor.rgb * smoothstep(-0.45, 0.90, direction.y) * 0.08;
    return color;
}

void main() {
    vec3 direction = normalize(skyPosition);
    float t = time;

    vec3 color = mode == 1 ? renderCosmos(direction, t) : renderPlasma(direction, t);
    color *= 0.82 + smoothstep(-0.2, 0.85, direction.y) * 0.18;

    fragColor = vec4(color, 1.0);
}
