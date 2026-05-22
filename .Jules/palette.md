
## 2024-05-13 - Missing ARIA Labels on Navigation Icons
**Learning:** Icon-only navigation buttons in custom components (like the custom calendar in OccupancyView.vue) frequently miss accessible names, rendering them invisible or confusing to screen readers.
**Action:** When auditing custom UI components with icon-only controls, always verify the presence of `aria-label` or equivalent screen-reader text to ensure full keyboard and assistive technology accessibility.
