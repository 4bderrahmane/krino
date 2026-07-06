# Security Policy

## Supported versions

Krino is developed on a single branch. Only the latest commit on `main` is supported;
there are no maintained release lines.

## Reporting a vulnerability

Please **do not open a public issue** for security problems.

- Preferred: report privately via
  [GitHub security advisories](https://github.com/4bderrahmane/krino/security/advisories/new).
- Alternatively: email **me@4bderrahmane.com** with `[krino security]` in the subject.

Include what you can of: the affected endpoint or component, reproduction steps, the
impact you believe it has, and any proof-of-concept. You'll get an acknowledgement within
**7 days** (this is a solo-maintained project) and a status update once the report is
triaged. Fixed issues are credited in the advisory unless you prefer otherwise.

## Scope

In scope is the code in this repository: the Spring Boot API (`server/`), the React client
(`client/`), and the Docker/Compose configuration.

Out of scope: findings that require a compromised host or database, reports about
third-party dependencies without a demonstrated impact on this application, volumetric
denial of service, and social engineering.

## Safe harbor

Good-faith security research on your own deployment of this project is welcome. Don't
access or modify data that isn't yours, don't degrade service for others, and give a
reasonable window to fix before disclosing publicly. Research following these rules will
not be met with legal action.

For the design-level view of the threats considered and the controls in place, see the
[threat model](THREAT_MODEL.md).
