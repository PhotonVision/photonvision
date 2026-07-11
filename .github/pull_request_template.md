## Description

**What changed?**
<!-- Describe the change concisely -->

**Why?**
<!-- Explain the reasoning and any issues this addresses -->

## Testing

- [ ] I have tested this change locally
- [ ] Test evidence (screenshots, videos, or test results):

<!-- Paste screenshots or video links here -->

## Related Issues

Closes #<!-- issue number -->

---

## AI Disclosure

- [ ] This PR was authored entirely by me
- [ ] This PR includes AI-generated code (e.g., from GitHub Copilot, ChatGPT)
  - [ ] If yes, I have reviewed all AI-generated code for correctness and security
  - [ ] If yes, please describe which parts were AI-assisted:

<!-- Describe AI involvement here if applicable -->

---

## Merge Checklist

- [ ] PR title is [short, imperative summary](https://cbea.ms/git-commit/) of changes
- [ ] Description documents the *what* and *why*


### Additional Checks (if applicable)

- [ ] **User-facing changes?** User documentation is updated
- [ ] **Breaking changes?** Migration guide is included in description
- [ ] **Bug fix?** Regression test is added
- [ ] **New dependency?** License compatibility verified
- [ ] **Serde changes?** Run `./gradlew photon-serde:generateProto` and confirm no unexpected hash changes
- [ ] **Configuration changes?** Backwards compatible with previous season's last release
- [ ] **Pipeline/data exchange changes?** Frontend types updated in `src/main/vue/types/`
