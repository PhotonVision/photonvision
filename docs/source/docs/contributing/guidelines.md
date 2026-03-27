# Welcome!

First and foremost, welcome to PhotonVision Development! We're pumped that you're interested to jump in, and help out!

Like most things in FIRST, PhotonVision is reliant on the hard work of dedicated volunteers. It doesn't exist without you. You're joining in with a strong tradition of open-source software development.

This page talks a bit about how we develop PhotonVision, as a community. It applies to all repos and community aspects for PhotonVision.

## Getting Started

The very first thing we'd recommend - get your computer set up to be able to build and run PhotonVision. [Docs for that are here](building-photon.md).

The two best ways to figure out what to do first:

1. Take a look at [the main PhotonVision Repo's Issues](https://github.com/PhotonVision/photonvision/issues) - especially those marked `bug` or `good first issue`!
2. Connect on [the Discord Server](https://discord.gg/wYxTwym) - Introduce yourself, and talk about what you're interested in!

From there - assign yourself to an issue if you intend to work it. Create a Fork in github, and make and test your changes. Once passing builds, meeting your expectations, and passing CI, open a pull request back to the main repo.

## Submitting A PR

Pull Requests are the mechanism used to ensure we only merge high-quality, reviewed, concrete, and organized changes to the codebase.

Things that peer reviewers will look for include:

* All CI checks are passing on the server.
* Documentation - does the PR match the issue? Is the description detailed?
* Cohesiveness - does the PR express a singular, related set of changes?
* Architecture - is the code consistent with other code around it? Is it maintainable for the long term?
* Testing - have unit tests been added as appropriate? Has the change been tested on real hardware?

Work as you can to clean up any changes requested. Once all changes are addressed, the contain should get merged, and included in the next release! Horary!

## General Developer Interaction

The main guiding principle: remember that we're all volunteers. While promptness is always appreciated (and occasionally required as release deadlines approach), it's important to remember each individual is only contributing when their personal schedule allows. Expect delays, prefer asynchronous communication, and be polite with reminders.

While most of the community members are either FIRST robotics mentors or students, the PhotonVision development team is primarily focused on delivering high quality software. Mentorship can and does occur, but is not the primary goal. Members are expected to do their learning fairly independently.

Seek to build trust in the quality of your work. Think carefully on your opinions before asking others to think about them too.

Bias toward action, and productionizable code. Limit the number of active PR's to help keep focus.

Finally, be sure to embody the ethos of Gracious Professionalism in all your actions, on all platforms in the project. See more in our [code of conduct](https://github.com/PhotonVision/.github/blob/738dfcb792fdbfc2e8408c0135e389179fc483c0/codeofcoduct.md)

### AI Usage

Coding assistants driven by Large Language Models ("AI") are extremely powerful tools, and have been used on more than one occasion to accelerate development.

PhotonVision still maintains a fundamental philosophy that the human submitting the pull request is responsible for the code and its behavior, regardless of the tools used to create it.

These tools can also generate a large volume of code changes, very rapidly. The above guidelines on PR quality still apply - large, undirected, or overly-scoped PR's are likely to be ignored, regardless of tooling used to generate them.

### Violations

We're thankful that we've rarely experienced major issues in our community. In all cases, the project leads shall have the final decision making authority when dealing with violations of these guidelines.

## Yearly Development Cycle

PhotonVision's Development Cycle follows the same general flow as the FRC Build Season. Larger experiments get done over the summer, fall focuses on testing and "production-readiness", build season focuses on keeping all teams running smoothly.

The actual priorities also shift as developers have more or time to commit, or express interest in specific direction.

PR's are absolutely always welcome. Just note depending on the scope and size, they may not be reviewed or merged immediately.

## Project Governance

This project is jointly lead by Matt and Banks, who may be contacted on Discord. They serve as a "Benevolent leader for life" role, coordinating and approving architecture as needed, and curating the team of developers who have Pull Request review and merge responsibilities.
