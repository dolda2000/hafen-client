#pp header

#pp order 750
#pp entry vcel_s_f
void vcel_s_f(inout vec4 res);

#pp main

varying vec3 vlight_spec;

vec3 celramp2(vec3 c)
{
    float m = max(max(c.r, c.g), c.b);
    if(m < 0.01)
	return(vec3(0.0, 0.0, 0.0));
    float v;
    if(m > 0.5)
	v = 1.0;
    else if(m > 0.1)
	v = 0.5;
    else
	v = 0.0;
    return(c * v / m);
}

void vcel_s_f(inout vec4 res) {
    res += vec4(celramp2(clamp(vlight_spec, 0.0, 1.0)), 0.0);
}
