# Parpt - Project Audit & Revenue Prioritization Tool

[![CI Pipeline](https://github.com/Stephenson-Software/Parpt/actions/workflows/ci.yml/badge.svg)](https://github.com/Stephenson-Software/Parpt/actions/workflows/ci.yml)
[![Build and Test](https://github.com/Stephenson-Software/Parpt/actions/workflows/build.yml/badge.svg)](https://github.com/Stephenson-Software/Parpt/actions/workflows/build.yml)

Parpt is an interactive CLI tool that helps developers, indie creators and teams evaluate and prioritize their software projects using structured metrics like ICE and RICE.

## Features
- Guided project scoring using ICE and RICE methods
- Calculate ICE (Impact, Confidence, Ease) scores
- Calculate RICE (Reach, Impact, Confidence, Effort) scores
- Save projects to a local JSON file and export them to Markdown
- Obsidian-compatible markdown export with sorting by name, any scoring field or ICE/RICE scores
- List projects sorted by name, by any scoring field (impact, confidence, ease, reach, effort) or by ICE/RICE score
- Spring Boot architecture with interactive shell
- 100% local-first and open source (your projects never leave your machine; see [Usage reporting](#usage-reporting))

## Evaluation Framework
Each project is evaluated across 5 dimensions using a detailed 1-5 scoring system:
- **Impact**: Monetary value, user happiness, competitive advantage, problem-solving
- **Confidence**: Requirement clarity, team experience, skill fit, requirement stability
- **Ease**: Build complexity, tooling readiness, code reuse potential, testability
- **Reach**: User coverage, new user attraction, visibility, usage frequency
- **Effort**: Development time, team size, maintenance burden, ongoing work

## Installation
Clone and build manually (requires JDK 21):
```bash
git clone https://github.com/Stephenson-Software/Parpt.git
cd Parpt
./gradlew build
```

The build writes two jars to `build/libs/`: the runnable `Parpt-<version>.jar` and a `Parpt-<version>-plain.jar` that cannot be run on its own. Use the one without `-plain` (`<version>` is the `version` set in `build.gradle`). A bare `build/libs/Parpt-*.jar` glob matches both, and the shell may pass the plain jar first, in which case Java fails with `no main manifest attribute`.

## Getting Started
Run the CLI from the directory where your data should live:
```bash
java -jar build/libs/Parpt-<version>.jar
```

Available commands:
- `create` - Create a new project with guided scoring, or straight from options
- `list` (alias: `ls`) - List all projects with scores, optionally sorted
- `view <project-name>` - View detailed project information
- `update <project-name>` - Change a project's description or scores
- `delete <project-name>` (alias: `rm`) - Delete a project by name
- `export` - Export all projects to Markdown format, sorted by ICE score unless another order is chosen
- `help` - Show available commands

You'll be prompted to enter:
- Project name and description
- Detailed scoring for each category (1-5 scale): each category asks four questions, and its score is their average, rounded to the nearest whole number
- Entering `q` or `quit` at any prompt (or pressing Ctrl-D) cancels creation without saving anything
- Parpt will then:
    - Calculate ICE and RICE scores
    - Save the results to `projects.json` in the directory Parpt was started from
    - Allow you to export to `projects.md` (in the same directory) sorted by priority
    - Help you sort and review your efforts over time

### Create Examples
```bash
# Answer every question interactively
create

# Skip the questions by giving everything as options
create --name "My Project" --description "A short pitch" --impact 4 --confidence 3 --ease 5 --reach 2 --effort 3
create -n "My Project" -d "A short pitch" -i 4 -c 3 -e 5 -r 2 -f 3

# Give some options and be asked only for the rest
create --name "My Project" --impact 4
```

Scores given as options are used as they are rather than averaged; a score outside 1-5 is rejected with `All scores must be between 1 and 5.` and nothing is saved. A name already in use is rejected before any further question is asked.

### List Examples
```bash
# List projects in the order they were created (default)
list

# The 'ls' alias behaves identically
ls

# List projects sorted by a score, highest first
list --sort ice
list --sort rice

# List projects sorted by name (A to Z) or by any scoring field, highest first
list --sort name
list -s impact
```

Supported `--sort` values are `name`, `impact`, `confidence`, `ease`, `reach`, `effort`, `ice` and `rice`. Every value except `name` sorts from highest to lowest; omitting `--sort` keeps the order in which projects were created.

### Update Examples
```bash
# Change a project's description
update "My Project" --description "A sharper pitch"

# Change one or more scores (1-5); fields not given keep their current values
update "My Project" --impact 5 --effort 2
update "My Project" -c 4 -r 3
```

`update` accepts `--description` (`-d`), `--impact` (`-i`), `--confidence` (`-c`), `--ease` (`-e`), `--reach` (`-r`) and `--effort` (`-f`), and at least one of them must be given. The change is saved to `projects.json` immediately and the project keeps its place in creation order; the new ICE and RICE scores are printed. If no project has the given name, `update` reports `Project not found: <project-name>`; an empty description or a score outside 1-5 is rejected and nothing is changed. A project cannot be renamed with `update`.

### Delete Examples
```bash
# Delete a project by name
delete "My Project"

# The 'rm' alias behaves identically
rm "My Project"
```

Deleting a project removes it from `projects.json` immediately; there is no confirmation prompt and no undo. If no project has the given name, `delete` reports `Project not found: <project-name>` and nothing is changed.

### Export Examples
```bash
# Export projects sorted by ICE score (default)
export

# Export projects sorted by RICE score
export --sort rice

# Export projects sorted by name (A to Z) or by any scoring field, highest first
export --sort name
export -s impact
```

`export` accepts the same `--sort` values as `list`: `name`, `impact`, `confidence`, `ease`, `reach`, `effort`, `ice` and `rice`. Every value except `name` sorts from highest to lowest, and the chosen order is recorded in the exported file's header. Unlike `list`, `export` always sorts — omitting `--sort` sorts by ICE score.

## Roadmap
- [x] Project input and validation loop
- [x] Score calculation engine
- [x] Markdown and JSON writer modules
- [x] CLI configuration and persistence
- [x] Obsidian-compatible markdown export with sorting
- [ ] Visualization of project scores
- [ ] Batch project comparison features

## Usage reporting
Usage reporting is on by default: Parpt reports that it was used to the maintainers' [trace](https://danielstephenson.dev/usage-reporting) service at `https://trace.danielstephenson.dev`, sending a `startup` event carrying its name and version once per run, and a `project-created` event carrying only the version when a project is saved. Nothing about your projects is sent: no project names, descriptions, scores or files, and no usernames, hostnames, IP addresses, paths or anything typed at the prompt. A one-line notice is printed the first time it runs on a machine (recorded in `~/.config/parpt/usage-reporting-notice-shown`).

Every event also carries a random installation ID (the tag `install`), so installations can be counted rather than events. It is a random UUID written the first time reporting runs to `~/.config/parpt/trace-install-id`, next to the notice marker, and reused after that; it identifies no person, account or address. Delete the file to get a new one, or set `TRACE_INSTALL_ID` in the environment to pin one. Every opt-out below also stops it: when reporting is off, no ID is made up and the file is neither read nor written.

To turn it off, any one of these is enough:

- `java -Dusage-reporting.enabled=false -jar build/libs/Parpt-<version>.jar`
- `USAGE_REPORTING_ENABLED=false` in the environment
- `TRACE_USAGE_REPORTING=off` (also `false`, `0`, `no`) in the environment — the switch every trace client honours, checked before Parpt's own setting
- `DO_NOT_TRACK=1` (also `true`, `yes`) in the environment, per [consoledonottrack.com](https://consoledonottrack.com)

The key under `usage-reporting.key` in `application.yaml` is the write key issued to Parpt; it can only add usage events and is not secret.

Details on what trace collects and why: https://danielstephenson.dev/usage-reporting

## Contributing
This project is in early development. Contributions, suggestions and issue reports are welcome!

- Open an issue
- Fork and submit a PR
- Join the discussion

## License
This project is licensed under the **Stephenson Software Non-Commercial License (Stephenson-NC)**.  
© 2025 Daniel McCoy Stephenson. All rights reserved.  

You may use, modify, and share this software for **non-commercial purposes only**.  
Commercial use is prohibited without explicit written permission from the copyright holder.  

Full license text: [Stephenson-NC License](https://github.com/Stephenson-Software/stephenson-nc-license)  
SPDX Identifier: `Stephenson-NC`

## About Preponderous
Preponderous Software builds developer tools, simulations and creative systems that help people focus their energy where it counts most.

Visit us at: [https://www.preponderous.org](https://www.preponderous.org)
