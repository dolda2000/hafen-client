#pp header

#pp order 100
#pp entry plight_v eyev eyen fcol
void plight_v(vec4 pos, vec3 norm, out vec4 col);

#pp main

varying vec3 plight_pos;
varying vec3 plight_nrm;

uniform mat4 pslight_txf;
varying vec4 pslight_stc;

void plight_v(vec4 pos, vec3 norm, out vec4 col)
{
    col = vec4(1.0, 1.0, 1.0, 1.0);
    plight_pos = pos.xyz;
    plight_nrm = norm;
    pslight_stc = pslight_txf * pos;
}
