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
    mat2 rot = mat2(0.80, -0.60, 0.60, 0.80);
    for (int i = 0; i < 8; i++) {
        if (i >= quality) break;
        value += noise(p) * amplitude;
        p = rot * p * 1.85 + vec2(7.13, 3.71);
        amplitude *= 0.52;
    }
    return value;
}

void main() {
    vec2 uv = clamp(vAxis.xy, 0.0, 1.0);
    float sideMask = vAxis.z;
    vec2 p = (uv - 0.5) * 2.0;
    float t = time * 0.85;

    vec2 flow = vec2(
        fbm(p * 1.8 + vec2(t * 0.52, -t * 0.21)),
        fbm(p * 1.8 + vec2(-t * 0.29, t * 0.44))
    );

    vec2 warp = p + (flow - 0.5) * (0.95 + sideMask * 0.25);
    float plasmaA = sin(warp.x * 7.5 + t * 2.7);
    float plasmaB = sin(warp.y * 8.8 - t * 3.1);
    float plasmaC = sin((warp.x + warp.y) * 5.2 + t * 1.9);
    float plasmaD = sin(length(warp * vec2(1.15, 1.45)) * 8.4 - t * 3.6);
    float plasma = (plasmaA + plasmaB + plasmaC + plasmaD) * 0.125 + 0.5;

    float filament = pow(1.0 - abs(plasma * 2.0 - 1.0), 4.2);
    float streaks = pow(max(0.0, sin((warp.x * 10.0 - warp.y * 6.5) + t * 4.4) * 0.5 + 0.5), 6.0);
    float turbulence = fbm(warp * 2.4 + vec2(t * 0.35, -t * 0.28));
    float coreGlow = exp(-dot(p * vec2(1.05, 1.30), p * vec2(1.05, 1.30)) * 1.7);
    float rim = pow(1.0 - min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y)), 1.75);

    vec3 plasmaBase = mix(primaryColor.rgb, secondaryColor.rgb, clamp(plasma * 0.75 + turbulence * 0.25, 0.0, 1.0));
    vec3 hotCore = mix(secondaryColor.rgb, color.rgb, clamp(filament * 0.85 + streaks * 0.35, 0.0, 1.0));

    vec3 finalColor = plasmaBase * (0.42 + plasma * 0.34);
    finalColor += hotCore * filament * (0.85 + sideMask * 0.25);
    finalColor += color.rgb * streaks * (0.35 + sideMask * 0.20);
    finalColor += secondaryColor.rgb * coreGlow * 0.18;
    finalColor += vec3(1.0) * (filament * 0.08 + streaks * 0.05 + rim * 0.03);
    finalColor *= 0.92 + turbulence * 0.12;

    float outAlpha = max(color.a, max(primaryColor.a, secondaryColor.a));
    float alphaMask = 0.68 + plasma * 0.10 + filament * 0.18 + streaks * 0.10 + coreGlow * 0.06;
    fragColor = vec4(finalColor, outAlpha * alphaMask);
}
