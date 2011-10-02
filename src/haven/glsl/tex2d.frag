#pp header

#pp order 500
#pp entry tex2d_f
void tex2d_f(inout vec4 res);

#pp main

uniform sampler2D tex2d;

void tex2d_f(inout vec4 res)
{
    res *= texture2D(tex2d, gl_TexCoord[0].st);
}
