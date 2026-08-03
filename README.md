# Parpt - Project Audit & Revenue Prioritization Tool

[![CI Pipeline](https://github.com/Stephenson-Software/Parpt/actions/workflows/ci.yml/badge.svg)](https://github.com/Stephenson-Software/Parpt/actions/workflows/ci.yml)
[![Build and Test](https://github.com/Stephenson-Software/Parpt/actions/workflows/build.yml/badge.svg)](https://github.com/Stephenson-Software/Parpt/actions/workflows/build.yml)

Parpt is an interactive CLI tool that helps developers, indie creators and teams evaluate and prioritize their software projects using structured metrics like ICE and RICE.

## Features
- Guided project scoring using ICE and RICE methods
- Calculate ICE (Impact, Confidence, Ease) scores
- Calculate RICE (Reach, Impact, Confidence, Effort) scores
- Export projects to Markdown and JSON formats
- Obsidian-compatible markdown export with sorting by ICE/RICE scores
- Rank and sort projects by monetization, potential, feasibility and effort
- Spring Boot architecture with interactive shell
- 100% local-first and open source

## Evaluation Framework
Each project is evaluated across 5 dimensions using a detailed 1-5 scoring system:
- **Impact**: Monetary value, user happiness, competitive advantage, problem-solving
- **Confidence**: Requirement clarity, team experience, skill fit, requirement stability
- **Ease**: Build complexity, tooling readiness, code reuse potential, testability
- **Reach**: User coverage, new user attraction, visibility, usage frequency
- **Effort**: Development time, team size, maintenance burden, ongoing work

## Installation
Clone and build manually:
git clone https://www.github.com/Preponderous/Parpt.git
cd Parpt
./gradlew build
java -jar build/libs/parpt.jar

## Getting Started
Run the CLI:
java -jar parpt.jar

Available commands:
- `create` - Create a new project with guided scoring
- `list` - List all projects with scores
- `view <project-name>` - View detailed project information
- `export` - Export all projects to Markdown format
- `help` - Show available commands

You'll be prompted to enter:
- Project name and description
- Detailed scoring for each category (1-5 scale)
- Parpt will then:
    - Calculate ICE and RICE scores
    - Save the results to `projects.json`
    - Allow you to export to `projects.md` sorted by priority
    - Help you sort and review your efforts over time

### Export Examples
```bash
# Export projects sorted by ICE score (default)
export

# Export projects sorted by RICE score
export --sort rice
```

## Roadmap
- [x] Project input and validation loop
- [x] Score calculation engine
- [x] Markdown and JSON writer modules
- [x] CLI configuration and persistence
- [x] Obsidian-compatible markdown export with sorting
- [ ] Visualization of project scores
- [ ] Batch project comparison features

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
