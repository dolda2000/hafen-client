#pp header

#pp order 100
#pp entry vlight_v eyev eyen fcol
void vlight_v(vec4 pos, vec3 norm, out vec4 col);

#pp main

uniform int nlights;
varying vec3 vlight_spec;

void vlight_v(vec4 pos, vec3 norm, out vec4 col)
{
    vec3 edir = normalize(-pos.xyz);
    col = gl_FrontMaterial.emission;
    vlight_spec = vec3(0.0, 0.0, 0.0);
    for(int i = 0; i < nlights; i++) {
	if(gl_LightSource[i].position.w == 0.0) {
	    col += gl_FrontMaterial.ambient * gl_LightSource[i].ambient;
	    vec3 dir = normalize(gl_LightSource[i].position.xyz);
	    float df = max(dot(norm, dir), 0.0);
	    if(df > 0.0) {
		col += gl_FrontMaterial.diffuse * gl_LightSource[i].diffuse * df;
		if(gl_FrontMaterial.shininess > 0.5) {
		    vec3 hv = normalize(edir + dir);
		    vlight_spec += gl_FrontMaterial.specular.rgb * gl_LightSource[i].specular.rgb *
			pow(max(dot(norm, hv), 0.0), gl_FrontMaterial.shininess);
		    /*
		    vlight_spec += gl_FrontMaterial.specular.rgb * gl_LightSource[i].specular.rgb *
			pow(max(dot(edir, normalize(reflect(dir, norm))), 0.0), gl_FrontMaterial.shininess);
		    */
		}
	    }
	} else {
	    vec3 rel = gl_LightSource[i].position.xyz - pos.xyz;
	    vec3 dir = normalize(rel);
	    float dist = length(rel);
	    float att = 1.0 / (gl_LightSource[i].constantAttenuation
			       + (gl_LightSource[i].linearAttenuation * dist)
			       + (gl_LightSource[i].quadraticAttenuation * dist * dist));
	    col += gl_FrontMaterial.ambient * gl_LightSource[i].ambient * att;
	    float df = max(dot(norm, dir), 0.0);
	    if(df > 0.0) {
		col += gl_FrontMaterial.diffuse * gl_LightSource[i].diffuse * df * att;
		if(gl_FrontMaterial.shininess > 0.5) {
		    vec3 hv = normalize(edir + dir);
		    vlight_spec += gl_FrontMaterial.specular.rgb * gl_LightSource[i].specular.rgb * att *
			pow(max(dot(norm, hv), 0.0), gl_FrontMaterial.shininess);
		}
	    }
	}
    }
    col.a = gl_FrontMaterial.diffuse.a;
}
