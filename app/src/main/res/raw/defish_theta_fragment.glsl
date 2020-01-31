precision highp float;

uniform sampler2D uSampler;

varying vec2 vTextureCoord;

const float radio = 0.443;
const vec4 _offset = vec4(0.125, 0.0, 0.125, 0.0);
const float _distortion = 1.0;
const float _photo = 0.0;

#define blurringWidth 0.005
#define blur 100.0
#define PI 3.1415926535897932384626433832795
#define PImid 1.5707963267949
//por que la esfera esta achatada un 1,111111%
#define flattenCircle 8.0 / 9.0

vec4 una(vec2 v, bool delante) {
    if (!delante){
        v.x -= 0.5;
    }
    //ajusta el tamaño de la esfera
    v.x *= 2.0;
    v *= PI;

    vec3 p = vec3(cos(v.x) * _distortion, cos(v.y), sin(v.x));
    p.xz *= sqrt(1.0 - p.y * p.y);

    float r = 1.0 - asin(p.z) / PImid;
    vec2 st = vec2(p.x, p.y);
    st *= r * radio / sqrt(1.0 - p.z * p.z);
    st += 0.5;

    if(delante){
        st.y = 1.0 - st.y;
        st.y *= 0.5;
        st.y += 0.5;
        st.x = 1.0 - st.x;
        st.xy += _offset.zw;
    } else {
        st.y *= 0.5;
        st.xy += _offset.xy;
    }
    st.x *= flattenCircle;
    //invertimos las posiciones para girar 90º
    return texture2D(uSampler, vec2(st.y, st.x));
}

float circularClamp(float x, float min, float max){
    float r = x;
    if (r >= min && r <= max) return r;

    if (r < min){
        while(r < min){
            r = max - min - r;
        }
        return r;
    }

    while(r > max){
        r = min + r - max;
    }
    return r;
}

void main(){
    vec2 v = vec2(circularClamp(vTextureCoord.x, 0.0, 1.0), vTextureCoord.y);
    //blurring derecha
    if(v.x > 0.75 - blurringWidth && v.x < 0.75 + blurringWidth){
      //cojemos el pixel del 3º cuarto y el pixel del 4º cuarto y los mezclamos con la importancia de alpha
      float alpha = blur * (v.x - 0.75 + blurringWidth);
      gl_FragColor = una(vec2(v.x + 0.25, v.y), false) * (1.0 - alpha) + una(vec2(v.x - 0.75, v.y), true) * alpha;

    //blurring izquierda
    } else if (v.x > 0.25 - blurringWidth && v.x < 0.25 + blurringWidth){
      //cojemos el pixel del 1º cuarto y el pixel del 2º cuarto y los mezclamos con la importancia de alpha
      float alpha = blur * (v.x - 0.25 + blurringWidth);
      gl_FragColor = una(vec2(v.x + 0.25, v.y), true) * (1.0 - alpha) + una(vec2(v.x - 0.75, v.y), false) * alpha;

    //1º cuarto de la imagen
    } else if (v.x < 0.25) {
        //movemos una posicion a la izquierda
        gl_FragColor = una(vec2(v.x + 0.25, v.y), true);

    //2º cuarto de la imagen
    } else if (v.x > 0.25 && v.x <= 0.5) {
       //movemos 3 posiciones a la derecha
       gl_FragColor = una(vec2(v.x - 0.75, v.y), false);

    //3º cuarto de la imagen
    } else if (v.x > 0.5 && v.x <= 0.75) {
        //movemos una posicion a la izquierda
        gl_FragColor = una(vec2(v.x + 0.25, v.y), false);

    //4º cuarto de la imagen
    } else {
        //movemos 3 posiciones a la derecha
        gl_FragColor = una(vec2(v.x - 0.75, v.y), true);
    }
}
