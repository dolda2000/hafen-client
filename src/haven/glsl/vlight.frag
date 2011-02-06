#pp header

#pp order 750
#pp entry vlight_f
void vlight_f(inout vec4 res);

#pp main

varying vec3 vlight_spec;

void vlight_f(inout vec4 res) {
    res += vec4(vlight_spec, 0.0);
}
