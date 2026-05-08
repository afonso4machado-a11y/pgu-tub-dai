## 2024-05-24 - [Remove Hardcoded Secret]
**Vulnerability:** Hardcoded admin password in `Sistema.java` fallback.
**Learning:** Found a critical hardcoded credential `tub_uminho26` used as a fallback when the environment variable `PGU_ADMIN_PASSWORD` is not set.
**Prevention:** Always fail securely by refusing authentication when required secrets are missing, rather than falling back to a hardcoded string.
