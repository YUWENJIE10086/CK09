import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { Presentation, PresentationFile } from "@oai/artifact-tool";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const ROOT = "/Users/yuwenjie/Documents/Traeppp08/汇报0818";
const OUT = path.join(ROOT, "共享烤房_省局发布版.pptx");
const WORK = __dirname;

const ASSETS = {
  coverBg: path.join(ROOT, "ppt_build/images/bg-cover_16x9.jpg"),
  closingBg: path.join(ROOT, "ppt_build/images/bg-closing_16x9.jpg"),
  blueprint: path.join(ROOT, "0820-1蓝图.png"),
  demo1: path.join(ROOT, "共享烤房0820/slide-14.png"),
  demo2: path.join(ROOT, "共享烤房0820/slide-16.png"),
  demo3: path.join(ROOT, "共享烤房0820/slide-17.png"),
};

const FONT = "PingFang SC";
const C = {
  bg: "#f7fbff",
  paper: "#ffffff",
  paper2: "#f3f8ff",
  line: "#dbe7f4",
  text: "#0f172a",
  sub: "#475569",
  muted: "#64748b",
  blue: "#2f6bff",
  blue2: "#d9e8ff",
  teal: "#14b8a6",
  teal2: "#d9f7f3",
  amber: "#f59e0b",
  amber2: "#fff2cf",
  green: "#22c55e",
  green2: "#dcfce7",
  rose: "#f43f5e",
  rose2: "#ffe2e8",
  violet: "#7c3aed",
  violet2: "#ede4ff",
};

const theme = {
  name: "Shared Baking Room Light",
  themeColors: {
    accent1: C.blue,
    accent2: C.teal,
    accent3: C.amber,
    accent4: C.green,
    accent5: C.violet,
    accent6: C.rose,
    bg1: "#ffffff",
    bg2: C.bg,
    tx1: C.text,
    tx2: C.sub,
    dk1: "#000000",
    dk2: "#1f2937",
    lt1: "#ffffff",
    lt2: "#e2e8f0",
    hlink: C.blue,
    folHlink: C.violet,
  },
};

async function readImage(file) {
  const bytes = await fs.readFile(file);
  return bytes;
}

function notes(text, sources = []) {
  const parts = [];
  parts.push("【讲解稿】");
  parts.push(text.trim());
  if (sources.length) {
    parts.push("");
    parts.push("【Sources】");
    for (const source of sources) parts.push(`- ${source}`);
  }
  return parts.join("\n");
}

function addBox(slide, {
  x,
  y,
  w,
  h,
  fill = "white",
  line = C.line,
  radius = "rounded-2xl",
  shadow = "shadow-sm",
  text,
  size = 18,
  color = C.text,
  bold = false,
  align = "left",
  valign = "middle",
  typeface = FONT,
  name,
  geometry = "roundRect",
}) {
  const shape = slide.shapes.add({
    geometry,
    name,
    position: { left: x, top: y, width: w, height: h },
    fill,
    line: { style: "solid", fill: line, width: line === "none" ? 0 : 1 },
    borderRadius: radius,
    shadow,
  });
  if (text !== undefined) {
    shape.text = text;
    shape.text.style = {
      fontSize: size,
      color,
      bold,
      typeface,
      alignment: align,
    };
  }
  return shape;
}

function addText(slide, {
  x,
  y,
  w,
  h,
  text,
  size = 18,
  color = C.text,
  bold = false,
  align = "left",
  typeface = FONT,
  name,
}) {
  const shape = slide.shapes.add({
    geometry: "textbox",
    name,
    position: { left: x, top: y, width: w, height: h },
    fill: "none",
    line: { style: "solid", fill: "none", width: 0 },
  });
  shape.text = text;
  shape.text.style = {
    fontSize: size,
    color,
    bold,
    typeface,
    alignment: align,
  };
  return shape;
}

function addPill(slide, {
  x,
  y,
  w,
  h,
  text,
  fill,
  color = C.text,
  border = fill,
  size = 16,
  bold = true,
  name,
}) {
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
    bold,
    align: "center",
    name,
  });
}

function addSectionHeader(slide, idx, title, subtitle, accent = C.blue) {
  addPill(slide, {
    x: 52,
    y: 38,
    w: 46,
    h: 46,
    text: String(idx).padStart(2, "0"),
    fill: accent,
    color: "#ffffff",
    size: 18,
    name: `section-pill-${idx}`,
  });
  addText(slide, {
    x: 112,
    y: 34,
    w: 900,
    h: 36,
    text: title,
    size: 28,
    bold: true,
    color: C.text,
    name: `section-title-${idx}`,
  });
  if (subtitle) {
    addText(slide, {
      x: 112,
      y: 66,
      w: 920,
      h: 24,
      text: subtitle,
      size: 13,
      color: C.muted,
      name: `section-subtitle-${idx}`,
    });
  }
  addBox(slide, {
    x: 52,
    y: 102,
    w: 1176,
    h: 2,
    fill: accent,
    line: accent,
    radius: 0,
    shadow: "shadow-none",
    geometry: "rect",
    name: `section-rule-${idx}`,
  });
}

function addFooter(slide, idx, total = 17) {
  addText(slide, {
    x: 1090,
    y: 684,
    w: 120,
    h: 18,
    text: `${String(idx).padStart(2, "0")} / ${String(total).padStart(2, "0")}`,
    size: 11,
    color: C.muted,
    align: "right",
    name: `footer-${idx}`,
  });
}

function addBulletCard(slide, x, y, w, h, title, body, accentFill, accentLine, titleColor = C.text) {
  addBox(slide, {
    x,
    y,
    w,
    h,
    fill: "#ffffff",
    line: accentLine,
    radius: "rounded-2xl",
    shadow: "shadow-sm",
  });
  addPill(slide, {
    x: x + 18,
    y: y + 16,
    w: 68,
    h: 28,
    text: title,
    fill: accentFill,
    color: titleColor,
    border: accentLine,
    size: 14,
  });
  addText(slide, {
    x: x + 18,
    y: y + 58,
    w: w - 36,
    h: h - 72,
    text: body,
    size: 18,
    color: C.sub,
    name: `${title}-body`,
  });
}

function addCircleStep(slide, x, y, num, title, body, accent, fill) {
  addBox(slide, {
    x,
    y,
    w: 46,
    h: 46,
    fill: accent,
    line: accent,
    radius: "rounded-full",
    shadow: "shadow-none",
    text: String(num),
    size: 17,
    color: "#ffffff",
    bold: true,
    align: "center",
    name: `step-${num}-circle`,
  });
  addText(slide, {
    x: x + 62,
    y: y - 2,
    w: 290,
    h: 24,
    text: title,
    size: 20,
    bold: true,
    color: C.text,
    name: `step-${num}-title`,
  });
  addBox(slide, {
    x: x + 58,
    y: y + 30,
    w: 290,
    h: 92,
    fill,
    line: accent,
    radius: "rounded-2xl",
    shadow: "shadow-none",
    text: body,
    size: 16,
    color: C.sub,
    name: `step-${num}-body`,
  });
}

function addMetric(slide, x, y, w, h, value, label, accent, valueSize = 30) {
  addBox(slide, {
    x,
    y,
    w,
    h,
    fill: "#ffffff",
    line: accent,
    radius: "rounded-2xl",
    shadow: "shadow-sm",
  });
  addText(slide, {
    x: x + 16,
    y: y + 16,
    w: w - 32,
    h: 44,
    text: value,
    size: valueSize,
    bold: true,
    color: accent,
    name: `metric-${label}-value`,
  });
  addText(slide, {
    x: x + 16,
    y: y + 58,
    w: w - 32,
    h: 26,
    text: label,
    size: 15,
    color: C.sub,
    name: `metric-${label}-label`,
  });
}

function addChart(slide, { x, y, w, h, title, categories, values, seriesName, fill, yMax }) {
  slide.charts.add("bar", {
    position: { left: x, top: y, width: w, height: h },
    title,
    titlePlacement: "aboveChart",
    categories,
    series: [{ name: seriesName, values, fill }],
    hasLegend: false,
    barOptions: { direction: "column", grouping: "clustered", gapWidth: 90 },
    xAxis: {
      textStyle: { fill: C.sub, fontSize: 12 },
      line: { style: "solid", fill: "#dbe7f4", width: 1 },
    },
    yAxis: {
      min: 0,
      max: yMax,
      majorGridlines: { style: "solid", fill: "#e8f0fb", width: 1 },
      textStyle: { fill: C.sub, fontSize: 12 },
      line: { style: "solid", fill: "#dbe7f4", width: 1 },
    },
    dataLabels: {
      showValue: true,
      position: "outEnd",
      textStyle: { fill: C.text, fontSize: 12, bold: true },
    },
    chartFill: "#ffffff",
    plotAreaFill: "#ffffff",
  });
}

function setNotes(slide, talk, sources = []) {
  slide.speakerNotes.textFrame.setText(notes(talk, sources));
  slide.speakerNotes.setVisible(true);
}

async function main() {
  await fs.mkdir(WORK, { recursive: true });
  const coverBg = await readImage(ASSETS.coverBg);
  const closingBg = await readImage(ASSETS.closingBg);
  const blueprint = await readImage(ASSETS.blueprint);
  const demo1 = await readImage(ASSETS.demo1);
  const demo2 = await readImage(ASSETS.demo2);
  const demo3 = await readImage(ASSETS.demo3);

  const presentation = Presentation.create({ slideSize: { width: 1280, height: 720 } });
  presentation.theme.colorScheme = theme;

  // 1 cover
  {
    const slide = presentation.slides.add();
    slide.background.fill = "#ffffff";
    slide.images.add({
      blob: coverBg,
      contentType: "image/jpeg",
      alt: "烟田背景",
      fit: "cover",
      position: { left: 0, top: 0, width: 1280, height: 720 },
    });
    addBox(slide, {
      x: 0,
      y: 0,
      w: 1280,
      h: 720,
      fill: "#ffffffd0",
      line: "none",
      radius: 0,
      shadow: "shadow-none",
      geometry: "rect",
    });
    addBox(slide, {
      x: 48,
      y: 58,
      w: 468,
      h: 604,
      fill: "#ffffffdd",
      line: "#eff6ff",
      radius: "rounded-3xl",
      shadow: "shadow-lg",
    });
    addPill(slide, {
      x: 78,
      y: 92,
      w: 140,
      h: 30,
      text: "共享烤房 · 建用管一体化",
      fill: "#eff6ff",
      color: C.blue,
      border: "#dbeafe",
      size: 14,
      name: "cover-pill",
    });
    addText(slide, {
      x: 78,
      y: 154,
      w: 350,
      h: 146,
      text: "“共享烤房”\n建用管一体化数智协同平台",
      size: 42,
      bold: true,
      color: C.text,
      name: "cover-title",
    });
    addText(slide, {
      x: 80,
      y: 320,
      w: 340,
      h: 72,
      text: "盘活烤房资源，数智共享烤房。\n把零散烤房变成可流转、可调度的公共烘烤资源。",
      size: 18,
      color: C.sub,
      name: "cover-subtitle",
    });
    const metrics = [
      { x: 80, label: "7个乡镇", value: "07", w: 120 },
      { x: 208, label: "2442座烤房", value: "2442", w: 128 },
      { x: 364, label: "704户烟农", value: "704", w: 120 },
    ];
    for (const m of metrics) {
      addBox(slide, {
        x: m.x,
        y: 420,
        w: m.w,
        h: 112,
        fill: "#ffffff",
        line: "#dbeafe",
        radius: "rounded-2xl",
        shadow: "shadow-sm",
      });
      addText(slide, {
        x: m.x + 12,
        y: 444,
        w: m.w - 24,
        h: 36,
        text: m.value,
        size: m.value.length >= 4 ? 24 : 28,
        bold: true,
        color: C.blue,
        align: "center",
      });
      addText(slide, {
        x: m.x + 10,
        y: 486,
        w: m.w - 20,
        h: 28,
        text: m.label,
        size: 12,
        color: C.sub,
        align: "center",
      });
    }
    addText(slide, {
      x: 80,
      y: 596,
      w: 352,
      h: 22,
      text: "湖北省烟草公司宜昌市公司  ·  烟叶条线",
      size: 13,
      color: C.muted,
      name: "cover-org",
    });
    addText(slide, {
      x: 80,
      y: 622,
      w: 352,
      h: 22,
      text: "汇报发布版",
      size: 13,
      color: C.muted,
      name: "cover-version",
    });
    addText(slide, {
      x: 580,
      y: 600,
      w: 620,
      h: 50,
      text: "一张图看懂共享烤房如何从“建、用、管”实现协同升级",
      size: 20,
      bold: true,
      color: "#0f172a",
      align: "right",
      name: "cover-tagline",
    });
    setNotes(
      slide,
      "开场先点题：这是围绕烤房资源盘活的省局汇报版。用一段简洁的话把项目价值讲清楚，让领导先抓住三个关键词，建、用、管。封面不堆字，重点是留下“共享烤房”和“数智协同”两个记忆点。",
      [
        `背景图：${ASSETS.coverBg}`,
        "项目内容：用户提供的封面要求与整体说明",
      ],
    );
    addFooter(slide, 1);
  }

  // 2 intro video
  {
    const slide = presentation.slides.add();
    slide.background.fill = "#f4f9ff";
    addSectionHeader(slide, 2, "引入视频", "建议作为开场情境短片：从“传统烤房”走向“共享烤房”", C.teal);
    addBox(slide, {
      x: 76,
      y: 146,
      w: 742,
      h: 466,
      fill: "#ffffff",
      line: "#d7e7f4",
      radius: "rounded-3xl",
      shadow: "shadow-md",
    });
    slide.images.add({
      blob: demo1,
      contentType: "image/png",
      alt: "视频引入封面",
      fit: "cover",
      position: { left: 96, top: 166, width: 702, height: 426 },
      geometry: "roundRect",
      borderRadius: "rounded-2xl",
    });
    addBox(slide, {
      x: 336,
      y: 292,
      w: 182,
      h: 126,
      fill: "#0f172ae6",
      line: "#0f172ae6",
      radius: "rounded-3xl",
      shadow: "shadow-lg",
    });
    addBox(slide, {
      x: 392,
      y: 322,
      w: 70,
      h: 70,
      fill: "#ffffff",
      line: "#ffffff",
      radius: "rounded-full",
      shadow: "shadow-none",
      text: "▶",
      size: 30,
      color: C.blue,
      bold: true,
      align: "center",
    });
    addText(slide, {
      x: 284,
      y: 432,
      w: 276,
      h: 30,
      text: "引入视频：传统烤房 → 共享烤房",
      size: 18,
      bold: true,
      color: "#ffffff",
      align: "center",
    });
    addBox(slide, {
      x: 858,
      y: 146,
      w: 352,
      h: 466,
      fill: "#ffffff",
      line: "#d7e7f4",
      radius: "rounded-3xl",
      shadow: "shadow-md",
    });
    addText(slide, {
      x: 890,
      y: 180,
      w: 286,
      h: 36,
      text: "视频里要讲什么",
      size: 26,
      bold: true,
      color: C.text,
    });
    addText(slide, {
      x: 890,
      y: 224,
      w: 274,
      h: 120,
      text: "• 过去：烤房分散、纸质台账、现场跑腿。\n• 现在：平台把资源、设备、工单和调度串起来。\n• 观众看到的不是“系统功能”，而是“烤房如何被重新组织起来”。",
      size: 18,
      color: C.sub,
    });
    addMetric(slide, 890, 376, 88, 104, "3端", "烟农 / 技术员 / 管理者", C.teal);
    addMetric(slide, 994, 376, 88, 104, "5大", "功能模块", C.blue);
    addMetric(slide, 1098, 376, 88, 104, "3个", "核心模型", C.amber);
    addText(slide, {
      x: 890,
      y: 514,
      w: 282,
      h: 62,
      text: "建议播放时长：30-45秒。\n用情境切换把观众带入项目现场。",
      size: 15,
      color: C.muted,
    });
    setNotes(
      slide,
      "这一页不讲技术细节，只做情境引入。短片的作用是把观众从传统管理方式拉到共享烤房的现实场景里，让后面的痛点、方案和模型顺势展开。播放时可以边看边引出一句话：我们不是做一个系统，而是在重构烤房资源的组织方式。",
      [
        `视频素材：${path.join(ROOT, "引入视频0820.mp4")}`,
        `参考视频页截图：${ASSETS.demo1}`,
      ],
    );
    addFooter(slide, 2);
  }

  // 3 pain points
  {
    const slide = presentation.slides.add();
    slide.background.fill = C.bg;
    addSectionHeader(slide, 3, "三大痛点把共享烤房推上前台", "资源、管护、决策三条链路都需要数智化升级", C.rose);
    addBulletCard(
      slide, 64, 162, 352, 450,
      "01",
      "资源不互通\n烤房供需信息割裂，空闲设备无法快速跨区域调剂周转；烘烤高峰期资源紧张，非烘烤期又长期闲置，整体利用效率偏低。",
      C.rose2,
      C.rose,
    );
    addBulletCard(
      slide, 464, 162, 352, 450,
      "02",
      "管护不精准\n运维依赖人工现场排查，缺少自动状态评估手段；维修工单、资金分配没有客观优先级依据，往往“看经验、凭印象”。",
      C.teal2,
      C.teal,
    );
    addBulletCard(
      slide, 864, 162, 352, 450,
      "03",
      "决策不科学\n新建烤房选址缺少数字化推演工具，仅靠经验判断点位，难以综合测算片区烟叶产能、承载上限和长期收益。",
      C.blue2,
      C.blue,
    );
    addText(slide, {
      x: 86,
      y: 636,
      w: 1100,
      h: 30,
      text: "结论：传统模式下，烤房是“资产”，但没有被当成“可流转资源”来管理。",
      size: 18,
      bold: true,
      color: C.text,
      align: "center",
    });
    setNotes(
      slide,
      "先把问题讲透。资源上是‘看不见’，管护上是‘管不准’，决策上是‘算不清’。这三句话要说得很直接，因为后面所有模型和功能，都只是为了把这三件事一次性解决。",
      [
        "用户提供的痛点描述",
        `项目底稿：${path.join(ROOT, "generate_report.js")}`,
      ],
    );
    addFooter(slide, 3);
  }

  // 4 solution overview
  {
    const slide = presentation.slides.add();
    slide.background.fill = "#f8fbff";
    addSectionHeader(slide, 4, "共享烤房模式：把零散烤房变成可流转、可调度的公共烘烤资源", "目标不是“多一个系统”，而是“让资源跑起来、让管理活起来”", C.blue);
    addBox(slide, {
      x: 86,
      y: 142,
      w: 1108,
      h: 72,
      fill: "#ffffff",
      line: "#dbeafe",
      radius: "rounded-2xl",
      shadow: "shadow-sm",
      text: "共享烤房：依托物联网与 AI 整合分散烘烤资源，从选址布局、智能管护到动态调度，对传统烤房模式进行数智化升级。",
      size: 21,
      bold: true,
      color: C.text,
      align: "center",
    });
    const centers = [
      { x: 148, y: 278, w: 220, h: 150, fill: C.blue2, line: C.blue, title: "智建", body: "科学选址\n优化赋能\n新建选址", accent: C.blue },
      { x: 530, y: 278, w: 220, h: 150, fill: C.teal2, line: C.teal, title: "智用", body: "常态运维\n稳定可用\n报修管护 / 烤房维护", accent: C.teal },
      { x: 912, y: 278, w: 220, h: 150, fill: C.amber2, line: C.amber, title: "智管", body: "盘活存量\n高效调配\n烤房调剂 / 预约分配", accent: C.amber },
    ];
    for (const box of centers) {
      addBox(slide, {
        x: box.x,
        y: box.y,
        w: box.w,
        h: box.h,
        fill: box.fill,
        line: box.line,
        radius: "rounded-3xl",
        shadow: "shadow-md",
      });
      addText(slide, {
        x: box.x + 20,
        y: box.y + 18,
        w: box.w - 40,
        h: 26,
        text: box.title,
        size: 26,
        bold: true,
        color: box.accent,
        align: "center",
      });
      addText(slide, {
        x: box.x + 18,
        y: box.y + 60,
        w: box.w - 36,
        h: 72,
        text: box.body,
        size: 18,
        bold: true,
        color: C.text,
        align: "center",
      });
    }
    addText(slide, {
      x: 232,
      y: 438,
      w: 80,
      h: 32,
      text: "核心模型",
      size: 14,
      color: C.blue,
      align: "center",
      bold: true,
    });
    addText(slide, {
      x: 614,
      y: 438,
      w: 80,
      h: 32,
      text: "核心模型",
      size: 14,
      color: C.teal,
      align: "center",
      bold: true,
    });
    addText(slide, {
      x: 996,
      y: 438,
      w: 80,
      h: 32,
      text: "核心模型",
      size: 14,
      color: C.amber,
      align: "center",
      bold: true,
    });
    addBox(slide, {
      x: 132,
      y: 498,
      w: 1016,
      h: 120,
      fill: "#ffffff",
      line: "#dbeafe",
      radius: "rounded-3xl",
      shadow: "shadow-sm",
    });
    addText(slide, {
      x: 156,
      y: 516,
      w: 958,
      h: 24,
      text: "三大核心模型提供五大功能",
      size: 22,
      bold: true,
      color: C.text,
      align: "center",
    });
    const funcs = [
      ["烤房调剂", C.rose],
      ["烤房维护", C.teal],
      ["新建选址", C.blue],
      ["反馈评价", C.green],
      ["资金分配", C.amber],
    ];
    funcs.forEach((f, i) => {
      addPill(slide, {
        x: 182 + i * 185,
        y: 556,
        w: 150,
        h: 34,
        text: f[0],
        fill: `${f[1]}22`,
        color: f[1],
        border: `${f[1]}44`,
        size: 15,
      });
    });
    addText(slide, {
      x: 144,
      y: 650,
      w: 990,
      h: 20,
      text: "Slogan：盘活烤房资源，数智共享烤房！",
      size: 18,
      bold: true,
      color: C.text,
      align: "center",
    });
    setNotes(
      slide,
      "这页把项目定义说清楚。先给出一句好记的 slogan，再说明共享烤房不是单点功能，而是围绕智建、智用、智管形成的资源网络。这里要把领导的注意力引导到‘资源被调度起来’这个结果上，而不是停在系统界面本身。",
      [
        "用户提供的共享烤房概念与三大方向说明",
        `项目底稿：${path.join(ROOT, "generate_report.js")}`,
      ],
    );
    addFooter(slide, 4);
  }

  // 5 blueprint
  {
    const slide = presentation.slides.add();
    slide.background.fill = "#f7fbff";
    addSectionHeader(slide, 5, "总体蓝图：从数据采集到 AI 决策的完整闭环", "这张图建议讲 2-3 分钟，讲的是“一个烤房如何被重新盘活”", C.violet);
    addBox(slide, {
      x: 56,
      y: 144,
      w: 842,
      h: 512,
      fill: "#ffffff",
      line: "#dbeafe",
      radius: "rounded-3xl",
      shadow: "shadow-md",
    });
    slide.images.add({
      blob: blueprint,
      contentType: "image/png",
      alt: "项目蓝图",
      fit: "contain",
      position: { left: 78, top: 166, width: 800, height: 468 },
    });
    addBox(slide, {
      x: 928,
      y: 144,
      w: 296,
      h: 512,
      fill: "#ffffff",
      line: "#dbeafe",
      radius: "rounded-3xl",
      shadow: "shadow-md",
    });
    addText(slide, {
      x: 952,
      y: 172,
      w: 248,
      h: 30,
      text: "讲述顺序建议",
      size: 24,
      bold: true,
      color: C.text,
    });
    const story = [
      ["1", "低代码采集", "从手机端、表单和巡检环节采集烤房基础信息、维修记录和状态留痕。"],
      ["2", "三大模型", "在中台完成选址、健康、调剂三类算法计算，把原始数据变成可执行决策。"],
      ["3", "五大功能", "烟农用预约和评价，技术员用巡检和报修，管理者用资金和全局监管。"],
      ["4", "三类对象", "所有结果回流到烟农、技术员、管理者三端，形成闭环协同。"],
    ];
    story.forEach((s, i) => {
      addBox(slide, {
        x: 952,
        y: 222 + i * 90,
        w: 240,
        h: 72,
        fill: i % 2 === 0 ? C.paper2 : "#ffffff",
        line: i % 2 === 0 ? C.blue2 : C.line,
        radius: "rounded-2xl",
        shadow: "shadow-none",
      });
      addBox(slide, {
        x: 966,
        y: 240 + i * 90,
        w: 26,
        h: 26,
        fill: i === 0 ? C.blue : i === 1 ? C.teal : i === 2 ? C.amber : C.green,
        line: "none",
        radius: "rounded-full",
        shadow: "shadow-none",
        text: s[0],
        size: 12,
        bold: true,
        color: "#ffffff",
        align: "center",
      });
      addText(slide, {
        x: 1002,
        y: 234 + i * 90,
        w: 172,
        h: 20,
        text: s[1],
        size: 16,
        bold: true,
        color: C.text,
      });
      addText(slide, {
        x: 1002,
        y: 254 + i * 90,
        w: 182,
        h: 40,
        text: s[2],
        size: 12,
        color: C.sub,
      });
    });
    addText(slide, {
      x: 56,
      y: 668,
      w: 842,
      h: 18,
      text: "左边讲结构，右边讲故事：先采数，再析数，最后用数。",
      size: 12,
      color: C.muted,
      align: "center",
    });
    setNotes(
      slide,
      "这一页是全场最重要的逻辑图，讲的时候要把自己想成一间烤房在‘回顾一生’。先说过去是分散、手工、靠经验，再说现在通过低代码和物联网把数据收进来，随后三大算法把数据算成决策，最后五大功能落到三类用户身上。讲完这页，观众就会明白后面的每一页模型都是在完成这张图中的一个环节。",
      [
        `蓝图文件：${ASSETS.blueprint}`,
        `项目底稿：${path.join(ROOT, "shared_baking_room_project_blueprint_adjusted.drawio")}`,
      ],
    );
    addFooter(slide, 5);
  }

  // 6 model 1 intro
  {
    const slide = presentation.slides.add();
    slide.background.fill = "#f8fbff";
    addSectionHeader(slide, 6, "智建：科学选址，让新建烤房一次算准", "关键词：AHP + 熵权 + TOPSIS + GIS 推演", C.blue);
    addBox(slide, { x: 60, y: 156, w: 526, h: 492, fill: "#ffffff", line: "#dbeafe", radius: "rounded-3xl", shadow: "shadow-md" });
    addText(slide, { x: 88, y: 182, w: 460, h: 24, text: "模型步骤", size: 22, bold: true, color: C.text });
    addCircleStep(slide, 88, 228, 1, "指标体系", "烟叶产能、道路可达性、坡度、供电条件、服务半径、区域承载等指标形成候选评分框架。", C.blue, C.blue2);
    addCircleStep(slide, 88, 372, 2, "方法融合", "AHP 负责主观权重，熵权法补充客观权重，TOPSIS 完成综合贴近度排序。", C.teal, C.teal2);
    addCircleStep(slide, 88, 516, 3, "输出结果", "输出候选点位排序、适宜度分级与选址评价报告，支撑会审决策。", C.amber, C.amber2);
    addBox(slide, { x: 626, y: 156, w: 594, h: 492, fill: "#ffffff", line: "#dbeafe", radius: "rounded-3xl", shadow: "shadow-md" });
    addText(slide, { x: 650, y: 182, w: 540, h: 24, text: "传统方式 vs 平台方式", size: 22, bold: true, color: C.text });
    addBulletCard(slide, 650, 224, 254, 168, "传统", "纸质表格、人工踏勘、经验判断、多人反复会商，选址过程耗时长且口径不统一。", C.rose2, C.rose);
    addBulletCard(slide, 918, 224, 278, 168, "平台", "在后台统一录入指标，系统自动完成评分、排序和报告生成，选址决策从“拍脑袋”变成“算出来”。", C.blue2, C.blue);
    addText(slide, { x: 650, y: 426, w: 540, h: 18, text: "实际变化", size: 18, bold: true, color: C.text });
    addMetric(slide, 650, 454, 164, 134, "30min → 5s", "会审推演时间", C.blue);
    addMetric(slide, 832, 454, 164, 134, "8项", "核心指标", C.teal);
    addMetric(slide, 1014, 454, 164, 134, "20个", "候选点位", C.amber);
    addText(slide, { x: 650, y: 604, w: 540, h: 18, text: "科学选址的价值，不是更快出结果，而是让结果可解释、可追溯。", size: 14, color: C.muted, align: "center" });
    setNotes(
      slide,
      "智建这页要讲清楚三件事：看什么、怎么算、算出什么。先把指标体系说完整，再把 AHP、熵权、TOPSIS 这套大家耳熟能详的方法串起来，最后落到报告和排序结果。讲案例时要强调，过去是纸质表格和现场跑腿，现在是平台一键出结果。",
      [
        "模型算法来源：项目底稿中的AHP+熵权+TOPSIS选址方案",
        `参考素材：${path.join(ROOT, "共享烤房-三大模型详情页.pptx")}`,
      ],
    );
    addFooter(slide, 6);
  }

  // 7 model 1 results
  {
    const slide = presentation.slides.add();
    slide.background.fill = C.bg;
    addSectionHeader(slide, 7, "智建成效：选址从人工会审走向秒级推演", "结果要用数字说话，也要让推广价值一眼看懂", C.blue);
    addMetric(slide, 72, 156, 206, 126, "30min → 5s", "会审推演时间", C.blue, 22);
    addMetric(slide, 292, 156, 206, 126, "8项", "指标体系", C.teal);
    addMetric(slide, 512, 156, 206, 126, "20个", "候选点位", C.amber);
    addMetric(slide, 732, 156, 206, 126, "更统一", "选址口径", C.green);
    addMetric(slide, 952, 156, 206, 126, "可追溯", "评价报告", C.violet);
    addBox(slide, { x: 72, y: 316, w: 514, h: 320, fill: "#ffffff", line: "#dbeafe", radius: "rounded-3xl", shadow: "shadow-md" });
    addText(slide, { x: 96, y: 340, w: 440, h: 24, text: "推广时的讲法", size: 22, bold: true, color: C.text });
    addText(slide, {
      x: 96,
      y: 384,
      w: 450,
      h: 210,
      text: "• 在县域层面先统一选址口径，减少重复踏勘。\n• 在乡镇层面用同一套评分规则比较不同点位。\n• 在会审层面把“推荐理由”一起输出，方便快速拍板。\n• 在推广层面保留每次评价结果，方便后续复盘。",
      size: 18,
      color: C.sub,
    });
    addBox(slide, { x: 618, y: 316, w: 590, h: 320, fill: "#ffffff", line: "#dbeafe", radius: "rounded-3xl", shadow: "shadow-md" });
    addText(slide, { x: 644, y: 340, w: 540, h: 24, text: "结果可视化", size: 22, bold: true, color: C.text });
    addChart(slide, {
      x: 644,
      y: 382,
      w: 536,
      h: 218,
      title: "模型输出关键量",
      categories: ["指标项", "候选点", "出图速度"],
      values: [8, 20, 5],
      seriesName: "数量 / 秒",
      fill: C.blue,
      yMax: 24,
    });
    addText(slide, { x: 644, y: 600, w: 536, h: 18, text: "推广要点：让新建烤房从“经验选址”升级为“数据选址”。", size: 13, color: C.muted, align: "center" });
    setNotes(
      slide,
      "这一页重点是把成效说成业务语言。不要只说模型更先进，而是说它把会审时长压缩到了 5 秒、把口径统一了、把推荐理由保留下来了。顺着这个逻辑再讲推广，会更像省局发布口径：既讲效率，也讲可复制、可审计。",
      [
        "用户提供的成效要求：30min缩短至5s、推广至秭归县等",
        `项目底稿：${path.join(ROOT, "generate_report.js")}`,
      ],
    );
    addFooter(slide, 7);
  }

  // 8 model 2 intro
  {
    const slide = presentation.slides.add();
    slide.background.fill = "#f8fbff";
    addSectionHeader(slide, 8, "智用：常态运维，让烤房始终稳定可用", "关键词：健康分算法 + 寿命预测 + 预防性维护", C.teal);
    addBox(slide, { x: 60, y: 156, w: 526, h: 492, fill: "#ffffff", line: "#d9f7f3", radius: "rounded-3xl", shadow: "shadow-md" });
    addText(slide, { x: 88, y: 182, w: 460, h: 24, text: "模型步骤", size: 22, bold: true, color: C.text });
    addCircleStep(slide, 88, 228, 1, "数据输入", "设备状态、温湿度、巡检结果、维修记录、部件健康、运行年限等数据进入健康评估链路。", C.teal, C.teal2);
    addCircleStep(slide, 88, 372, 2, "算法计算", "健康分模型识别健康等级，寿命预测模型识别退化趋势并给出预警。", C.blue, C.blue2);
    addCircleStep(slide, 88, 516, 3, "输出结果", "输出维修优先级、预警等级和处置建议，帮助技术员把问题前置化。", C.amber, C.amber2);
    addBox(slide, { x: 626, y: 156, w: 594, h: 492, fill: "#ffffff", line: "#d9f7f3", radius: "rounded-3xl", shadow: "shadow-md" });
    addText(slide, { x: 650, y: 182, w: 540, h: 24, text: "传统方式 vs 平台方式", size: 22, bold: true, color: C.text });
    addBulletCard(slide, 650, 224, 254, 168, "传统", "现场巡检靠经验，健康状态缺少统一量化标准；维修工单和资金安排往往滞后。", C.rose2, C.rose);
    addBulletCard(slide, 918, 224, 278, 168, "平台", "把设备健康变成可计算的分数，把寿命退化变成可预测的趋势，提前安排维修。", C.teal2, C.teal);
    addText(slide, { x: 650, y: 426, w: 540, h: 18, text: "输出目标", size: 18, bold: true, color: C.text });
    addMetric(slide, 650, 454, 164, 134, "95%+", "采集数据有效率", C.teal);
    addMetric(slide, 832, 454, 164, 134, "2h内", "维护需求响应", C.blue);
    addMetric(slide, 1014, 454, 164, 134, "预防性", "维修策略", C.amber);
    addText(slide, { x: 650, y: 604, w: 540, h: 18, text: "把“坏了再修”变成“快坏先修”。", size: 14, color: C.muted, align: "center" });
    setNotes(
      slide,
      "智用这页的重点是“把维护做准”。讲法可以很简洁：从设备数据进来开始，经过健康分和寿命预测两步，输出的不是一个分数，而是一张维修优先级清单。这样技术员拿到的就不是被动工单，而是前置提醒。",
      [
        "用户提供的健康分与寿命预测方向",
        `项目底稿：${path.join(ROOT, "generate_report.js")}`,
      ],
    );
    addFooter(slide, 8);
  }

  // 9 model 2 results
  {
    const slide = presentation.slides.add();
    slide.background.fill = C.bg;
    addSectionHeader(slide, 9, "智用成效：维护从被动排查变成主动预警", "让设备健康、工单优先级和资金投放都有依据", C.teal);
    addMetric(slide, 72, 156, 206, 126, "95%+", "设备采集数据有效率", C.teal);
    addMetric(slide, 292, 156, 206, 126, "2h内", "维护需求响应", C.blue);
    addMetric(slide, 512, 156, 206, 126, "85%", "人工工作量降低", C.amber);
    addMetric(slide, 732, 156, 206, 126, "前置化", "预警与处置", C.green);
    addMetric(slide, 952, 156, 206, 126, "可追溯", "健康档案", C.violet);
    addBox(slide, { x: 72, y: 316, w: 514, h: 320, fill: "#ffffff", line: "#d9f7f3", radius: "rounded-3xl", shadow: "shadow-md" });
    addText(slide, { x: 96, y: 340, w: 440, h: 24, text: "现场变化", size: 22, bold: true, color: C.text });
    addText(slide, {
      x: 96,
      y: 384,
      w: 450,
      h: 210,
      text: "• 维修工单不是“谁先报谁先修”，而是按健康分和风险等级排序。\n• 资金安排不是“平均分”，而是跟着设备状态和影响范围走。\n• 巡检结果不是只记录一次，而是沉淀成长期健康档案。\n• 管理者能看到趋势，技术员能看到优先级。",
      size: 18,
      color: C.sub,
    });
    addBox(slide, { x: 618, y: 316, w: 590, h: 320, fill: "#ffffff", line: "#d9f7f3", radius: "rounded-3xl", shadow: "shadow-md" });
    addText(slide, { x: 644, y: 340, w: 540, h: 24, text: "健康评估结果", size: 22, bold: true, color: C.text });
    addChart(slide, {
      x: 644,
      y: 382,
      w: 536,
      h: 218,
      title: "关键指标",
      categories: ["工作量降幅", "数据有效率", "响应及时率"],
      values: [85, 95, 98],
      seriesName: "百分比",
      fill: C.teal,
      yMax: 100,
    });
    addText(slide, { x: 644, y: 600, w: 536, h: 18, text: "让“巡检”变成“预测”，让“维修”变成“治理”。", size: 13, color: C.muted, align: "center" });
    setNotes(
      slide,
      "这页要继续强化‘主动’这个词。观众最容易记住的不是算法名，而是‘从被动排查变成主动预警’。成效部分尽量落到三个百分比：85% 的工作量降低、95% 以上的数据有效率、2 小时内响应，这些都是管理者能直接拿去汇报的语言。",
      [
        "用户提供的成效数据：95%+、2小时、85%",
        `项目底稿：${path.join(ROOT, "generate_report.js")}`,
      ],
    );
    addFooter(slide, 9);
  }

  // 10 model 3 intro
  {
    const slide = presentation.slides.add();
    slide.background.fill = "#f8fbff";
    addSectionHeader(slide, 10, "智管：盘活存量，让烤房调剂更快更准", "关键词：预约分配 + 高效调度 + 五因子加权评分", C.amber);
    addBox(slide, { x: 60, y: 156, w: 526, h: 492, fill: "#ffffff", line: "#fff2cf", radius: "rounded-3xl", shadow: "shadow-md" });
    addText(slide, { x: 88, y: 182, w: 460, h: 24, text: "模型步骤", size: 22, bold: true, color: C.text });
    addCircleStep(slide, 88, 228, 1, "指标体系", "烤房空闲度、容量、距离、健康状态、维修历史、烘烤需求等形成调剂候选集。", C.amber, C.amber2);
    addCircleStep(slide, 88, 372, 2, "方法融合", "五因子加权评分与智能排序联动，兼顾效率、公平和健康优先级。", C.blue, C.blue2);
    addCircleStep(slide, 88, 516, 3, "输出结果", "输出推荐烤房、预约列表和调剂优先级，支持秒级匹配。", C.teal, C.teal2);
    addBox(slide, { x: 626, y: 156, w: 594, h: 492, fill: "#ffffff", line: "#fff2cf", radius: "rounded-3xl", shadow: "shadow-md" });
    addText(slide, { x: 650, y: 182, w: 540, h: 24, text: "传统方式 vs 平台方式", size: 22, bold: true, color: C.text });
    addBulletCard(slide, 650, 224, 254, 168, "传统", "电话沟通、手工排期、反复协调，调剂响应慢、信息不透明，容易出现空闲与紧张并存。", C.rose2, C.rose);
    addBulletCard(slide, 918, 224, 278, 168, "平台", "按统一规则快速匹配，自动输出最优推荐与备选方案，调剂过程留痕可回溯。", C.amber2, C.amber);
    addText(slide, { x: 650, y: 426, w: 540, h: 18, text: "输出目标", size: 18, bold: true, color: C.text });
    addMetric(slide, 650, 454, 164, 134, "5分钟", "调剂匹配响应", C.amber);
    addMetric(slide, 832, 454, 164, 134, "98%+", "匹配成功率", C.blue);
    addMetric(slide, 1014, 454, 164, 134, "有序", "预约分配", C.teal);
    addText(slide, { x: 650, y: 604, w: 540, h: 18, text: "让闲置的烤房尽快回到烘烤现场。", size: 14, color: C.muted, align: "center" });
    setNotes(
      slide,
      "智管这页核心讲‘调度’。用户最关心的不是模型复杂度，而是高峰期能不能快、能不能准、能不能让空闲资源重新上线。这里建议把调剂说成一个预约分配网络，强调它既解决效率问题，也解决公平问题。",
      [
        "用户提供的调剂方向与响应指标",
        `项目底稿：${path.join(ROOT, "generate_report.js")}`,
      ],
    );
    addFooter(slide, 10);
  }

  // 11 model 3 results
  {
    const slide = presentation.slides.add();
    slide.background.fill = C.bg;
    addSectionHeader(slide, 11, "智管成效：调剂更快，预约更稳，资源更活", "通过统一调度口径，烤房从“闲着”变成“流转起来”", C.amber);
    addMetric(slide, 72, 156, 206, 126, "5分钟", "调剂响应", C.amber);
    addMetric(slide, 292, 156, 206, 126, "98%+", "匹配成功率", C.blue);
    addMetric(slide, 512, 156, 206, 126, "2442座", "烤房资源", C.teal);
    addMetric(slide, 732, 156, 206, 126, "7个乡镇", "业务覆盖", C.green);
    addMetric(slide, 952, 156, 206, 126, "704户", "烟农服务", C.violet);
    addBox(slide, { x: 72, y: 316, w: 514, h: 320, fill: "#ffffff", line: "#fff2cf", radius: "rounded-3xl", shadow: "shadow-md" });
    addText(slide, { x: 96, y: 340, w: 440, h: 24, text: "推广时的讲法", size: 22, bold: true, color: C.text });
    addText(slide, {
      x: 96,
      y: 384,
      w: 450,
      h: 210,
      text: "• 烘烤高峰期，系统优先把空闲资源推到最需要的地方。\n• 非高峰期，平台保留预约和待调剂列表，资源不会长期闲置。\n• 管理者能看到调剂全过程，烟农也能看到自己的排位和等待状态。\n• 资源调度从“人工协调”升级为“平台协同”。",
      size: 18,
      color: C.sub,
    });
    addBox(slide, { x: 618, y: 316, w: 590, h: 320, fill: "#ffffff", line: "#fff2cf", radius: "rounded-3xl", shadow: "shadow-md" });
    addText(slide, { x: 644, y: 340, w: 540, h: 24, text: "调度结果", size: 22, bold: true, color: C.text });
    addChart(slide, {
      x: 644,
      y: 382,
      w: 536,
      h: 218,
      title: "覆盖与效率",
      categories: ["乡镇", "烤房", "烟农"],
      values: [7, 2442, 704],
      seriesName: "规模",
      fill: C.amber,
      yMax: 2600,
    });
    addText(slide, { x: 644, y: 600, w: 536, h: 18, text: "调剂不是“凑合用”，而是“按规则用好”。", size: 13, color: C.muted, align: "center" });
    setNotes(
      slide,
      "这页要把共享资源的规模感讲出来。领导通常最关心覆盖面和推广性，所以把 7 个乡镇、2442 座烤房、704 户烟农这几个数字并排放在一起，会很有说服力。最后再回扣一句：调剂不是临时协调，而是长期调度。",
      [
        "用户提供的总体运行成效数据",
        `项目底稿：${path.join(ROOT, "generate_report.js")}`,
      ],
    );
    addFooter(slide, 11);
  }

  // 12 functions and users
  {
    const slide = presentation.slides.add();
    slide.background.fill = "#f8fbff";
    addSectionHeader(slide, 12, "三大模型落成五大功能，服务三类对象", "一个平台，三个对象，五个功能，最终都要回到业务现场", C.green);
    const objCards = [
      { x: 76, title: "烟农", accent: C.blue, body: "查看烤房\n预约分配\n临时调剂\n评价反馈" },
      { x: 442, title: "技术员", accent: C.teal, body: "查看面板\n巡检评分\n报修处理\n验收闭环" },
      { x: 808, title: "管理者", accent: C.amber, body: "全局监管\n资金安排\n选址决策\n绩效评估" },
    ];
    for (const c of objCards) {
      addBox(slide, {
        x: c.x,
        y: 170,
        w: 340,
        h: 172,
        fill: "#ffffff",
        line: c.accent,
        radius: "rounded-3xl",
        shadow: "shadow-md",
      });
      addPill(slide, {
        x: c.x + 22,
        y: 188,
        w: 72,
        h: 30,
        text: c.title,
        fill: `${c.accent}22`,
        color: c.accent,
        border: `${c.accent}44`,
        size: 15,
      });
      addText(slide, {
        x: c.x + 22,
        y: 232,
        w: 296,
        h: 96,
        text: c.body,
        size: 19,
        bold: true,
        color: C.text,
        align: "center",
      });
    }
    addBox(slide, { x: 76, y: 388, w: 1080, h: 234, fill: "#ffffff", line: "#dbeafe", radius: "rounded-3xl", shadow: "shadow-md" });
    addText(slide, { x: 98, y: 410, w: 300, h: 24, text: "五大功能", size: 22, bold: true, color: C.text });
    const func = [
      ["烤房调剂", C.rose],
      ["烤房维护", C.teal],
      ["新建选址", C.blue],
      ["反馈评价", C.green],
      ["资金分配", C.amber],
    ];
    func.forEach((f, i) => {
      addBox(slide, {
        x: 100 + i * 200,
        y: 460,
        w: 170,
        h: 96,
        fill: "#ffffff",
        line: `${f[1]}66`,
        radius: "rounded-2xl",
        shadow: "shadow-none",
      });
      addBox(slide, {
        x: 155 + i * 200,
        y: 476,
        w: 60,
        h: 28,
        fill: `${f[1]}22`,
        line: `${f[1]}22`,
        radius: "rounded-full",
        shadow: "shadow-none",
        text: "功能",
        size: 12,
        color: f[1],
        bold: true,
        align: "center",
      });
      addText(slide, {
        x: 116 + i * 200,
        y: 516,
        w: 138,
        h: 28,
        text: f[0],
        size: 18,
        bold: true,
        color: f[1],
        align: "center",
      });
    });
    addText(slide, {
      x: 160,
      y: 646,
      w: 960,
      h: 20,
      text: "三类对象各有入口，五大功能各自成链，最后都回到一张平台、一套口径。",
      size: 13,
      color: C.muted,
      align: "center",
    });
    setNotes(
      slide,
      "这一页是承上启下页，要把‘模型’和‘用户’连起来。建议直接讲三类对象，再讲五大功能，最后收回到‘一套口径’。这样后面的演示页就顺理成章：每个对象只讲自己最常用的功能，领导会更容易理解落地价值。",
      [
        "用户提供的五大功能与三类对象要求",
        `项目底稿：${path.join(ROOT, "0820-1蓝图.png")}`,
      ],
    );
    addFooter(slide, 12);
  }

  // 13 smoke farmer demo
  {
    const slide = presentation.slides.add();
    slide.background.fill = C.bg;
    addSectionHeader(slide, 13, "烟农端：看烤房、抢资源、做评价", "这一页建议讲“自己能不能用、用得顺不顺”", C.blue);
    addBox(slide, { x: 72, y: 150, w: 352, h: 508, fill: "#ffffff", line: "#dbeafe", radius: "rounded-3xl", shadow: "shadow-md" });
    addText(slide, { x: 96, y: 174, w: 300, h: 24, text: "烟农主要做什么", size: 22, bold: true, color: C.text });
    addText(slide, {
      x: 96,
      y: 222,
      w: 286,
      h: 236,
      text: "• 查看周边可用烤房\n• 发起预约或临时申请\n• 接收调剂结果与提醒\n• 完成烘烤后提交评价反馈",
      size: 19,
      color: C.sub,
    });
    addMetric(slide, 96, 490, 136, 118, "1部手机", "完成预约和反馈", C.blue);
    addMetric(slide, 248, 490, 98, 118, "24h", "随时查看", C.teal);
    addBox(slide, { x: 452, y: 150, w: 756, h: 508, fill: "#ffffff", line: "#dbeafe", radius: "rounded-3xl", shadow: "shadow-md" });
    slide.images.add({
      blob: demo1,
      contentType: "image/png",
      alt: "烟农端截图",
      fit: "cover",
      position: { left: 474, top: 170, width: 712, height: 468 },
      geometry: "roundRect",
      borderRadius: "rounded-2xl",
    });
    setNotes(
      slide,
      "烟农端先讲‘看得见’，再讲‘用得上’。重点不是功能有多少，而是烟农能不能快速看到可用烤房、能不能发起预约、能不能在结束后顺手评价。这样讲，用户会感觉平台真的跟自己有关。",
      [
        `示意截图：${ASSETS.demo1}`,
        `参考来源：${path.join(ROOT, "共享烤房0820.pptx")}`,
      ],
    );
    addFooter(slide, 13);
  }

  // 14 technician demo
  {
    const slide = presentation.slides.add();
    slide.background.fill = C.bg;
    addSectionHeader(slide, 14, "技术员端：看面板、巡检评分、闭环报修", "技术员的重点是把“巡检”变成“有据可循的处置”", C.teal);
    addBox(slide, { x: 72, y: 150, w: 352, h: 508, fill: "#ffffff", line: "#d9f7f3", radius: "rounded-3xl", shadow: "shadow-md" });
    addText(slide, { x: 96, y: 174, w: 300, h: 24, text: "技术员主要做什么", size: 22, bold: true, color: C.text });
    addText(slide, {
      x: 96,
      y: 222,
      w: 286,
      h: 236,
      text: "• 查看烤房健康面板\n• 进行巡检评分和留痕\n• 发起报修与维修流转\n• 验收闭环并回填结果",
      size: 19,
      color: C.sub,
    });
    addMetric(slide, 96, 490, 136, 118, "巡检", "由经验判断变评分", C.teal);
    addMetric(slide, 248, 490, 98, 118, "闭环", "工单可追溯", C.blue);
    addBox(slide, { x: 452, y: 150, w: 756, h: 508, fill: "#ffffff", line: "#d9f7f3", radius: "rounded-3xl", shadow: "shadow-md" });
    slide.images.add({
      blob: demo2,
      contentType: "image/png",
      alt: "技术员端截图",
      fit: "cover",
      position: { left: 474, top: 170, width: 712, height: 468 },
      geometry: "roundRect",
      borderRadius: "rounded-2xl",
    });
    setNotes(
      slide,
      "技术员端要强调两件事：第一，巡检不再是翻本子，而是看面板；第二，维修不再是口头通知，而是工单闭环。这样技术员会把它理解成工作减负，而不是额外负担。",
      [
        `示意截图：${ASSETS.demo2}`,
        `参考来源：${path.join(ROOT, "共享烤房0820.pptx")}`,
      ],
    );
    addFooter(slide, 14);
  }

  // 15 manager demo
  {
    const slide = presentation.slides.add();
    slide.background.fill = C.bg;
    addSectionHeader(slide, 15, "管理者端：看全局、管资金、做决策", "管理者的重点是把数据变成能下决心的依据", C.amber);
    addBox(slide, { x: 72, y: 150, w: 352, h: 508, fill: "#ffffff", line: "#fff2cf", radius: "rounded-3xl", shadow: "shadow-md" });
    addText(slide, { x: 96, y: 174, w: 300, h: 24, text: "管理者主要做什么", size: 22, bold: true, color: C.text });
    addText(slide, {
      x: 96,
      y: 222,
      w: 286,
      h: 236,
      text: "• 看全县资源分布与健康状态\n• 统筹维修资金和改造计划\n• 依据模型做新建选址决策\n• 用绩效看板评估整体成效",
      size: 19,
      color: C.sub,
    });
    addMetric(slide, 96, 490, 136, 118, "全局", "一屏看全县", C.amber);
    addMetric(slide, 248, 490, 98, 118, "决策", "有依据", C.teal);
    addBox(slide, { x: 452, y: 150, w: 756, h: 508, fill: "#ffffff", line: "#fff2cf", radius: "rounded-3xl", shadow: "shadow-md" });
    slide.images.add({
      blob: demo3,
      contentType: "image/png",
      alt: "管理者端截图",
      fit: "cover",
      position: { left: 474, top: 170, width: 712, height: 468 },
      geometry: "roundRect",
      borderRadius: "rounded-2xl",
    });
    setNotes(
      slide,
      "管理者端要强调‘全局’和‘依据’。要让领导感觉这个平台不是只给基层用的，而是能把资源、资金和选址三件事连起来，支撑整体治理。这里不必讲很多按钮，只讲可以看见什么、可以决定什么。",
      [
        `示意截图：${ASSETS.demo3}`,
        `参考来源：${path.join(ROOT, "共享烤房0820.pptx")}`,
      ],
    );
    addFooter(slide, 15);
  }

  // 16 assistants and results
  {
    const slide = presentation.slides.add();
    slide.background.fill = "#f8fbff";
    addSectionHeader(slide, 16, "三类 AI 助手把前中后台串成一个整体", "下一步重点是预警、定制化报告和年度报告", C.violet);
    const assistants = [
      { x: 78, title: "烟农助手", accent: C.blue, body: "查烤房、看预约、收提醒\n帮助烟农少跑腿" },
      { x: 442, title: "技术员助手", accent: C.teal, body: "查健康、看工单、做诊断\n帮助技术员快处置" },
      { x: 806, title: "管理者助手", accent: C.amber, body: "看成效、出报告、做预测\n帮助管理者做决策" },
    ];
    for (const a of assistants) {
      addBox(slide, {
        x: a.x,
        y: 154,
        w: 316,
        h: 166,
        fill: "#ffffff",
        line: a.accent,
        radius: "rounded-3xl",
        shadow: "shadow-md",
      });
      addPill(slide, {
        x: a.x + 20,
        y: 172,
        w: 92,
        h: 30,
        text: a.title,
        fill: `${a.accent}22`,
        color: a.accent,
        border: `${a.accent}44`,
        size: 15,
      });
      addText(slide, {
        x: a.x + 20,
        y: 220,
        w: 276,
        h: 84,
        text: a.body,
        size: 20,
        bold: true,
        color: C.text,
        align: "center",
      });
    }
    addBox(slide, { x: 78, y: 356, w: 1100, h: 272, fill: "#ffffff", line: "#dbeafe", radius: "rounded-3xl", shadow: "shadow-md" });
    addText(slide, { x: 104, y: 382, w: 300, h: 24, text: "整体运行成效", size: 22, bold: true, color: C.text });
    const stats = [
      ["7个乡镇", "业务覆盖"],
      ["2442座", "烤房资源"],
      ["704户", "烟农服务"],
      ["≤5分钟", "调剂响应"],
      ["≤2小时", "维护响应"],
      ["85%", "工作量降低"],
      ["95%+", "数据有效率"],
      ["98%+", "匹配成功率"],
    ];
    stats.forEach((s, i) => {
      const row = Math.floor(i / 4);
      const col = i % 4;
      addMetric(slide, 104 + col * 260, 430 + row * 112, 220, 96, s[0], s[1], [C.blue, C.teal, C.amber, C.green][i % 4]);
    });
    addText(slide, {
      x: 112,
      y: 646,
      w: 1038,
      h: 20,
      text: "未来深化：1 预警预测  2 定制化报告  3 年度报告  4 自然灾害前置预警",
      size: 14,
      color: C.muted,
      align: "center",
    });
    setNotes(
      slide,
      "这一页把三类助手和总体成效放在一起讲。先说三类助手分别服务谁，再把项目的核心指标一次性收口，最后顺带抬出未来深化方向：预警、报告、年度总结和灾害前置预警。这样既有成果，也有下一步空间。",
      [
        "用户提供的总体运行成效与未来深化要求",
        `项目底稿：${path.join(ROOT, "generate_report.js")}`,
      ],
    );
    addFooter(slide, 16);
  }

  // 17 closing
  {
    const slide = presentation.slides.add();
    slide.background.fill = "#ffffff";
    slide.images.add({
      blob: closingBg,
      contentType: "image/jpeg",
      alt: "收尾背景",
      fit: "cover",
      position: { left: 0, top: 0, width: 1280, height: 720 },
    });
    addBox(slide, {
      x: 0,
      y: 0,
      w: 1280,
      h: 720,
      fill: "#0f172a70",
      line: "none",
      radius: 0,
      shadow: "shadow-none",
      geometry: "rect",
    });
    addText(slide, {
      x: 700,
      y: 190,
      w: 500,
      h: 132,
      text: "让每一间烤房\n发挥最大价值",
      size: 42,
      bold: true,
      color: "#ffffff",
      align: "right",
    });
    addText(slide, {
      x: 726,
      y: 342,
      w: 470,
      h: 26,
      text: "共享烤房 · 绿色烟叶烘烤的数智化升级",
      size: 18,
      color: "#e2e8f0",
      align: "right",
    });
    addPill(slide, {
      x: 820,
      y: 394,
      w: 120,
      h: 34,
      text: "谢谢聆听",
      fill: "#ffffff22",
      color: "#ffffff",
      border: "#ffffff44",
      size: 16,
    });
    addPill(slide, {
      x: 954,
      y: 394,
      w: 120,
      h: 34,
      text: "欢迎交流",
      fill: "#ffffff22",
      color: "#ffffff",
      border: "#ffffff44",
      size: 16,
    });
    addText(slide, {
      x: 706,
      y: 478,
      w: 490,
      h: 26,
      text: "盘活烤房资源，数智共享烤房。",
      size: 21,
      bold: true,
      color: "#ffffff",
      align: "right",
    });
    addText(slide, {
      x: 706,
      y: 528,
      w: 490,
      h: 20,
      text: "湖北省烟草公司宜昌市公司 · 烟叶条线",
      size: 13,
      color: "#cbd5e1",
      align: "right",
    });
    setNotes(
      slide,
      "收尾页不要再展开功能，直接回到一句最容易记住的话：让每一间烤房发挥最大价值。最后补一句 slogan，形成完整闭环。这个页的职责是留印象，不是补信息。",
      [
        `背景图：${ASSETS.closingBg}`,
        "用户提供的收尾要求与主旨表达",
      ],
    );
    addFooter(slide, 17);
  }

  const pptx = await PresentationFile.exportPptx(presentation);
  await pptx.save(OUT);

  const renderedDir = path.join(WORK, "rendered");
  await fs.mkdir(renderedDir, { recursive: true });
  for (let i = 0; i < presentation.slides.items.length; i += 1) {
    const slide = presentation.slides.items[i];
    const png = await presentation.export({ slide, format: "png", scale: 1 });
    const bytes = new Uint8Array(await png.arrayBuffer());
    await fs.writeFile(path.join(renderedDir, `slide-${String(i + 1).padStart(2, "0")}.png`), bytes);
  }

  const montage = await presentation.export({ format: "webp", montage: true, scale: 1 });
  await fs.writeFile(path.join(WORK, "deck-montage.webp"), new Uint8Array(await montage.arrayBuffer()));
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
