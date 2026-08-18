#version 150

uniform float time;
uniform vec2 resolution;
uniform int quality;
uniform vec4 color;
uniform vec4 primaryColor;
uniform vec4 secondaryColor;

in vec3 vPos;
in vec3 vAxis;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.55;
    mat2 rot = mat2(0.84, -0.54, 0.54, 0.84);
    for (int i = 0; i < 8; i++) {
        if (i >= quality) break;
        value += noise(p) * amplitude;
        p = rot * p * 1.92 + vec2(8.3, 4.7);
        amplitude *= 0.52;
    }
    return value;
}

void main() {
    vec2 uv = clamp(vAxis.xy, 0.0, 1.0);
    float sideMask = vAxis.z;
    vec2 p = (uv - 0.5) * 2.0;
    float t = time * 0.72;

    vec2 warp = p;
    warp.x += sin(warp.y * 4.6 + t * 1.55) * 0.22;
    warp.y += cos(warp.x * 3.7 - t * 1.18) * 0.16;
    warp += (vec2(fbm(p * 1.3 + vec2(t * 0.22, -t * 0.12)), fbm(p * 1.3 + vec2(-t * 0.14, t * 0.18))) - 0.5) * 0.55;

    float velvet = sin((warp.x * 6.2 - warp.y * 2.8) + t * 2.1) * 0.5 + 0.5;
    float ribbon = pow(max(0.0, sin((warp.x + warp.y * 0.55) * 8.8 - t * 2.7) * 0.5 + 0.5), 4.5);
    float sweep = pow(max(0.0, sin((warp.x * 2.4 + warp.y * 1.7) * 3.1 + t * 1.35) * 0.5 + 0.5), 2.2);
    float shimmer = pow(1.0 - abs(fract((warp.x - warp.y * 0.72 + t * 0.18) * 3.1) - 0.5) * 2.0, 10.0);
    float centerGlow = exp(-dot(p * vec2(1.1, 1.35), p * vec2(1.1, 1.35)) * 1.4);
    float rim = pow(1.0 - min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y)), 1.6);

    vec3 base = mix(secondaryColor.rgb, primaryColor.rgb, clamp(velvet * 0.75 + sweep * 0.25, 0.0, 1.0));
    vec3 foil = mix(color.rgb, vec3(1.0), clamp(ribbon * 0.65 + shimmer * 0.35, 0.0, 1.0));

    vec3 finalColor = base * (0.45 + sweep * 0.26);
    finalColor += foil * ribbon * (0.70 + sideMask * 0.20);
    finalColor += color.rgb * shimmer * (0.18 + sideMask * 0.08);
    finalColor += secondaryColor.rgb * centerGlow * 0.16;
    finalColor += vec3(1.0) * (ribbon * 0.05 + shimmer * 0.04 + rim * 0.025);

    float outAlpha = max(color.a, max(primaryColor.a, secondaryColor.a));
    float alphaMask = 0.66 + sweep * 0.10 + ribbon * 0.12 + shimmer * 0.08 + centerGlow * 0.04;
    fragColor = vec4(finalColor, outAlpha * alphaMask);
}
