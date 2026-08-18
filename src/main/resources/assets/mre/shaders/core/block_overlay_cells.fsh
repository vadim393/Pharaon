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

float hash12(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

vec2 hash22(vec2 p) {
    float x = hash12(p + vec2(1.7, 9.2));
    float y = hash12(p + vec2(8.3, 2.8));
    return vec2(x, y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash12(i);
    float b = hash12(i + vec2(1.0, 0.0));
    float c = hash12(i + vec2(0.0, 1.0));
    float d = hash12(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    mat2 rot = mat2(0.82, -0.57, 0.57, 0.82);
    for (int i = 0; i < 8; i++) {
        if (i >= quality) break;
        value += noise(p) * amplitude;
        p = rot * p * 1.86 + vec2(6.7, 4.3);
        amplitude *= 0.53;
    }
    return value;
}

void updateNearest(float d, inout float first, inout float second) {
    if (d < first) {
        second = first;
        first = d;
    } else if (d < second) {
        second = d;
    }
}

void main() {
    vec2 uv = clamp(vAxis.xy, 0.0, 1.0);
    float sideMask = vAxis.z;
    vec2 p = (uv - 0.5) * 2.0;
    float t = time;

    vec2 flow = vec2(
        fbm(p * 1.55 + vec2(t * 0.18, -t * 0.11)),
        fbm(p * 1.55 + vec2(-t * 0.15, t * 0.16))
    ) - 0.5;

    float density = mix(3.6, 5.0, sideMask);
    vec2 gridUv = (p + flow * 0.34) * density;
    vec2 cellId = floor(gridUv);
    vec2 local = fract(gridUv) - 0.5;

    float nearest = 10.0;
    float secondNearest = 10.0;
    float nucleus = 0.0;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 offset = vec2(float(x), float(y));
            vec2 id = cellId + offset;
            vec2 rnd = hash22(id);

            float phase = t * mix(0.85, 1.35, rnd.x) + rnd.y * 6.2831853;
            vec2 drift = vec2(sin(phase), cos(phase * 0.83 + rnd.x * 5.2)) * 0.17;

            vec2 splitDir = hash22(id + 13.4) - 0.5;
            splitDir = normalize(splitDir + vec2(0.001, 0.0));
            float splitPulse = smoothstep(0.18, 0.95, sin(phase * 0.9 - rnd.x * 3.7) * 0.5 + 0.5);
            float splitAmount = splitPulse * (0.08 + rnd.y * 0.11);

            vec2 base = offset + rnd - 0.5 + drift - local;
            vec2 a = base + splitDir * splitAmount;
            vec2 b = base - splitDir * splitAmount * 0.92;

            float da = length(a);
            float db = length(b);
            updateNearest(da, nearest, secondNearest);
            updateNearest(db, nearest, secondNearest);

            nucleus = max(nucleus, exp(-da * da * 11.0) * (0.55 + splitPulse * 0.45));
            nucleus = max(nucleus, exp(-db * db * 11.0) * (0.50 + splitPulse * 0.40));
        }
    }

    float membrane = 1.0 - smoothstep(0.02, 0.12, secondNearest - nearest);
    float body = smoothstep(0.52, 0.08, nearest);
    float ripple = fbm(gridUv * 0.72 + vec2(t * 0.06, -t * 0.04));
    float shimmer = pow(max(0.0, sin((p.x * 4.8 - p.y * 3.4) + t * 1.55) * 0.5 + 0.5), 5.0);
    float rim = pow(1.0 - min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y)), 1.85);

    vec3 baseColor = mix(primaryColor.rgb, secondaryColor.rgb, clamp(body * 0.7 + ripple * 0.3, 0.0, 1.0));
    vec3 membraneColor = mix(secondaryColor.rgb, color.rgb, clamp(membrane * 0.75 + shimmer * 0.25, 0.0, 1.0));

    vec3 finalColor = baseColor * (0.34 + body * 0.24);
    finalColor += membraneColor * membrane * (0.72 + sideMask * 0.18);
    finalColor += color.rgb * nucleus * 0.38;
    finalColor += vec3(1.0) * (nucleus * 0.10 + membrane * 0.05 + rim * 0.02);
    finalColor *= 0.92 + ripple * 0.12;

    float outAlpha = max(color.a, max(primaryColor.a, secondaryColor.a));
    float alphaMask = 0.30 + body * 0.28 + membrane * 0.34 + nucleus * 0.16;
    fragColor = vec4(finalColor, outAlpha * clamp(alphaMask, 0.0, 1.0));
}
