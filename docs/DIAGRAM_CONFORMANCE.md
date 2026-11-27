# Diagram conformance

This report checks the code against three deliverables:
- **D5:** the design class diagram and the twelve sequence diagrams
- **D4:** the system sequence diagrams
- **D3:** the fully dressed use cases

Every message in a D5 sequence diagram is sent in the code, in the same order. Each one carries a comment with its diagram number, for example `// 2.3`, so the two can be read side by side.

## 1. Class diagram

Every class, interface and operation on the D5 design class diagram exists in the code with the same name and the same role. The table lists the classes; the full method-by-method check is in [Method check](#method-check) below.

| Diagram | Code | Stereotype |
|---|---|---|
| `RegistrationController`, `CourseController`, `StudyPlanController`, `ProgressController` | `controller/` | «controller» |
| `SyllabusParser` / `LineFormatSyllabusParser` | `service/` | «interface» + realization |
| `StudyPlanGenerator`, `PriorityAdvisor`, `GradeForecaster`, `PaceTracker`, `ProgressGraphBuilder` | `service/` | «pure fabrication» |
| `PriorityStrategy` / `WeightedPriorityStrategy` | `service/` | «interface» (Strategy) |
| `PersistentStorage` / `OraclePersistentStorage` | `storage/PersistentStorage`, `storage/SqlitePersistentStorage` | «interface» + realization |
| `StudentRegistry`, `Student`, `StudySettings`, `StudyAvailability`, `AvailabilitySlot`, `Friendship`, `PointTransaction` | `model/` | |
| `Semester`, `Course`, `Topic`, `TopicProgress`, `GradeScale`, `GradeBand` | `model/` | |
| `Assessment` {abstract} › `Quiz`, `Assignment`, `Exam` | `model/` | inheritance |
| `Score`, `StudyPlan`, `StudySession`, `Leaderboard`, `ProgressGraph`, `GraphPoint` | `model/` | |

The compositions on the diagram decide who creates what (Creator), and the code follows them:
- `StudentRegistry` creates `Student`.
- `Student` creates `Course`, `PointTransaction` and `StudySettings`.
- `Course` creates `Topic` and the three `Assessment` subclasses.
- `Topic` creates `TopicProgress`.
- `Assessment` creates `Score`.
- `StudyPlanGenerator` creates `StudyPlan`, and `StudyPlan` creates `StudySession`.
- `ProgressGraph` creates `GraphPoint`.

## 2. Sequence diagrams

Each table lists the messages one system operation sends.

### UC-01 Register Student Account (`RegistrationController`)

| Diagram | Code |
|---|---|
| 1 `startRegistration()` | returns the requested fields |
| 2.1 `validateDetails(studentId, email, password, confirmPassword)` | `registry.validateDetails(...)` |
| 2.2 `isTaken(studentId, email, displayName)` | `registry.isTaken(...)` |
| 2.3 `createStudent(...)` › 2.3.1 create Student › 2.3.1.1 create StudySettings | `registry.createStudent(...)`, `new Student(...)` (which creates its `StudySettings`) |
| 2.4 `save(s)` | `store.save(s)` |

### UC-02 Import Course Syllabus (`CourseController`)

| Diagram | Code |
|---|---|
| 2.1 `createCourse` › 2.1.1 create › 2.1.2 `addCourse(c)` | `student.createCourse(...)` |
| 2.2 `parseOutline(outline)` | `parser.parseOutline(outline)` |
| loop 2.3 `addTopic(weekNumber, title)` | `c.addTopic(...)` for each line |
| 2.4 `parseAssessments(assessments)` | `parser.parseAssessments(assessments)` |
| loop 2.5 `addAssessment(type, ...)`, alt Quiz / Exam / Assignment | `c.addAssessment(...)` creates `Quiz`, `Exam` or `Assignment` |
| 2.6 `save(c)` | `store.save(c)` |

### UC-03 Manage Courses and Exams (`CourseController`)

| Diagram | Code |
|---|---|
| 1.1 `getCourses()` | `manageCourses()` |
| 2.1 `getCourse`, 2.2 `getAssessment` | `selectChange(action, item)` |
| 3.1 `validateChange(date, weightage)` | `selectedCourse.validateChange(...)` |
| alt 3.2 `updateAssessment` › 3.2.1 `update` / 3.3 `addAssessment` / 3.4 `removeAssessment` | the three branches of `enterChange` |
| 3.5 `getActivePlan()`, 3.6 `markOutdated()` | `markPlanOutdated()` |
| 3.7 `save(c)` | `store.save(selectedCourse)` |

### UC-04 Generate Semester Study Plan (`StudyPlanController`)

| Diagram | Code |
|---|---|
| 1.1 `getAvailability()` | `generateStudyPlan()` |
| 2.1 `setAvailability(availableHours, daysOff)` | `student.setAvailability(...)` |
| 2.2 `generate(s)` › 2.2.1 `rankTopics` (getPendingTopics, getSettings, loop `score`) › 2.2.2 `getAvailability` › 2.2.3 create plan › loop 2.2.4 `addSession` | `StudyPlanGenerator.generate`, `PriorityAdvisor.rankTopics`, `StudyPlan.addSession` |
| 2.3 `addPlan(p)` | `student.addPlan(p)` (the new plan replaces the old one) |
| 2.4 `save(p)` | `store.save(p)` |

### UC-05 Complete Study Topic (`StudyPlanController`)

| Diagram | Code |
|---|---|
| 1.1 `findTopic`, 1.2 `getWeightage`, 1.3 `getDueDate` | `selectTopic(topicTitle)` |
| 2.1 `complete(actualTimeSpent)` › create TopicProgress | `t.complete(...)` |
| 2.2 `getPointsReward()` | `t.getPointsReward()` |
| 2.3 `addPoints(pts, "topic completed")` › create PointTransaction | `student.addPoints(...)` |
| 2.4 `getCurrentStreak()` | `student.getCurrentStreak()` |
| 2.5 `getActivePlan()`, 2.6 `rebalance(p, s)` | `student.getActivePlan()`, `generator.rebalance(p, student)` |
| 2.7 `getTopTopic(s)` | `advisor.getTopTopic(student)` |
| 2.8 `save(s)` | `store.save(student)` |

### UC-06 Log Quiz or Test Score (`CourseController`)

| Diagram | Code |
|---|---|
| 2.1 `getCourse`, 2.2 `getAssessment` | `student.getCourse(...)`, `c.getAssessment(...)` |
| 2.3 `recordScore(...)` › create Score | `a.recordScore(...)` |
| 2.4 `getCurrentPercentage()` (loop `getScore`) | `c.getCurrentPercentage()` |
| 2.5 `getPointsReward()`, 2.6 `addPoints(pts, "score logged")` | `a.getPointsReward()`, `student.addPoints(...)` (only the first time, see 9a) |
| 2.7 `getActivePlan()`, 2.8 `rebalance(p, s)` | `student.getActivePlan()`, `generator.rebalance(...)` |
| 2.9 `save(s)` | `store.save(student)` |

### UC-07 View Daily Study Plan (`StudyPlanController`)

| Diagram | Code |
|---|---|
| 1.1 `getActivePlan`, 1.2 `getSessionsFor(today)`, 1.3 `getTotalPlannedTime(today)` | `viewDailyPlan()` |
| loop 2.1 `getSessionsFor(date)` | `viewPlanForDate(date)` |
| 3.1 `getSession`, 3.2 `getTopic`, 3.3 `getReason`, 3.4 `getWeightage`, 3.5 `getDaysLeft` | `selectSession(startTime)` |

### UC-08 View Priority Suggestion (`StudyPlanController`)

| Diagram | Code |
|---|---|
| 1.1 `getTopTopic(s)` › `rankTopics` › loop `score` (getWeightage, getDaysLeft, getWeaknessLevel) | `advisor.getTopTopic(student)`, `WeightedPriorityStrategy.score` |
| 1.2 `explain(top)` › `explain(top)` on the strategy | `advisor.explain(top)` |
| 2.1 `rankTopics(s)` | `viewFullRanking()` |

### UC-09 View Grade Forecast (`ProgressController`)

| Diagram | Code |
|---|---|
| 1.1 `getCourses`, loop 1.2 `getCurrentPercentage`, 1.3 `forecast(c)`, 1.4 `toLetter(c, fp)` | `viewGradeForecast()` |
| 2.1 `getCourse`, 2.2 `requiredAverage(c, targetGrade)` (getGradeScale, minimumFor, getCurrentPercentage, getGradedWeight) | `enterTargetGrade(...)`, `GradeForecaster.requiredAverage` |

Post-condition 2 of UC-09 says that viewing the forecast changes nothing, so the target grade is not saved to the database.

### UC-10 Track Study Pace (`ProgressController`)

| Diagram | Code |
|---|---|
| 1.1 `getCourses`, loop 1.2 `getPaceStatus` (countDueTopics, countCompletedTopics), 1.3 `getTopicsAheadOrBehind`, 1.4 `getOverallStatus` | `viewStudyPace()` |
| 2.1 `getCourse`, 2.2 `getOverdueTopics`, 2.3 `getAvailability`, 2.4 `extraHoursPerDay` | `selectCourse(courseName)` |

### UC-11 View Progress Graphs (`ProgressController`)

| Diagram | Code |
|---|---|
| 1.1 `getCourses()` | `viewProgressGraphs()` |
| 2.1 `buildGradeGraph` (create graph, loop `getScoresIn`, `addPoint`) | `graphBuilder.buildGradeGraph(...)` |
| 2.2 `buildStudyTimeGraph` (create graph, loop `getStudyTimeIn`, `addPoint`) | `graphBuilder.buildStudyTimeGraph(...)` |
| loop 3.1 `findPoint(point)` | `selectGraphPoint(point)` |

The builders are called once per chosen course, so each course is its own labelled line. That is UC-11 special requirement 3, and the diagram's reply is "graphs per course".

### UC-12 View Leaderboard (`ProgressController`)

| Diagram | Code |
|---|---|
| 1.1 `getAllStudents`, 1.2 `refresh(all, "everyone")` (getTotalPoints, getCurrentStreak, sortByPointsThenStreak), 1.3 `getRanking`, 1.4 `getPositionOf(s)` | `viewLeaderboard()` |
| alt 2.1 `getFriends` / 2.2 `getClassmates(s)`, 2.3 `refresh`, 2.4 `getRanking`, 2.5 `getPositionOf(s)` | `filterLeaderboard(scope)` |

## 3. GRASP checks

- **Controller:** controllers receive the D4 system events and delegate. Every check on domain data sits in the class that owns that data:
  - `StudentRegistry` checks names when it creates a Student.
  - `Course` checks assessment names.
  - `Assessment` checks a score's date.
  - `StudySettings` balances its own weights.
- **Information Expert:** examples beyond the diagram's own:
  - `Semester` works out its own date range from its courses.
  - `StudyPlan` finds its next session.
  - `Student.getCourse` finds a course by name or by code.
- **Low Coupling:** only controllers use `PersistentStorage`. Domain classes never touch the database.
- **Pure Fabrication / High Cohesion:** planning, ranking, forecasting, pace, graphs and parsing each live in their own service class.

## 4. Use-case extensions (D3)

| Extension | Where |
|---|---|
| UC-01 4a / 4b / 6a | `StudentRegistry.validateDetails`, `isTaken`, and "Sign in instead" on the sign-in screen |
| UC-02 4a–4d, 6a | `LineFormatSyllabusParser` warnings, required fields, weightage total = 100%, `importSyllabus(..., replaceExisting)` |
| UC-03 5a, 8b | inline remove confirmation, past-date warning with "Save anyway" |
| UC-04 3a, 5a, 8a | hours outside 0–24 rejected, "Nothing to plan yet", replace confirmation |
| UC-05 3a, 4a, 5a, *a | already completed, streak restarted, course covered, `undoCompletion` (same day only) |
| UC-06 9a | an overwritten score earns no second reward |
| UC-07 4a, 4b | next upcoming session, carried-over sessions shown first |
| UC-08 3b, 4a, 4b, 4c | all caught up, no-scores note, tie goes to the nearer exam, finished courses left out |
| UC-09 4a, 4b, 7a | no scores, "Approximate" label, "cannot be reached" |
| UC-10 4a, 5a | pace alerts on the Study pace screen |
| UC-11 6a, 6b | empty state, single point drawn instead of a line |
| UC-12 6a, 6b | add friend by student ID, empty friends list |

## 5. Deliberate differences

| Difference | Reason |
|---|---|
| `SqlitePersistentStorage` in place of `OraclePersistentStorage` | The project uses SQLite, so it runs on any PC without a database server. It implements the same `PersistentStorage` interface (`save`, `loadStudent`), so swapping back changes no other class. |
| `importSyllabus(..., boolean replaceExisting)` | UC-02 extension 6a asks the student before replacing a course with the same code. The five-argument diagram version still exists and calls this one with `false`. |
| `undoCompletion(topicTitle)` | UC-05 extension *a (undo the same day). |
| `updateSettings(weightage, urgency, weakness)` | UC-08 special requirement 4: the weighting can be adjusted. |
| `addFriend(studentId)` | UC-12 extension 6a. |
| `login(idOrEmail, password)` | The app needs a sign-in, which is not one of the twelve use cases. |
| Overload `Course.validateChange(a, date, weightage)` | When an assessment is changed, its own old weightage must not count toward the 100% limit. |

## 6. How it is tested

`src/test/java/com/cramless/FlowTest.java` drives the four controllers through every use case against a real SQLite file. It has 26 tests.

Four of them pin the diagram behaviour:
- registration checks the display name when the Student is created
- the UC-09 target grade is not saved
- `selectSession` returns the reason, weightage and days left
- the own position comes back with the leaderboard

Run them with `mvn test` or `start.bat test`.

## Method check

These are the classes and operations taken from the D5 class diagram and checked against the code. A missing method fails the check.

```
PersistentStorage            save loadStudent
RegistrationController       startRegistration register
CourseController             startSyllabusImport importSyllabus manageCourses selectChange enterChange startScoreLogging logScore
StudyPlanController          generateStudyPlan enterAvailability selectTopic completeTopic viewDailyPlan viewPlanForDate
                             selectSession requestNextTopic viewFullRanking
ProgressController           viewGradeForecast enterTargetGrade viewStudyPace selectCourse viewProgressGraphs
                             selectGraphScope selectGraphPoint viewLeaderboard filterLeaderboard
SyllabusParser               parseOutline parseAssessments
StudyPlanGenerator           generate rebalance
PriorityAdvisor              rankTopics getTopTopic explain
PriorityStrategy             score explain
GradeForecaster              forecast toLetter requiredAverage
PaceTracker                  getPaceStatus getTopicsAheadOrBehind getOverallStatus extraHoursPerDay
ProgressGraphBuilder         buildGradeGraph buildStudyTimeGraph
Leaderboard                  refresh getRanking getPositionOf sortByPointsThenStreak
ProgressGraph                addPoint findPoint
StudentRegistry              validateDetails isTaken createStudent getAllStudents getClassmates
Student                      createCourse getCourses getCourse findTopic getPendingTopics getSettings getAvailability
                             setAvailability getActivePlan addPlan addPoints getTotalPoints getCurrentStreak getFriends
StudyAvailability            isDayOff
Semester                     addCourse
Course                       addTopic validateChange addAssessment getAssessment updateAssessment removeAssessment getTopics
                             getCurrentPercentage getGradedWeight getGradeScale countDueTopics countCompletedTopics
                             getOverdueTopics getScoresIn getStudyTimeIn
Topic                        getWeightage getDueDate getDaysLeft getWeaknessLevel complete isCompleted getPointsReward
TopicProgress                isCompleted
StudyPlan                    addSession getSessionsFor getSession getTotalPlannedTime markOutdated
StudySession                 getTopic getReason
GradeScale                   toLetter minimumFor
Assessment, Quiz, Assignment, Exam   recordScore getScore isGraded update getPointsReward
Score                        getPercentage
```

Result: all present.
