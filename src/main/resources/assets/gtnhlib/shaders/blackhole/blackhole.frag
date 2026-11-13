#version 330 core

// Positions are in the hole's own space, where its sphere has radius 1.
uniform mat4 u_ModelViewProjection;
uniform vec3 u_Camera;
uniform float u_Horizon;
// Fog distances are in blocks.
uniform float u_FogStart;
uniform float u_FogEnd;
uniform vec3 u_FogColor;
uniform float u_BlocksPerUnit;
uniform sampler2D u_Scene;
uniform sampler2D u_SceneDepth;
uniform samplerCube u_Capture;
// Indexed by the ray's starting angle from straight out, over pi: how far it turns towards the hole, 1 if it falls
// in, and how far around the hole it goes. u_Outside starts at the sphere's edge, u_Inside at the camera inside it.
uniform sampler2D u_Outside;
uniform sampler2D u_Inside;
// Centre and radius.
uniform vec4 u_Others[4];
uniform int u_OtherCount;

in vec3 v_Position;

out vec4 o_Color;

const float PI = 3.14159265;

// -1 if the ray misses the unit sphere or meets it only behind o.
float enter(vec3 o, vec3 dir) {
    float b = dot(o, dir);
    float disc = b * b - dot(o, o) + 1.0;
    if (disc < 0.0 || sqrt(disc) < b) return -1.0;
    return max(0.0, -b - sqrt(disc));
}

// Moves p to where the ray leaves the sphere and bends dir. Returns how much of it fell in.
float lens(sampler2D table, inout vec3 p, inout vec3 dir) {
    vec3 outward = normalize(p);
    float along = dot(dir, outward);
    vec3 baked = texture(table, vec2(acos(clamp(along, -1.0, 1.0)) / PI, 0.5)).xyz;
    // The table's plane, mapped onto the ray's.
    vec3 around = dir - along * outward;
    float sine = length(around);
    if (sine < 0.000001) {
        p = dir;
        return baked.y;
    }
    around /= sine;
    dir = cos(baked.x) * dir + sin(baked.x) * (along * around - sine * outward);
    p = cos(baked.z) * outward + sin(baked.z) * around;
    return baked.y;
}

vec3 worldAlong(vec3 dir, vec3 straight) {
    // A camera image is rotated 180 degrees from GL's cube face layout.
    vec3 a = abs(dir);
    vec3 flip = a.x >= a.y && a.x >= a.z ? vec3(1.0, -1.0, -1.0) : a.y >= a.z ? vec3(-1.0, 1.0, -1.0) : vec3(-1.0, -1.0, 1.0);
    vec3 captured = texture(u_Capture, dir * flip).rgb;

    vec4 clip = u_ModelViewProjection * vec4(dir, 0.0);
    // Darkening rays bent behind the camera near the horizon is what makes falling in look like falling in.
    if (clip.w <= 0.0) return captured * smoothstep(1.0, 1.5, length(u_Camera) / u_Horizon);
    vec2 ndc = clip.xy / clip.w;
    float offScreen = smoothstep(0.9, 1.0, max(abs(ndc.x), abs(ndc.y)));
    // Barely bent rays stay on the screen copy, or the capture's small differences outline the screen's edge.
    float bent = smoothstep(0.0001, 0.002, 1.0 - dot(dir, straight));
    return mix(texture(u_Scene, ndc * 0.5 + 0.5).rgb, captured, offScreen * bent);
}

void main() {
    vec3 straight = normalize(v_Position - u_Camera);

    // Depth test at the straight ray's closest point to the hole, but never at the camera, where depth is undefined.
    vec3 nearest = u_Camera + straight * max(dot(-u_Camera, straight), 0.02);
    vec4 clip = u_ModelViewProjection * vec4(nearest, 1.0);
    if (clip.z / clip.w * 0.5 + 0.5 > texelFetch(u_SceneDepth, ivec2(gl_FragCoord.xy), 0).r) discard;

    float entry = enter(u_Camera, straight);
    if (entry < 0.0) discard;
    vec3 p = u_Camera + straight * entry;
    vec3 dir = straight;
    float alpha;
    if (dot(u_Camera, u_Camera) < 1.0) alpha = lens(u_Inside, p, dir);
    else alpha = lens(u_Outside, p, dir);

    int hit = -1;
    float hitAt = 0.0;
    for (int i = 0; i < 4 && i < u_OtherCount; i++) {
        float t = enter((p - u_Others[i].xyz) / u_Others[i].w, dir);
        if (t >= 0.0 && (hit < 0 || t < hitAt)) {
            hit = i;
            hitAt = t;
        }
    }
    if (hit >= 0 && alpha < 1.0) {
        vec3 q = (p - u_Others[hit].xyz) / u_Others[hit].w + dir * hitAt;
        alpha += (1.0 - alpha) * lens(u_Outside, q, dir);
    }

    if (alpha < 0.001 && length(cross(dir, straight)) < 0.0005) discard;

    vec3 background = alpha < 0.999 ? worldAlong(dir, straight) : vec3(0.0);
    float fog = clamp((u_FogEnd - length(nearest - u_Camera) * u_BlocksPerUnit) / (u_FogEnd - u_FogStart), 0.0, 1.0);
    o_Color = vec4(u_FogColor * alpha * (1.0 - fog) + (1.0 - alpha) * background, 1.0);
}
