import fs from 'fs';
import path from 'path';
import PptxGenJS from '/Users/yuwenjie/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/pptxgenjs/dist/pptxgen.cjs.js';

const base = '/Users/yuwenjie/Documents/Traeppp08/.codex_build_0820_editable';
const data = JSON.parse(fs.readFileSync(path.join(base, 'drawio.json'), 'utf8'));
const out = '/Users/yuwenjie/Documents/Traeppp08/汇报0818/0820_蓝图_可编辑版.pptx';

const pptx = new PptxGenJS();
pptx.layout = 'LAYOUT_WIDE';
pptx.author = 'Codex';
pptx.company = 'OpenAI';
pptx.subject = 'Draw.io editable blueprint';
pptx.title = '0820 蓝图 可编辑版';
pptx.lang = 'zh-CN';

const SW = 13.333;
const SH = 7.5;
const PX = 1920;
const PY = 1080;
const SX = SW / PX;
const SY = SH / PY;

function scaleX(v) { return +(v * SX).toFixed(4); }
function scaleY(v) { return +(v * SY).toFixed(4); }

function escText(v) {
  return (v || '').replace(/\r/g, '').trim();
}

function styleMap(style) {
  const o = {};
  for (const p of (style || '').split(';')) {
    if (!p || !p.includes('=')) continue;
    const [k, ...rest] = p.split('=');
    o[k] = rest.join('=');
  }
  return o;
}

function hex(v, fallback = '000000') {
  if (!v || v === 'default' || v === 'none') return fallback;
  return v.replace('#', '').toUpperCase();
}

function num(v, fallback = 0) {
  const n = Number(v);
  return Number.isFinite(n) ? n : fallback;
}

function shapeFor(style) {
  const s = styleMap(style);
  const shape = s.shape || '';
  if (shape === 'parallelogram') return pptx.ShapeType.parallelogram;
  if (shape === 'cylinder3' || shape === 'mxgraph.flowchart.database') return pptx.ShapeType.can;
  if (shape === 'singleArrow') return (s.rotation && Number(s.rotation) < 0) ? pptx.ShapeType.downArrow : pptx.ShapeType.upArrow;
  if (s.swimlane !== undefined || style.includes('swimlane')) return pptx.ShapeType.roundRect;
  if (s.ellipse === '1' || shape === 'ellipse') return pptx.ShapeType.ellipse;
  if (style.includes('rounded=1')) return pptx.ShapeType.roundRect;
  return pptx.ShapeType.rect;
}

function textOpts(cell) {
  const s = cell.styleMap;
  const fs = num(s.fontSize, 12);
  const fontStyle = num(s.fontStyle, 0);
  return {
    fontFace: s.fontFamily || 'Microsoft YaHei',
    fontSize: fs,
    color: hex(s.fontColor, '000000'),
    bold: !!(fontStyle & 1),
    italic: !!(fontStyle & 2),
    align: (s.align || 'center'),
    valign: (s.verticalAlign === 'top' ? 'top' : s.verticalAlign === 'bottom' ? 'bottom' : 'mid'),
    margin: 0.03,
    fit: 'shrink',
    breakLine: false,
  };
}

function addCell(slide, cell) {
  const g = cell.geometry || {};
  if (g.width == null || g.height == null) return;
  const x = scaleX(g.x || 0);
  const y = scaleY(g.y || 0);
  const w = scaleX(g.width || 0);
  const h = scaleY(g.height || 0);
  const s = cell.styleMap;
  const fillColor = hex(s.fillColor, null);
  const strokeColor = hex(s.strokeColor, null);
  const opacity = s.opacity ? num(s.opacity, 100) : null;
  const hasFill = fillColor && fillColor !== 'NONE';
  const hasStroke = strokeColor && strokeColor !== 'NONE';
  const txt = escText(cell.text);
  const opts = {
    x, y, w, h,
    line: hasStroke ? { color: strokeColor, pt: Math.max(0.5, num(s.strokeWidth, 1) / 2) } : { color: 'FFFFFF', transparency: 100, pt: 0 },
  };
  if (hasFill) {
    opts.fill = { color: fillColor, transparency: opacity == null ? 0 : Math.max(0, 100 - opacity) };
  } else {
    opts.fill = { color: 'FFFFFF', transparency: 100 };
  }
  const shape = shapeFor(cell.style);
  if (!txt && !hasFill && !hasStroke) return;
  if (!txt) {
    slide.addShape(shape, opts);
    return;
  }
  const t = textOpts(cell);
  if (!hasFill && !hasStroke) {
    slide.addText(txt, { x, y, w, h, ...t });
    return;
  }
  slide.addShape(shape, { ...opts, text: txt, ...t });
}

function center(cell) {
  const g = cell.geometry || {};
  return { cx: scaleX((g.x || 0) + (g.width || 0) / 2), cy: scaleY((g.y || 0) + (g.height || 0) / 2) };
}

function addEdge(slide, cell, map) {
  const src = map.get(cell.source);
  const tgt = map.get(cell.target);
  if (!src || !tgt) return;
  const p1 = center(src), p2 = center(tgt);
  const dx = p2.cx - p1.cx, dy = p2.cy - p1.cy;
  const x = Math.min(p1.cx, p2.cx), y = Math.min(p1.cy, p2.cy), w = Math.abs(dx), h = Math.abs(dy);
  const shape = (dx * dy >= 0) ? pptx.ShapeType.line : pptx.ShapeType.lineInv;
  slide.addShape(shape, {
    x, y, w: Math.max(w, 0.01), h: Math.max(h, 0.01),
    line: {
      color: hex(styleMap(cell.style).strokeColor, '000000'),
      pt: 1,
      endArrowType: 'triangle',
      beginArrowType: 'none',
    }
  });
}

for (const diag of data) {
  const slide = pptx.addSlide();
  slide.background = { color: hex(diag.background, 'F8FAFC') };
  const map = new Map();
  for (const c of diag.cells) map.set(c.id, c);
  for (const c of diag.cells) if (c.vertex) addCell(slide, c);
  for (const c of diag.cells) if (c.edge) addEdge(slide, c, map);
  // Keep empty pages visible for fidelity.
  if (diag.cells.filter(c => c.vertex || c.edge).length === 0) {
    slide.addText(diag.name || '空白页', { x: 0.4, y: 0.4, w: 2, h: 0.3, fontFace: 'Microsoft YaHei', fontSize: 14, color: '666666' });
  }
}

await pptx.writeFile({ fileName: out });
console.log(out);
