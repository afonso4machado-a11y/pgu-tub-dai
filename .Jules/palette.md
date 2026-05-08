## 2024-05-18 - Missing aria-label on icon-only buttons
**Learning:** Found that multiple icon-only buttons (like theme switchers, notifications, and logout) in the TopHeader and PassengerApp components lacked `aria-label`s. Screen readers cannot properly identify these buttons without them.
**Action:** Always ensure any icon-only interactive elements in the application have a descriptive `aria-label` for better accessibility.
