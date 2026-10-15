import * as k from "../kit/iso-kit.mjs";
import * as G from "../kit/lathe.mjs";

const VERTICAL_MILL_CSS = `
.vertical-mill{
  --mill-accent:var(--anatomy-blue);--mill-rotation-accent:#f0a43a;
  --anatomy-paper:#14181c;--anatomy-face:#1a2025;--anatomy-top:#303941;
  --anatomy-shade-0:#161b20;--anatomy-shade-1:#1d2329;--anatomy-shade-2:#252d34;--anatomy-shade-3:#2d363e;
  --anatomy-lit-top:#414c55;--anatomy-lit-shade:#343e46;
  --anatomy-hi:#e2e9ee;--anatomy-mid:#a0adb7;--anatomy-lo:#6d7a84;--anatomy-faint:#354049;
  --anatomy-text:#a0adb7;--anatomy-strong:#e2e9ee;--anatomy-muted:#71808b;--anatomy-dot:#e2e9ee;
}
.vertical-mill #pieza{
  --anatomy-paper:#343d45;--anatomy-face:#3e4851;--anatomy-top:#59656e;
  --anatomy-shade-0:#303941;--anatomy-shade-1:#3a444d;--anatomy-shade-2:#45515b;--anatomy-shade-3:#515e68;
  --anatomy-lit-top:#697680;--anatomy-lit-shade:#59656f;
  --anatomy-hi:#f2f6f8;--anatomy-mid:#c3cdd4;--anatomy-lo:#89969f;--anatomy-faint:#66737d;
  --anatomy-dot:#f2f6f8;
}
.vertical-mill #vertical-fresa-tool{transform-box:fill-box;transform-origin:center;animation:vertical-spindle-turn 1.7s linear infinite}
.vertical-mill .mill-face{fill:var(--anatomy-shade-2);stroke:var(--anatomy-hi);stroke-width:.55}
.vertical-mill .mill-insert{fill:var(--mill-accent);stroke:var(--anatomy-paper);stroke-width:.4}
.vertical-mill .mill-index{fill:var(--mill-rotation-accent);stroke:var(--anatomy-strong);stroke-width:.45}
.vertical-mill .mill-ring{fill:none;stroke:var(--anatomy-mid);stroke-width:.55}
.vertical-mill .panel-bg{fill:var(--anatomy-card);stroke:var(--anatomy-mid);stroke-width:.65}
.vertical-mill .panel-rule{stroke:var(--anatomy-faint);stroke-width:.65}
.vertical-mill .panel-accent{fill:var(--mill-accent)}
.vertical-mill .panel-kicker{fill:var(--anatomy-muted);font:9px var(--anatomy-mono);letter-spacing:.16em}
.vertical-mill .panel-copy{fill:var(--anatomy-strong);font:12px var(--anatomy-mono);letter-spacing:.025em}
@keyframes vertical-spindle-turn{to{transform:rotate(360deg)}}
@media(prefers-reduced-motion:reduce){.vertical-mill #vertical-fresa-tool{animation:none}}
`;

export function verticalMillFigure() {
  const WIDTH = 1000;
  const HEIGHT = 520;
  const MACHINE_WIDTH = 720;

  const BASE = { x: -72, y: -68, w: 144, d: 116, r: 9 };
  const COLUMN = { x: -30, y: -62, w: 60, d: 34, r: 3 };
  const HEAD = { x: -22, y: -39, w: 44, d: 36, r: 3 };
  const MOTOR = { x: -17, y: -35, w: 34, d: 25, r: 4 };
  const KNEE = { x: -36, y: -38, w: 72, d: 34, r: 3 };
  const SADDLE = { x: -50, y: -34, w: 100, d: 48, r: 2 };
  const TABLE = { x: -61, y: -31, w: 122, d: 48, r: 2 };
  const VISE = { x: -34, y: -25, w: 68, d: 38, r: 2 };
  const PIECE = { x: -25, y: -20, w: 50, d: 30, r: 1.2 };
  const SLOT_LEFT = { x: -25, y: -20, w: 19, d: 30, r: 0.6 };
  const SLOT_RIGHT = { x: 6, y: -20, w: 19, d: 30, r: 0.6 };
  const TABLE_TOP = 48;
  const STOCK_BASE = 51;
  const STOCK_TOP = 62;
  const POCKET_FLOOR = 56;
  const TOOL_X = 0;
  const TOOL_Y = -5;
  const TOOL_RADIUS = 5.5;
  const VZ = G.frameOf([0, 0, 0], [0, 0, 1], [1, 0, 0], [0, 1, 0]);

  const P = k.fitProjection(
    [
      ...k.boxCorners(BASE, -18, 0),
      ...k.boxCorners(COLUMN, 0, 82),
      ...k.boxCorners(HEAD, 68, 85),
      ...k.boxCorners(MOTOR, 85, 101),
      ...k.boxCorners(KNEE, 18, 38),
      ...k.boxCorners(TABLE, 42, TABLE_TOP),
      ...k.boxCorners(PIECE, STOCK_BASE, STOCK_TOP),
      [TOOL_X - TOOL_RADIUS, TOOL_Y - TOOL_RADIUS, POCKET_FLOOR],
      [TOOL_X + TOOL_RADIUS, TOOL_Y + TOOL_RADIUS, 76],
    ],
    MACHINE_WIDTH,
    HEIGHT,
    { pad: 24, azimuth: 45, elevation: 30 },
  );

  const at = (p) => k.iso(p, P);
  const S = (paths, style) => k.solidSvg(paths, style);
  const L = (d, style) => k.lineSvg(d, style);
  const D = (points, style) => k.dotsSvg(points, style);

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
    const out = [S(k.slabOf(COLUMN, 0, 82, P, 7, 1.2), { tone: "mid" })];
    for (const x of [COLUMN.x + 5, COLUMN.x + COLUMN.w - 9]) {
      out.push(S(k.slabOf({ x, y: COLUMN.y + COLUMN.d - 1, w: 4, d: 2.2, r: 0.5 }, 12, 66, P, 3, 0.45), { tone: "hi", crease: "lo" }));
    }
    out.push(L(k.sideSeam(COLUMN, 70, P, 7), { tone: "faint" }));
    out.push(L(k.sideSeam(COLUMN, 16, P, 7), { tone: "faint" }));
    return out.join("");
  }

  function headAndMotor() {
    const out = [];
    // Vertical head is bolted to the column front; the quill axis is Z-down.
    out.push(S(k.slabOf(HEAD, 68, 17, P, 5, 0.8), { tone: "mid" }));
    out.push(L(k.planOutline(k.insetPlan(HEAD, 2.4), 85, P), { tone: "faint" }));
    out.push(L(k.sideSeam(HEAD, 74, P, 5), { tone: "faint" }));
    // Motor sits vertically on top of the head.
    const motorAxis = G.frameOf([0, -22.5, 0], [0, 0, 1], [1, 0, 0], [0, 1, 0]);
    out.push(S(G.solidOf([[85, 7], [87, 11, 1], [99, 11, 1], [101, 7, 1]], motorAxis, P, { steps: 52, smooth: true }), { tone: "hi", crease: "lo" }));
    for (const z of [89, 93, 97]) out.push(S(G.ringBand(z, z + 0.7, 10.7, 12, motorAxis, P), { tone: "mid", crease: "lo" }));
    // Quill and spindle nose descend from the underside of the head.
    out.push(S(G.disc(64, 71, 5.8, VZ, P, { steps: 40 }), { tone: "mid", crease: "lo" }));
    out.push(S(G.ringBand(70, 73, 3.2, 5.2, VZ, P), { tone: "hi", crease: "lo" }));
    return out.join("");
  }

  function tableCarriage() {
    const out = [];
    out.push(S(k.slabOf(KNEE, 18, 20, P, 5, 0.8), { tone: "mid" }));
    out.push(L(k.planOutline(k.insetPlan(KNEE, 3), 38, P), { tone: "faint" }));
    out.push(S(k.slabOf(SADDLE, 38, 4, P, 4, 0.6), { tone: "mid" }));
    out.push(L(k.planOutline(k.insetPlan(SADDLE, 3), 42, P), { tone: "faint" }));
    return out.join("");
  }

  function table() {
    const out = [S(k.slabOf(TABLE, 42, 6, P, 5, 0.8), { tone: "mid", crease: "lo" })];
    for (const y of [-23, -7, 9]) {
      const slot = { x: -56, y: y - 1.4, w: 112, d: 2.8, r: 0.6 };
      out.push(S(k.slabOf(slot, TABLE_TOP, 0.22, P, 3), { tone: "faint", crease: "none" }));
      out.push(L(k.planOutline(k.insetPlan(slot, 0.3), TABLE_TOP + 0.22, P, 3), { tone: "lo" }));
    }
    out.push(L(k.planOutline(k.insetPlan(TABLE, 2.2), TABLE_TOP, P), { tone: "faint" }));
    return out.join("");
  }

  function vise() {
    const out = [S(k.slabOf(VISE, TABLE_TOP, 3, P, 4, 0.5), { tone: "mid" })];
    out.push(S(k.slabOf({ x: -34, y: -25, w: 8, d: 38, r: 1 }, 51, 10, P, 3, 0.6), { tone: "mid", crease: "lo" }));
    out.push(S(k.slabOf({ x: 26, y: -25, w: 8, d: 38, r: 1 }, 51, 10, P, 3, 0.6), { tone: "mid", crease: "lo" }));
    for (const x of [-30, 30]) out.push(D([-18, 2].map((y) => at([x, y, 61])), { size: 0.52, tone: "mid" }));
    return out.join("");
  }

  function workpiece() {
    const out = [S(k.slabOf(PIECE, STOCK_BASE, POCKET_FLOOR - STOCK_BASE, P, 3, 0.3), { tone: "hi", crease: "lo", lit: true })];
    // Uncut stock shoulders leave a straight, clearly visible milled channel.
    out.push(S(k.slabOf(SLOT_LEFT, POCKET_FLOOR, STOCK_TOP - POCKET_FLOOR, P, 3, 0.3), { tone: "hi", crease: "lo", lit: true }));
    out.push(S(k.slabOf(SLOT_RIGHT, POCKET_FLOOR, STOCK_TOP - POCKET_FLOOR, P, 3, 0.3), { tone: "hi", crease: "lo", lit: true }));
    out.push(L(k.planOutline(k.insetPlan(SLOT_LEFT, 1.5), STOCK_TOP, P), { tone: "faint" }));
    out.push(L(k.planOutline(k.insetPlan(SLOT_RIGHT, 1.5), STOCK_TOP, P), { tone: "faint" }));
    return out.join("");
  }

  function cutterBody() {
    const out = [];
    out.push(S(G.disc(POCKET_FLOOR, 66, 5.5, VZ, P, { steps: 48, bevel: 0.45 }), { tone: "hi", crease: "lo" }));
    out.push(S(G.disc(66, 73, 3.4, VZ, P, { steps: 32 }), { tone: "mid", crease: "lo" }));
    return out.join("");
  }

  function rotatingSpindleIndex() {
    const z = POCKET_FLOOR;
    const center = at([TOOL_X, TOOL_Y, z]);
    const x = at([TOOL_X + 1, TOOL_Y, z]);
    const y = at([TOOL_X, TOOL_Y + 1, z]);
    const matrix = `matrix(${(x[0] - center[0]).toFixed(3)} ${(x[1] - center[1]).toFixed(3)} ${(y[0] - center[0]).toFixed(3)} ${(y[1] - center[1]).toFixed(3)} ${center[0].toFixed(2)} ${center[1].toFixed(2)})`;
    const circle = (r, n = 48) => k.pathOf(Array.from({ length: n }, (_, i) => [r * Math.cos((i / n) * Math.PI * 2), r * Math.sin((i / n) * Math.PI * 2)]), true);
    return `<g transform="${matrix}"><g id="vertical-fresa-tool" data-layer="fresa-tool" aria-label="Indicador giratorio del husillo vertical">
      <path class="mill-face" d="${circle(5.4)}"/>
      <path class="mill-ring" d="${circle(4.5)}"/>
      <path class="mill-index" d="${k.pathOf([[1.1, -0.9], [4.2, -0.9], [5.1, 0], [4.2, 0.9], [1.1, 0.9], [1.8, 0]], true)}"/>
      <path class="mill-ring" d="${circle(1.3)}"/>
    </g></g>`;
  }

  function servicePanel() {
    return `<g id="vertical-interactive-labels" data-layer="interactive-labels" aria-label="Servicio: mecanizado y fabricación de piezas de precisión">
      <path class="panel-bg" d="M758 132H972Q980 132 980 140V306Q980 314 972 314H758Q750 314 750 306V140Q750 132 758 132Z"/>
      <path class="panel-accent" d="M750 150H753V296H750Z"/>
      <text class="panel-kicker" x="770" y="160">SERVICIO:</text>
      <path class="panel-rule" d="M770 174H958"/>
      <text class="panel-copy" x="770" y="202">MECANIZADO Y</text>
      <text class="panel-copy" x="770" y="226">FABRICACIÓN</text>
      <text class="panel-copy" x="770" y="250">DE PIEZAS DE</text>
      <text class="panel-copy" x="770" y="274">PRECISIÓN</text>
      <path class="panel-rule" d="M770 289H958"/>
    </g>`;
  }

  const svgBody = [
    `<style>${k.ISO_CSS}${VERTICAL_MILL_CSS}</style>`,
    `<g id="vertical-base" data-layer="base">${base()}</g>`,
    `<g id="vertical-machine-column" data-layer="machine-column">${column()}</g>`,
    `<g id="vertical-head" data-layer="head">${headAndMotor()}</g>`,
    `<g id="vertical-table-carriage" data-layer="table-carriage">${tableCarriage()}</g>`,
    `<g id="vertical-mesa" data-layer="mesa">${table()}</g>`,
    `<g id="vertical-vise" data-layer="vise">${vise()}</g>`,
    `<g id="vertical-pieza" data-layer="pieza">${workpiece()}</g>`,
    `<g id="vertical-fresa-body" data-layer="fresa-body">${cutterBody()}</g>`,
    rotatingSpindleIndex(),
    servicePanel(),
  ].join("");

  return k.figureSvg({
    width: WIDTH,
    height: HEIGHT,
    className: "iso vertical-mill",
    label: "Fresadora vertical de taller en proyección isométrica ortográfica a 30 grados. El motor y el cabezal quedan sobre la mesa; el husillo desciende verticalmente y una fresa de extremo entra en un canal de la pieza sujeta en la mordaza. El panel indica servicio de mecanizado y fabricación de piezas de precisión.",
    body: svgBody,
  });
}
