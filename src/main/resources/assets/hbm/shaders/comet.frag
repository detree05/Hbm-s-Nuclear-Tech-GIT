// Copyright (c) 2025 @OGAmeDivision on X. All Rights Reserved. / DM me if you'd like to use it somewhere :)

#define STEPS 160
#define MAX_DIST 280.0
#define PI 3.14159265359

// --- COLORS ---
const vec3 COL_ION = vec3(0.2, 0.6, 1.7);       
const vec3 COL_DUST = vec3(2.5, 2.2, 1.8);      
const vec3 COL_COMA = vec3(0.0, 2.0, 1.4);      

const vec3 SUN_DIR = normalize(vec3(0.6, 0.5, -0.4));

float hash(float n) { return fract(sin(n) * 43758.5453123); }

float noise(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f*f*(3.0-2.0*f);
    float n = p.x + p.y*57.0 + 113.0*p.z;
    return mix(mix(mix(hash(n+0.0), hash(n+1.0),f.x),
                   mix(hash(n+57.0), hash(n+58.0),f.x),f.y),
               mix(mix(hash(n+113.0), hash(n+114.0),f.x),
                   mix(hash(n+170.0), hash(n+171.0),f.x),f.y),f.z);
}

float fbm(vec3 p) {
    float f = 0.0; float amp = 0.5;
    for(int i=0; i<3; i++) { f += amp * noise(p); p *= 1.7; amp *= 0.5; }
    return f;
}

float fbmIonStructure(vec3 p) {
    float f = 0.0; float amp = 0.5;
    mat3 m = mat3(0.0, 0.8, 0.6, -0.8, 0.36, -0.48, -0.6, -0.48, 0.64);
    for(int i=0; i<3; i++) { f += amp * noise(p); p = m * p * 1.6; amp *= 0.5; }
    return f;
}

float ridgedFbmIon(vec3 p) {
    float f = 0.0; float amp = 0.5;
    for(int i=0; i<3; i++) {
        float n = 1.0 - abs(noise(p) * 2.0 - 1.0);
        f += amp * n * n * n; 
        p *= 1.8; 
        amp *= 0.5;
    }
    return f;
}

vec3 getFlowIon(vec3 p, float t) {
    vec3 q = vec3(fbmIonStructure(p+vec3(0,t*0.5,0)), fbmIonStructure(p+vec3(5.2,1.3,t*0.2)), fbmIonStructure(p+vec3(2.4,6.1,t*0.3)));
    return vec3(fbmIonStructure(p+2.0*q+vec3(4.1,t,9.2)), fbmIonStructure(p+2.0*q+vec3(8.3,2.1,t)), fbmIonStructure(p+2.0*q+vec3(1.1,5.5,3.2)));
}

mat2 rot2D(float a) { float cr=cos(a), sr=sin(a); return mat2(cr,-sr,sr,cr); }

// --- ION TAIL (Blue) ---
float mapIon(vec3 p) {
    vec3 q = p;
    q.xz *= rot2D(0.2); 
    
    float r = length(q.xy);
    if(q.z < -5.0 || r > 25.0) return 0.0; 

    float twist = q.z * 0.15 - iTime * 0.3;
    q.xy *= rot2D(twist);

    vec3 flow = q * vec3(1.2, 1.2, 0.2); 
    flow.z -= iTime * 0.8; 
    
    vec3 warp = getFlowIon(flow * 0.15, 0.0);
    float basePlasma = fbmIonStructure(flow * 0.4 + warp);
    float lightning = ridgedFbmIon(flow * 0.5 + warp * 0.5);
    
    float den = mix(basePlasma, lightning, 0.6) + 0.1; 
    den = smoothstep(0.0, 1.6, den); 

    float width = 0.02 + q.z * 0.04; 
    float mask = smoothstep(width + 3.0, width, r); 
    mask *= smoothstep(-5.0, 2.0, q.z);
    mask *= exp(-q.z * 0.09);
    
    return den * mask * 3.5; 
}

// --- DUST TAIL (White) ---
float mapDust(vec3 p) {
    vec3 q = p;
    q.xz *= rot2D(-0.3); 
    
    float curve = 0.04 * q.z * q.z;
    q.x -= curve;
    q.y *= 2.0; 

    float r = length(q.xy);
    if(q.z < -5.0 || r > 60.0) return 0.0;

    vec3 dustCoord = q * 0.25; 
    dustCoord.z -= iTime * 0.2; 
    float den = fbm(dustCoord); 
    den += fbm(q * vec3(1.2, 0.1, 0.1)) * 0.4;

    float coreBridge = exp(-length(p) * 0.7);
    den += coreBridge * 1.5;

    float width = 0.2 + max(0.0, q.z) * 0.6; 
    
    float mask = smoothstep(width + 8.0, width * 0.1, r);
    mask *= smoothstep(-3.5, 0.5, q.z); 
    mask *= exp(-q.z * 0.09);

    return den * mask * 1.8; 
}

float mapComa(vec3 p) {
    float r = length(p);
    if(r > 14.0) return 0.0;
    float den = 1.0 / (r * r + 0.1); 
    den += fbm(p * 1.5 - iTime) * 0.2 * exp(-r);
    return den;
}

vec3 map(vec3 p) {
    if(length(p) > 280.0) return vec3(0.0);
    vec3 q = p;
    return vec3(mapIon(q), mapDust(q), mapComa(q));
}

float henyeyGreenstein(float cosTheta, float g) {
    float g2 = g * g;
    return (1.0 - g2) / (4.0 * PI * pow(1.0 + g2 - 2.0 * g * cosTheta, 1.5));
}

vec4 renderVolumetric(vec3 ro, vec3 rd, vec2 fragCoord) {
    vec3 sumCol = vec3(0.0);
    float transmittance = 1.0;

    // Shadertoy uses fragCoord for screen position
    float dither = fract(sin(dot(fragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
    float t = 1.0 + dither * 0.2; 

    float cosTheta = dot(rd, SUN_DIR);
    float forwardScatter = henyeyGreenstein(cosTheta, 0.6);
    float isotropicFill = 0.05; 
    float dustLight = (forwardScatter * 8.0) + isotropicFill + 0.3;

    for(int i=0; i<STEPS; i++) {
        if(transmittance < 0.001 || t > MAX_DIST) break;

        vec3 p = ro + rd * t;
        vec3 d = map(p); 
        float totalDensity = d.x + d.y + d.z;

        if(totalDensity > 0.001) {
            vec3 src = vec3(0.0);
            src += COL_ION * d.x * 2.5;
            src += COL_DUST * dustLight * d.y;

            float coreMix = smoothstep(0.0, 6.0, d.z); 
            vec3 comaColor = mix(COL_COMA, vec3(8.0), coreMix); 
            src += comaColor * d.z;

            float alphaDen = (d.x * 0.2) + (d.y * 0.1) + (d.z * 0.5);
            float alpha = 1.0 - exp(-alphaDen * 0.4); 

            sumCol += src * alpha * transmittance;
            transmittance *= (1.0 - alpha);
        }
        float stepSize = 0.07 + t * 0.025; 
        t += stepSize;
    }
    return vec4(sumCol, transmittance);
}

float hash13(vec3 p3) {
    p3  = fract(p3 * .1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}
float valueNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f); 
    return mix(
        mix(mix(hash13(i+vec3(0,0,0)), hash13(i+vec3(1,0,0)), f.x),
            mix(hash13(i+vec3(0,1,0)), hash13(i+vec3(1,1,0)), f.x), f.y),
        mix(mix(hash13(i+vec3(0,0,1)), hash13(i+vec3(1,0,1)), f.x),
            mix(hash13(i+vec3(0,1,1)), hash13(i+vec3(1,1,1)), f.x), f.y), f.z);
}

vec3 getStarsRealistic(vec3 rd) {
    vec3 col = vec3(0.0);
    float n1 = valueNoise(rd * 480.0); 
    if(n1 > 0.92) { 
        float b = pow((n1 - 0.92) * 12.5, 4.0); 
        col += vec3(0.65, 0.7, 0.9) * b * 8.0; 
    }
    float n2 = valueNoise(rd * 280.0 + vec3(43.0)); 
    if(n2 > 0.94) {
        float b = pow((n2 - 0.94) * 16.0, 5.0);
        float t = hash13(floor(rd * 280.0));
        vec3 starCol = mix(vec3(1.0, 0.6, 0.4), vec3(0.6, 0.8, 1.0), t);
        if(t > 0.4 && t < 0.6) starCol = vec3(1.0, 0.98, 0.95);
        col += starCol * b * 45.0;
    }
    return col;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec2 uv = (fragCoord - 0.5 * iResolution.xy) / iResolution.y;

    float drift = iTime * 0.05;

    // --- CAMERA SETUP ---
    // ro: Positioned on the side (+X).
    vec3 ro = vec3(
            52.0 + 3.0 * sin(drift), 
            6.0 + 2.0 * cos(drift * 0.8), 
        -10.0 + 4.0 * cos(drift * 0.5)
    );

    // Look Target (ta):
    vec3 ta = vec3(0.0, 0.0, 6.0); 

    vec3 fwd = normalize(ta - ro);
    
    // --- DIAGONAL ROTATION ---
    vec3 right_std = normalize(cross(fwd, vec3(0.0, 1.0, 0.0)));
    vec3 up_std    = cross(right_std, fwd);
    
    // Roll: 135 degrees 
    float roll = radians(135.0); 
    float cr = cos(roll); 
    float sr = sin(roll); 
    
    vec3 right = right_std * cr - up_std * sr;
    vec3 up    = right_std * sr + up_std * cr;
    
    // WIDER FOV
    vec3 rd = normalize(fwd * 1.6 + right * uv.x + up * uv.y);

    // --- RENDER ---
    vec4 volume = renderVolumetric(ro, rd, fragCoord);

    vec3 col = volume.rgb;

    col *= 1.5; 
    
    // ACES Tone Mapping
    float a = 2.51, b = 0.03, c = 2.43, d = 0.59, e = 0.14;
    col = clamp((col * (a * col + b)) / (col * (c * col + d) + e), 0.0, 1.0);
    col = pow(col, vec3(1.0/2.2));

    // Grain
    float brightness = length(col);
    float grain = (hash(dot(uv, vec2(12.3, 45.6)) + iTime) - 0.5) * 0.01;
    if(brightness > 0.01) col += grain * sqrt(brightness);

    // Vignette
    col *= pow(1.0 - length(uv * 0.45), 0.8);
    
    fragColor = vec4(col, 1.0);
}