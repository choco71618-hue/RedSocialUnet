import * as k from "../kit/iso-kit.mjs";
import * as G from "../kit/lathe.mjs";

const esc = (s) => s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");

export function bridgeportVerticalFigure() {
  const WIDTH = 1000, HEIGHT = 520, MACHINE_WIDTH = 720;
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
  const TABLE_TOP = 48, STOCK_BASE = 51, STOCK_TOP = 62, SLOT_FLOOR = 56;
  const TOOL_X = 0, TOOL_Y = -5, TOOL_R = 5.5;
  const VZ = G.frameOf([0, 0, 0], [0, 0, 1], [1, 0, 0], [0, 1, 0]);

  const P = k.fitProjection([
    ...k.boxCorners(BASE, -18, 0),
    ...k.boxCorners(COLUMN, 0, 82),
    ...k.boxCorners(HEAD, 68, 85),
    ...k.boxCorners(MOTOR, 85, 101),
    ...k.boxCorners(KNEE, 18, 38),
    ...k.boxCorners(TABLE, 42, TABLE_TOP),
    ...k.boxCorners(PIECE, STOCK_BASE, STOCK_TOP),
    [TOOL_X - TOOL_R, TOOL_Y - TOOL_R, SLOT_FLOOR],
    [TOOL_X + TOOL_R, TOOL_Y + TOOL_R, 76],
  ], MACHINE_WIDTH, HEIGHT, { pad: 24, azimuth: 45, elevation: 30 });

  const at = (p) => k.iso(p, P);
  const S = (paths, style) => k.solidSvg(paths, style);
  const L = (d, style) => k.lineSvg(d, style);
  const D = (points, style) => k.dotsSvg(points, style);
  const fill = (d, css, extra = "") => `<path${extra} d="${d}" style="fill:${css};stroke:none"/>`;
  const metalCss = `
.bridgeport{
 --bridge-accent:var(--anatomy-blue);--bridge-rotor:#f0a43a;
 --anatomy-paper:#14181c;--anatomy-face:#1a2025;--anatomy-top:#303941;
 --anatomy-shade-0:#161b20;--anatomy-shade-1:#1d2329;--anatomy-shade-2:#252d34;--anatomy-shade-3:#2d363e;
 --anatomy-lit-top:#414c55;--anatomy-lit-shade:#343e46;
 --anatomy-hi:#e2e9ee;--anatomy-mid:#a0adb7;--anatomy-lo:#6d7a84;--anatomy-faint:#354049;
 --anatomy-text:#a0adb7;--anatomy-strong:#e2e9ee;--anatomy-muted:#71808b;--anatomy-dot:#e2e9ee
}
.bridgeport #bp-piece{
 --anatomy-paper:#343d45;--anatomy-face:#3e4851;--anatomy-top:#59656e;
 --anatomy-shade-0:#303941;--anatomy-shade-1:#3a444d;--anatomy-shade-2:#45515b;--anatomy-shade-3:#515e68;
 --anatomy-lit-top:#697680;--anatomy-lit-shade:#59656f;
 --anatomy-hi:#f2f6f8;--anatomy-mid:#c3cdd4;--anatomy-lo:#89969f;--anatomy-faint:#66737d;--anatomy-dot:#f2f6f8
}
.bridgeport .bp-axis-x{animation:bp-axis-x 6.8s ease-in-out infinite}
.bridgeport .bp-axis-y{animation:bp-axis-y 8.2s ease-in-out -1.6s infinite}
.bridgeport .bp-axis-z{animation:bp-axis-z 9.6s ease-in-out -3.1s infinite}
.bridgeport #bp-spindle-index{transform-box:fill-box;transform-origin:center;animation:bp-spindle-turn 1.65s linear infinite}
.bridgeport .bp-tool-face{fill:var(--anatomy-shade-2);stroke:var(--anatomy-hi);stroke-width:.55}
.bridgeport .bp-tool-ring{fill:none;stroke:var(--anatomy-mid);stroke-width:.55}
.bridgeport .bp-tool-mark{fill:var(--bridge-rotor);stroke:var(--anatomy-strong);stroke-width:.4}
.bridgeport .bp-panel-bg{fill:var(--anatomy-card);stroke:var(--anatomy-mid);stroke-width:.65}
.bridgeport .bp-panel-rule{stroke:var(--anatomy-faint);stroke-width:.65}
.bridgeport .bp-panel-accent{fill:var(--bridge-accent)}
.bridgeport .bp-panel-kicker{fill:var(--anatomy-muted);font:9px var(--anatomy-mono);letter-spacing:.15em}
.bridgeport .bp-panel-title{fill:var(--anatomy-strong);font:11px var(--anatomy-mono);letter-spacing:.05em}
.bridgeport .bp-panel-detail{fill:var(--anatomy-text);font:8.7px var(--anatomy-mono)}
.bridgeport .bp-hotspot{cursor:help;outline:none}
.bridgeport .bp-hotspot-dot{fill:var(--anatomy-paper);stroke:var(--bridge-accent);stroke-width:1.2}
.bridgeport .bp-hotspot-ring{fill:none;stroke:var(--bridge-accent);stroke-width:.55;opacity:.65}
.bridgeport .bp-hotspot-tip{opacity:0;pointer-events:none;transition:opacity .14s ease}
.bridgeport .bp-hotspot-tip rect{fill:var(--anatomy-card);stroke:var(--anatomy-mid);stroke-width:.55}
.bridgeport .bp-hotspot-tip text{fill:var(--anatomy-strong);font:9px var(--anatomy-mono)}
.bridgeport .bp-hotspot:hover .bp-hotspot-dot,.bridgeport .bp-hotspot:focus .bp-hotspot-dot{fill:var(--bridge-accent);stroke:var(--anatomy-strong)}
.bridgeport .bp-hotspot:hover .bp-hotspot-tip,.bridgeport .bp-hotspot:focus .bp-hotspot-tip{opacity:1}
@keyframes bp-axis-x{0%,100%{transform:translate(0px,0px)}50%{transform:translate(__XDX__px,__XDY__px)}}
@keyframes bp-axis-y{0%,100%{transform:translate(0px,0px)}50%{transform:translate(__YDX__px,__YDY__px)}}
@keyframes bp-axis-z{0%,100%{transform:translate(0px,0px)}50%{transform:translate(__ZDX__px,__ZDY__px)}}
@keyframes bp-spindle-turn{to{transform:rotate(360deg)}}
@media(prefers-reduced-motion:reduce){.bridgeport .bp-axis-x,.bridgeport .bp-axis-y,.bridgeport .bp-axis-z,.bridgeport #bp-spindle-index{animation:none}}
`;

  function baseColumn() {
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
    const coolant = { x: 38, y: 20, w: 25, d: 19, r: 2 };
    out.push(S(k.slabOf(coolant, 0.1, 0.3, P, 4), { tone: "lo", crease: "none" }));
    out.push(L(k.planOutline(k.insetPlan(coolant, 2), 0.4, P), { tone: "faint" }));
    const bolts = k.corners(BASE, 10);
    out.push(L(bolts.map(([x, y]) => k.ring(x, y, 2.1, 0, P, 18)).join(""), { tone: "lo" }));
    out.push(D(bolts.map(([x, y]) => at([x, y, 0])), { size: 0.48, tone: "mid" }));

    out.push(S(k.slabOf(COLUMN, 0, 82, P, 7, 1.2), { tone: "mid" }));
    for (const x of [COLUMN.x + 5, COLUMN.x + COLUMN.w - 9]) {
      out.push(S(k.slabOf({ x, y: COLUMN.y + COLUMN.d - 1, w: 4, d: 2.2, r: 0.5 }, 12, 66, P, 3, 0.45), { tone: "hi", crease: "lo" }));
    }
    out.push(L(k.sideSeam(COLUMN, 70, P, 7), { tone: "faint" }));
    out.push(L(k.sideSeam(COLUMN, 16, P, 7), { tone: "faint" }));
    // The elevating screw and pedestal stay attached to the base as the knee travels.
    const screwFrame = G.frameOf([0, -18, 0], [0, 0, 1], [1, 0, 0], [0, 1, 0]);
    out.push(S(k.cylinder(0, -18, 5.5, 0, 5, P, 32), { tone: "lo" }));
    out.push(S(G.solidOf([[4, 1.8], [19, 1.8]], screwFrame, P, { steps: 18, rims: "none" }), { tone: "lo", crease: "none" }));
    return out.join("");
  }

  function headDrive() {
    const out = [];
    out.push(S(k.slabOf(HEAD, 68, 17, P, 5, 0.8), { tone: "mid" }));
    out.push(L(k.planOutline(k.insetPlan(HEAD, 2.4), 85, P), { tone: "faint" }));
    out.push(L(k.sideSeam(HEAD, 74, P, 5), { tone: "faint" }));
    // Bridgeport nameplate on the forward casting face.
    out.push(S(k.slabOf({ x: -14, y: -4, w: 28, d: 1.2, r: 0.8 }, 76, 5, P, 3), { tone: "lo", crease: "none" }));
    out.push(k.faceTextSvg(k.sideMatrix([0, -2.8], 80, P, "left"), "BRIDGEPORT", { size: 2.8, tone: "hi", anchor: "middle" }));

    const motorFrame = G.frameOf([0, -22.5, 0], [0, 0, 1], [1, 0, 0], [0, 1, 0]);
    out.push(S(G.solidOf([[85, 7], [87, 11, 1], [99, 11, 1], [101, 7, 1]], motorFrame, P, { steps: 52, smooth: true }), { tone: "hi", crease: "lo" }));
    for (const z of [89, 93, 97]) out.push(S(G.ringBand(z, z + 0.7, 10.7, 12, motorFrame, P), { tone: "mid", crease: "lo" }));

    // Quill, spindle sleeve, collet and end-mill shank descend from the head.
    out.push(S(G.disc(66, 76, 5.7, VZ, P, { steps: 40 }), { tone: "mid", crease: "lo" }));
    out.push(S(G.ringBand(70, 73, 3.2, 5.2, VZ, P), { tone: "hi", crease: "lo" }));
    out.push(S(G.disc(64, 68, 3.8, VZ, P, { steps: 32 }), { tone: "mid", crease: "lo" }));

    // Quill feed lever, fine-feed wheel, and spindle-lock lever.
    out.push(S(k.slabOf({ x: 18, y: -8, w: 3, d: 3, r: 1 }, 75, 2, P, 3), { tone: "mid" }));
    out.push(S(k.slabOf({ x: 20, y: -7, w: 15, d: 2.4, r: 1 }, 76, 1.2, P, 3), { tone: "hi", crease: "none" }));
    out.push(S(G.sphereOf([35, -5.8, 77], 1.8, P, { steps: 20 }), { tone: "hi" }));
    out.push(S(G.disc(0, 2, 4.5, G.frameAlong([22, -5, 72], [1, 0, 0]), P), { tone: "mid" }));
    out.push(S(k.slabOf({ x: 19, y: -6, w: 10, d: 2, r: 0.8 }, 68, 1.2, P, 3), { tone: "mid" }));
    return out.join("");
  }

  function handwheel(center, axis, radius = 6) {
    const F = G.frameAlong(center, axis);
    const out = [S(G.disc(-1.3, 1.3, radius, F, P, { steps: 28 }), { tone: "mid" })];
    out.push(L(G.circleOf(F, 1.3, radius - 0.9, P), { tone: "lo" }));
    for (let i = 0; i < 5; i++) {
      const a = (i / 5) * Math.PI * 2;
      const da = 0.09;
      const poly = [
        G.pointOf(F, 1.32, 2.1, a - da), G.pointOf(F, 1.32, radius - 1, a - da * 0.55),
        G.pointOf(F, 1.32, radius - 1, a + da * 0.55), G.pointOf(F, 1.32, 2.1, a + da),
      ].map(at);
      out.push(fill(k.pathOf(poly, true), "var(--anatomy-shade-3)"));
    }
    out.push(S(G.disc(1.3, 2.6, 2.2, F, P, { steps: 20 }), { tone: "hi" }));
    out.push(S(G.sphereOf(G.pointOf(F, 3.2, radius - 0.8, 0), 1.2, P, { steps: 18 }), { tone: "hi" }));
    return out.join("");
  }

  function kneeSystemParts() {
    const out = [];
    out.push(S(k.slabOf(KNEE, 18, 20, P, 5, 0.8), { tone: "mid" }));
    out.push(L(k.planOutline(k.insetPlan(KNEE, 3), 38, P), { tone: "faint" }));
    out.push(handwheel([0, 0, 28], [0, 1, 0], 6.2));
    return out.join("");
  }

  function saddleAndYControls() {
    const out = [];
    out.push(S(k.slabOf(SADDLE, 38, 4, P, 4, 0.6), { tone: "mid" }));
    out.push(L(k.planOutline(k.insetPlan(SADDLE, 3), 42, P), { tone: "faint" }));
    out.push(handwheel([0, 22, 40], [0, 1, 0], 5.5));
    const lock = { x: 43, y: 10, w: 8, d: 3, r: 0.8 };
    out.push(S(k.slabOf(lock, 42, 2.6, P, 3), { tone: "lo" }));
    return out.join("");
  }

  function tableAndXControls() {
    const out = [];
    out.push(S(k.slabOf(TABLE, 42, 6, P, 5, 0.8), { tone: "mid", crease: "lo" }));
    for (const y of [-23, -7, 9]) {
      const slot = { x: -56, y: y - 1.4, w: 112, d: 2.8, r: 0.6 };
      out.push(S(k.slabOf(slot, 48, 0.22, P, 3), { tone: "faint", crease: "none" }));
      out.push(L(k.planOutline(k.insetPlan(slot, 0.3), 48.22, P, 3), { tone: "lo" }));
    }
    out.push(L(k.planOutline(k.insetPlan(TABLE, 2.2), 48, P), { tone: "faint" }));
    out.push(handwheel([-65, -6, 46], [-1, 0, 0], 5.8));
    out.push(handwheel([65, -6, 46], [1, 0, 0], 5.8));
    for (const x of [-54, 54]) out.push(S(k.slabOf({ x: x - 1.5, y: 10, w: 3, d: 5, r: 0.8 }, 47, 2, P, 3), { tone: "lo" }));
    return out.join("");
  }

  function viseAndPiece() {
    const out = [S(k.slabOf(VISE, 48, 3, P, 4, 0.5), { tone: "mid" })];
    out.push(S(k.slabOf({ x: -34, y: -23, w: 8, d: 36, r: 1 }, 51, 9, P, 3, 0.6), { tone: "mid", crease: "lo" }));
    out.push(S(k.slabOf({ x: 26, y: -23, w: 8, d: 36, r: 1 }, 51, 9, P, 3, 0.6), { tone: "mid", crease: "lo" }));
    out.push(S(k.slabOf(PIECE, STOCK_BASE, SLOT_FLOOR - STOCK_BASE, P, 3, 0.3), { tone: "hi", crease: "lo", lit: true }));
    out.push(S(k.slabOf(SLOT_LEFT, SLOT_FLOOR, STOCK_TOP - SLOT_FLOOR, P, 3, 0.3), { tone: "hi", crease: "lo", lit: true }));
    out.push(S(k.slabOf(SLOT_RIGHT, SLOT_FLOOR, STOCK_TOP - SLOT_FLOOR, P, 3, 0.3), { tone: "hi", crease: "lo", lit: true }));
    out.push(L(k.planOutline(k.insetPlan(SLOT_LEFT, 1.5), STOCK_TOP, P), { tone: "faint" }));
    out.push(L(k.planOutline(k.insetPlan(SLOT_RIGHT, 1.5), STOCK_TOP, P), { tone: "faint" }));
    for (const x of [-30, 30]) out.push(D([-17, 2].map((y) => at([x, y, 60])), { size: 0.48, tone: "mid" }));
    return out.join("");
  }

  function fresaAndHotspots() {
    const out = [];
    out.push(S(G.disc(SLOT_FLOOR, 66, 5.5, VZ, P, { steps: 48, bevel: 0.45 }), { tone: "hi", crease: "lo" }));
    out.push(S(G.disc(66, 73, 3.4, VZ, P, { steps: 32 }), { tone: "mid", crease: "lo" }));
    // X/Y/Z controls animate their linked carriage groups; the end-mill axis stays vertical.
    const c0 = at([0, TOOL_Y, SLOT_FLOOR]);
    const cx = at([1, TOOL_Y, SLOT_FLOOR]);
    const cy = at([0, TOOL_Y + 1, SLOT_FLOOR]);
    const matrix = `matrix(${(cx[0] - c0[0]).toFixed(3)} ${(cx[1] - c0[1]).toFixed(3)} ${(cy[0] - c0[0]).toFixed(3)} ${(cy[1] - c0[1]).toFixed(3)} ${c0[0].toFixed(2)} ${c0[1].toFixed(2)})`;
    const circle = (r, n = 48) => k.pathOf(Array.from({ length: n }, (_, i) => [r * Math.cos((i / n) * Math.PI * 2), r * Math.sin((i / n) * Math.PI * 2)]), true);
    out.push(`<g transform="${matrix}"><g id="bp-spindle-index">
      <path class="bp-index-face" d="${circle(5.4)}"/>
      <path class="bp-index-ring" d="${circle(4.4)}"/>
      <path class="bp-index-mark" d="${k.pathOf([[1.2,-.95],[4.2,-.95],[5.2,0],[4.2,.95],[1.2,.95],[1.9,0]], true)}"/>
      <path class="bp-index-ring" d="${circle(1.3)}"/>
    </g></g>`);
    return out.join("");
  }

  function escape(s) { return esc(s); }
  function hotspot(id, label, detail, point) {
    const [x, y] = at(point);
    const width = Math.min(156, Math.max(82, label.length * 5.8 + 12));
    const dx = x > 650 ? -width - 8 : 8;
    const text = escape(label);
    const accessible = escape(`${label}: ${detail}`);
    return `<g id="bp-hotspot-${id}" class="bp-hotspot" data-layer="interactive-labels" tabindex="0" role="button" aria-label="${accessible}">
      <title>${accessible}</title>
      <circle class="bp-hotspot-ring" cx="${x.toFixed(1)}" cy="${y.toFixed(1)}" r="5.6"/>
      <circle class="bp-hotspot-dot" cx="${x.toFixed(1)}" cy="${y.toFixed(1)}" r="2.4"/>
      <g class="bp-hotspot-tip" transform="translate(${(x + dx).toFixed(1)} ${(y - 11).toFixed(1)})">
        <rect x="0" y="-10" width="${width}" height="17" rx="3"/>
        <text x="6" y="1.5">${text}</text>
      </g>
    </g>`;
  }

  function servicePanel() {
    return `<g id="bp-information-panel" data-layer="interactive-labels" aria-label="Panel de anatomía de la fresadora Bridgeport">
      <path class="bp-panel-bg" d="M758 62H972Q980 62 980 70V466Q980 474 972 474H758Q750 474 750 466V70Q750 62 758 62Z"/>
      <path class="bp-panel-accent" d="M750 80H753V458H750Z"/>
      <text class="bp-panel-kicker" x="770" y="88">FRESADORA VERTICAL</text>
      <text class="bp-panel-title" x="770" y="106">BRIDGEPORT · ANATOMÍA</text>
      <path class="bp-panel-rule" d="M770 118H958"/>
      <text class="bp-panel-kicker" x="770" y="139">01 · CABEZAL / TRANSMISIÓN</text>
      <text class="bp-panel-detail" x="770" y="156">Motor · carcasa · husillo · pinza</text>
      <text class="bp-panel-detail" x="770" y="171">Fresa · caña · avance · bloqueo</text>
      <path class="bp-panel-rule" d="M770 182H958"/>
      <text class="bp-panel-kicker" x="770" y="202">02 · MESA / CARRO</text>
      <text class="bp-panel-detail" x="770" y="219">Mesa T · carro Y · volantes X</text>
      <text class="bp-panel-detail" x="770" y="234">Manivela Y · bloqueos de eje</text>
      <path class="bp-panel-rule" d="M770 245H958"/>
      <text class="bp-panel-kicker" x="770" y="265">03 · RODILLERA / EJE Z</text>
      <text class="bp-panel-detail" x="770" y="282">Rodillera · guías de columna</text>
      <text class="bp-panel-detail" x="770" y="297">Tornillo · pedestal · manivela Z</text>
      <path class="bp-panel-rule" d="M770 308H958"/>
      <text class="bp-panel-kicker" x="770" y="328">04 · BASE / COLUMNA</text>
      <text class="bp-panel-detail" x="770" y="345">Fundición · depósito de refrigerante</text>
      <path class="bp-panel-rule" d="M770 358H958"/>
      <text class="bp-panel-kicker" x="770" y="380">SERVICIO</text>
      <text class="bp-panel-detail" x="770" y="399">MECANIZADO Y FABRICACIÓN</text>
      <text class="bp-panel-detail" x="770" y="414">DE PIEZAS DE PRECISIÓN</text>
      <text class="bp-panel-detail" x="770" y="446">Pasa sobre los puntos para identificar partes</text>
    </g>`;
  }

  const [xdx, xdy] = k.axisVector(P, [3.5, 0, 0]);
  const [ydx, ydy] = k.axisVector(P, [0, 3.5, 0]);
  const [zdx, zdy] = k.axisVector(P, [0, 0, 2.5]);
  const fmt = (n) => (Math.abs(n) < 0.005 ? 0 : +n.toFixed(2));
  const axisCss = `<style>${k.ISO_CSS}
.bridgeport{--bp-accent:var(--anatomy-blue);--bp-rotor:#f0a43a}
.bridgeport .bp-axis-x{animation:bp-x 6.8s ease-in-out infinite}
.bridgeport .bp-axis-y{animation:bp-y 8.2s ease-in-out -1.6s infinite}
.bridgeport .bp-axis-z{animation:bp-z 9.6s ease-in-out -3.1s infinite}
.bridgeport #bp-spindle-index{transform-box:fill-box;transform-origin:center;animation:bp-spindle 1.7s linear infinite}
.bridgeport .bp-index-face{fill:var(--anatomy-shade-2);stroke:var(--anatomy-hi);stroke-width:.55}
.bridgeport .bp-index-ring{fill:none;stroke:var(--anatomy-mid);stroke-width:.55}
.bridgeport .bp-index-mark{fill:var(--bp-rotor);stroke:var(--anatomy-strong);stroke-width:.4}
.bridgeport .bp-panel-bg{fill:var(--anatomy-card);stroke:var(--anatomy-mid);stroke-width:.65}
.bridgeport .bp-panel-rule{stroke:var(--anatomy-faint);stroke-width:.65}
.bridgeport .bp-panel-accent{fill:var(--bp-accent)}
.bridgeport .bp-panel-kicker{fill:var(--anatomy-muted);font:9px var(--anatomy-mono);letter-spacing:.12em}
.bridgeport .bp-panel-title{fill:var(--anatomy-strong);font:10.5px var(--anatomy-mono);letter-spacing:.03em}
.bridgeport .bp-panel-detail{fill:var(--anatomy-text);font:8.7px var(--anatomy-mono)}
.bridgeport .bp-hotspot{cursor:help;outline:none}
.bridgeport .bp-hotspot-dot{fill:var(--anatomy-paper);stroke:var(--bp-accent);stroke-width:1.15}
.bridgeport .bp-hotspot-ring{fill:none;stroke:var(--bp-accent);stroke-width:.55;opacity:.72}
.bridgeport .bp-hotspot-tip{opacity:0;pointer-events:none;transition:opacity .14s ease}
.bridgeport .bp-hotspot-tip rect{fill:var(--anatomy-card);stroke:var(--anatomy-mid);stroke-width:.55}
.bridgeport .bp-hotspot-tip text{fill:var(--anatomy-strong);font:9px var(--anatomy-mono)}
.bridgeport .bp-hotspot:hover .bp-hotspot-dot,.bridgeport .bp-hotspot:focus .bp-hotspot-dot{fill:var(--bp-accent);stroke:var(--anatomy-strong)}
.bridgeport .bp-hotspot:hover .bp-hotspot-tip,.bridgeport .bp-hotspot:focus .bp-hotspot-tip{opacity:1}
@keyframes bp-x{0%,100%{transform:translate(0,0)}50%{transform:translate(${fmt(xdx)}px,${fmt(xdy)}px)}}
@keyframes bp-y{0%,100%{transform:translate(0,0)}50%{transform:translate(${fmt(ydx)}px,${fmt(ydy)}px)}}
@keyframes bp-z{0%,100%{transform:translate(0,0)}50%{transform:translate(${fmt(zdx)}px,${fmt(zdy)}px)}}
@keyframes bp-spindle{to{transform:rotate(360deg)}}
@media(prefers-reduced-motion:reduce){.bridgeport .bp-axis-x,.bridgeport .bp-axis-y,.bridgeport .bp-axis-z,.bridgeport #bp-spindle-index{animation:none}}
</style>`;

  const baseColumnHotspots = [
    ["base", "Base", "Fundición anclada al suelo", [0, 38, -5]],
    ["coolant", "Depósito de refrigerante", "Canal integrado en la base", [50, 29, 0]],
    ["column", "Columna", "Estructura rígida de fundición", [0, -48, 54]],
    ["ways", "Guías de columna", "Superficies rectificadas para el eje Z", [-24, -27, 50]],
    ["elevating-screw", "Tornillo elevador", "Eleva y desciende la rodillera", [0, -18, 12]],
    ["elevating-pedestal", "Pedestal de elevación", "Apoyo del tornillo elevador", [0, -18, 3]],
  ];
  const headHotspots = [
    ["motor", "Motor de accionamiento", "Motor vertical sobre la carcasa", [0, -22, 97]],
    ["head-casting", "Carcasa del cabezal", "Casting Bridgeport con placa frontal", [0, -4, 78]],
    ["spindle", "Husillo vertical", "Eje que gira y transmite potencia", [0, -5, 69]],
    ["collet", "Pinza de sujeción", "Sujeta el vástago de la fresa", [0, -5, 64]],
    ["quill-lever", "Palanca de avance de la caña", "Desplazamiento manual del husillo", [31, -5, 77]],
    ["fine-feed", "Volante de ajuste fino", "Avance preciso de la caña", [22, -5, 72]],
    ["spindle-lock", "Palanca de bloqueo", "Fija la posición vertical del husillo", [24, -5, 68]],
  ];
  const kneeHotspots = [
    ["knee", "Rodillera", "Carro vertical que soporta la mesa", [0, -20, 30]],
    ["knee-crank", "Manivela de elevación Z", "Sube y baja la rodillera", [0, 4, 28]],
  ];
  const saddleHotspots = [
    ["saddle", "Carro transversal Y", "Mueve la mesa hacia delante y atrás", [0, -8, 40]],
    ["saddle-crank", "Manivela Y", "Avance transversal del carro", [0, 22, 40]],
    ["saddle-lock", "Bloqueo del carro", "Fija el eje Y", [47, 10, 42]],
  ];
  const tableHotspots = [
    ["table", "Mesa de trabajo", "Superficie con tres ranuras T", [0, 12, 48]],
    ["x-left", "Volante X izquierdo", "Avance lateral de la mesa", [-65, -6, 46]],
    ["x-right", "Volante X derecho", "Avance lateral de la mesa", [65, -6, 46]],
    ["table-lock", "Bloqueo de mesa", "Fija el eje X", [54, 12, 47]],
    ["vise", "Mordaza", "Sujeta la pieza sobre la mesa", [31, -4, 56]],
    ["piece", "Pieza mecanizada", "Pieza sujeta con canal fresado", [18, 1, 59]],
  ];
  const toolHotspots = [
    ["end-mill", "Fresa de extremo", "Herramienta vertical que mecaniza el canal", [0, -5, 59]],
    ["spindle-rotation", "Rotación del husillo", "Indicador solidario al eje vertical", [0, TOOL_Y, SLOT_FLOOR]],
  ];

  function hotpoints(items) {
    return items.map(([id, label, detail, point]) => {
      const [x, y] = at(point);
      const width = Math.min(156, Math.max(82, label.length * 5.8 + 12));
      const dx = x > 650 ? -width - 8 : 8;
      const name = esc(label), accessible = esc(`${label}: ${detail}`);
      return `<g id="bp-hotspot-${id}" class="bp-hotspot" data-layer="interactive-labels" tabindex="0" role="button" aria-label="${accessible}">
        <title>${accessible}</title>
        <circle class="bp-hotspot-ring" cx="${x.toFixed(1)}" cy="${y.toFixed(1)}" r="5.4"/>
        <circle class="bp-hotspot-dot" cx="${x.toFixed(1)}" cy="${y.toFixed(1)}" r="2.3"/>
        <g class="bp-hotspot-tip" transform="translate(${(x + dx).toFixed(1)} ${(y - 11).toFixed(1)})">
          <rect x="0" y="-10" width="${width}" height="17" rx="3"/><text x="6" y="1.5">${name}</text>
        </g>
      </g>`;
    }).join("");
  }

  function wheel(center, axis, radius = 6) {
    const F = G.frameAlong(center, axis);
    const out = [S(G.disc(-1.3, 1.3, radius, F, P, { steps: 28 }), { tone: "mid" })];
    out.push(L(G.circleOf(F, 1.3, radius - 0.9, P), { tone: "lo" }));
    for (let i = 0; i < 5; i++) {
      const a = (i / 5) * Math.PI * 2, da = 0.09;
      const poly = [
        G.pointOf(F, 1.32, 2.1, a - da), G.pointOf(F, 1.32, radius - 1, a - da * 0.55),
        G.pointOf(F, 1.32, radius - 1, a + da * 0.55), G.pointOf(F, 1.32, 2.1, a + da),
      ].map(at);
      out.push(fill(k.pathOf(poly, true), "var(--anatomy-shade-3)"));
    }
    out.push(S(G.disc(1.3, 2.6, 2.2, F, P, { steps: 20 }), { tone: "hi" }));
    out.push(S(G.sphereOf(G.pointOf(F, 3.2, radius - 0.8, 0), 1.2, P, { steps: 18 }), { tone: "hi" }));
    return out.join("");
  }

  // Compose four named mechanical systems and preserve actual back-to-front order.
  const xMotion = `<g id="bp-axis-x" class="bp-axis-x"><g id="bp-table-x-assembly" data-layer="x-table">${tableAndXControls()}${viseAndPiece()}${hotpoints(tableHotspots)}</g></g>`;
  const tableSaddleSystem = `<g id="bp-system-table-saddle" data-system="Work Table and Saddle Assembly"><g id="bp-axis-y" class="bp-axis-y"><g id="bp-saddle" data-layer="saddle">${saddleAndYControls()}${hotpoints(saddleHotspots)}</g>${xMotion}</g>${hotpoints([["table-saddle-node", "Mesa y carro", "Desplazamiento transversal Y y longitudinal X", [0, -26, 44]]])}</g>`;
  const zMotion = `<g id="bp-axis-z" class="bp-axis-z"><g id="bp-knee" data-layer="knee">${kneeSystemParts()}${hotpoints(kneeHotspots)}</g>${tableSaddleSystem}</g>`;

  const body = [
    `<style>${axisCss}</style>`,
    `<g id="bridgeport-vertical-root" data-node="Milling Machine - Vertical">`,
    `<g id="bp-system-base-column" data-system="Base and Column">${baseColumn()}${hotpoints(baseColumnHotspots)}</g>`,
    `<g id="bp-system-head-drive" data-system="Spindle Head Assembly">${headDrive()}${hotpoints(headHotspots)}</g>`,
    `<g id="bp-system-knee-z" data-system="Knee and Elevating System">${zMotion}</g>`,
    `<g id="bp-spindle-tool" data-system-ref="Spindle Head Assembly">${fresaAndHotspots()}${hotpoints(toolHotspots)}</g>`,
    servicePanel(),
    `</g>`,
  ].join("");

  return k.figureSvg({
    width: WIDTH,
    height: HEIGHT,
    className: "iso bridgeport",
    standalone: "dark",
    label: "Fresadora vertical de columna y rodillera estilo Bridgeport: motor, cabezal, caña, husillo vertical descendente, pinza y fresa de extremo sobre una pieza con canal fresado en una mordaza. La mesa se desplaza en X, el carro en Y y la rodillera en Z; puntos etiquetados identifican mandos y componentes.",
    body,
  });
}
