import fs from 'fs';
import path from 'path';
import PptxGenJS from '/Users/yuwenjie/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/pptxgenjs/dist/pptxgen.cjs.js';

const base = '/Users/yuwenjie/Documents/Traeppp08/.codex_build_0820_editable';
const data = JSON.parse(fs.readFileSync(path.join(base, 'drawio.json'), 'utf8'));
const diag = data.find(d => d.name === '01 总览') || data[0];
const out = '/Users/yuwenjie/Documents/Traeppp08/汇报0818/0820_蓝图_共享烤房_可编辑版.pptx';

const overrides = new Map([
  ['Cdiq-YqZMxSvyfJLaPm9-37', '三大业务'],
  ['jxCOpvoT1MxucT_nHBN7-34', '烟农 / 预约分配・临时调剂\n提报维修・评价反馈'],
  ['jxCOpvoT1MxucT_nHBN7-35', '合作社/技术员 / 预约管理・日常巡检\n维修工单响应・验收闭环'],
  ['jxCOpvoT1MxucT_nHBN7-37', '管理者 / 维修决策・资金投入\n新建决策・查收反馈'],
  ['jxCOpvoT1MxucT_nHBN7-38', '用户'],
  ['jX7jUDlCh9Qc9EDUexrO-83', '应用层'],
  ['jX7jUDlCh9Qc9EDUexrO-40', '微信小程序'],
  ['muDalQN7EUF30YjKA5s0-39', '业务中台（PC端）'],
  ['muDalQN7EUF30YjKA5s0-37', '预警研判'],
  ['muDalQN7EUF30YjKA5s0-36', 'AI助手'],
  ['muDalQN7EUF30YjKA5s0-38', '绩效分析\n评价回流'],
  ['muDalQN7EUF30YjKA5s0-35', '综合报表'],
  ['Cdiq-YqZMxSvyfJLaPm9-49', '烤房调剂'],
  ['jX7jUDlCh9Qc9EDUexrO-37', '发起预约\n应急调剂'],
  ['Cdiq-YqZMxSvyfJLaPm9-52', '烤房调剂推荐算法'],
  ['jX7jUDlCh9Qc9EDUexrO-35', '使用状态・容量・距离・健康度\n多因子智能推荐'],
  ['jX7jUDlCh9Qc9EDUexrO-36', '冲突防重\n到期释放\n烟农评价'],
  ['jX7jUDlCh9Qc9EDUexrO-71', '烤房管护'],
  ['7URzI2JP4421-KiuUnfn-115', '主动服务'],
  ['7URzI2JP4421-KiuUnfn-116', '报修'],
  ['7URzI2JP4421-KiuUnfn-118', '一键报修'],
  ['7URzI2JP4421-KiuUnfn-155', '人货匹配算法\n维修联系人/备件库'],
  ['jX7jUDlCh9Qc9EDUexrO-73', '查看维修进度'],
  ['7URzI2JP4421-KiuUnfn-156', '维修完成'],
  ['jX7jUDlCh9Qc9EDUexrO-75', '健康分算法\n寿命预测算法'],
  ['jX7jUDlCh9Qc9EDUexrO-72', '紧迫性优先级\n安排维修'],
  ['7URzI2JP4421-KiuUnfn-167', '维修完成'],
  ['7URzI2JP4421-KiuUnfn-30', '评价回流'],
  ['7URzI2JP4421-KiuUnfn-32', '总体'],
  ['7URzI2JP4421-KiuUnfn-33', '烟农'],
  ['7URzI2JP4421-KiuUnfn-54', '设施设备'],
  ['7URzI2JP4421-KiuUnfn-55', '服务'],
  ['7URzI2JP4421-KiuUnfn-61', '资金投入'],
  ['7URzI2JP4421-KiuUnfn-62', '维修部件'],
  ['7URzI2JP4421-KiuUnfn-64', '规划选址'],
  ['jX7jUDlCh9Qc9EDUexrO-76', '维修决策'],
  ['jX7jUDlCh9Qc9EDUexrO-77', '健康分算法\n& 寿命预测算法'],
  ['jX7jUDlCh9Qc9EDUexrO-80', '烤房负载率算法\n村级新建沙盘推演'],
  ['jX7jUDlCh9Qc9EDUexrO-78', '维修优先级推荐\n部件ROI测算'],
  ['jX7jUDlCh9Qc9EDUexrO-79', '新建点位建议\n5的倍数规划'],
  ['jX7jUDlCh9Qc9EDUexrO-31', '智能投建'],
  ['7-jdDMCfGzeXBmtqDvE1-29', '资源共享'],
  ['7-jdDMCfGzeXBmtqDvE1-26', '共享调度'],
  ['7-jdDMCfGzeXBmtqDvE1-27', '智能管护'],
  ['7-jdDMCfGzeXBmtqDvE1-28', '投建决策'],
  ['7-jdDMCfGzeXBmtqDvE1-31', '未来演进'],
  ['7URzI2JP4421-KiuUnfn-7', '低代码平台'],
  ['7URzI2JP4421-KiuUnfn-8', '物联网传感器'],
  ['7URzI2JP4421-KiuUnfn-9', '表单导入'],
  ['n1vBywr-MDB_Wz8ghvrR-1', '烤房基础信息'],
  ['7URzI2JP4421-KiuUnfn-6', '维修记录'],
  ['jxCOpvoT1MxucT_nHBN7-39', '数据采集层'],
]);

const cells = diag.cells.map(c => {
  const v = overrides.has(c.id) ? overrides.get(c.id) : c.value;
  return { ...c, value: v };
});
const cellMap = new Map(cells.map(c => [c.id, c]));

const cache = new Map();
function absPos(id) {
  if (cache.has(id)) return cache.get(id);
  const c = cellMap.get(id);
  const g = c?.geometry || {};
  let x = Number(g.x || 0);
  let y = Number(g.y || 0);
  const p = c?.parent;
  if (p && p !== '0' && p !== '1' && cellMap.has(p)) {
    const pp = absPos(p);
    x += pp.x;
    y += pp.y;
  }
  const v = { x, y, w: Number(g.width || 0), h: Number(g.height || 0) };
  cache.set(id, v);
  return v;
}

let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
for (const c of cells) {
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
pptx.subject = 'Shared baking room editable blueprint';
pptx.title = '共享烤房蓝图 可编辑版';
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
function clean(v) {
  if (!v) return '';
  return String(v).replace(/<br\s*\/?>/gi, '\n').replace(/<[^>]+>/g, '').replace(/\r/g, '').trim();
}
function isBold(v, style) {
  return /<b>/i.test(String(v || '')) || (n(sm(style).fontStyle, 0) & 1);
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
  const t = clean(c.value);
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
  slide.addShape(shapeFor(c.style), opts);
  slide.addText(t, { x, y, w, h, ...textOpts });
}

function center(c) {
  const g = absPos(c.id);
  return { x: sx(g.x + tx + g.w / 2), y: sy(g.y + ty + g.h / 2) };
}

function addEdge(slide, c) {
  const src = cellMap.get(c.source);
  const tgt = cellMap.get(c.target);
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
for (const c of cells) if (c.vertex) addCell(slide, c);
for (const c of cells) if (c.edge) addEdge(slide, c);

// Small overlay callouts to better match the new story while staying editable.
slide.addText('数智解忧：智享、智管、智投', {
  x: 0.85, y: 0.18, w: 4.2, h: 0.28,
  fontFace: 'Microsoft YaHei', fontSize: 13, bold: true, color: '2F5E3F',
  fill: { color: 'FFFFFF', transparency: 100 }, line: { color: 'FFFFFF', transparency: 100 },
});
slide.addText('让每一座烤房都在线', {
  x: 10.55, y: 0.18, w: 1.95, h: 0.26,
  fontFace: 'Microsoft YaHei', fontSize: 11, bold: true, color: '6D8764',
  align: 'right', fill: { color: 'FFFFFF', transparency: 100 }, line: { color: 'FFFFFF', transparency: 100 },
});

await pptx.writeFile({ fileName: out });
console.log(out);
