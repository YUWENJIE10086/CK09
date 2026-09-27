---
name: "interaction-design"
description: "Adds fluid transitions, micro-interactions, and dynamic feedback to web pages. Invoke when pages feel static, need skeleton screens, hover effects, scroll parallax, or smooth transitions."
---

# Interaction Design Skill

You are an interaction design specialist who transforms static pages into engaging, responsive experiences. Every interaction should feel intentional, smooth, and delightful.

## Core Philosophy

- **Feedback for every action** — Users should always know their action was received
- **Progressive disclosure** — Reveal complexity gradually
- **Meaningful motion** — Animations should communicate state changes, not just decorate
- **Performance first** — Use GPU-accelerated properties (transform, opacity) only

## Interaction Categories

### 1. Skeleton Screens (骨架屏)

Use during data loading to prevent layout shift:

```css
.skeleton {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: 4px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
```

**When to use**: API calls, page transitions, lazy-loaded content

### 2. Button Micro-Feedback (按钮微反馈)

```css
/* Press effect */
.btn-press {
  transition: transform 0.1s ease, box-shadow 0.1s ease;
}
.btn-press:active {
  transform: scale(0.96);
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.1);
}

/* Ripple effect */
.btn-ripple {
  position: relative;
  overflow: hidden;
}
.btn-ripple::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at var(--x) var(--y), rgba(255,255,255,0.3) 0%, transparent 60%);
  opacity: 0;
  transition: opacity 0.3s;
}
.btn-ripple:active::after {
  opacity: 1;
}

/* Hover glow */
.btn-glow:hover {
  box-shadow: 0 0 20px rgba(64, 158, 255, 0.4);
  transform: translateY(-1px);
}
```

### 3. Scroll Parallax (滚动视差)

```css
.parallax-container {
  perspective: 1px;
  height: 100vh;
  overflow-x: hidden;
  overflow-y: auto;
}

.parallax-layer-back {
  transform: translateZ(-2px) scale(3);
}

.parallax-layer-front {
  transform: translateZ(0);
}
```

**JavaScript approach** (more control):
```typescript
window.addEventListener('scroll', () => {
  const scrollY = window.scrollY
  document.querySelectorAll('[data-parallax]').forEach(el => {
    const speed = parseFloat(el.dataset.parallax || '0.5')
    el.style.transform = `translateY(${scrollY * speed}px)`
  })
})
```

### 4. Page Transitions (页面过渡)

```css
/* Fade transition */
.page-enter-active, .page-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(20px);
}
.page-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

/* Slide transition */
.slide-enter-active, .slide-leave-active {
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.slide-enter-from { transform: translateX(100%); }
.slide-leave-to { transform: translateX(-100%); }
```

### 5. List Animations (列表动画)

```css
.list-enter-active, .list-leave-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.list-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}
.list-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
.list-move {
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
```

### 6. Card Hover Effects (卡片悬浮效果)

```css
/* 3D Tilt */
.card-tilt {
  transition: transform 0.3s ease;
  transform-style: preserve-3d;
}
.card-tilt:hover {
  transform: perspective(1000px) rotateX(2deg) rotateY(-2deg) scale(1.02);
}

/* Reveal border */
.card-border-reveal {
  position: relative;
  border: 1px solid transparent;
  transition: border-color 0.3s ease;
}
.card-border-reveal:hover {
  border-color: #409eff;
}

/* Spotlight effect */
.card-spotlight {
  position: relative;
  overflow: hidden;
}
.card-spotlight::before {
  content: '';
  position: absolute;
  width: 200px; height: 200px;
  background: radial-gradient(circle, rgba(255,255,255,0.15), transparent 70%);
  border-radius: 50%;
  transform: translate(var(--mouse-x, -100px), var(--mouse-y, -100px));
  pointer-events: none;
  transition: opacity 0.3s;
  opacity: 0;
}
.card-spotlight:hover::before { opacity: 1; }
```

### 7. Loading States (加载状态)

```css
/* Spinner */
.spinner {
  width: 24px; height: 24px;
  border: 3px solid #e0e0e0;
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Progress bar */
.progress-bar {
  height: 3px;
  background: linear-gradient(90deg, #409eff, #67c23a);
  animation: progress 2s ease-in-out;
}
@keyframes progress { from { width: 0; } to { width: 100%; } }

/* Pulse dot */
.pulse-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #409eff;
  animation: pulse 1.5s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.5); }
}
```

### 8. Toast & Notification Animations (通知动画)

```css
.toast-enter-active { animation: slideInRight 0.3s ease; }
.toast-leave-active { animation: slideOutRight 0.3s ease; }

@keyframes slideInRight {
  from { transform: translateX(100%); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}
@keyframes slideOutRight {
  from { transform: translateX(0); opacity: 1; }
  to { transform: translateX(100%); opacity: 0; }
}
```

### 9. Form Interactions (表单交互)

```css
/* Floating label */
.float-label input:focus + label,
.float-label input:not(:placeholder-shown) + label {
  transform: translateY(-24px) scale(0.85);
  color: #409eff;
}

/* Input focus ring */
.input-focus:focus {
  outline: none;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.2);
  border-color: #409eff;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

/* Validation shake */
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-8px); }
  75% { transform: translateX(8px); }
}
.shake { animation: shake 0.3s ease; }
```

### 10. Scroll-Triggered Animations (滚动触发动画)

```typescript
// Intersection Observer approach
const observer = new IntersectionObserver((entries) => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      entry.target.classList.add('animate-in')
      observer.unobserve(entry.target)
    }
  })
}, { threshold: 0.1 })

document.querySelectorAll('[data-animate]').forEach(el => observer.observe(el))
```

```css
[data-animate] {
  opacity: 0;
  transform: translateY(30px);
  transition: opacity 0.6s ease, transform 0.6s ease;
}
[data-animate].animate-in {
  opacity: 1;
  transform: translateY(0);
}

/* Stagger children */
[data-animate].animate-in > *:nth-child(1) { transition-delay: 0.05s; }
[data-animate].animate-in > *:nth-child(2) { transition-delay: 0.1s; }
[data-animate].animate-in > *:nth-child(3) { transition-delay: 0.15s; }
```

## Easing Functions Reference

```css
:root {
  --ease-out-expo: cubic-bezier(0.16, 1, 0.3, 1);
  --ease-out-back: cubic-bezier(0.34, 1.56, 0.64, 1);
  --ease-in-out-quart: cubic-bezier(0.76, 0, 0.24, 1);
  --ease-spring: cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
```

## Performance Rules

1. Only animate `transform` and `opacity` — they're GPU-composited
2. Use `will-change` sparingly and remove after animation
3. Prefer CSS animations over JS for simple effects
4. Use `requestAnimationFrame` for JS animations
5. Debounce scroll handlers
6. Respect `prefers-reduced-motion`:

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

## Workflow

1. **Audit the page** — identify static elements that need life
2. **Prioritize** — loading states > hover feedback > scroll effects > decorative
3. **Apply progressively** — start with essential interactions, add delight layers
4. **Test performance** — ensure 60fps on target devices
5. **Verify accessibility** — all interactions work with keyboard, respect reduced motion
