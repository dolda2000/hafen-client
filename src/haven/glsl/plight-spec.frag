#pp header

#pp order 750
#pp entry plight_f_spec
void plight_f_spec(inout vec4 res);

#pp main

varying vec3 plight_pos;
varying vec3 plight_nrm;

uniform int nlights;

void plight_f_spec(inout vec4 res)
{
    if(gl_FrontMaterial.shininess > 0.5) {
	vec3 norm = normalize(plight_nrm);
	vec3 edir = normalize(-plight_pos.xyz);
	vec3 spec = vec3(0.0, 0.0, 0.0);
	for(int i = 0; i < nlights; i++) {
	    if(gl_LightSource[i].position.w == 0.0) {
		vec3 dir = gl_LightSource[i].position.xyz;
		spec += gl_FrontMaterial.specular.rgb * gl_LightSource[i].specular.rgb *
		    pow(max(dot(edir, reflect(-dir, norm)), 0.0), gl_FrontMaterial.shininess);
	    } else {
		vec3 rel = gl_LightSource[i].position.xyz - plight_pos.xyz;
		vec3 dir = normalize(rel);
		float dist = length(rel);
		float att = 1.0 / (gl_LightSource[i].constantAttenuation
				   + (gl_LightSource[0].linearAttenuation * dist)
				   + (gl_LightSource[0].quadraticAttenuation * dist * dist));
		spec += gl_FrontMaterial.specular.rgb * gl_LightSource[i].specular.rgb * att *
		    pow(max(dot(edir, reflect(-dir, norm)), 0.0), gl_FrontMaterial.shininess);
	    }
	}
	res += vec4(spec, 0.0);
    }
}
