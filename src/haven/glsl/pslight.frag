#pp header

#pp order 750
#pp entry plight_f
void plight_f(inout vec4 res);

#pp main

varying vec3 plight_pos;
varying vec3 plight_nrm;
varying vec4 pslight_stc;

uniform int nlights, pslight_sl;
uniform sampler2D pslight_map;

void dolight(int i, vec3 norm, vec3 edir, float shadow, inout vec3 diff, inout vec3 spec)
{
    if(gl_LightSource[i].position.w == 0.0) {
	diff += gl_FrontMaterial.ambient.rgb * gl_LightSource[i].ambient.rgb;
	vec3 dir = gl_LightSource[i].position.xyz;
	float df = max(dot(norm, dir), 0.0);
	if(i == pslight_sl)
	    df *= shadow;
	if(df > 0.0) {
	    diff += gl_FrontMaterial.diffuse.rgb * gl_LightSource[i].diffuse.rgb * df;
	    if(gl_FrontMaterial.shininess > 0.5) {
		spec += gl_FrontMaterial.specular.rgb * gl_LightSource[i].specular.rgb *
		    pow(max(dot(edir, reflect(-dir, norm)), 0.0), gl_FrontMaterial.shininess);
	    }
	}
    } else {
	vec3 rel = gl_LightSource[i].position.xyz - plight_pos.xyz;
	vec3 dir = normalize(rel);
	float dist = length(rel);
	float att = 1.0 / (gl_LightSource[i].constantAttenuation
			   + (gl_LightSource[i].linearAttenuation * dist)
			   + (gl_LightSource[i].quadraticAttenuation * dist * dist));
	diff += gl_FrontMaterial.ambient.rgb * gl_LightSource[i].ambient.rgb * att;
	float df = max(dot(norm, dir), 0.0);
	if(i == pslight_sl)
	    df *= shadow;
	if(df > 0.0) {
	    diff += gl_FrontMaterial.diffuse.rgb * gl_LightSource[i].diffuse.rgb * df * att;
	    if(gl_FrontMaterial.shininess > 0.5) {
		spec += gl_FrontMaterial.specular.rgb * gl_LightSource[i].specular.rgb * att *
		    pow(max(dot(edir, reflect(-dir, norm)), 0.0), gl_FrontMaterial.shininess);
	    }
	}
    }
}

void plight_f(inout vec4 res)
{
    float sdw = 0.0;
    if(pslight_sl != -1) {
	vec3 barda = pslight_stc.xyz / pslight_stc.w;
	float xo, yo;
	for(yo = -0.00075; yo < 0.00076; yo += 0.0005) {
	    for(xo = -0.00075; xo < 0.00076; xo += 0.0005) {
		if((texture2D(pslight_map, barda.xy + vec2(xo, yo)).z + 0.0005) > barda.z) sdw += 0.0625;
	    }
	}
	/*
	for(yo = -0.0015; yo < 0.0016; yo += 0.001) {
	    for(xo = -0.0015; xo < 0.0016; xo += 0.001) {
		if((texture2D(pslight_map, barda.xy + vec2(xo, yo)).z + 0.0005) > barda.z) sdw += 0.0625;
	    }
	}
	*/
    }
    
    vec3 norm = normalize(plight_nrm);
    vec3 edir;
    if(gl_FrontMaterial.shininess > 0.5)
	edir = normalize(-plight_pos.xyz);
    vec3 diff = gl_FrontMaterial.emission.rgb;
    vec3 spec = vec3(0.0, 0.0, 0.0);
    if(nlights > 0)
	dolight(0, norm, edir, sdw, diff, spec);
    if(nlights > 1)
	dolight(1, norm, edir, sdw, diff, spec);
    if(nlights > 2)
	dolight(2, norm, edir, sdw, diff, spec);
    if(nlights > 3)
	dolight(3, norm, edir, sdw, diff, spec);
    res *= vec4(min(diff, 1.0), gl_FrontMaterial.diffuse.a);
    res += vec4(min(spec, 1.0), 0.0);
}
