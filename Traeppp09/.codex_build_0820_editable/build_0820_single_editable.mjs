import fs from 'fs';
import path from 'path';
import PptxGenJS from '/Users/yuwenjie/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/pptxgenjs/dist/pptxgen.cjs.js';

const base = '/Users/yuwenjie/Documents/Traeppp08/.codex_build_0820_editable';
const data = JSON.parse(fs.readFileSync(path.join(base, 'drawio.json'), 'utf8'));
const diag = data.find(d => d.name === '01 总览') || data[0];
const out = '/Users/yuwenjie/Documents/Traeppp08/汇报0818/0820_蓝图_可编辑版_单页.pptx';

const cells = new Map(diag.cells.map(c => [c.id, c]));
const cache = new Map();
function absPos(id) {
  if (cache.has(id)) return cache.get(id);
  const c = cells.get(id);
  const g = c?.geometry || {};
  let x = Number(g.x || 0);
  let y = Number(g.y || 0);
  const p = c?.parent;
  if (p && p !== '0' && p !== '1' && cells.has(p)) {
    const pp = absPos(p);
    x += pp.x;
    y += pp.y;
  }
  const v = { x, y, w: Number(g.width || 0), h: Number(g.height || 0) };
  cache.set(id, v);
  return v;
}

let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
for (const c of diag.cells) {
  if (!c.vertex) continue;
  const g = absPos(c.id);
  minX = Math.min(minX, g.x);
  minY = Math.min(minY, g.y);
  maxX = Math.max(maxX, g.x + g.w);
  maxY = Math.max(maxY, g.y + g.h);
}
const pad = 8;
minX -= pad; minY -= pad; maxX += pad; maxY += pad;
const pageW = maxX - minX;
const pageH = maxY - minY;

const pptx = new PptxGenJS();
pptx.defineLayout({ name: 'BLUEPRINT', width: 13.333, height: 13.333 * (pageH / pageW) });
pptx.layout = 'BLUEPRINT';
pptx.author = 'Codex';
pptx.company = 'OpenAI';
pptx.subject = 'Draw.io editable blueprint';
pptx.title = '0820 蓝图 可编辑单页';
pptx.lang = 'zh-CN';

const SW = 13.333;
const SH = 13.333 * (pageH / pageW);
const SX = SW / pageW;
const SY = SH / pageH;
const tx = -minX;
const ty = -minY;

function sx(v) { return +(v * SX).toFixed(4); }
function sy(v) { return +(v * SY).toFixed(4); }
function n(v, d = 0) { const x = Number(v); return Number.isFinite(x) ? x : d; }
function sm(style) {
  const o = {};
  for (const p of (style || '').split(';')) {
    if (!p || !p.includes('=')) continue;
    const [k, ...r] = p.split('=');
    o[k] = r.join('=');
  }
  return o;
}
function hex(v, f = '000000') {
  if (!v || v === 'default') return f;
  if (v === 'none') return null;
  return v.replace('#', '').toUpperCase();
}
function text(v) {
  if (!v) return '';
  return String(v).replace(/<br\s*\/?>/gi, '\n').replace(/<[^>]+>/g, '').replace(/\r/g, '').trim();
}
function isBold(v, style) {
  return /<b>/i.test(v || '') || (n(sm(style).fontStyle, 0) & 1);
}
function shapeFor(style) {
  const s = sm(style);
  if (s.shape === 'mxgraph.flowchart.database') return pptx.ShapeType.can;
  if (s.shape === 'cylinder3') return pptx.ShapeType.can;
  if (s.shape === 'singleArrow') return n(s.rotation, 0) < 0 ? pptx.ShapeType.downArrow : pptx.ShapeType.upArrow;
  if (style.includes('swimlane')) return pptx.ShapeType.roundRect;
  if (s.ellipse === '1' || s.shape === 'ellipse') return pptx.ShapeType.ellipse;
  if (style.includes('rounded=1')) return pptx.ShapeType.roundRect;
  return pptx.ShapeType.rect;
}

function addCell(slide, c) {
  const g = absPos(c.id);
  const x = sx(g.x + tx);
  const y = sy(g.y + ty);
  const w = sx(g.w);
  const h = sy(g.h);
  const s = sm(c.style);
  const fill = hex(s.fillColor);
  const stroke = hex(s.strokeColor);
  const opacity = s.opacity ? Math.max(0, 100 - n(s.opacity, 100)) : 0;
  const opts = {
    x, y, w, h,
    line: stroke ? { color: stroke, pt: Math.max(0.5, n(s.strokeWidth, 1) / 2) } : { color: 'FFFFFF', transparency: 100, pt: 0 },
    fill: fill ? { color: fill, transparency: opacity } : { color: 'FFFFFF', transparency: 100 },
  };
  const t = text(c.value);
  if (!t) {
    if (fill || stroke) slide.addShape(shapeFor(c.style), opts);
    return;
  }
  const textOpts = {
    fontFace: s.fontFamily || 'Microsoft YaHei',
    fontSize: n(s.fontSize, 12) * 0.8,
    color: hex(s.fontColor, '000000'),
    bold: isBold(c.value, c.style),
    align: s.align || 'center',
    valign: s.verticalAlign === 'top' ? 'top' : s.verticalAlign === 'bottom' ? 'bottom' : 'mid',
    margin: 0.02,
    fit: 'shrink',
    breakLine: false,
  };
  if (!fill && !stroke) {
    slide.addText(t, { x, y, w, h, ...textOpts });
    return;
  }
  slide.addShape(shapeFor(c.style), { ...opts, text: t, ...textOpts });
}

function center(c) {
  const g = absPos(c.id);
  return { x: sx(g.x + tx + g.w / 2), y: sy(g.y + ty + g.h / 2) };
}

function addEdge(slide, c) {
  const src = cells.get(c.source);
  const tgt = cells.get(c.target);
  if (!src || !tgt) return;
  const a = center(src), b = center(tgt);
  const x = Math.min(a.x, b.x);
  const y = Math.min(a.y, b.y);
  const w = Math.max(Math.abs(a.x - b.x), 0.01);
  const h = Math.max(Math.abs(a.y - b.y), 0.01);
  slide.addShape((a.x - b.x) * (a.y - b.y) >= 0 ? pptx.ShapeType.line : pptx.ShapeType.lineInv, {
    x, y, w, h,
    line: { color: hex(sm(c.style).strokeColor, '000000'), pt: 1, endArrowType: 'triangle' },
  });
}

const slide = pptx.addSlide();
slide.background = { color: hex(diag.background, 'F8FAFC') };
for (const c of diag.cells) if (c.vertex) addCell(slide, c);
for (const c of diag.cells) if (c.edge) addEdge(slide, c);

await pptx.writeFile({ fileName: out });
console.log(out);
