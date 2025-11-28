# Sprint Planning & Task Tracking

## Sprint Structure

This project uses **2-week sprints** aligned with development phases:
- **Sprint 1-2**: Phase 1 (Foundation)
- **Sprint 3-4**: Phase 2 (Actions)
- **Sprint 5-6**: Phase 3 (Memory & Planning)
- **Sprint 7**: Phase 4 (Communication)
- **Sprint 8**: Phase 5 (Advanced AI)
- **Sprint 9**: Phase 6 (Optimization & Polish)

---

## Sprint Template

### Sprint [N]: [Phase Name]

**Duration**: [Start Date] to [End Date] (2 weeks)
**Goal**: [Primary objective for this sprint]
**Milestone**: [Associated milestone]

#### Sprint Planning
**Date**: [Planning date]
**Attendees**: [Team members]

#### Sprint Goals
1. [Goal 1]
2. [Goal 2]
3. [Goal 3]

#### Selected Tasks
| ID | Task | Priority | Estimate | Assignee | Status |
|----|------|----------|----------|----------|--------|
| 1.1 | [Task name] | P0 | 4h | [Name] | 🟡 Ready |
| ... | ... | ... | ... | ... | ... |

#### Definition of Done
- [ ] All code committed
- [ ] All tests pass
- [ ] Code reviewed
- [ ] Documentation updated
- [ ] Demo-ready

---

## Sprint 1: Foundation - Part 1

**Duration**: Week 1
**Goal**: Set up development environment and project structure
**Milestone**: M1 (Foundation)

### Sprint Tasks

| ID | Task | Priority | Estimate | Status | Notes |
|----|------|----------|----------|--------|-------|
| 1.1 | Dev environment setup | P0 | 1d | 🟡 Ready | JDK, IDE, Minecraft |
| 1.2 | Fabric project structure | P0 | 4h | 🔴 Blocked | Needs 1.1 |
| 1.3 | Configuration system | P1 | 6h | 🔴 Blocked | Needs 1.2 |
| 1.4 | Logging & debug system | P1 | 3h | 🔴 Blocked | Needs 1.2 |

### Daily Standup Notes

**Day 1** (Monday):
- Starting: Task 1.1
- Blockers: None
- Goal: Complete dev environment setup

**Day 2** (Tuesday):
- Completed: Task 1.1 ✅
- Starting: Task 1.2
- Blockers: None

**Day 3** (Wednesday):
- Progress: Task 1.2 (50% complete)
- Issues: Gradle dependency resolution
- Plan: Finish 1.2, start 1.3

**Day 4** (Thursday):
- Completed: Task 1.2 ✅
- Starting: Task 1.3
- Blockers: None

**Day 5** (Friday):
- Completed: Task 1.3 ✅, Task 1.4 ✅
- Sprint Progress: 100% Week 1 tasks complete
- Next Week: Tasks 1.5-1.7

### Sprint 1 Retrospective
**What went well:**
- [List successes]

**What could improve:**
- [List improvements]

**Action items:**
- [List actions for next sprint]

---

## Sprint 2: Foundation - Part 2

**Duration**: Week 2-3
**Goal**: Create AI player entity with basic movement
**Milestone**: M1 (Foundation)

### Sprint Tasks

| ID | Task | Priority | Estimate | Status | Notes |
|----|------|----------|----------|--------|-------|
| 1.5 | FakePlayer entity creation | P0 | 1.5d | 🟡 Ready | Core functionality |
| 1.6 | Basic movement controller | P0 | 2d | 🔴 Blocked | Needs 1.5 |
| 1.7 | Basic perception system | P1 | 1.5d | 🔴 Blocked | Needs 1.5 |
| 1.8 | Command system | P1 | 1d | 🔴 Blocked | Needs 1.6 |
| 1.9 | Basic AI brain structure | P0 | 1.5d | 🔴 Blocked | Needs 1.7 |
| 1.10 | Phase 1 integration testing | P1 | 1d | 🔴 Blocked | Needs 1.9 |

### Week 2 Daily Standups
[Same format as Sprint 1]

### Week 3 Daily Standups
[Same format as Sprint 1]

### Sprint 2 Retrospective
[Fill after completion]

---

## Task Status Definitions

### Status Indicators
- 🔴 **Blocked**: Cannot start (dependencies not met)
- 🟡 **Ready**: Dependencies met, can start anytime
- 🟢 **In Progress**: Currently being worked on
- 🔵 **In Review**: Code complete, awaiting review
- ✅ **Complete**: Done and tested
- ⏸️ **Paused**: Temporarily stopped
- ❌ **Cancelled**: Will not be completed

### Task Lifecycle
```
🔴 Blocked → 🟡 Ready → 🟢 In Progress → 🔵 In Review → ✅ Complete
                          ↓
                       ⏸️ Paused
                          ↓
                       ❌ Cancelled
```

---

## Daily Standup Template

**Date**: [Date]
**Sprint**: [Sprint N]
**Day**: [X of 10]

### What I did yesterday
- [Task/Activity 1]
- [Task/Activity 2]

### What I'm doing today
- [Task/Activity 1]
- [Task/Activity 2]

### Blockers/Issues
- [Blocker 1]
- [Issue 1]

### Progress Update
- Sprint completion: [X%]
- Tasks completed: [X/Y]
- On track: ✅ Yes / ⚠️ At risk / ❌ Behind

---

## Sprint Review Template

**Sprint**: [Sprint N]
**Review Date**: [Date]
**Attendees**: [Names]

### Sprint Goals
- [ ] Goal 1
- [ ] Goal 2
- [ ] Goal 3

### Completed Tasks
| ID | Task | Actual Time | Estimated Time | Variance |
|----|------|-------------|----------------|----------|
| X.X | [Task] | 6h | 4h | +2h |
| ... | ... | ... | ... | ... |

**Total Completed**: [X/Y tasks]
**Completion Rate**: [X%]

### Incomplete Tasks
| ID | Task | Reason | Action |
|----|------|--------|--------|
| X.X | [Task] | [Reason] | [Move to next sprint / Cancel] |

### Demo
**Demo Given**: ✅ Yes / ❌ No
**Demo Notes**: [What was demonstrated]
**Feedback**: [Stakeholder feedback]

### Metrics
- **Velocity**: [X story points or hours]
- **Burndown**: [On track / Ahead / Behind]
- **Quality**: [# of bugs, test coverage]

---

## Sprint Retrospective Template

**Sprint**: [Sprint N]
**Retro Date**: [Date]

### What Went Well ✅
1. [Success 1]
2. [Success 2]
3. [Success 3]

### What Could Improve ⚠️
1. [Issue 1]
2. [Issue 2]
3. [Issue 3]

### What to Stop Doing ❌
1. [Bad practice 1]
2. [Bad practice 2]

### What to Start Doing 🆕
1. [New practice 1]
2. [New practice 2]

### Action Items
| Action | Owner | Due Date | Status |
|--------|-------|----------|--------|
| [Action 1] | [Name] | [Date] | 🟡 Todo |
| ... | ... | ... | ... |

### Sprint Rating
**Overall**: [1-10]
**Reasoning**: [Why this rating]

---

## Kanban Board Structure

### Backlog
- All tasks from ROADMAP.md not yet started
- Prioritized by P0 > P1 > P2 > P3
- Ordered by dependencies

### Ready
- Dependencies met
- Fully specified
- Estimated
- Can be started immediately

### In Progress (WIP Limit: 3)
- Currently being worked on
- Limited to prevent context switching
- Daily updates required

### In Review
- Code complete
- Awaiting testing/review
- Should not stay here >1 day

### Done
- Tested and working
- Code committed
- Documentation updated
- Accepted by reviewer

---

## Time Tracking Template

### Daily Time Log

**Date**: [Date]
**Total Hours**: [X hours]

| Time | Task ID | Task Name | Hours | Notes |
|------|---------|-----------|-------|-------|
| 9:00 | 1.5 | FakePlayer entity | 2.0h | Created base class |
| 11:00 | 1.5 | FakePlayer entity | 1.5h | Added spawn logic |
| ... | ... | ... | ... | ... |

### Weekly Summary

**Week**: [Week N]
**Total Hours**: [X hours]
**Tasks Completed**: [X tasks]

| Task ID | Task Name | Total Time | Status |
|---------|-----------|------------|--------|
| 1.1 | Dev setup | 4h | ✅ |
| 1.2 | Project structure | 4h | ✅ |
| ... | ... | ... | ... |

**Variance Analysis**:
- Faster than estimated: [Tasks]
- Slower than estimated: [Tasks]
- Reasons: [Analysis]

---

## Risk Tracking

### Active Risks

| ID | Risk | Impact | Probability | Mitigation | Owner | Status |
|----|------|--------|-------------|------------|-------|--------|
| R1 | LLM costs too high | High | Medium | Use caching, local models | [Name] | 🟡 Monitoring |
| R2 | Performance issues | High | Medium | Profile early, optimize | [Name] | 🟡 Monitoring |
| R3 | FakePlayer API changes | Medium | Low | Follow Fabric updates | [Name] | 🟢 Low risk |

### Risk Status
- 🔴 **Critical**: Immediate action required
- 🟡 **Monitoring**: Watch closely
- 🟢 **Low**: Acceptable risk

---

## Bug Tracking Template

### Bug Report

**ID**: BUG-[N]
**Title**: [Short description]
**Severity**: Critical / High / Medium / Low
**Priority**: P0 / P1 / P2 / P3
**Status**: Open / In Progress / Fixed / Closed

**Description**:
[Detailed description of the bug]

**Steps to Reproduce**:
1. Step 1
2. Step 2
3. Step 3

**Expected Behavior**:
[What should happen]

**Actual Behavior**:
[What actually happens]

**Environment**:
- Minecraft Version: [version]
- Mod Version: [version]
- OS: [OS]
- Java Version: [version]

**Logs/Screenshots**:
[Attach relevant logs or screenshots]

**Fix**:
[Description of fix, if known]

---

## Definition of Ready (DoR)

A task is ready to start when:
- [ ] Task is clearly described
- [ ] Acceptance criteria defined
- [ ] Dependencies are met
- [ ] Estimate is provided
- [ ] Priority is assigned
- [ ] Necessary resources available

---

## Definition of Done (DoD)

A task is done when:
- [ ] Code is written and works
- [ ] Unit tests written (if applicable)
- [ ] Code is committed to repository
- [ ] Code follows style guidelines
- [ ] No new warnings or errors
- [ ] Documentation updated
- [ ] Tested in game environment
- [ ] Acceptance criteria met
- [ ] Reviewed by peer (if applicable)

---

## Estimation Guidelines

### Time Estimates
- **Small**: <4 hours
- **Medium**: 4-8 hours (0.5-1 day)
- **Large**: 1-2 days
- **X-Large**: 2+ days (break down into smaller tasks)

### Estimation Process
1. Understand requirements
2. Consider dependencies
3. Account for testing time
4. Add 20% buffer for unknowns
5. Re-estimate if >2 days (break down)

### Velocity Calculation
**Velocity** = Total hours completed per sprint

Track over time to improve future estimates.

---

## Tools Recommendations

### Project Management
- **GitHub Projects**: Kanban board, integrated with issues
- **Trello**: Simple kanban board
- **Notion**: All-in-one workspace
- **Plain Markdown**: Simple, version-controlled tracking

### Time Tracking
- **Toggl**: Simple time tracking
- **Clockify**: Free time tracking
- **Manual**: Daily log in markdown

### Communication
- **GitHub Discussions**: Project discussions
- **Discord**: Real-time chat (if team)
- **Comments**: In-code documentation

---

## Quick Reference Commands

### Start Sprint
1. Review ROADMAP.md for next tasks
2. Create sprint plan (copy template)
3. Move tasks to "Ready" column
4. Set sprint goals
5. Begin first task

### Daily Workflow
1. Update standup notes
2. Move task to "In Progress"
3. Work on task
4. Commit progress
5. Update task status
6. Plan tomorrow

### End Sprint
1. Complete sprint review
2. Run retrospective
3. Calculate velocity
4. Plan next sprint
5. Archive completed tasks

---

## Progress Visualization

### Burndown Chart (Manual)

**Sprint [N]** - Total Hours: [X]

| Day | Remaining | Completed | Notes |
|-----|-----------|-----------|-------|
| 1 | 80h | 0h | Sprint start |
| 2 | 72h | 8h | Task 1.1 done |
| 3 | 66h | 14h | Task 1.2 done |
| ... | ... | ... | ... |
| 10 | 0h | 80h | Sprint complete! |

### Velocity Chart

| Sprint | Planned | Completed | Velocity |
|--------|---------|-----------|----------|
| 1 | 40h | 38h | 95% |
| 2 | 40h | 42h | 105% |
| 3 | 45h | 43h | 96% |

**Average Velocity**: [X%]
**Trend**: [Improving / Stable / Declining]

---

## Next Steps for Sprint Planning

1. **Choose your first sprint**: Likely Sprint 1 (Foundation Part 1)
2. **Copy the sprint template** into a new file: `sprints/sprint-1.md`
3. **Fill in the tasks** from ROADMAP.md Task 1.1-1.4
4. **Set your start date** and commit to the timeline
5. **Begin Task 1.1**: Development environment setup
6. **Update daily**: Keep standup notes and time logs
7. **Review weekly**: Check progress against goals

---

**Remember**: Consistent small progress beats irregular big pushes. Aim for 1-2 hours daily if you can't do full-time development.
