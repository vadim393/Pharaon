#version 150

#moj_import <mre:common.glsl>

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
uniform float BlurRadius;

out vec4 OutColor;

void main() {
    float mask = ralpha(Size, FragCoord, Radius, Smoothness);
    if (mask <= 0.0) {
        discard;
    }

    if (BlurRadius <= 0.0) {
        vec4 color = texture(Sampler0, TexCoord) * FragColor;
        color.a *= mask;
        OutColor = color;
        return;
    }

    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec2 step1 = texel * max(1.0, BlurRadius * 0.45);
    vec2 step2 = step1 * 2.0;

    vec3 blur = vec3(0.0);
    float weightSum = 0.0;

    blur += texture(Sampler0, TexCoord).rgb * 0.20;
    weightSum += 0.20;

    blur += texture(Sampler0, TexCoord + vec2(step1.x, 0.0)).rgb * 0.09;
    blur += texture(Sampler0, TexCoord - vec2(step1.x, 0.0)).rgb * 0.09;
    blur += texture(Sampler0, TexCoord + vec2(0.0, step1.y)).rgb * 0.09;
    blur += texture(Sampler0, TexCoord - vec2(0.0, step1.y)).rgb * 0.09;
    weightSum += 0.36;

    blur += texture(Sampler0, TexCoord + vec2(step1.x, step1.y)).rgb * 0.07;
    blur += texture(Sampler0, TexCoord + vec2(-step1.x, step1.y)).rgb * 0.07;
    blur += texture(Sampler0, TexCoord + vec2(step1.x, -step1.y)).rgb * 0.07;
    blur += texture(Sampler0, TexCoord + vec2(-step1.x, -step1.y)).rgb * 0.07;
    weightSum += 0.28;

    blur += texture(Sampler0, TexCoord + vec2(step2.x, 0.0)).rgb * 0.04;
    blur += texture(Sampler0, TexCoord - vec2(step2.x, 0.0)).rgb * 0.04;
    blur += texture(Sampler0, TexCoord + vec2(0.0, step2.y)).rgb * 0.04;
    blur += texture(Sampler0, TexCoord - vec2(0.0, step2.y)).rgb * 0.04;
    weightSum += 0.16;

    blur /= max(weightSum, 1e-5);

    vec4 color = vec4(blur, 1.0) * FragColor;
    color.a *= mask;
    OutColor = color;
}
