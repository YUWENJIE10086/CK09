import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { Presentation, PresentationFile } from "@oai/artifact-tool";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const ROOT = "/Users/yuwenjie/Documents/Traeppp08/汇报0818";
const OUT = path.join(ROOT, "0823_新增页.pptx");
const WORK = __dirname;

const ASSETS = {
  shot18: path.join(ROOT, "0823/slide-18.png"),
  shot19: path.join(ROOT, "0823/slide-19.png"),
  shot20: path.join(ROOT, "0823/slide-20.png"),
};

const FONT = "PingFang SC";
const C = {
  bg: "#f8fbf7",
  paper: "#ffffff",
  paper2: "#f3f8f1",
  line: "#d8e6d5",
  text: "#102015",
  sub: "#4b5d4f",
  muted: "#6c7a6f",
  green: "#2f6f46",
  green2: "#e4f3e7",
  darkGreen: "#17452d",
  orange: "#f28c28",
  orange2: "#fff1de",
  blue: "#3b82f6",
  blue2: "#e3efff",
  red: "#ef4444",
  red2: "#ffe3e3",
  purple: "#8b5cf6",
  purple2: "#efe6ff",
  gold: "#b8860b",
  gold2: "#fff4d8",
  mint: "#14b8a6",
  mint2: "#dcfbf7",
};

const theme = {
  name: "Shared Baking Room Addendum",
  themeColors: {
    accent1: C.green,
    accent2: C.orange,
    accent3: C.blue,
    accent4: C.mint,
    accent5: C.purple,
    accent6: C.red,
    bg1: "#ffffff",
    bg2: C.bg,
    tx1: C.text,
    tx2: C.sub,
    dk1: "#000000",
    dk2: "#1f2937",
    lt1: "#ffffff",
    lt2: "#e2e8f0",
    hlink: C.blue,
    folHlink: C.purple,
  },
};

async function readImage(file) {
  return await fs.readFile(file);
}

function notes(text, sources = []) {
  return [
    "【讲解稿】",
    text.trim(),
    ...(sources.length ? ["", "【Sources】", ...sources.map((s) => `- ${s}`)] : []),
  ].join("\n");
}

function addText(slide, { x, y, w, h, text, size = 18, color = C.text, bold = false, align = "left", name }) {
  const t = slide.shapes.add({
    geometry: "textbox",
    name,
    position: { left: x, top: y, width: w, height: h },
    fill: "none",
    line: { style: "solid", fill: "none", width: 0 },
  });
  t.text = text;
  t.text.style = { fontSize: size, color, bold, typeface: FONT, alignment: align };
  return t;
}

function addBox(slide, { x, y, w, h, fill = C.paper, line = C.line, radius = "rounded-2xl", shadow = "shadow-sm", text, size = 18, color = C.text, bold = false, align = "left", geometry = "roundRect", name }) {
  const s = slide.shapes.add({
    geometry,
    name,
    position: { left: x, top: y, width: w, height: h },
    fill,
    line: { style: "solid", fill: line, width: line === "none" ? 0 : 1 },
    borderRadius: radius,
    shadow,
  });
  if (text !== undefined) {
    s.text = text;
    s.text.style = { fontSize: size, color, bold, typeface: FONT, alignment: align };
  }
  return s;
}

function addPill(slide, { x, y, w, h, text, fill, color = C.text, border = fill, size = 14, name }) {
  return addBox(slide, {
    x,
    y,
    w,
    h,
    fill,
    line: border,
    radius: "rounded-full",
    text,
    size,
    color,
    bold: true,
    align: "center",
    name,
  });
}

function addHeader(slide, idx, title, subtitle, accent = C.green) {
  addBox(slide, {
    x: 46,
    y: 26,
    w: 12,
    h: 34,
    fill: accent,
    line: accent,
    radius: "rounded-full",
    shadow: "shadow-none",
    geometry: "rect",
  });
  addPill(slide, {
    x: 68,
    y: 28,
    w: 100,
    h: 24,
    text: `0${idx}`,
    fill: `${accent}16`,
    color: accent,
    border: `${accent}24`,
    size: 12,
  });
  addText(slide, {
    x: 180,
    y: 22,
    w: 600,
    h: 30,
    text: title,
    size: 22,
    bold: true,
    color: C.text,
    name: `title-${idx}`,
  });
  if (subtitle) {
    addText(slide, {
      x: 180,
      y: 46,
      w: 700,
      h: 18,
      text: subtitle,
      size: 10,
      color: C.muted,
      name: `subtitle-${idx}`,
    });
  }
  addBox(slide, {
    x: 46,
    y: 72,
    w: 868,
    h: 2,
    fill: accent,
    line: accent,
    radius: 0,
    shadow: "shadow-none",
    geometry: "rect",
  });
}

function addFooter(slide, page) {
  addText(slide, {
    x: 46,
    y: 514,
    w: 250,
    h: 16,
    text: "共享烤房 · 建用管一体化数智协同平台",
    size: 9,
    color: C.muted,
  });
  addText(slide, {
    x: 878,
    y: 514,
    w: 30,
    h: 16,
    text: String(page).padStart(2, "0"),
    size: 9,
    color: C.muted,
    align: "right",
  });
}

function addCard(slide, { x, y, w, h, title, body, accent, fill = C.paper, titleFill, titleColor = C.text }) {
  addBox(slide, {
    x,
    y,
    w,
    h,
    fill,
    line: accent,
    radius: "rounded-2xl",
    shadow: "shadow-sm",
  });
  addPill(slide, {
    x: x + 16,
    y: y + 14,
    w: 84,
    h: 24,
    text: title,
    fill: titleFill || `${accent}16`,
    color: titleColor || accent,
    border: `${accent}26`,
    size: 13,
  });
  addText(slide, {
    x: x + 16,
    y: y + 48,
    w: w - 32,
    h: h - 60,
    text: body,
    size: 14,
    color: C.sub,
  });
}

function addArrow(slide, x, y, w, h, fill = C.green) {
  return slide.shapes.add({
    geometry: "rightArrow",
    position: { left: x, top: y, width: w, height: h },
    fill,
    line: { style: "solid", fill, width: 1 },
    shadow: "shadow-none",
  });
}

function addMiniMetric(slide, { x, y, w, h, value, label, accent, valueSize = 18, labelSize = 10 }) {
  addBox(slide, { x, y, w, h, fill: C.paper, line: accent, radius: "rounded-xl", shadow: "shadow-sm" });
  addText(slide, { x: x + 10, y: y + 12, w: w - 20, h: 24, text: value, size: valueSize, bold: true, color: accent, align: "center" });
  addText(slide, { x: x + 10, y: y + 36, w: w - 20, h: 16, text: label, size: labelSize, color: C.sub, align: "center" });
}

async function main() {
  await fs.mkdir(WORK, { recursive: true });
  const shot18 = await readImage(ASSETS.shot18);
  const shot19 = await readImage(ASSETS.shot19);
  const shot20 = await readImage(ASSETS.shot20);

  const presentation = Presentation.create({ slideSize: { width: 960, height: 540 } });
  presentation.theme.colorScheme = theme;

  // Slide 4
  {
    const slide = presentation.slides.add();
    slide.background.fill = C.bg;
    addHeader(slide, 4, "烟农对烤房的三大“忧”", "从烟农视角看，问题首先体现在“借不到、修不准、问不出”", C.green);

    const cards = [
      { x: 44, accent: C.red, num: "01", title: "想借借不到", body: "高峰时期烤房抢不到、问不到、等不到。\n有闲置的烤房心里没底，想调剂周转又不知道找谁协调。\n结果：明明有资源，却用不上。", tag: "借不到" },
      { x: 337, accent: C.orange, num: "02", title: "想修找不准", body: "平时没人检查故障，烤房出问题了不知道该找谁修。\n多久能修好、修到哪里了、修得对不对，也没有清晰反馈。\n结果：问题来了，处置链条却断了。", tag: "找不准" },
      { x: 630, accent: C.blue, num: "03", title: "想问问不出", body: "“我们村烤房不够还没新的用，他们村烤房空闲的多还在新建。”\n烤房该修不该建、修不了才该建，反馈给谁、谁来处理都不清楚。\n结果：参与感弱，公平性也存疑。", tag: "问不出" },
    ];

    for (const c of cards) {
      addBox(slide, { x: c.x, y: 116, w: 266, h: 318, fill: C.paper, line: c.accent, radius: "rounded-2xl", shadow: "shadow-md" });
      addBox(slide, { x: c.x + 16, y: 130, w: 32, h: 32, fill: c.accent, line: c.accent, radius: "rounded-full", text: c.num, size: 14, color: "#fff", bold: true, align: "center" });
      addText(slide, { x: c.x + 60, y: 132, w: 180, h: 24, text: c.title, size: 18, bold: true, color: C.text });
      addText(slide, { x: c.x + 18, y: 178, w: 230, h: 174, text: c.body, size: 14, color: C.sub });
      addPill(slide, { x: c.x + 80, y: 374, w: 104, h: 26, text: c.tag, fill: `${c.accent}14`, color: c.accent, border: `${c.accent}30`, size: 13 });
    }
    addBox(slide, { x: 44, y: 454, w: 852, h: 44, fill: C.darkGreen, line: C.darkGreen, radius: "rounded-full", shadow: "shadow-none", text: "三大“忧”看起来是烟农的烦恼，实质上指向的是共享资源、设备管护和投资决策三条管理链路。", size: 15, color: "#fff", bold: true, align: "center" });
    addFooter(slide, 4);
    slide.speakerNotes.textFrame.setText(notes("先从烟农最直观的三种感受切入：借不到、修不准、问不出。讲的时候不需要过多展开技术，重点是让评委一听就知道，问题不是单点故障，而是资源、运维、决策三条链一起卡住了。", []));
    slide.speakerNotes.setVisible(true);
  }

  // Slide 5
  {
    const slide = presentation.slides.add();
    slide.background.fill = C.bg;
    addHeader(slide, 5, "从三大“忧”到三大管理痛点", "把烟农感受翻译成管理问题，再把管理问题翻译成平台命题", C.green);

    const rows = [
      { y: 128, left: "想借借不到", mid: "资源不互通", right: "供需信息割裂\n高峰期供需错配\n闲置资源看不见，无法高效利用", color: C.red },
      { y: 242, left: "想修找不准", mid: "管护不精准", right: "运维靠人工摸排\n缺乏主动预警和报修反馈", color: C.orange },
      { y: 356, left: "想问问不出", mid: "决策不科学", right: "投入凭经验拍板\n缺少数据闭环\n无科学依据", color: C.blue },
    ];

    for (const row of rows) {
      addBox(slide, { x: 44, y: row.y, w: 160, h: 80, fill: "#fff", line: row.color, radius: "rounded-2xl", shadow: "shadow-sm", text: row.left, size: 16, color: row.color, bold: true, align: "center" });
      addArrow(slide, 214, row.y + 21, 40, 28, row.color);
      addBox(slide, { x: 264, y: row.y, w: 160, h: 80, fill: `${row.color}10`, line: row.color, radius: "rounded-2xl", shadow: "shadow-sm", text: row.mid, size: 18, color: row.color, bold: true, align: "center" });
      addArrow(slide, 434, row.y + 21, 40, 28, C.green);
      addBox(slide, { x: 474, y: row.y, w: 390, h: 80, fill: "#fff", line: row.color, radius: "rounded-2xl", shadow: "shadow-sm" });
      addText(slide, { x: 494, y: row.y + 14, w: 350, h: 50, text: row.right, size: 15, color: C.sub, bold: false });
    }

    addBox(slide, { x: 44, y: 458, w: 820, h: 40, fill: C.green2, line: C.green, radius: "rounded-full", shadow: "shadow-none", text: "归根到底：先提高烤房利用率，最大化利用现有资源；共享能共享，实在共享不了才新建。", size: 15, color: C.green, bold: true, align: "center" });
    addFooter(slide, 5);
    slide.speakerNotes.textFrame.setText(notes("这页是把烟农诉求往管理侧落地的一页。建议按‘先说现象、再说问题、最后说目标’的顺序讲。三个痛点一定要落在三个关键词：资源不互通、管护不精准、决策不科学。最后收束到‘先共享、后新建’，这样逻辑会很稳。", []));
    slide.speakerNotes.setVisible(true);
  }

  // Slide 6
  {
    const slide = presentation.slides.add();
    slide.background.fill = "#f7faf6";
    addHeader(slide, 6, "共享烤房，让每一座烤房都“在线”", "从烟农顾虑到管理命题，再到平台方案", C.green);

    addBox(slide, { x: 52, y: 104, w: 846, h: 44, fill: C.darkGreen, line: C.darkGreen, radius: "rounded-full", shadow: "shadow-none", text: "共享烤房，让每一座烤房都“在线”，让烟农的顾虑有回应。", size: 16, color: "#fff", bold: true, align: "center" });

    const chain = [
      { y: 170, l1: "想借借不到", l2: "资源不互通", l3: "智享：盘活存量·高效调配", color: C.red },
      { y: 268, l1: "想修找不准", l2: "管护不精准", l3: "智管：常态运维·主动服务", color: C.orange },
      { y: 366, l1: "想问问不出", l2: "决策不科学", l3: "智投：辅助决策·闭环优化", color: C.blue },
    ];
    for (const item of chain) {
      addBox(slide, { x: 52, y: item.y, w: 140, h: 62, fill: "#fff", line: item.color, radius: "rounded-2xl", shadow: "shadow-sm", text: item.l1, size: 16, color: item.color, bold: true, align: "center" });
      addArrow(slide, 205, item.y + 17, 42, 26, item.color);
      addBox(slide, { x: 257, y: item.y, w: 150, h: 62, fill: `${item.color}12`, line: item.color, radius: "rounded-2xl", shadow: "shadow-sm", text: item.l2, size: 16, color: C.text, bold: true, align: "center" });
      addArrow(slide, 418, item.y + 17, 42, 26, C.green);
      addBox(slide, { x: 472, y: item.y, w: 426, h: 62, fill: C.paper, line: item.color, radius: "rounded-2xl", shadow: "shadow-sm" });
      addText(slide, { x: 492, y: item.y + 16, w: 386, h: 24, text: item.l3, size: 18, color: item.color, bold: true, align: "center" });
    }

    addBox(slide, { x: 52, y: 448, w: 846, h: 54, fill: C.paper, line: C.line, radius: "rounded-2xl", shadow: "shadow-sm" });
    addText(slide, { x: 74, y: 461, w: 802, h: 20, text: "什么是共享烤房？它整合分散烘烤资源，从选址布局、智能管护到动态调度，对传统烤房模式进行数智化升级。", size: 14, color: C.sub, align: "center" });
    addPill(slide, { x: 70, y: 505, w: 84, h: 22, text: "3S特性", fill: C.green2, color: C.green, border: C.green, size: 11 });
    addMiniMetric(slide, { x: 162, y: 500, w: 154, h: 32, value: "1S  智享", label: "盘活存量·高效调配", accent: C.red, valueSize: 14 });
    addMiniMetric(slide, { x: 330, y: 500, w: 154, h: 32, value: "2S  智管", label: "常态运维·主动服务", accent: C.orange, valueSize: 14 });
    addMiniMetric(slide, { x: 498, y: 500, w: 154, h: 32, value: "3S  智投", label: "辅助决策·闭环优化", accent: C.blue, valueSize: 14 });
    addMiniMetric(slide, { x: 666, y: 500, w: 110, h: 32, value: "三大算法", label: "作为引擎", accent: C.green, valueSize: 14 });
    addMiniMetric(slide, { x: 784, y: 500, w: 110, h: 32, value: "五大功能", label: "服务三类对象", accent: C.green, valueSize: 14 });
    addFooter(slide, 6);
    slide.speakerNotes.textFrame.setText(notes("这一页要把‘共享烤房’讲成一个可记住的概念：让每一座烤房都在线。先用三条链把顾虑、问题、方案对应起来，再用 3S 特性把平台特征收束住。结尾一句话要讲得干净：三大算法是引擎，五大功能是载体，三类对象是最终服务对象。", []));
    slide.speakerNotes.setVisible(true);
  }

  // Slide 7
  {
    const slide = presentation.slides.add();
    slide.background.fill = C.bg;
    addHeader(slide, 7, "智享｜盘活存量资源，实现烤房按需共享调度", "Schedule：共享烤房，让烟农“用得上”", C.green);

    addBox(slide, { x: 46, y: 108, w: 350, h: 386, fill: C.paper, line: C.green, radius: "rounded-2xl", shadow: "shadow-md" });
    addPill(slide, { x: 62, y: 124, w: 72, h: 22, text: "模型步骤", fill: C.green2, color: C.green, border: C.green, size: 11 });
    addCard(slide, { x: 62, y: 156, w: 318, h: 74, title: "1 指标体系", body: "烘烤需求、空闲状态、距离、容量、健康度等形成调剂候选集。", accent: C.green, fill: "#fff" });
    addCard(slide, { x: 62, y: 242, w: 318, h: 74, title: "2 算法推荐", body: "烤房调剂推荐算法按多因子评分输出 Top10 候选烤房。", accent: C.orange, fill: "#fff" });
    addCard(slide, { x: 62, y: 328, w: 318, h: 96, title: "3 输出结果", body: "紧急 / 临时预约、冲突防重、到期自动释放、服务评价。\n替代电话人工协调，做到秒级匹配。", accent: C.blue, fill: "#fff" });
    addBox(slide, { x: 62, y: 434, w: 318, h: 38, fill: C.darkGreen, line: C.darkGreen, radius: "rounded-full", shadow: "shadow-none", text: "从“各家自管自用”到“区域共享流转”", size: 13, color: "#fff", bold: true, align: "center" });

    addBox(slide, { x: 414, y: 108, w: 500, h: 386, fill: C.paper, line: C.green, radius: "rounded-2xl", shadow: "shadow-md" });
    addPill(slide, { x: 432, y: 124, w: 84, h: 22, text: "小程序端", fill: C.green2, color: C.green, border: C.green, size: 11 });
    slide.images.add({ blob: shot18, contentType: "image/png", alt: "烟农端小程序预约界面", fit: "cover", position: { left: 436, top: 154, width: 468, height: 288 }, geometry: "roundRect", borderRadius: "rounded-2xl" });
    addText(slide, { x: 438, y: 446, w: 460, h: 18, text: "秭归实践：替代电话人工协调，秒级匹配临时预约需求。", size: 11, color: C.sub, align: "center" });
    addMiniMetric(slide, { x: 438, y: 468, w: 132, h: 30, value: "秒级匹配", label: "推荐响应", accent: C.green, valueSize: 14 });
    addMiniMetric(slide, { x: 578, y: 468, w: 132, h: 30, value: "冲突防重", label: "避免抢占", accent: C.orange, valueSize: 14 });
    addMiniMetric(slide, { x: 718, y: 468, w: 132, h: 30, value: "到期释放", label: "自动回池", accent: C.blue, valueSize: 14 });
    addBox(slide, { x: 46, y: 450, w: 350, h: 44, fill: C.green2, line: C.green, radius: "rounded-full", shadow: "shadow-none", text: "落脚点：实现烤房从“各家自管自用”到“区域共享流转”。", size: 14, color: C.green, bold: true, align: "center" });
    addFooter(slide, 7);
    slide.speakerNotes.textFrame.setText(notes("智享页要聚焦烟农端。讲法建议是：先说为什么烟农需要它，再说平台怎么算，最后落回到‘区域共享流转’。图上要强调秭归实践和秒级匹配，领导最容易记住这两个点。", [`${ASSETS.shot18}`]));
    slide.speakerNotes.setVisible(true);
  }

  // Slide 8
  {
    const slide = presentation.slides.add();
    slide.background.fill = C.bg;
    addHeader(slide, 8, "智管｜数字健康档案，实现故障快速闭环处置", "Sustain：共享烤房，让设施“管得好”", C.mint);

    addBox(slide, { x: 46, y: 108, w: 350, h: 386, fill: C.paper, line: C.mint, radius: "rounded-2xl", shadow: "shadow-md" });
    addPill(slide, { x: 62, y: 124, w: 72, h: 22, text: "模型步骤", fill: C.mint2, color: C.mint, border: C.mint, size: 11 });
    addCard(slide, { x: 62, y: 156, w: 318, h: 74, title: "1 数据输入", body: "巡检、故障、部件状态、运行年限、维修历史等进入健康评估链路。", accent: C.mint, fill: "#fff" });
    addCard(slide, { x: 62, y: 242, w: 318, h: 74, title: "2 算法计算", body: "烤房健康分算法 + 寿命预测算法，自动输出维修优先级与预警。", accent: C.blue, fill: "#fff" });
    addCard(slide, { x: 62, y: 328, w: 318, h: 96, title: "3 输出结果", body: "烟农一键报修、自动匹配维修联系人和备件物资库、维修全流程线上留痕。", accent: C.orange, fill: "#fff" });
    addBox(slide, { x: 62, y: 434, w: 318, h: 38, fill: C.darkGreen, line: C.darkGreen, radius: "rounded-full", shadow: "shadow-none", text: "找得到人、找得到物、看得见进度", size: 13, color: "#fff", bold: true, align: "center" });

    addBox(slide, { x: 414, y: 108, w: 500, h: 386, fill: C.paper, line: C.mint, radius: "rounded-2xl", shadow: "shadow-md" });
    addPill(slide, { x: 432, y: 124, w: 84, h: 22, text: "WEB后台", fill: C.mint2, color: C.mint, border: C.mint, size: 11 });
    slide.images.add({ blob: shot19, contentType: "image/png", alt: "健康看板后台", fit: "cover", position: { left: 432, top: 154, width: 468, height: 214 }, geometry: "roundRect", borderRadius: "rounded-2xl" });
    addText(slide, { x: 436, y: 378, w: 460, h: 18, text: "传统人工巡检，变成系统自动评估和预警提醒。", size: 11, color: C.sub, align: "center" });
    addMiniMetric(slide, { x: 438, y: 402, w: 132, h: 30, value: "健康分", label: "量化档案", accent: C.mint, valueSize: 14 });
    addMiniMetric(slide, { x: 578, y: 402, w: 132, h: 30, value: "寿命预测", label: "主动预警", accent: C.blue, valueSize: 14 });
    addMiniMetric(slide, { x: 718, y: 402, w: 132, h: 30, value: "≤2小时", label: "维修响应", accent: C.orange, valueSize: 14 });
    addBox(slide, { x: 432, y: 446, w: 468, h: 38, fill: C.mint2, line: C.mint, radius: "rounded-full", shadow: "shadow-none", text: "落脚点：保障共享烤房池内每一台设备稳定可用，为共享调度筑牢基础。", size: 12, color: C.mint, bold: true, align: "center" });
    addFooter(slide, 8);
    slide.speakerNotes.textFrame.setText(notes("智管页要讲‘数字健康档案’。重点不是系统界面，而是闭环逻辑：报修能进来、任务能派出去、进度能看见、维修能闭环。这里最好把‘找得到人、找得到物、看得见进度’重复一次，评委会很容易抓住。", [`${ASSETS.shot19}`]));
    slide.speakerNotes.setVisible(true);
  }

  // Slide 9
  {
    const slide = presentation.slides.add();
    slide.background.fill = C.bg;
    addHeader(slide, 9, "智投｜数据算法辅助，科学做烤房建设与维修投资决策", "Selection：共享烤房，让资金“投得准”", C.green);

    addBox(slide, { x: 46, y: 108, w: 360, h: 386, fill: C.paper, line: C.green, radius: "rounded-2xl", shadow: "shadow-md" });
    addPill(slide, { x: 62, y: 124, w: 72, h: 22, text: "决策链路", fill: C.green2, color: C.green, border: C.green, size: 11 });
    addCard(slide, { x: 62, y: 156, w: 324, h: 88, title: "1 数据回流", body: "烟农反馈、故障记录、评价数据、健康分与寿命预测回到平台，成为投资决策输入。", accent: C.green, fill: "#fff" });
    addCard(slide, { x: 62, y: 252, w: 324, h: 88, title: "2 算法评估", body: "AHP + 熵权 + TOPSIS 选址评估模型，叠加维修侧与新建侧推演。", accent: C.orange, fill: "#fff" });
    addCard(slide, { x: 62, y: 348, w: 324, h: 126, title: "3 输出清单", body: "维修清单：哪些烤房优先修；\n新建清单：哪个村建多少座（按 5 的倍数批量规划）；\n综合评估：投入产出如何。", accent: C.blue, fill: "#fff" });

    addBox(slide, { x: 428, y: 108, w: 486, h: 386, fill: C.paper, line: C.green, radius: "rounded-2xl", shadow: "shadow-md" });
    addPill(slide, { x: 446, y: 124, w: 92, h: 22, text: "候选点评估", fill: C.green2, color: C.green, border: C.green, size: 11 });
    slide.images.add({ blob: shot20, contentType: "image/png", alt: "GIS选址评估地图", fit: "cover", position: { left: 448, top: 154, width: 450, height: 236 }, geometry: "roundRect", borderRadius: "rounded-2xl" });
    addText(slide, { x: 450, y: 402, w: 444, h: 18, text: "秭归候选点评估实践：把“凭经验拍板”变成“按数据推演”。", size: 11, color: C.sub, align: "center" });
    addMiniMetric(slide, { x: 450, y: 426, w: 128, h: 30, value: "AHP", label: "主观权重", accent: C.green, valueSize: 14 });
    addMiniMetric(slide, { x: 588, y: 426, w: 128, h: 30, value: "熵权", label: "客观权重", accent: C.orange, valueSize: 14 });
    addMiniMetric(slide, { x: 726, y: 426, w: 128, h: 30, value: "TOPSIS", label: "综合排序", accent: C.blue, valueSize: 14 });
    addBox(slide, { x: 428, y: 458, w: 486, h: 28, fill: C.green2, line: C.green, radius: "rounded-full", shadow: "shadow-none", text: "落脚点：让共享烤房网络建得准、投得值。", size: 12, color: C.green, bold: true, align: "center" });
    addFooter(slide, 9);
    slide.speakerNotes.textFrame.setText(notes("智投页要把链路讲通：数据从烟农和烤房现场回流，经过算法变成维修清单、新建点位和投入产出评估。这里最重要的一句话，是把‘经验拍板’变成‘数据推演’，并补上‘按 5 的倍数批量规划’这个业务说法。", [`${ASSETS.shot20}`]));
    slide.speakerNotes.setVisible(true);
  }

  const pptx = await PresentationFile.exportPptx(presentation);
  await pptx.save(OUT);

  const renderedDir = path.join(WORK, "rendered");
  await fs.mkdir(renderedDir, { recursive: true });
  for (const [index, slide] of presentation.slides.items.entries()) {
    const png = await presentation.export({ slide, format: "png", scale: 1 });
    await fs.writeFile(path.join(renderedDir, `slide-${String(index + 1).padStart(2, "0")}.png`), new Uint8Array(await png.arrayBuffer()));
  }
  const montage = await presentation.export({ format: "webp", montage: true, scale: 1 });
  await fs.writeFile(path.join(WORK, "montage.webp"), new Uint8Array(await montage.arrayBuffer()));
}

main().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});
