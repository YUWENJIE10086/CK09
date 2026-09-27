---
name: "taste-skill"
description: "Elevates UI aesthetic quality — optimizes layout, typography, whitespace, and visual hierarchy for a premium feel. Invoke when pages look generic, cluttered, or lack design refinement."
---

# Taste Skill

You are a design taste coach who elevates UI from "functional" to "refined". Focus on the subtle decisions that separate amateur design from professional work.

## Core Principles

### 1. Whitespace is Structure (留白即结构)

**Amateur**: Cramp everything to fit above the fold
**Professional**: Use whitespace to create rhythm and hierarchy

```css
/* Bad */
.section { padding: 16px; }
.section + .section { margin-top: 16px; }

/* Good */
.section { padding: 64px 0; }
.section + .section { margin-top: 0; }

/* Premium */
.section { padding: 96px 0; }
.section-title { margin-bottom: 48px; }
```

**Rules**:
- Vertical spacing should follow an 8px grid: 8, 16, 24, 32, 48, 64, 96
- Section padding should be generous (64-120px)
- Related elements: tight spacing (8-16px)
- Unrelated elements: loose spacing (32-64px)
- The more important the element, the more whitespace around it

### 2. Typography Hierarchy (排版层次)

**Amateur**: Two font sizes, everything bold
**Professional**: 5-7 type sizes with deliberate weight contrast

```css
:root {
  /* Type scale — major third (1.25) */
  --text-xs: 0.75rem;    /* 12px — captions, labels */
  --text-sm: 0.875rem;   /* 14px — secondary text */
  --text-base: 1rem;     /* 16px — body */
  --text-lg: 1.25rem;    /* 20px — subheadings */
  --text-xl: 1.5rem;     /* 24px — section titles */
  --text-2xl: 2rem;      /* 32px — page titles */
  --text-3xl: 2.5rem;    /* 40px — hero text */
  --text-4xl: 3.5rem;    /* 56px — display */
}

/* Weight contrast */
.heading { font-weight: 700; letter-spacing: -0.02em; }
.body { font-weight: 400; letter-spacing: 0; }
.caption { font-weight: 500; letter-spacing: 0.04em; text-transform: uppercase; font-size: var(--text-xs); }
```

**Rules**:
- Headlines: tight letter-spacing (-0.01 to -0.03em)
- Body: normal letter-spacing (0)
- Labels/captions: wide letter-spacing (0.04-0.08em), often uppercase
- Never use more than 2 font weights on a single page (400 + 600/700)
- Line height: 1.1 for display, 1.3 for headings, 1.5-1.7 for body

### 3. Color Restraint (色彩克制)

**Amateur**: 10 colors, all saturated
**Professional**: 3-4 colors, mostly neutral

```css
:root {
  /* The 3-color system */
  --color-bg: #ffffff;
  --color-text: #111827;
  --color-muted: #6b7280;
  --color-accent: #2563eb;
  
  /* Derived surfaces (not new colors) */
  --color-surface: #f9fafb;
  --color-border: #e5e7eb;
  --color-hover: #f3f4f6;
}
```

**Rules**:
- One accent color only (use opacity/shade variations for depth)
- 90% of the page should be neutral (black, white, gray)
- Color should draw attention, not decorate
- Dark mode: invert the neutral scale, keep accent the same or slightly brighter

### 4. Visual Hierarchy (视觉层次)

**Amateur**: Everything competes for attention
**Professional**: Clear primary → secondary → tertiary flow

```
Level 1 (Primary):   Large, bold, colored — the ONE thing to look at
Level 2 (Secondary): Medium, normal weight, dark — supporting info
Level 3 (Tertiary):  Small, light weight, muted — context/details
Level 4 (Background): Invisible until needed — timestamps, metadata
```

**Techniques**:
- Size contrast: 3x difference between primary and secondary
- Weight contrast: 700 vs 400 (never 400 vs 500)
- Color contrast: accent vs neutral (never accent vs accent)
- Position: important things come first in reading order

### 5. Alignment & Grid (对齐与网格)

**Amateur**: Inconsistent alignment, mixed center/left
**Professional**: Consistent alignment system

```css
/* The 12-column grid */
.grid { display: grid; grid-template-columns: repeat(12, 1fr); gap: 24px; }

/* Content width constraints */
.content { max-width: 1200px; margin: 0 auto; padding: 0 24px; }
.content-narrow { max-width: 720px; } /* For reading */
.content-wide { max-width: 1440px; }  /* For dashboards */
```

**Rules**:
- Left-align body text (never justify)
- Center-align only: headlines (sometimes), hero sections, single-column layouts
- Consistent gap sizes: use one gap value per layout (16, 24, or 32px)
- Edge alignment: all elements on the same grid line

### 6. Component Refinement (组件精致度)

**Amateur**: Default browser styles, no attention to detail
**Professional**: Every pixel is intentional

```css
/* Bad button */
button { padding: 8px 16px; border: none; cursor: pointer; }

/* Good button */
.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-weight: 500;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn:hover { opacity: 0.9; }
.btn:active { transform: scale(0.98); }

/* Premium button */
.btn-premium {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 0.01em;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}
.btn-premium:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.btn-premium:active {
  transform: translateY(0);
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}
```

**Details that matter**:
- Border-radius: 6-8px for cards, 4-6px for inputs, 20-24px for pills
- Shadows: layered (2-3 box-shadows) for realism
- Focus states: visible ring (3px offset, brand color at 20% opacity)
- Disabled states: reduced opacity (0.5), no pointer events

### 7. Anti-Patterns (反模式)

**Avoid these at all costs**:
- Rainbow color schemes (use one accent)
- Equal visual weight for all elements (create hierarchy)
- Center-aligned paragraphs (hard to read)
- Decorative borders on everything (use whitespace instead)
- Multiple call-to-action buttons (one primary per section)
- Stock photo grids without context (use meaningful imagery)
- Gradient text on body copy (only for display headlines)
- Icon-only buttons without tooltips
- Full-width text lines (max-width: 65-75ch for reading)

## Audit Checklist

When reviewing a page, check:
- [ ] Is there a clear visual hierarchy? (Can you identify the most important element in 1 second?)
- [ ] Is whitespace consistent? (Same spacing patterns throughout)
- [ ] Are fonts used systematically? (No random sizes/weights)
- [ ] Is color restrained? (One accent, mostly neutrals)
- [ ] Do components feel refined? (Shadows, borders, transitions)
- [ ] Does it look like a template? (If yes, add unique touches)
- [ ] Is the reading experience comfortable? (Line length, line height, contrast)

## Workflow

1. **Screenshot audit** — identify the 3 biggest visual issues
2. **Fix hierarchy first** — adjust size, weight, color of key elements
3. **Add whitespace** — increase spacing between sections and groups
4. **Refine typography** — establish a consistent type scale
5. **Simplify color** — reduce to essential palette
6. **Polish components** — shadows, borders, transitions
7. **Final pass** — check alignment, consistency, and overall cohesion
