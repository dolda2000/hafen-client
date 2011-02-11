#pp header

#pp order 100
#pp entry vlight_v eyev eyen fcol
void vlight_v(vec4 pos, vec3 norm, out vec4 col);

#pp main

uniform int nlights;
varying vec3 vlight_spec;

void vlight_v(vec4 pos, vec3 norm, out vec4 col)
{
    col = gl_FrontMaterial.emission;
    vlight_spec = vec3(0.0, 0.0, 0.0);
    for(int i = 0; i < nlights; i++) {
	if(gl_LightSource[i].position.w == 0.0) {
	    col += gl_FrontMaterial.ambient * gl_LightSource[i].ambient;
	    float df = max(dot(norm, vec3(gl_LightSource[i].position.xyz)), 0.0);
	    if(df > 0.0) {
		col += gl_FrontMaterial.diffuse * gl_LightSource[i].diffuse * df;
		if(gl_FrontMaterial.shininess > 0.5) {
		    vlight_spec += gl_FrontMaterial.specular.rgb * gl_LightSource[i].specular.rgb *
			pow(max(dot(norm, normalize(gl_LightSource[i].halfVector.xyz)), 0.0), gl_FrontMaterial.shininess);
		    /*
		    vec3 edir = normalize(-vec3(pos));
		    vlight_spec += gl_FrontMaterial.specular.rgb * gl_LightSource[i].specular.rgb *
			pow(max(dot(edir, normalize(reflect(gl_LightSource[i].position.xyz, norm))), 0.0), gl_FrontMaterial.shininess);
		    */
		}
	    }
	} else {
	    
	}
    }
    col.a = gl_FrontMaterial.diffuse.a;
}
