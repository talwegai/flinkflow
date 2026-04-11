# Security Policy

The Flinkflow team takes the security of our streaming platform and the data it processes very seriously. This document describes our policy for handling security vulnerabilities.

## Supported Versions

Users are encouraged to use the latest stable release of Flinkflow to ensure they have the latest security patches.

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

If you discover a security vulnerability within Flinkflow, please report it to us as soon as possible. **Do not open a public GitHub issue.** Instead, please send an email to **security@talweg.ai**.

### What to include:
- A detailed description of the vulnerability.
- Steps to reproduce the issue (PoC).
- Any potential impact on users or infrastructure.

### What to expect:
- **Acknowledgement**: You will receive an initial response within 48 hours.
- **Triage**: We will investigate and validate the report.
- **Remediation**: Once confirmed, we will work on a fix and coordinate a disclosure timeline.
- **Credit**: If requested, we will credit you for the discovery in our release notes once the issue is resolved.

## Security Architecture

Flinkflow is designed with a **Zero-Trust** security model for guest code execution (Java/Python). 

For a detailed technical breakdown of our sandboxing architecture, including GraalVM and Janino isolation layers, please refer to the **[System Architecture Guide](01_ARCHITECTURE.md#security-sandboxing-zero-trust)**.


---
*Thank you for helping keep Flinkflow secure!*
