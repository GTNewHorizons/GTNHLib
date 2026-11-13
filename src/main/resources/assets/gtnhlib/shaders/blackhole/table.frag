#version 330 core

// Each texel traces one ray starting u_Start from the hole, at an angle from straight out of 0 to pi across the table.
uniform float u_Start;
uniform float u_Width;
uniform float u_Horizon;
uniform float u_FadeStart;
uniform float u_FadeEnd;

// Angles count past a full turn for rays that loop.
out vec4 o_Table;

const int MAX_STEPS = 400;
const float PI = 3.14159265;

// Light bending around a non-spinning black hole, clamped at the horizon where it blows up.
vec3 bend(vec3 p, float h2) {
    float r = length(p);
    float rc = max(r, u_Horizon);
    return -1.5 * u_Horizon * h2 * p / (rc * rc * rc * rc * rc) * (1.0 - smoothstep(u_FadeStart, u_FadeEnd, r));
}

// Steps shrink smoothly towards the hole. Abrupt changes show as bands in the image.
float stepSize(float r) {
    return clamp((r - u_Horizon) * 0.25, 0.002, 0.05);
}

// Leapfrog stays accurate for paths that loop around the hole.
void leapfrog(vec3 pos, vec3 dir, vec3 accel, float h2, float dt, out vec3 next, out vec3 nextDir, out vec3 nextAccel) {
    next = pos + (dir + accel * dt * 0.5) * dt;
    nextAccel = bend(next, h2);
    nextDir = dir + (accel + nextAccel) * dt * 0.5;
}

void main() {
    float a = gl_FragCoord.x / u_Width * PI;
    // The hole is on the ray's anticlockwise side, so turning towards it and going around it both count up.
    vec3 pos = vec3(u_Start, 0.0, 0.0);
    vec3 dir = vec3(cos(a), sin(a), 0.0);
    vec3 h = cross(pos, dir);
    float h2 = dot(h, h);
    vec3 accel = bend(pos, h2);
    float turned = 0.0;
    float around = 0.0;
    float fell = 0.0;
    // From inside the horizon, look out instead of seeing black.
    bool captures = u_Start > u_Horizon;
    for (int i = 0; i < MAX_STEPS; i++) {
        float r = length(pos);
        if (captures && r < u_Horizon) {
            fell = 1.0;
            break;
        }
        if (r > 1.0 && dot(pos, dir) > 0.0) break;
        vec3 next, nextDir, nextAccel;
        leapfrog(pos, dir, accel, h2, stepSize(r), next, nextDir, nextAccel);
        turned += atan(dir.x * nextDir.y - dir.y * nextDir.x, dot(dir, nextDir));
        around += atan(pos.x * next.y - pos.y * next.x, dot(pos, next));
        pos = next;
        dir = nextDir;
        accel = nextAccel;
    }
    o_Table = vec4(turned, fell, around, 0.0);
}
