#version 120

uniform float iTime;
uniform sampler2D iChannel1;

const float pi = 3.1415927;


// Approximate blackbody color from temperature in Kelvin (1000K - 40000K)
vec3 kelvinToRGB(float K){
    K = clamp(K, 1000.0, 40000.0) / 100.0;

    float r, g, b;

    // Red
    if (K <= 66.0) r = 1.0;
    else r = clamp(1.292936186062745 * pow(K - 60.0, -0.1332047592), 0.0, 1.0);

    // Green
    if (K <= 66.0) g = clamp(0.3900815787690196 * log(K) - 0.6318414437886275, 0.0, 1.0);
    else g = clamp(1.129890860895294 * pow(K - 60.0, -0.0755148492), 0.0, 1.0);

    // Blue
    if (K >= 66.0) b = 1.0;
    else if (K <= 19.0) b = 0.0;
    else b = clamp(0.5432067891101961 * log(K - 10.0) - 1.19625408914, 0.0, 1.0);

    return vec3(r, g, b);
}

//From Dave (https://www.shadertoy.com/view/XlfGWN)
float hash13(vec3 p){
	p  = fract(p * vec3(.16532,.17369,.15787));
	p += dot(p.xyz, p.yzx + 19.19);
	return fract(p.x * p.y * p.z);
}

float hash(float x){ return fract(cos(x*124.123)*412.0); }
float hash(vec2 x){ return fract(cos(dot(x.xy,vec2(2.31,53.21))*124.123)*412.0); }
float hash(vec3 x){ return fract(cos(dot(x.xyz,vec3(2.31,53.21,17.7))*124.123)*412.0); }

float sdSphere( vec3 p, float s ) {
	return length(p)-s;
}

float sdCappedCylinder( vec3 p, vec2 h ) {
	vec2 d = abs(vec2(length(p.xz),p.y)) - h;
	return min(max(d.x,d.y),0.0) + length(max(d,0.0));
}

float sdTorus( vec3 p, vec2 t ) {
	vec2 q = vec2(length(p.xz)-t.x,p.y);
	return length(q)-t.y;
}

vec2 quantize(vec2 inp, vec2 period) {
	return floor(inp / period) * period;
}

void main() {
	// vec2 pp = quantize(gl_TexCoord[0].xy, vec2(0.03125, 0.03125));
	vec2 pp = gl_TexCoord[0].xy;
	pp = -1.0 + 2.0 * pp;
	pp *= 2.0;

	vec3 lookAt = vec3(0.0, -0.1, 0.0);

	float eyer = 2.0;
	float eyea = 0.0;
	float eyea2 = -0.22 * pi * 2.0;

	vec3 ro = vec3(
		eyer * cos(eyea) * sin(eyea2),
		eyer * cos(eyea2),
		eyer * sin(eyea) * sin(eyea2)
	);

	vec3 front = normalize(lookAt - ro);
	vec3 left = normalize(cross(normalize(vec3(0.0, 1, -0.1)), front));
	vec3 up = normalize(cross(front, left));
	vec3 rd = normalize(front * 1.5 + left * pp.x + up * pp.y);

	vec3 bh = vec3(0.0, 0.0, 0.0);
	float bhr = 0.3;
	float bhmass = 5.0;
	bhmass *= 0.001; // premul G

	vec3 p = ro;
	vec3 pv = rd;

	p += pv * hash13(rd + vec3(iTime)) * 0.02;

	float dt = 0.02;
	vec3 col = vec3(0.0);
	float alpha = 1.0;
	float noncaptured = 1.0;

	vec3 c1 = vec3(0.5, 0.35, 0.1);
	vec3 c2 = vec3(1.0, 0.8, 0.6);

	for(float t = 0.0; t < 1.0; t += 0.005) {
		p += pv * dt * noncaptured;

		vec3 bhv = bh - p;
		float r = dot(bhv, bhv);
		pv += normalize(bhv) * ((bhmass) / r);

		noncaptured = smoothstep(0.0, 0.01, sdSphere(p - bh, bhr));

		float dr = length(bhv.xz);
		float da = atan(bhv.x, bhv.z);
		vec2 ra = vec2(dr, da * (0.01 + (dr - bhr) * 0.002) + 2.0 * pi + iTime * 0.002);
		ra *= vec2(10.0, 20.0);

		// Thin-disk style temperature falloff (T ~ r^(-3/4)) mapped to blackbody color.
		// r0 matches the torus major radius used below (approx inner edge of the visible disk).
		float r0 = 0.8;
		float Tin = 24000.0;   // inner edge temperature (K)
		float Tout = 3500.0;   // outer edge temperature (K)
		float tempK = Tin * pow(max(dr, r0) / r0, -0.75);
		tempK = clamp(tempK, Tout, Tin);
		tempK *= 0.4; // a touch of extra blue

		vec3 bb = kelvinToRGB(tempK);
		bb /= max(max(bb.r, bb.g), bb.b); // preserve hue under high intensity
		float inner = smoothstep(0.4, 0.0, (length(bhv) - bhr) / bhr);
		bb.b *= 1.0 + 0.4 * inner;
		bb.r *= 1.0 - 0.15 * inner;

		vec3 dcol = bb * max(0.0, texture2D(iChannel1, ra * vec2(0.1, 0.5)).r + 0.15) * (4.0 / ((0.001 + (length(bhv) - bhr) * 50.0)));

		col += max(vec3(0.0),dcol * step(0.0,-sdTorus( (p * vec3(1.0,50.0,1.0)) - bh, vec2(0.8,0.99))) * noncaptured);

		//col += dcol * (1.0/dr) * noncaptured * 0.01;

		// glow
		col += vec3(0.9, 0.95, 1.0) * (3.0/vec3(dot(bhv,bhv))) * 0.00035 * noncaptured * clamp(r, 0.8, 0.9);
		col -= 0.0004;

		if(r < 0.5) {
			alpha = 1.0;
		} else {
			alpha = col.r;
		}
	}

	col = max(col, 0.0);
	float exposure = 0.35;
	col = vec3(1.0) - exp(-col * exposure);
	col = pow(col, vec3(1.0/2.2));
	gl_FragColor = vec4(col, alpha);
}