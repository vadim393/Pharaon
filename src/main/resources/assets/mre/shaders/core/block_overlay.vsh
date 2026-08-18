#version 150

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 vPos;
out vec3 vAxis;

void main() {
    vPos = Position;
    vAxis = Color.rgb;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
