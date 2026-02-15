#version 120

varying vec3 position;

void main() {

    // Eye-coordinate position of vertex, needed in various calculations
    vec4 ecPosition = gl_ModelViewMatrix * gl_Vertex;

    gl_Position = ftransform();
    gl_FrontColor = gl_FrontLightModelProduct.sceneColor;

    gl_TexCoord[0] = gl_MultiTexCoord0;

    position = ecPosition.xyz;
}
