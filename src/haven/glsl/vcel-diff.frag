#pp header

#pp order 250
#pp entry vcel_d_f
void vcel_d_f(inout vec4 res);

#pp main

vec3 celramp1(vec3 c)
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

void vcel_d_f(inout vec4 res) {
    res = vec4(celramp1(clamp(res.rgb, 0.0, 1.0)), res.a);
}
