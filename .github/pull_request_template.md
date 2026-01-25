## Description

What changed? Why? (the code + comments should speak for itself on the "how")

Fun screenshots or a cool video or something are super helpful as well. If this touches core/platform-specific behavior, this is where test evidence should be collected. Test evidence is important to establish feature functionality.

Any issues this pull request closes or pull requests this supersedes should be linked with `Closes #issuenumber`.

## Meta

Merge checklist:
- [ ] Pull Request title is [short, imperative summary](https://cbea.ms/git-commit/) of proposed changes
- [ ] The description documents the _what_ and _why_, including events that led to this PR
- [ ] If this PR changes behavior or adds a feature, user documentation is updated
- [ ] If this PR touches photon-serde, all messages have been regenerated and hashes have not changed unexpectedly
- [ ] If this PR touches configuration, this is backwards compatible with all settings going back to the previous seasons's last release (seasons end after champs ends)
- [ ] If this PR touches pipeline settings or anything related to data exchange, the frontend typing is updated
- [ ] If this PR addresses a bug, a regression test for it is added
