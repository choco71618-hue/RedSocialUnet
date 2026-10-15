import * as k from "../kit/iso-kit.mjs";
import * as G from "../kit/lathe.mjs";

/*
 * Object anchor: universal workshop milling machine, orthographic isometric.
 * Layer order: base -> column/drive -> knee/table -> vice/workpiece -> arbor/tool -> panel.
 * Motion: only the visible carbide face rotates; it represents the horizontal spindle.
 */
const MILL_CSS = `
.mill{
  --mill-accent:var(--anatomy-blue);--mill-rotation-accent:#f0a43a;
  --anatomy-paper:#14181c;--anatomy-face:#1a2025;--anatomy-top:#303941;
  --anatomy-shade-0:#161b20;--anatomy-shade-1:#1d2329;--anatomy-shade-2:#252d34;--anatomy-shade-3:#2d363e;
  --anatomy-lit-top:#414c55;--anatomy-lit-shade:#343e46;
  --anatomy-hi:#e2e9ee;--anatomy-mid:#a0adb7;--anatomy-lo:#6d7a84;--anatomy-faint:#354049;
  --anatomy-text:#a0adb7;--anatomy-strong:#e2e9ee;--anatomy-muted:#71808b;--anatomy-dot:#e2e9ee;
}
.mill #pieza{
  --anatomy-paper:#343d45;--anatomy-face:#3e4851;--anatomy-top:#59656e;
  --anatomy-shade-0:#303941;--anatomy-shade-1:#3a444d;--anatomy-shade-2:#45515b;--anatomy-shade-3:#515e68;
  --anatomy-lit-top:#697680;--anatomy-lit-shade:#59656f;
  --anatomy-hi:#f2f6f8;--anatomy-mid:#c3cdd4;--anatomy-lo:#89969f;--anatomy-faint:#66737d;
  --anatomy-dot:#f2f6f8;
}
.mill #fresa-tool{transform-box:fill-box;transform-origin:center;animation:mill-tool-turn 1.7s linear infinite}
.mill .mill-cutter-face{fill:var(--anatomy-shade-2);stroke:var(--anatomy-hi);stroke-width:.55}
.mill .mill-insert{fill:var(--mill-accent);stroke:var(--anatomy-paper);stroke-width:.45}
.mill .mill-tool-index{fill:var(--mill-rotation-accent);stroke:var(--anatomy-strong);stroke-width:.45}
.mill .mill-tool-ring{fill:none;stroke:var(--anatomy-mid);stroke-width:.55}
.mill .mill-tool-hub{fill:var(--anatomy-shade-0);stroke:var(--anatomy-hi);stroke-width:.55}
.mill .mill-panel-bg{fill:var(--anatomy-card);stroke:var(--anatomy-mid);stroke-width:.65}
.mill .mill-panel-rule{stroke:var(--anatomy-faint);stroke-width:.65}
.mill .mill-panel-accent{fill:var(--mill-accent)}
.mill .mill-panel-kicker{fill:var(--anatomy-muted);font:9px var(--anatomy-mono);letter-spacing:.16em}
.mill .mill-panel-copy{fill:var(--anatomy-strong);font:12px var(--anatomy-mono);letter-spacing:.025em}
@keyframes mill-tool-turn{to{transform:rotate(360deg)}}
@media(prefers-reduced-motion:reduce){.mill #fresa-tool{animation:none}}
`;

export function millFigure() {
  const WIDTH = 1000;
  const HEIGHT = 520;
  const MACHINE_WIDTH = 730;

  // Floor plans and heights use world units; z is vertical.
  const BASE = { x: -72, y: -70, w: 144, d: 112, r: 9 };
  const COLUMN = { x: -29, y: -66, w: 58, d: 34, r: 3 };
  const KNEE = { x: -35, y: -38, w: 70, d: 34, r: 3 };
  const SADDLE = { x: -49, y: -34, w: 98, d: 50, r: 2 };
  const TABLE = { x: -61, y: -31, w: 122, d: 48, r: 2 };
  const VISE = { x: -34, y: -28, w: 68, d: 40, r: 2 };
  const RAW = { x: -25, y: -22, w: 25, d: 32, r: 1.5 };
  const FINISHED = { x: 0, y: -22, w: 25, d: 32, r: 1.5 };
  const OVERARM = { x: -12, y: -62, w: 24, d: 76, r: 2 };
  const MOTOR = { x: -17, y: -66, w: 34, d: 23, r: 3 };
  const TABLE_TOP = 50;
  const TOOL_Z = 66;
  const TOOL_R = 8;
  const TOOL_FRONT_Y = 2;
  const ARBOR = G.frameOf([0, 0, TOOL_Z], [0, 1, 0], [1, 0, 0], [0, 0, 1]);

  const P = k.fitProjection(
    [
      ...k.boxCorners(BASE, -18, 0),
      ...k.boxCorners(COLUMN, 0, 84),
      ...k.boxCorners(OVERARM, 82, 90),
      ...k.boxCorners(MOTOR, 90, 106),
      ...k.boxCorners(KNEE, 20, 40),
      ...k.boxCorners(TABLE, 44, TABLE_TOP),
      ...k.boxCorners(RAW, TABLE_TOP + 3, TABLE_TOP + 15),
      ...k.boxCorners(FINISHED, TABLE_TOP + 3, TABLE_TOP + 8),
      [-18, -37, TOOL_Z + TOOL_R + 2],
      [18, 20, TOOL_Z - TOOL_R - 2],
    ],
    MACHINE_WIDTH,
    HEIGHT,
    { pad: 24, azimuth: 45, elevation: 30 },
  );

  const at = (point) => k.iso(point, P);
  const S = (paths, style) => k.solidSvg(paths, style);
  const L = (path, style) => k.lineSvg(path, style);
  const D = (points, style) => k.dotsSvg(points, style);
  const raw = (path, className = "") => `<path${className ? ` class="${className}"` : ""} d="${path}"/>`;
  const rawFill = (path, fill, className = "") => `<path${className ? ` class="${className}"` : ""} d="${path}" style="fill:${fill};stroke:none"/>`;

  function base() {
    const out = [`<path class="iso-halo" d="${k.haloOf(BASE, -24, 6, P)}"/>`];
    const feet = k.corners(BASE, 23).sort((a, b) =>
      k.depthOf([a[0], a[1], 0], P) - k.depthOf([b[0], b[1], 0], P),
    );
    for (const [x, y] of feet) {
      out.push(S(k.cylinder(x, y, 7, -24, 2, P, 28), { tone: "lo" }));
      out.push(S(k.cylinder(x, y, 5.2, -22, 5, P, 28), { tone: "lo" }));
    }
    out.push(S(k.slabOf(BASE, -18, 18, P, 8, 1.4), { tone: "lo" }));
    out.push(L(k.planOutline(k.insetPlan(BASE, 5), 0, P), { tone: "faint" }));
    const bolts = k.corners(BASE, 10);
    out.push(L(bolts.map(([x, y]) => k.ring(x, y, 2.1, 0, P, 18)).join(""), { tone: "lo" }));
    out.push(D(bolts.map(([x, y]) => at([x, y, 0])), { size: 0.48, tone: "mid" }));
    return out.join("");
  }

  function column() {
    const out = [];
    out.push(S(k.slabOf(COLUMN, 0, 84, P, 7, 1.2), { tone: "mid" }));
    // Dovetail ways are raised solid rails, not decorative floating strokes.
    for (const x of [COLUMN.x + 5, COLUMN.x + COLUMN.w - 9]) {
      out.push(S(k.slabOf({ x, y: COLUMN.y + COLUMN.d - 1, w: 4, d: 2.2, r: 0.5 }, 13, 68, P, 3, 0.45), { tone: "hi", crease: "lo" }));
    }
    const access = { x: COLUMN.x + 7, y: COLUMN.y + COLUMN.d - 0.4, w: COLUMN.w - 14, d: 0.4, r: 0.2 };
    out.push(L(k.sideSeam(COLUMN, 76, P, 7), { tone: "faint" }));
    out.push(L(k.sideSeam(COLUMN, 18, P, 7), { tone: "faint" }));
    out.push(S(k.slabOf(access, 18, 42, P, 2), { tone: "mid", crease: "none" }));
    // Column cap and front plate give the head a real mounting face.
    out.push(S(k.slabOf({ x: -27, y: -36, w: 54, d: 4, r: 1 }, 74, 10, P, 3, 0.5), { tone: "mid" }));
    out.push(D([-21, 21].flatMap((x) => [at([x, -31, 79]), at([x, -31, 48])]), { size: 0.42, tone: "lo" }));
    return out.join("");
  }

  function upperDrive() {
    const out = [];
    // Overarm, motor and spindle cartridge: one connected drive train.
    out.push(S(k.slabOf(OVERARM, 82, 8, P, 4, 0.9), { tone: "mid" }));
    out.push(L(k.planOutline(k.insetPlan(OVERARM, 2.2), 90, P), { tone: "faint" }));
    const motorF = G.frameOf([0, -54.5, 0], [0, 0, 1], [1, 0, 0], [0, 1, 0]);
    out.push(S(G.solidOf([[90, 7], [92, 11, 1], [104, 11, 1], [106, 7, 1]], motorF, P, { steps: 52, smooth: true }), { tone: "hi", crease: "lo" }));
    for (const z of [94, 98, 102]) {
      out.push(S(G.ringBand(z, z + 0.8, 10.7, 12, motorF, P), { tone: "mid", crease: "lo" }));
    }
    // Spindle nose fixed to the column, pointing toward the work table.
    out.push(S(k.slabOf({ x: -17, y: -39, w: 34, d: 16, r: 2 }, 61, 21, P, 4, 0.8), { tone: "mid" }));
    out.push(S(G.disc(-39, -23, 6.4, ARBOR, P, { steps: 40 }), { tone: "hi", crease: "lo" }));
    out.push(S(G.ringBand(-25, -21, 3.4, 5.4, ARBOR, P), { tone: "mid" }));
    return out.join("");
  }

  function carriage() {
    const out = [];
    out.push(S(k.slabOf(KNEE, 20, 20, P, 5, 0.8), { tone: "mid" }));
    out.push(L(k.planOutline(k.insetPlan(KNEE, 3), 40, P), { tone: "faint" }));
    out.push(S(k.slabOf(SADDLE, 40, 4, P, 4, 0.6), { tone: "mid" }));
    out.push(L(k.planOutline(k.insetPlan(SADDLE, 3), 44, P), { tone: "faint" }));
    // Feed screw and its collars under the table.
    const feedF = G.frameOf([-60, -7, 35], [1, 0, 0], [0, 1, 0], [0, 0, 1]);
    out.push(S(G.solidOf([[0, 1.5], [116, 1.5]], feedF, P, { steps: 18, rims: "none" }), { tone: "lo", crease: "none" }));
    for (const x of [-51, 51]) out.push(S(G.disc(x + 60 - 1.2, x + 60 + 1.2, 3.2, feedF, P, { steps: 24 }), { tone: "mid" }));
    return out.join("");
  }

  function table() {
    const out = [];
    out.push(S(k.slabOf(TABLE, 44, 6, P, 5, 0.8), { tone: "mid", crease: "lo" }));
    // Three T-slots are inset strips with closed outlines.
    for (const y of [-23, -7, 9]) {
      const slot = { x: -56, y: y - 1.5, w: 112, d: 3, r: 0.65 };
      out.push(S(k.slabOf(slot, 50, 0.22, P, 3), { tone: "faint", crease: "none" }));
      out.push(L(k.planOutline(k.insetPlan(slot, 0.35), 50.22, P, 3), { tone: "lo" }));
    }
    out.push(L(k.planOutline(k.insetPlan(TABLE, 2.2), 50, P), { tone: "faint" }));
    // Table end caps and two grounded feed handles.
    out.push(S(k.slabOf({ x: -62, y: -28, w: 5, d: 42, r: 1 }, 44, 6, P, 3), { tone: "mid" }));
    out.push(S(k.slabOf({ x: 57, y: -28, w: 5, d: 42, r: 1 }, 44, 6, P, 3), { tone: "mid" }));
    out.push(S(G.disc(0, 2, 6, G.frameAlong([66, -6, 47], [1, 0, 0]), P), { tone: "mid" }));
    out.push(S(G.disc(0, 2, 2.2, G.frameAlong([68, -6, 47], [1, 0, 0]), P), { tone: "hi" }));
    return out.join("");
  }

  function vice() {
    const out = [];
    out.push(S(k.slabOf(VISE, TABLE_TOP, 3, P, 4, 0.5), { tone: "mid" }));
    // Fixed and moving jaw blocks rise on both sides of the workpiece.
    out.push(S(k.slabOf({ x: -34, y: -27, w: 8, d: 40, r: 1 }, 53, 11, P, 3, 0.6), { tone: "mid", crease: "lo" }));
    out.push(S(k.slabOf({ x: 26, y: -27, w: 8, d: 40, r: 1 }, 53, 11, P, 3, 0.6), { tone: "mid", crease: "lo" }));
    // Jaw liners and two retaining bolts on each jaw.
    for (const x of [-30, 30]) {
      out.push(S(k.slabOf({ x: x - 2.2, y: -25, w: 4.4, d: 36, r: 0.5 }, 62, 1, P, 3), { tone: "lo", crease: "none" }));
      out.push(D([-19, 3].map((y) => at([x, y, 64])), { size: 0.52, tone: "mid" }));
    }
    // Leadscrew and end thrust collars along the vise front.
    const screwF = G.frameOf([0, 0, 54], [1, 0, 0], [0, 1, 0], [0, 0, 1]);
    out.push(S(G.solidOf([[-34, 1.25], [34, 1.25]], screwF, P, { steps: 16, rims: "none" }), { tone: "lo", crease: "none" }));
    out.push(S(G.disc(-39, -34, 3, screwF, P, { steps: 24 }), { tone: "mid" }));
    out.push(S(G.disc(34, 39, 3, screwF, P, { steps: 24 }), { tone: "mid" }));
    return out.join("");
  }

  function workpiece() {
    const out = [];
    // Stock on the left; the lower, freshly milled pass is on the right.
    out.push(S(k.slabOf(RAW, TABLE_TOP + 3, 12, P, 3, 0.35), { tone: "hi", crease: "lo", lit: true }));
    out.push(S(k.slabOf(FINISHED, TABLE_TOP + 3, 5, P, 3, 0.35), { tone: "hi", crease: "lo", lit: true }));
    // Narrow finish bands belong to the machined face, not floating annotations.
    for (const y of [-16, -8, 0]) {
      const mark = { x: 2, y, w: 20, d: 0.35, r: 0.15 };
      out.push(S(k.slabOf(mark, 58.08, 0.08, P, 2), { tone: "faint", crease: "none" }));
    }
    return out.join("");
  }

  function arborSupport() {
    const out = [];
    // Arbor connects the spindle nose to the overarm's outboard support.
    out.push(S(G.solidOf([[-43, 2.7], [21, 2.7]], ARBOR, P, { steps: 28, rims: "none" }), { tone: "mid", crease: "lo" }));
    out.push(S(k.slabOf({ x: -10, y: 12, w: 20, d: 9, r: 1.5 }, 72, 18, P, 3, 0.6), { tone: "mid" }));
    out.push(S(G.solidOf([[18, 0], [18, 5.5], [24, 5.5], [24, 0]], ARBOR, P, { steps: 30 }), { tone: "hi", crease: "lo" }));
    out.push(S(G.ringBand(9.8, 12.2, 2.8, 5.8, ARBOR, P), { tone: "mid" }));
    return out.join("");
  }

  function cutterBody() {
    const out = [];
    out.push(S(G.disc(-24, TOOL_FRONT_Y, TOOL_R, ARBOR, P, { steps: 52, bevel: 0.55 }), { tone: "hi", crease: "lo" }));
    // Side flutes are shallow solid reliefs and stay aligned with the face inserts.
    const fluteMarks = G.ribsOf(ARBOR, -24, TOOL_FRONT_Y, TOOL_R, 10, P, { phase: 0.08, twist: 0.42, fade: [0.05, 0.52], seams: true });
    out.push(k.fadedSvg(fluteMarks, { tone: "faint" }));
    return out.join("");
  }

  function rotatingToolFace() {
    const c0 = at([0, TOOL_FRONT_Y, TOOL_Z]);
    const cX = at([1, TOOL_FRONT_Y, TOOL_Z]);
    const cZ = at([0, TOOL_FRONT_Y, TOOL_Z + 1]);
    const matrix = `matrix(${(cX[0] - c0[0]).toFixed(3)} ${(cX[1] - c0[1]).toFixed(3)} ${(cZ[0] - c0[0]).toFixed(3)} ${(cZ[1] - c0[1]).toFixed(3)} ${c0[0].toFixed(2)} ${c0[1].toFixed(2)})`;
    const circle = (radius, n = 56) => k.pathOf(
      Array.from({ length: n }, (_, i) => [radius * Math.cos((i / n) * Math.PI * 2), radius * Math.sin((i / n) * Math.PI * 2)]),
      true,
    );
    const teeth = [];
    for (let i = 0; i < 8; i++) {
      const a = (i / 8) * Math.PI * 2;
      const da = 0.18;
      teeth.push(k.pathOf([
        [6.1 * Math.cos(a - da), 6.1 * Math.sin(a - da)],
        [7.7 * Math.cos(a - 0.12), 7.7 * Math.sin(a - 0.12)],
        [7.95 * Math.cos(a + 0.12), 7.95 * Math.sin(a + 0.12)],
        [6.1 * Math.cos(a + da), 6.1 * Math.sin(a + da)],
      ], true));
    }
    return `<g transform="${matrix}"><g id="fresa-tool" aria-label="Fresa giratoria">
      <path class="mill-cutter-face" d="${circle(TOOL_R)}"/>
      ${teeth.map((d) => `<path class="mill-insert" d="${d}"/>`).join("")}
      <path class="mill-tool-ring" d="${circle(5.5)}"/>
      <path class="mill-tool-index" d="${k.pathOf([[1.8, -1.2], [5.9, -1.2], [7.15, 0], [5.9, 1.2], [1.8, 1.2], [2.5, 0]], true)}"/>
      <path class="mill-tool-hub" d="${circle(2.5)}"/>
      <path class="mill-tool-ring" d="${k.pathOf([[-0.6, -2.3], [0.6, -2.3], [0.6, 2.3], [-0.6, 2.3]], true)}"/>
    </g></g>`;
  }

  function servicePanel() {
    return `<g id="interactive-labels" aria-label="Servicio: mecanizado y fabricación de piezas de precisión">
      <path class="mill-panel-bg" d="M758 132H972Q980 132 980 140V306Q980 314 972 314H758Q750 314 750 306V140Q750 132 758 132Z"/>
      <path class="mill-panel-accent" d="M750 150H753V296H750Z"/>
      <text class="mill-panel-kicker" x="770" y="160">SERVICIO:</text>
      <path class="mill-panel-rule" d="M770 174H958"/>
      <text class="mill-panel-copy" x="770" y="202">MECANIZADO Y</text>
      <text class="mill-panel-copy" x="770" y="226">FABRICACIÓN</text>
      <text class="mill-panel-copy" x="770" y="250">DE PIEZAS DE</text>
      <text class="mill-panel-copy" x="770" y="274">PRECISIÓN</text>
      <path class="mill-panel-rule" d="M770 289H958"/>
    </g>`;
  }

  const contents = [
    `<style>${MILL_CSS}</style>`,
    `<g id="base">${base()}</g>`,
    `<g id="machine-column">${column()}</g>`,
    `<g id="upper-drive">${upperDrive()}</g>`,
    `<g id="table-carriage">${carriage()}</g>`,
    `<g id="mesa">${table()}</g>`,
    `<g id="vice">${vice()}</g>`,
    `<g id="pieza">${workpiece()}</g>`,
    `<g id="internal-parts">${arborSupport()}</g>`,
    `<g id="fresa-body">${cutterBody()}</g>`,
    rotatingToolFace(),
    servicePanel(),
  ].join("");

  return k.figureSvg({
    width: WIDTH,
    height: HEIGHT,
    className: "mill",
    standalone: "dark",
    label: "Fresadora universal de taller en proyección isométrica ortográfica a 30 grados. La bancada sostiene columna, carnero, mesa, mordaza y bloque de metal; un árbol horizontal lleva una fresa con plaquitas que gira sobre la pieza. Panel lateral: Servicio: mecanizado y fabricación de piezas de precisión.",
    body: contents,
  });
}
