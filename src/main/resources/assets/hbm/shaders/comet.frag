#version 120

uniform float iTime;

// If you have a resolution uniform available in your pack, you can use it for better aspect correction.
// If you don't, this still works fine — just slightly stretched on non-square targets.
// uniform vec2 iResolution;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise1(vec2 p) {
    // cheap value noise (GLSL 120 friendly)
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash(i + vec2(0.0, 0.0));
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

vec3 cometRamp(float t) {
    // t: 0 at head, 1 far down the tail
    vec3 head = vec3(0.80, 0.95, 1.25); // cool white/blue
    vec3 tail = vec3(1.05, 0.70, 0.35); // warm-ish
    return mix(head, tail, smoothstep(0.0, 1.0, t));
}

void main() {
    vec2 uv = gl_TexCoord[0].xy;     // 0..1
    vec2 p  = uv * 2.0 - 1.0;        // -1..1
	
	// ---- 90s timeline ----
	float t90 = mod(iTime, 90.0);  // 0..90 loop

	// Helper ramps (0..1)
	float headIn   = smoothstep(0.0, 2.0, t90);           // 0->1 over first 2s
	float tailIn   = smoothstep(3.0, 10.0, t90);          // 0->1 over next 3s
	float headOut  = 1.0 - smoothstep(80.0, 82.0, t90);   // 1->0 over 80..82s
	float tailOut  = 1.0 - smoothstep(82.0, 85.0, t90);   // 1->0 over 82..85

	float headEnv  = headIn * headOut;  // final head visibility 0..1
	float tailEnv  = tailIn * tailOut;  // final tail visibility 0..1

    // Aspect correction (best effort without iResolution):
    // Most Minecraft shader targets are close enough; if you *do* have iResolution, use it instead.
    // p.x *= iResolution.x / iResolution.y;

    // Fixed comet head position in the center
    vec2 headPos = vec2(0.0, 0.0);

    // Fixed direction: tail goes down-left from the head (so motion would be up-right)
    // Change this to rotate the comet: e.g. normalize(vec2(1.0, 0.2)) etc.
    vec2 dir = normalize(vec2(1.0, 0.12)); // "forward" direction (imaginary motion)
    vec2 nrm = vec2(-dir.y, dir.x);

    vec2 r = p - headPos;

    float along = dot(r, dir);     // + in front of head, - behind (tail region)
    float cross = dot(r, nrm);

    // ---- Head glow (tight core + soft halo) ----
    float headR = length(r);
	float headCore = exp(-headR * headR * 240.0);

	// subtle pulsation (kept small so it doesn't look like breathing)
	float haloPulse = 0.85 + 0.15 * sin(iTime * 2.0);

	// smaller halo = bigger falloff multiplier (75 -> 95), and slightly lower strength
	float headHalo = exp(-headR * headR * 95.0) * 0.55 * haloPulse;

	float head = headCore + headHalo;

    // ---- Tail shape (taper, slight turbulence, subtle breakup) ----
    float tailLen = 1.2;                         // longer = bigger number
    float t = clamp((-along) / tailLen, 0.0, 1.0); // 0 at head, 1 far tail
    float tailMask = step(along, 0.0);           // only behind head

    // Tail widens slowly then diffuses
    float width = mix(0.012, 0.085, t);

    // Minimal “realism” turbulence: very small lateral variation increasing with t
    // (less wobbly than a sine bend; more like faint flow noise)
    float n = noise1(vec2(t * 10.0, iTime * 0.35));
    float turb = (n - 0.5) * 0.025 * (0.2 + 0.8 * t);  // small amplitude
    float cross2 = cross + turb;

    // Main tail density
    float tailShape = exp(-(cross2 * cross2) / (width * width));

    // Fade down the tail (density falloff)
    float tailFade = exp(-t * 3.2);

    // “Breakup” / clumping (subtle streaky particulate look)
    // Stronger near the head, softer far out
    float streakNoise = noise1(vec2(t * 22.0, iTime * 0.6 + 3.0));
    float breakup = mix(0.85, 1.15, streakNoise) * (1.0 - 0.25 * t);

    float tail = tailShape * tailFade * breakup * tailMask;

    // Thin bright core streak inside the tail
    float coreWidth = mix(0.006, 0.020, t);
    float core = exp(-(cross * cross) / (coreWidth * coreWidth)) * exp(-t * 2.4) * tailMask;

    // Slight head flicker only (tiny, realistic)
    float flick = 0.96 + 0.04 * sin(iTime * 14.0);

    // ---- Color composition ----
	vec3 tailCol = cometRamp(t) * tail * 1.25 * tailEnv;
	vec3 coreCol = vec3(0.85, 0.95, 1.15) * core * 0.9 * tailEnv;  // core belongs to tail
	vec3 headCoreCol = vec3(1.35, 1.32, 1.20) * headCore * flick * headEnv;  // warm-white core
	vec3 headHaloCol = vec3(0.25, 0.55, 1.35) * headHalo * headEnv;          // blue glow halo
	vec3 headCol = headCoreCol + headHaloCol;

    vec3 col = tailCol + coreCol + headCol;

    // Gentle tonemap so head doesn’t clip too hard
    col = col / (1.0 + col);

    // ---- Transparent background ----
    // Alpha should represent how “present” the comet is.
	float alpha = clamp(
		(tail * 0.9 + core * 0.6) * tailEnv +
		(headCore * 1.1 + headHalo * 0.9) * headEnv,
		0.0, 1.0
	);

    gl_FragColor = vec4(col, alpha);
}
