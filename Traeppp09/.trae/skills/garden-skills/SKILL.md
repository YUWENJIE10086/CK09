---
name: "garden-skills"
description: "25 curated style recipes (Linear, Aesop, Pentagram, etc) with specific palettes, fonts, and design techniques. Invoke when user wants to apply a proven, named visual style to their project."
---

# Garden Skills — 25 Style Recipes

Each recipe is a complete design system you can apply to a project. Pick one and commit fully.

---

## 1. Linear (线性科技)

**Vibe**: Clean, fast, developer-tool aesthetic. Dark by default.

| Token | Value |
|-------|-------|
| BG | #0A0A0F |
| Surface | #12121A |
| Border | rgba(255,255,255,0.06) |
| Text Primary | #E8E8ED |
| Text Muted | #6B6B80 |
| Accent | #5B6CF7 |
| Accent Hover | #7B8AFF |
| Font Heading | Inter |
| Font Body | Inter |
| Font Mono | JetBrains Mono |
| Radius | 8px |
| Shadow | 0 2px 8px rgba(0,0,0,0.3) |

**Signature moves**: Subtle purple accent on dark, keyboard shortcut badges, minimal borders, compact spacing
**Anti-pattern**: Never use bright backgrounds, never use rounded pill shapes

---

## 2. Aesop (艾索普)

**Vibe**: Luxurious, editorial, slow-paced. Warm neutrals.

| Token | Value |
|-------|-------|
| BG | #F7F5F0 |
| Surface | #EDEAE3 |
| Border | #D4CFC5 |
| Text Primary | #2C2A25 |
| Text Muted | #7A7568 |
| Accent | #8B7355 |
| Font Heading | "Cormorant Garamond", serif |
| Font Body | "Source Sans Pro", sans-serif |
| Radius | 0px |
| Shadow | none |

**Signature moves**: No border-radius (sharp corners), serif headings, generous padding (80px+ sections), warm earth tones
**Anti-pattern**: Never use bright colors, never use rounded corners, never use sans-serif headings

---

## 3. Pentagram (五角设计)

**Vibe**: Bold, typographic, Swiss-inspired. High contrast.

| Token | Value |
|-------|-------|
| BG | #FFFFFF |
| Surface | #F5F5F5 |
| Border | #E0E0E0 |
| Text Primary | #000000 |
| Text Muted | #666666 |
| Accent | #FF3300 |
| Font Heading | "Helvetica Neue", sans-serif |
| Font Body | "Helvetica Neue", sans-serif |
| Radius | 0px |
| Shadow | none |

**Signature moves**: Red accent only, oversized typography (72px+), strict grid, no decoration, black/white/red only
**Anti-pattern**: Never use gradients, never use rounded corners, never use more than 3 colors

---

## 4. Stripe (条纹支付)

**Vibe**: Trustworthy, polished, gradient-forward.

| Token | Value |
|-------|-------|
| BG | #F6F9FC |
| Surface | #FFFFFF |
| Border | #E3E8EE |
| Text Primary | #1A1F36 |
| Text Muted | #697386 |
| Accent | #635BFF |
| Accent Gradient | linear-gradient(135deg, #635BFF, #7C3AED) |
| Font Heading | "Plus Jakarta Sans" |
| Font Body | "Plus Jakarta Sans" |
| Radius | 8px |
| Shadow | 0 2px 4px rgba(0,0,0,0.05), 0 8px 24px rgba(0,0,0,0.08) |

**Signature moves**: Purple gradient accents, layered shadows, clean card layouts, subtle background gradient mesh
**Anti-pattern**: Never use harsh shadows, never use flat backgrounds without subtle gradient

---

## 5. Notion (诺申)

**Vibe**: Clean, minimal, content-first. Monochrome with one accent.

| Token | Value |
|-------|-------|
| BG | #FFFFFF |
| Surface | #F7F6F3 |
| Border | #E9E9E7 |
| Text Primary | #37352F |
| Text Muted | #9B9A97 |
| Accent | #2383E2 |
| Font Heading | "Noto Sans SC", sans-serif |
| Font Body | "Noto Sans SC", sans-serif |
| Radius | 4px |
| Shadow | none |

**Signature moves**: Almost no shadows, 4px radius, text-driven UI, toggle/collapse patterns, subtle hover backgrounds
**Anti-pattern**: Never use large shadows, never use bright accent colors beyond blue

---

## 6. Vercel (维尔赛)

**Vibe**: Dark, sleek, developer-focused. Monochrome.

| Token | Value |
|-------|-------|
| BG | #000000 |
| Surface | #111111 |
| Border | #222222 |
| Text Primary | #FAFAFA |
| Text Muted | #888888 |
| Accent | #FFFFFF |
| Font Heading | "Inter" |
| Font Body | "Inter" |
| Font Mono | "Fira Code" |
| Radius | 8px |
| Shadow | 0 0 0 1px rgba(255,255,255,0.1) |

**Signature moves**: Pure black background, white accent, gradient borders on hover, monospace code blocks, triangle logo aesthetic
**Anti-pattern**: Never use colored backgrounds, never use rounded pill shapes

---

## 7. Apple (苹果)

**Vibe**: Premium, spacious, refined. Light with depth.

| Token | Value |
|-------|-------|
| BG | #FBFBFD |
| Surface | #FFFFFF |
| Border | rgba(0,0,0,0.08) |
| Text Primary | #1D1D1F |
| Text Muted | #86868B |
| Accent | #0071E3 |
| Font Heading | "SF Pro Display", "Inter" |
| Font Body | "SF Pro Text", "Inter" |
| Radius | 12px |
| Shadow | 0 2px 8px rgba(0,0,0,0.04), 0 8px 32px rgba(0,0,0,0.08) |

**Signature moves**: Massive whitespace, subtle shadows, blue accent, 12px radius, SF Pro typography, gradient text for hero
**Anti-pattern**: Never use harsh borders, never use small padding

---

## 8. Spotify (声田)

**Vibe**: Dark, vibrant, music-forward. Green accent.

| Token | Value |
|-------|-------|
| BG | #121212 |
| Surface | #181818 |
| Border | #282828 |
| Text Primary | #FFFFFF |
| Text Muted | #B3B3B3 |
| Accent | #1DB954 |
| Font Heading | "Circular", "Inter" |
| Font Body | "Circular", "Inter" |
| Radius | 8px |
| Shadow | none |

**Signature moves**: Green accent on dark, card hover → lift + green tint, rounded album art, gradient overlays
**Anti-pattern**: Never use light backgrounds, never use thin fonts

---

## 9. Figma (菲格玛)

**Vibe**: Playful, collaborative, design-tool. Purple accent.

| Token | Value |
|-------|-------|
| BG | #F5F5F5 |
| Surface | #FFFFFF |
| Border | #E5E5E5 |
| Text Primary | #333333 |
| Text Muted | #999999 |
| Accent | #A259FF |
| Font Heading | "Inter" |
| Font Body | "Inter" |
| Radius | 6px |
| Shadow | 0 1px 3px rgba(0,0,0,0.08) |

**Signature moves**: Purple accent, compact tool UI, icon-heavy, subtle shadows, friendly rounded corners
**Anti-pattern**: Never use dark backgrounds for main UI, never use serif fonts

---

## 10. GitHub (吉特哈布)

**Vibe**: Code-focused, functional, accessible.

| Token | Value |
|-------|-------|
| BG | #0D1117 |
| Surface | #161B22 |
| Border | #30363D |
| Text Primary | #C9D1D9 |
| Text Muted | #8B949E |
| Accent | #58A6FF |
| Font Heading | "Inter" |
| Font Body | "Inter" |
| Font Mono | "JetBrains Mono" |
| Radius | 6px |
| Shadow | 0 1px 3px rgba(0,0,0,0.3) |

**Signature moves**: Blue accent on dark, monospace code, diff colors (green/red), status dots, tab navigation
**Anti-pattern**: Never use bright backgrounds, never use decorative elements

---

## 11. Framer (弗拉默)

**Vibe**: Motion-first, creative, gradient-rich.

| Token | Value |
|-------|-------|
| BG | #000000 |
| Surface | #0A0A0A |
| Border | #1A1A1A |
| Text Primary | #FFFFFF |
| Text Muted | #888888 |
| Accent | #0055FF |
| Accent Gradient | linear-gradient(135deg, #0055FF, #00AAFF, #00FFCC) |
| Font Heading | "Inter" |
| Font Body | "Inter" |
| Radius | 12px |
| Shadow | 0 20px 60px rgba(0,85,255,0.15) |

**Signature moves**: Blue gradient, animated backgrounds, glassmorphism panels, 12px radius, glow effects
**Anti-pattern**: Never use flat solid backgrounds, never use small radius

---

## 12. Raycast (雷卡斯特)

**Vibe**: Spotlight-style, command palette, keyboard-first.

| Token | Value |
|-------|-------|
| BG | #1E1E2E |
| Surface | #2A2A3C |
| Border | #3A3A4E |
| Text Primary | #E8E8ED |
| Text Muted | #6C6C80 |
| Accent | #FF6363 |
| Font Heading | "SF Pro" |
| Font Body | "SF Pro" |
| Font Mono | "JetBrains Mono" |
| Radius | 12px |
| Shadow | 0 16px 64px rgba(0,0,0,0.5) |

**Signature moves**: Red accent, search-first UI, keyboard shortcuts everywhere, large rounded corners, deep shadows
**Anti-pattern**: Never use light mode, never use sharp corners

---

## 13. Arc Browser (弧光浏览器)

**Vibe**: Colorful, modern, sidebar-driven.

| Token | Value |
|-------|-------|
| BG | #F8F8FC |
| Surface | #FFFFFF |
| Border | #E8E8F0 |
| Text Primary | #1A1A2E |
| Text Muted | #6B6B80 |
| Accent | #7B68EE |
| Font Heading | "Inter" |
| Font Body | "Inter" |
| Radius | 12px |
| Shadow | 0 4px 16px rgba(0,0,0,0.06) |

**Signature moves**: Purple accent, sidebar navigation, colorful space icons, rounded everything, friendly feel
**Anti-pattern**: Never use harsh borders, never use monochrome

---

## 14. Tailwind UI (泰风UI)

**Vibe**: Component-driven, utility-first, professional.

| Token | Value |
|-------|-------|
| BG | #F9FAFB |
| Surface | #FFFFFF |
| Border | #E5E7EB |
| Text Primary | #111827 |
| Text Muted | #6B7280 |
| Accent | #4F46E5 |
| Font Heading | "Inter" |
| Font Body | "Inter" |
| Radius | 8px |
| Shadow | 0 1px 2px rgba(0,0,0,0.05) |

**Signature moves**: Indigo accent, clean card layouts, consistent 8px spacing, subtle shadows, professional feel
**Anti-pattern**: Never use decorative elements, never break the grid

---

## 15. Supabase (超基底)

**Vibe**: Developer-friendly, green-accented, documentation-first.

| Token | Value |
|-------|-------|
| BG | #1C1C1C |
| Surface | #2C2C2C |
| Border | #3C3C3C |
| Text Primary | #EDEDED |
| Text Muted | #8F8F8F |
| Accent | #3ECF8E |
| Font Heading | "Inter" |
| Font Body | "Inter" |
| Font Mono | "Fira Code" |
| Radius | 6px |
| Shadow | 0 4px 12px rgba(0,0,0,0.3) |

**Signature moves**: Green accent on dark, code examples, terminal aesthetic, clean docs layout
**Anti-pattern**: Never use light backgrounds, never use warm colors

---

## 16-25: Quick Reference

| # | Name | BG | Accent | Font | Radius | Key Trait |
|---|------|----|--------|------|--------|-----------|
| 16 | **Meridian** | #FAFAF8 | #C0562F | "DM Serif Display" + "DM Sans" | 4px | Warm editorial |
| 17 | **Kanban** | #F4F5F7 | #0079BF | "Nunito" | 4px | Board/task management |
| 18 | **Monocle** | #FFFCF0 | #2D2D2D | "Georgia", serif | 0px | Reading/long-form |
| 19 | **Neon** | #0A0A0A | #00FF88 | "Space Grotesk" + "JetBrains Mono" | 8px | Terminal/hacker |
| 20 | **Pastel** | #FFF8F0 | #FF8C6B | "Quicksand" + "Nunito" | 16px | Soft/friendly |
| 21 | **Brutalist** | #FFFFFF | #000000 | "Courier New", monospace | 0px | Raw/intentional |
| 22 | **Cosmos** | #0B0D17 | #6C63FF | "Space Grotesk" | 12px | Space/sci-fi |
| 23 | **Bamboo** | #F5F0E8 | #5B7B5E | "Noto Serif SC" + "Noto Sans SC" | 6px | Chinese/zen |
| 24 | **Copper** | #1A1410 | #C87941 | "Playfair Display" + "Lato" | 2px | Luxury/warm dark |
| 25 | **Ice** | #F0F4FF | #2563EB | "Outfit" + "Inter" | 10px | Cool/corporate |

---

## How to Apply a Recipe

1. **Pick one recipe** — don't mix recipes
2. **Set CSS variables** — copy the token table into `:root`
3. **Apply fonts** — import from Google Fonts or local files
4. **Build components** — follow the signature moves
5. **Avoid anti-patterns** — these are what make the style authentic
6. **Consistency check** — every element should feel like it belongs to the same system

## Recipe Extension

When a recipe doesn't perfectly fit, extend it minimally:
- Add 1-2 semantic colors (success, warning, danger) derived from the accent
- Adjust spacing scale if content density requires it
- Never change the core identity (BG, accent, font pairing, radius)
