## Description

What changed? Why? (the code + comments should speak for itself on the "how")

Include fun testing screenshots or a cool video, to collect test evidence in a place where we can later reference it. Including proof this change was tested makes reviewing easier, helps us make sure we tested all our edge cases, and helps provide context for the future.

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
