#pp header

#pp order 250
#pp entry plight_f_diff
void plight_f_diff(inout vec4 col);

#pp main

varying vec3 plight_pos;
varying vec3 plight_nrm;

uniform int nlights;

void plight_f_diff(inout vec4 col)
{
    vec3 norm = normalize(plight_nrm);
    col = gl_FrontMaterial.emission;
    for(int i = 0; i < nlights; i++) {
	if(gl_LightSource[i].position.w == 0.0) {
	    col += gl_FrontMaterial.ambient * gl_LightSource[i].ambient;
	    vec3 dir = gl_LightSource[i].position.xyz;
	    float df = max(dot(norm, dir), 0.0);
	    col += gl_FrontMaterial.diffuse * gl_LightSource[i].diffuse * df;
	} else {
	    vec3 rel = gl_LightSource[i].position.xyz - plight_pos.xyz;
	    vec3 dir = normalize(rel);
	    float dist = length(rel);
	    float att = 1.0 / (gl_LightSource[i].constantAttenuation
			       + (gl_LightSource[0].linearAttenuation * dist)
			       + (gl_LightSource[0].quadraticAttenuation * dist * dist));
	    col += gl_FrontMaterial.ambient * gl_LightSource[i].ambient * att;
	    float df = max(dot(norm, dir), 0.0);
	    col += gl_FrontMaterial.diffuse * gl_LightSource[i].diffuse * df * att;
	}
    }
    col.a = gl_FrontMaterial.diffuse.a;
}
