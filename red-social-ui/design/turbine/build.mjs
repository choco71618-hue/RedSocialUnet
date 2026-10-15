import { writeFileSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import * as k from "../kit/iso-kit.mjs";
import * as G from "../kit/lathe.mjs";
import { millFigure } from "../mill/mill.mjs";
import { verticalMillFigure } from "../mill/vertical-mill.mjs";
import { bridgeportVerticalFigure } from "../mill/bridgeport-vertical.mjs";

const HERE = dirname(fileURLToPath(import.meta.url));
const OUT = join(HERE, "..", "..", "src", "main", "resources", "static", "generados");

const WIDTH = 940;
const HEIGHT = 470;

const AXIS_Z = 30;
const CASE_IN = 23;
const CASE_OUT = 26;

const AX = G.frameOf([0, 0, AXIS_Z], [1, 0, 0], [0, 1, 0], [0, 0, 1]);

const BASE = { x: -178, y: -46, w: 404, d: 92, r: 12 };
const BASE_Z0 = -18;
const BASE_TOP = 0;

const COMP_TIP = 22;
const TURB_TIP = 24.5;

const ROTOR_STAGES = [
  { x: 96, root: 7 },
  { x: 79, root: 9.4 },
  { x: 62, root: 12 },
  { x: 45, root: 14.6 },
  { x: 28, root: 17 },
];
const STATOR_STAGES = [
  { x: 87.5, rIn: 9.4 },
  { x: 70.5, rIn: 12 },
  { x: 53.5, rIn: 14.6 },
  { x: 36.5, rIn: 17 },
];
const TURB_STAGES = [
  { x: -38, root: 16.5, tip: 24.5 },
  { x: -55, root: 14.5, tip: 25 },
  { x: -72, root: 12.5, tip: 25.5 },
  { x: -89, root: 10.5, tip: 26 },
];
const TURB_STATOR = [
  { x: -46.5, rIn: 15.5 },
  { x: -63.5, rIn: 13.5 },
  { x: -80.5, rIn: 11.5 },
];

const CAN_R = 15.5;
const CANS = [
  { phi: (35 * Math.PI) / 180, x: -9, r: 6.6 },
  { phi: (140 * Math.PI) / 180, x: -7, r: 6.2 },
  { phi: (215 * Math.PI) / 180, x: -11, r: 6.4 },
  { phi: (295 * Math.PI) / 180, x: -8, r: 6.2 },
];

const at = (p) => k.iso(p, P);
const S = (paths, style) => k.solidSvg(paths, style);
const L = (d, style) => k.lineSvg(d, style);
const D = (points, style) => k.dotsSvg(points, style);
const path3 = (points) => k.pathOf(points.map((p) => at(p)));
const depth = (p) => k.depthOf(p, P);

// ------------------------------------------------------------------ projection
const P = k.fitProjection(
  [
    ...k.boxCorners(BASE, BASE_Z0, BASE_TOP),
    [190, 0, AXIS_Z + CASE_OUT],
    [-178, 0, AXIS_Z + 34],
    [190, 0, AXIS_Z - CASE_OUT],
    [-178, 0, AXIS_Z - 34],
    [105, 0, AXIS_Z + 52],
  ],
  WIDTH,
  HEIGHT,
  { pad: 30, azimuth: 45, elevation: 30 },
);

// ------------------------------------------------------------------ raw paths
const shade = (n) => `var(--anatomy-shade-${n})`;
const rawFill = (d, color, extra = "") => `<path d="${d}" style="fill:${color};stroke:none" ${extra}/>`;

// a crescent: the far half of a ring around the x-axis, seen through the section
const FAR0 = Math.PI / 2;
const FAR1 = (3 * Math.PI) / 2;
function farRing(x0, x1, rIn, rOut, steps = 66) {
  const o1 = [], i1 = [], o0 = [], i0 = [];
  for (let s = 0; s <= steps; s++) {
    const a = FAR0 + ((FAR1 - FAR0) * s) / steps;
    o1.push(at(G.pointOf(AX, x1, rOut, a)));
    i1.push(at(G.pointOf(AX, x1, rIn, a)));
    o0.push(at(G.pointOf(AX, x0, rOut, a)));
    i0.push(at(G.pointOf(AX, x0, rIn, a)));
  }
  return {
    inner: k.pathOf([...i1, ...i0.slice().reverse()], true),
    face: k.pathOf([...o1, ...i1.slice().reverse()], true),
    outer: k.pathOf(o1),
    innerEdge: k.pathOf(i1),
    endTop: k.pathOf([o1[0], i1[0]]),
    endBottom: k.pathOf([o1[o1.length - 1], i1[i1.length - 1]]),
  };
}

function farRingSvg(x0, x1, rIn, rOut, { faceTone = 1, innerTone = 0, line = true } = {}) {
  const r = farRing(x0, x1, rIn, rOut);
  const out = [rawFill(r.inner, shade(innerTone))];
  out.push(rawFill(r.face, shade(faceTone)));
  if (line) {
    out.push(L(r.outer, { tone: "lo" }));
    out.push(L(r.innerEdge, { tone: "faint" }));
    out.push(L(r.endTop, { tone: "lo" }));
    out.push(L(r.endBottom, { tone: "lo" }));
  }
  return out.join("");
}

// filled stator vanes on the far half of a ring (no dangling line ends)
function vaneRow(F, s, rIn, rOut, count) {
  const out = [];
  for (let i = 0; i < count; i++) {
    const t = FAR0 + ((i + 0.5) / count) * (FAR1 - FAR0);
    const w = ((FAR1 - FAR0) / count) * 0.34;
    const quad = [
      G.pointOf(F, s, rIn, t - w),
      G.pointOf(F, s, rOut, t - w * 0.6),
      G.pointOf(F, s, rOut, t + w * 0.6),
      G.pointOf(F, s, rIn, t + w),
    ].map((p) => at(p));
    out.push(`<path d="${k.pathOf(quad, true)}" style="fill:${shade(2)};stroke:var(--anatomy-shade-0);stroke-width:0.5"/>`);
  }
  return out.join("");
}

// ------------------------------------------------------------------ bedplate
function plate() {
  const out = [`<path class="iso-halo" d="${k.haloOf(BASE, BASE_Z0 - 6, 6, P)}"/>`];
  const feet = k.corners(BASE, 26).sort((a, b) => depth([a[0], a[1], 0]) - depth([b[0], b[1], 0]));
  for (const [x, y] of feet) {
    out.push(S(k.cylinder(x, y, 8, BASE_Z0 - 8, 2, P, 30), { tone: "lo" }));
    out.push(S(k.cylinder(x, y, 6.2, BASE_Z0 - 6, 6, P, 30), { tone: "lo" }));
  }
  out.push(S(k.slabOf(BASE, BASE_Z0, BASE_TOP - BASE_Z0, P, 9, 1.6), { tone: "lo" }));
  out.push(L(k.planOutline(k.insetPlan(BASE, 5), 0, P), { tone: "faint" }));
  const screws = k.corners(BASE, 11);
  out.push(L(screws.map(([x, y]) => k.ring(x, y, 2.4, 0, P, 16)).join(""), { tone: "lo" }));
  out.push(D(screws.map(([x, y]) => at([x, y, 0])), { size: 0.5 }));

  // ruler strip along the front edge
  const strip = { x: -168, y: BASE.y + BASE.d - 16, w: 356, d: 8, r: 1 };
  out.push(S(k.slabOf(strip, 0, 0.6, P, 3), { tone: "lo", crease: "none" }));
  const ticks = k.topTicks(strip.x + 6, strip.x + strip.w - 6, 6, 5, strip.y + 1, 0.6, [2.2, 4], P);
  out.push(L(ticks.minor, { tone: "lo" }), L(ticks.major, { tone: "mid" }));
  out.push(D([at([strip.x + 3, strip.y + 4, 0.6]), at([strip.x + strip.w - 3, strip.y + 4, 0.6])], { size: 0.45 }));

  // nameplate
  const np = { x: -150, y: BASE.y + 8, w: 74, d: 22, r: 2 };
  out.push(S(k.slabOf(np, 0, 1, P, 4, 0.5), { tone: "mid", crease: "none" }));
  out.push(L(k.planOutline(k.insetPlan(np, 2), 1, P), { tone: "faint" }));
  out.push(
    k.faceTextSvg(k.topMatrix([np.x + 5, np.y + 11], 1, P, "x"), "TURBINA DE GAS", { size: 3.4, tone: "hi" }),
  );
  out.push(
    k.faceTextSvg(k.topMatrix([np.x + 5, np.y + 16.5], 1, P, "x"), "FLUJO AXIAL / 4 ETAPAS", { size: 2.6, tone: "mid" }),
  );
  out.push(D([at([np.x + np.w - 4, np.y + 4, 1]), at([np.x + np.w - 4, np.y + np.d - 4, 1])], { size: 0.45 }));
  return out.join("");
}

// ------------------------------------------------------------------ casing
function casing() {
  const out = [];
  // subtle shadow of the shell
  const inletLip = farRing(112, 116, CASE_IN, CASE_OUT + 1.4);
  out.push(rawFill(inletLip.inner, shade(0)));

  // far-half stiffener rings along the casing
  const rings = [
    [106, 110, CASE_IN, CASE_OUT + 1.8],
    [84, 87.5, CASE_IN, CASE_OUT + 1.2],
    [64, 67.5, CASE_IN, CASE_OUT + 1.2],
    [44, 47.5, CASE_IN, CASE_OUT + 1.2],
    [24, 27.5, CASE_IN, CASE_OUT + 1.2],
    [6, 10, CASE_IN, CASE_OUT + 2.2],
    [-30, -26, CASE_IN, CASE_OUT + 1.6],
    [-52, -49, CASE_IN, CASE_OUT + 1.4],
    [-70, -67, CASE_IN, CASE_OUT + 1.4],
    [-90, -87, CASE_IN, CASE_OUT + 1.4],
  ];
  for (const [x0, x1, rIn, rOut] of rings) out.push(farRingSvg(x0, x1, rIn, rOut, { faceTone: 1, innerTone: 0 }));

  // cut-edge rails: top and bottom long bars where the shell was sliced open
  for (const z of [AXIS_Z + CASE_IN + 1, AXIS_Z - CASE_IN - 1]) {
    out.push(S(G.disc(-30, 108, 1.6, G.frameOf([0, 0, z], [1, 0, 0], [0, 1, 0], [0, 0, 1]), P), { tone: "mid" }));
    out.push(S(G.disc(-96, -30, 1.9, G.frameOf([0, 0, z], [1, 0, 0], [0, 1, 0], [0, 0, 1]), P), { tone: "mid" }));
    out.push(L(G.arcOf(G.frameOf([0, 0, z], [1, 0, 0], [0, 1, 0], [0, 0, 1]), -30, 1.6, P, { least: -1 }), { tone: "lo" }));
  }

  // top inlet bell on its own tilted axis
  const bellAxis = [0.42, -0.08, 0.9];
  const nb = G.unit3(bellAxis);
  const bellF = G.frameAlong([74, 4, AXIS_Z + CASE_OUT - 4], nb);
  out.push(
    S(
      G.lathe(
        [
          [0, 13],
          [3, 13, 1],
          [9, 16, 1],
          [15, 18.5, 1],
          [16, 18.5],
        ],
        bellF,
        P,
        { bevel: 0.8, smooth: true },
      ),
      { tone: "mid", crease: "lo" },
    ),
  );
  out.push(L(G.circleOf(bellF, 3.2, 10.4, P), { tone: "faint" }));
  out.push(L(G.circleOf(bellF, 3.9, 10.4, P), { tone: "lo" }));
  return out.join("");
}

// ------------------------------------------------------------------ rotor
function wheel(x, half, root, tip, count) {
  return [
    S(G.disc(x - half, x + half, tip, AX, P, { steps: 46 }), { tone: "hi", crease: "lo" }),
    S(G.disc(x - half, x + half, root + 0.9, AX, P, { steps: 34 }), { tone: "hi", crease: "none" }),
    L(G.circleOf(AX, x + half, tip, P, 150), { tone: "lo" }),
    L(G.circleOf(AX, x + half, root + 0.9, P, 100), { tone: "faint" }),
    L(G.spokesOf(AX, x + half, root + 0.9, tip, count, P, { phase: x }), { tone: "lo" }),
  ].join("");
}

function rotor() {
  const out = [];
  // shaft
  out.push(S(G.solidOf([[-104, 3.4], [118, 3.4]], AX, P, { steps: 28, rims: "none" }), { tone: "mid", crease: "lo" }));
  // conical drum through the compressor
  out.push(
    S(
      G.solidOf(
        [
          [116, 6],
          [112, 6],
          [98, 6.6],
          [30, 17.4],
          [18, 17.4],
        ],
        AX,
        P,
        { steps: 40 },
      ),
      { tone: "hi", crease: "lo" },
    ),
  );
  // turbine drum taper
  out.push(
    S(
      G.solidOf(
        [
          [-24, 17.6],
          [-36, 17],
          [-95, 9],
          [-100, 7.5],
        ],
        AX,
        P,
        { steps: 40 },
      ),
      { tone: "hi", crease: "lo" },
    ),
  );

  // compressor rotor wheels
  for (const st of ROTOR_STAGES) out.push(wheel(st.x, 3.4, st.root, COMP_TIP, 34));
  // compressor stator vanes (far half only, hung from the casing)
  for (const st of STATOR_STAGES) {
    out.push(farRingSvg(st.x - 2.4, st.x + 2.4, st.rIn, CASE_IN, { faceTone: 2, innerTone: 1 }));
    out.push(vaneRow(AX, st.x + 2.4, st.rIn, CASE_IN, 18));
  }
  // turbine rotor wheels
  for (const st of TURB_STAGES) out.push(wheel(st.x, 3.8, st.root, st.tip, 48));
  for (const st of TURB_STATOR) {
    out.push(farRingSvg(st.x - 2.6, st.x + 2.6, st.rIn + 4, CASE_IN, { faceTone: 2, innerTone: 1 }));
    out.push(vaneRow(AX, st.x + 2.6, st.rIn + 4, CASE_IN, 18));
  }
  // central tie rod + nut at the front of the drum
  out.push(S(G.disc(116, 121, 5, AX, P), { tone: "mid" }));
  out.push(D(G.dotsOf(AX, 121, 3.4, 8, P, { all: true }), { size: 0.45, tone: "lo" }));
  return out.join("");
}

// ------------------------------------------------------------------ combustor cans
function can(c) {
  const cy = CAN_R * Math.cos(c.phi);
  const cz = AXIS_Z + CAN_R * Math.sin(c.phi);
  const F = G.frameAlong([c.x, cy, cz], [1, 0, 0]);
  const r = c.r;
  const out = [];
  out.push(
    S(
      G.solidOf(
        [
          [-17, r * 0.72],
          [-14, r, 1],
          [-2, r, 1],
          [10, r, 1],
          [15, r * 0.86, 1],
          [17, r * 0.52, 1],
          [17.6, r * 0.4],
        ],
        F,
        P,
        { steps: 60, smooth: true },
      ),
      { tone: "hi", crease: "lo" },
    ),
  );
  // cooling lands
  out.push(L(G.circleOf(F, -6, r, P), { tone: "faint" }));
  out.push(L(G.circleOf(F, 5, r, P), { tone: "faint" }));
  out.push(L(G.circleOf(F, 15, r * 0.86, P), { tone: "faint" }));
  // fuel nozzle
  out.push(S(G.solidOf([[17.6, r * 0.42], [20, r * 0.42], [21.4, 2.1], [25, 2.1]], F, P, { steps: 40 }), { tone: "mid" }));
  out.push(S(G.disc(25, 27.4, 3.1, F, P), { tone: "mid" }));
  out.push(D(G.dotsOf(F, 27.4, 2.1, 6, P, { all: true }), { size: 0.4, tone: "lo" }));
  // transition duct toward the turbine
  out.push(S(G.solidOf([[-17, r * 0.72], [-21, 5.4], [-26, 5], [-28, 2]], F, P, { steps: 40 }), { tone: "mid" }));
  return out.join("");
}

// ------------------------------------------------------------------ bearings, generator, exhaust
function drivetrain() {
  const out = [];
  // compressor front bearing housing
  out.push(S(G.solidOf([[124, 8], [128, 10.5, 1], [136, 10.5, 1], [140, 8, 1]], AX, P, { steps: 56, smooth: true }), { tone: "mid", crease: "lo" }));
  out.push(D(G.dotsOf(AX, 136, 8.4, 10, P, { fade: [0.05, 0.4] }), { size: 0.5, tone: "lo" }));
  const ped1 = { x: 125, y: -9, w: 15, d: 18, r: 2 };
  out.push(S(k.slabOf(ped1, BASE_TOP, AXIS_Z - 8, P, 4, 0.6), { tone: "mid" }));
  out.push(L(k.planOutline(k.insetPlan(ped1, 2), AXIS_Z - 8, P), { tone: "faint" }));
  out.push(D(k.corners(ped1, 3.5).map(([x, y]) => at([x, y, AXIS_Z - 8])), { size: 0.45 }));

  // generator
  const gen = [
    [176, 19],
    [180, 21, 1],
    [214, 21, 1],
    [218, 19, 1],
  ];
  out.push(S(G.solidOf(gen, AX, P, { steps: 48, smooth: true }), { tone: "hi", crease: "lo" }));
  const genRibs = G.ribsOf(AX, 182, 212, 21, 34, P, { phase: 0.1, fade: [0.02, 0.5], seams: true });
  out.push(k.fadedSvg(genRibs, { tone: "faint" }));
  out.push(L(G.arcOf(AX, 182, 21, P, { least: -1 }), { tone: "lo" }));
  // terminal box on top of the generator
  const box = { x: 182, y: -12, w: 26, d: 24, r: 2 };
  out.push(S(k.slabOf(box, AXIS_Z + 17, 17, P, 4, 0.8), { tone: "mid" }));
  out.push(L(k.planOutline(k.insetPlan(box, 2.2), AXIS_Z + 34, P), { tone: "faint" }));
  out.push(L(k.sideSeam(box, AXIS_Z + 26, P, 4), { tone: "faint" }));
  out.push(D(k.corners(box, 4).map(([x, y]) => at([x, y, AXIS_Z + 34])), { size: 0.45 }));
  // terminal strip
  out.push(S(k.slabOf({ x: 184, y: -4, w: 20, d: 8, r: 1 }, AXIS_Z + 34, 1.4, P, 3), { tone: "hi", crease: "none" }));
  out.push(D([188, 192, 196, 200].map((x) => at([x, 0, AXIS_Z + 35.4])), { size: 0.5, tone: "mid" }));
  // generator feet
  for (const y of [-16, 16]) {
    out.push(S(k.slabOf({ x: 184, y: y - 4, w: 30, d: 8, r: 1.5 }, BASE_TOP, AXIS_Z - 19, P, 4), { tone: "mid" }));
  }

  // coupling guard between bearing and generator
  out.push(S(G.ringBand(142, 176, 8.5, 11.5, AX, P, { steps: 48 }), { tone: "mid" }));
  out.push(L(G.arcOf(AX, 159, 11.5, P, { least: -1 }), { tone: "lo" }));
  out.push(L(G.arcOf(AX, 159, 8.5, P, { least: -1 }), { tone: "faint" }));

  // rear bearing + exhaust frame
  out.push(S(G.solidOf([[-104, 8], [-108, 10.5, 1], [-116, 10.5, 1], [-120, 8, 1]], AX, P, { steps: 56, smooth: true }), { tone: "mid", crease: "lo" }));
  out.push(D(G.dotsOf(AX, -116, 8.4, 10, P, { fade: [0.05, 0.4] }), { size: 0.5, tone: "lo" }));
  const ped2 = { x: -121, y: -9, w: 17, d: 18, r: 2 };
  out.push(S(k.slabOf(ped2, BASE_TOP, AXIS_Z - 8, P, 4, 0.6), { tone: "mid" }));
  out.push(L(k.planOutline(k.insetPlan(ped2, 2), AXIS_Z - 8, P), { tone: "faint" }));
  out.push(D(k.corners(ped2, 3.5).map(([x, y]) => at([x, y, AXIS_Z - 8])), { size: 0.45 }));

  // exhaust diffuser
  const exh = [
    [-118, 0],
    [-118, 24],
    [-126, 26, 1],
    [-152, 31, 1],
    [-162, 34, 1],
    [-170, 35],
    [-170, 31],
    [-160, 30, 1],
    [-132, 25, 1],
    [-118, 20],
  ];
  out.push(S(G.lathe(exh, AX, P, { steps: 52, smooth: true, fill: "surface" }), { tone: "mid", crease: "lo" }));
  const exh2 = G.ribsOf(AX, -160, -124, 30, 40, P, { phase: 0.05, r1: 25.5, fade: [0.02, 0.5], seams: true });
  out.push(k.fadedSvg(exh2, { tone: "faint" }));
  out.push(L(G.circleOf(AX, -170, 33, P), { tone: "lo" }));
  return out.join("");
}

// ------------------------------------------------------------------ flow streamlines
function flow() {
  const lines = [];
  const specs = [
    { phi: -20, r: 16 },
    { phi: -60, r: 18.5 },
    { phi: -100, r: 19.5 },
    { phi: -140, r: 18.5 },
    { phi: 200, r: 9 },
  ];
  for (const s of specs) {
    const c = Math.cos(s.phi * (Math.PI / 180));
    const sn = Math.sin(s.phi * (Math.PI / 180));
    const R0 = s.r;
    const pts = [
      [82, 3, AXIS_Z + CASE_OUT + 9],
      [88, R0 * c * 0.85, AXIS_Z + 8],
      [64, R0 * c, AXIS_Z + R0 * sn],
      [34, R0 * c, AXIS_Z + R0 * sn],
      [10, 15.5 * c, AXIS_Z + 15.5 * sn],
      [-22, 15.5 * c, AXIS_Z + 15.5 * sn],
      [-48, 20 * c, AXIS_Z + 20 * sn],
      [-92, 20 * c, AXIS_Z + 20 * sn],
      [-150, 29 * c, AXIS_Z + 29 * sn],
      [-178, 33 * c, AXIS_Z + 33 * sn],
    ];
    const sm = G.fillet(pts, 14, 6).map((p) => at(p));
    const cold = k.pathOf(sm.slice(0, 5));
    const hot = k.pathOf(sm.slice(4));
    lines.push(L(cold, { tone: "blue", flow: true, free: true }));
    lines.push(L(hot, { tone: "red", flow: true, free: true }));
  }
  return lines.join("");
}

// ------------------------------------------------------------------ rotating fan (generator cooling fan)
function fan() {
  const X = 219.4;
  const c0 = k.iso([X, 0, AXIS_Z], P);
  const cY = k.iso([X, 1, AXIS_Z], P);
  const cZ = k.iso([X, 0, AXIS_Z + 1], P);
  const a = cY[0] - c0[0];
  const b = cY[1] - c0[1];
  const c = cZ[0] - c0[0];
  const d = cZ[1] - c0[1];
  const matrix = `matrix(${a.toFixed(3)} ${b.toFixed(3)} ${c.toFixed(3)} ${d.toFixed(3)} ${c0[0].toFixed(2)} ${c0[1].toFixed(2)})`;
  const hub = 4;
  const R = 18;
  const blades = [];
  const N = 13;
  for (let i = 0; i < N; i++) {
    const t = (i / N) * Math.PI * 2;
    const w0 = 0.16;
    const w1 = 0.5;
    const pts = [
      [hub * Math.cos(t), hub * Math.sin(t)],
      [R * Math.cos(t + w1 * 0.5 - w0), R * Math.sin(t + w1 * 0.5 - w0)],
      [R * Math.cos(t + w1 * 0.5 + w0), R * Math.sin(t + w1 * 0.5 + w0)],
      [(hub + 1.6) * Math.cos(t + 0.22), (hub + 1.6) * Math.sin(t + 0.22)],
    ];
    blades.push(k.pathOf(pts, true));
  }
  const disc = k.pathOf(
    Array.from({ length: 40 }, (_, i) => [R * Math.cos((i / 40) * Math.PI * 2), R * Math.sin((i / 40) * Math.PI * 2)]),
    true,
  );
  return `
    <g transform="${matrix}">
      <circle r="${R + 0.6}" class="fan-plate"/>
      <g class="fan-rotor">
        <path d="${disc}" class="fan-disc"/>
        ${blades.map((d) => `<path d="${d}" class="fan-blade"/>`).join("")}
        <circle r="${hub}" class="fan-hub"/>
        <path d="${k.pathOf([[-hub - 1, 0], [0, -1.6], [hub + 1, 0], [0, 1.6]], true)}" class="fan-mark"/>
      </g>
    </g>`;
}

// ------------------------------------------------------------------ assemble
const tag = (name, svg) => `<!-- ${name} -->${svg}`;
const svg = k.figureSvg({
  width: WIDTH,
  height: HEIGHT,
  className: "iso turbo",
  label:
    "Vista isométrica seccionada de una turbina de gas industrial de flujo axial: el cárter delantero se ha cortado para dejar ver el compresor de cinco etapas, la cámara de combustión de cuatro cápsulas, la turbina de cuatro etapas, el difusor de escape y el generador acoplado al eje.",
  body: [
    tag("plate", plate()),
    tag("casing", casing()),
    ...CANS.map((c, i) => (Math.cos(c.phi) < 0 ? tag("can-far-" + i, can(c)) : "")),
    tag("rotor", rotor()),
    ...CANS.map((c, i) => (Math.cos(c.phi) >= 0 ? tag("can-near-" + i, can(c)) : "")),
    tag("drivetrain", drivetrain()),
    tag("fan", fan()),
    tag("flow", flow()),
  ].join(""),
});

// ------------------------------------------------------------------ card + page
function readoutPanel() {
  return `
    <div class="turbo-panel" aria-hidden="true">
      <div class="turbo-panel__head"><span class="turbo-panel__dot"></span>GENERADOR · LÍNEA 1</div>
      <dl class="turbo-panel__rows">
        <div><dt>SALIDA</dt><dd>500<span>MW</span></dd></div>
        <div><dt>EFICIENCIA</dt><dd>92<span>%</span></dd></div>
        <div><dt>RÉGIMEN</dt><dd>3 000<span>rpm</span></dd></div>
      </dl>
      <div class="turbo-panel__bar"><i style="width:92%"></i></div>
    </div>`;
}

function cardTurbine() {
  return `
    <article class="gen-card gen-card--turbine">
      <header class="gen-card__head">
        <span class="gen-card__index">01</span>
        <span class="gen-card__kind">Isométrico · SVG</span>
        <span class="gen-card__time">hace 2 min</span>
      </header>
      <div class="gen-card__stage">
        <div class="turbo-stage">${svg}</div>
        ${readoutPanel()}
      </div>
      <div class="gen-card__body">
        <h2 class="gen-card__title">Turbina de gas de flujo axial</h2>
        <p class="gen-card__text">Corte de un turbogenerador de una sola línea de eje. El aire entra por la campana, atraviesa el compresor de cinco etapas, se quema en la cámara de cápsulas y se expande por la turbina antes de salir por el difusor.</p>
        <ul class="gen-card__tags">
          <li>Compresor axial</li>
          <li>Cámara anular</li>
          <li>Turbina axial</li>
          <li>500 MW</li>
        </ul>
      </div>
      <footer class="gen-card__foot">
        <span class="gen-card__meta">LABORATORIO DE TURBOMÁQUINAS — CICLO DE ENERGÍA</span>
        <span class="gen-card__mark">Generado con <b>Anatomy</b></span>
      </footer>
    </article>`;
}

const millSvg = millFigure();
const verticalMillSvg = verticalMillFigure();
const bridgeportVerticalSvg = bridgeportVerticalFigure();

function cardMill() {
  return `
    <article class="gen-card gen-card--mill">
      <header class="gen-card__head">
        <span class="gen-card__index">02</span>
        <span class="gen-card__kind">Isométrico · SVG</span>
        <span class="gen-card__time">ahora</span>
      </header>
      <div class="gen-card__stage">
        <div class="mill-stage">${millSvg}</div>
      </div>
      <div class="gen-card__body">
        <h2 class="gen-card__title">Fresadora universal de taller</h2>
        <p class="gen-card__text">Una fresadora universal de árbol horizontal: columna, carnero, mesa y mordaza sostienen la pieza mientras una fresa de plaquitas gira sobre la superficie de trabajo.</p>
        <ul class="gen-card__tags">
          <li>Árbol horizontal</li>
          <li>Mordaza mecánica</li>
          <li>Mesa con ranuras T</li>
        </ul>
      </div>
      <footer class="gen-card__foot">
        <span class="gen-card__meta">SERVICIO — MECANIZADO Y FABRICACIÓN</span>
        <span class="gen-card__mark">Generado con <b>Anatomy</b></span>
      </footer>
    </article>`;
}

function cardVerticalMill() {
  return `
    <article class="gen-card gen-card--mill-vertical">
      <header class="gen-card__head">
        <span class="gen-card__index">03</span>
        <span class="gen-card__kind">Isométrico · SVG</span>
        <span class="gen-card__time">ahora</span>
      </header>
      <div class="gen-card__stage">
        <div class="mill-stage">${verticalMillSvg}</div>
      </div>
      <div class="gen-card__body">
        <h2 class="gen-card__title">Fresadora vertical de taller</h2>
        <p class="gen-card__text">En esta variante, el cabezal queda sobre la mesa y el husillo desciende verticalmente hasta el canal de la pieza, sujeta en una mordaza mecánica.</p>
        <ul class="gen-card__tags">
          <li>Husillo vertical</li>
          <li>Fresa de extremo</li>
          <li>Mordaza mecánica</li>
        </ul>
      </div>
      <footer class="gen-card__foot">
        <span class="gen-card__meta">SERVICIO — MECANIZADO Y FABRICACIÓN</span>
        <span class="gen-card__mark">Generado con <b>Anatomy</b></span>
      </footer>
    </article>`;
}

function cardBridgeportMill() {
  return `
    <article class="gen-card gen-card--bridgeport">
      <header class="gen-card__head">
        <span class="gen-card__index">04</span>
        <span class="gen-card__kind">Anatomía técnica · SVG/CSS</span>
        <span class="gen-card__time">ahora</span>
      </header>
      <div class="gen-card__stage">
        <div class="mill-stage">${bridgeportVerticalSvg}</div>
      </div>
      <div class="gen-card__body">
        <h2 class="gen-card__title">Fresadora vertical Bridgeport</h2>
        <p class="gen-card__text">Los cuatro subsistemas quedan separados por función: cabezal de transmisión, mesa y carro, rodillera con elevación Z, y base-columna. Pasa o enfoca los puntos azules para identificar mandos y componentes.</p>
        <ul class="gen-card__tags">
          <li>Husillo vertical descendente</li>
          <li>Avances X · Y · Z</li>
          <li>Quill y pinza</li>
          <li>Base con depósito</li>
        </ul>
      </div>
      <footer class="gen-card__foot">
        <span class="gen-card__meta">SERVICIO — MECANIZADO Y FABRICACIÓN DE PIEZAS DE PRECISIÓN</span>
        <span class="gen-card__mark">Generado con <b>Anatomy</b></span>
      </footer>
    </article>`;
}

function cardSkeleton(n, kind, title) {
  return `
    <article class="gen-card gen-card--skeleton">
      <header class="gen-card__head">
        <span class="gen-card__index">${n}</span>
        <span class="gen-card__kind">${kind}</span>
        <span class="gen-card__time">en cola</span>
      </header>
      <div class="gen-card__stage gen-card__stage--shimmer">
        <span class="gen-shimmer"></span>
      </div>
      <div class="gen-card__body">
        <h2 class="gen-card__title gen-card__title--ghost">${title}</h2>
        <span class="gen-line" style="--w:92%"></span>
        <span class="gen-line" style="--w:78%"></span>
        <span class="gen-line" style="--w:60%"></span>
      </div>
    </article>`;
}

const ISO = k.ISO_CSS;
const pageCss = readFileSync(join(HERE, "page.css"), "utf8");

const html = `<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Generados · Banco de piezas</title>
<style>
${ISO}
${pageCss}
</style>
</head>
<body>
<main class="feed">
  <header class="feed__head">
    <p class="feed__eyebrow">Red Social · Banco de piezas</p>
    <h1 class="feed__title">Generados</h1>
    <p class="feed__lede">Ilustraciones isométricas que se reciclan tal cual en la home. Cada tarjeta es una pieza autónoma: figura, panel de datos y ficha.</p>
  </header>
  ${cardTurbine()}
  ${cardMill()}
  ${cardVerticalMill()}
  ${cardBridgeportMill()}
  ${cardSkeleton("05", "Isométrico · SVG", "Intercambiador de calor de placas")}
  <footer class="feed__foot">5 generados · 4 listos, 1 en cola</footer>
</main>
</body>
</html>
`;

writeFileSync(join(OUT, "turbine.svg"), svg);
writeFileSync(join(OUT, "fresadora.svg"), millSvg);
writeFileSync(join(OUT, "fresadora-vertical.svg"), verticalMillSvg);
writeFileSync(join(OUT, "bridgeport-fresadora-vertical.svg"), bridgeportVerticalSvg);
console.log("wrote standalone SVGs: turbine.svg, fresadora.svg, fresadora-vertical.svg, bridgeport-fresadora-vertical.svg");
