## Description

What changed? Why? (the code + comments should speak for itself on the "how")

Fun screenshots ir a cool video or something are super helpful as well. If this touches platform-specific behavior, this is where test evidence should be collected.

## Meta

Merge checklist:
- [ ] Pull Request title is [short, imperitive summary](https://cbea.ms/git-commit/) of proposed changes
- [ ] The description documents the _what_ and _why_
- [ ] If this PR touches photon-serde, all messages have been regenerated and hashes have not changed unexpectedly
- [ ] If this PR touches configuration, this is backwards compatible with settings back to v2024.3.1
- [ ] If this PR addresses a bug, a regression test for it is added
