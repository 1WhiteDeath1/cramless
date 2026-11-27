<p align="center"><img src="src/main/resources/images/logo-144.png" width="72" alt="CramLess logo"></p>

# CramLess

**Plan the semester. Skip the panic.**

CramLess turns a course syllabus into a semester-long study plan. It schedules 30-minute study blocks before each exam, says what to study next and why, forecasts grades, tracks study pace, and keeps students going with points, streaks and a leaderboard.

This is the Software Design and Analysis (CS3004) course project at FAST-NUCES Islamabad, Section D, taught by Sir Furqan Nasir. The project partners are M. Fahad Afridi (25i-0644) and M. Hassan Sarmad (25i-0504).

The code implements the Deliverable 5 design:
- the design class diagram (GRASP)
- the twelve sequence diagrams, one per use case UC-01 to UC-12
- the extensions written in the Deliverable 3 fully dressed use cases

[`docs/DIAGRAM_CONFORMANCE.md`](docs/DIAGRAM_CONFORMANCE.md) maps every diagram message to the line of code that sends it.

**Stack:** Java 17, JavaFX, SQLite and Maven.

## Run it

**Windows:** double-click `start.bat`.
- It uses the Java 17+ and Maven already on the PC.
- If either is missing, it downloads a private copy into `.tools` the first time.
- `start.bat test` runs the tests.

**Any OS with JDK 17+ and Maven:**

```
mvn javafx:run      # start the app
mvn test            # run the 26 logic tests (no UI needed)
mvn package         # build target/cramless-1.0.jar
```

The database is created at `~/.cramless/cramless.db` on first run. To use another file, run `mvn javafx:run -Dcramless.db=/path/to/file.db`.

## Screens and use cases

| Use case | Screen | Controller |
|---|---|---|
| UC-01 Register Student Account | Sign-in, "Create account" | `RegistrationController` |
| UC-02 Import Course Syllabus | Courses › Add course (4-step wizard) | `CourseController` |
| UC-03 Manage Courses and Exams | Courses | `CourseController` |
| UC-04 Generate Semester Study Plan | Study plan › Edit availability | `StudyPlanController` |
| UC-05 Complete Study Topic | Today › Mark complete (undo the same day) | `StudyPlanController` |
| UC-06 Log Quiz or Test Score | Log a score (Courses, Grade forecast) | `CourseController` |
| UC-07 View Daily Study Plan | Today | `StudyPlanController` |
| UC-08 View Priority Suggestion | Study next | `StudyPlanController` |
| UC-09 View Grade Forecast | Grade forecast | `ProgressController` |
| UC-10 Track Study Pace | Study pace | `ProgressController` |
| UC-11 View Progress Graphs | Graphs | `ProgressController` |
| UC-12 View Leaderboard | Leaderboard | `ProgressController` |

**Design:**
- Light and dark themes, plus a sidebar that can be collapsed. Both choices are remembered.
- Screens show the main task first. Details open in drawers and expanders.
- Fonts are Lexend for the interface and Silkscreen for the numerals, both under the SIL Open Font License.
- Icons are from Lucide (ISC licence). The licences are in `src/main/resources/licenses`.

## Package layout

```
com.cramless.controller  four session controllers: they receive the SSD system events and only delegate
com.cramless.model       domain classes: Student, Semester, Course, Topic, Assessment > Quiz/Assignment/Exam,
                         StudyPlan, StudySession, Score, GradeScale, Leaderboard, ProgressGraph, ...
com.cramless.service     pure fabrications: SyllabusParser, StudyPlanGenerator, PriorityAdvisor (+ PriorityStrategy),
                         GradeForecaster, PaceTracker, ProgressGraphBuilder
com.cramless.storage     PersistentStorage interface and its SQLite implementation
com.cramless.ui          JavaFX screens, built in code (no FXML), styled by cramless.css and themes/*.css
com.cramless.util        AppClock, DateRange, ValidationException, PasswordHasher
```

## Rules the code applies

- **Topic coverage:** a topic is covered by every assessment dated on or after the start of its week. An assessment's weightage is split equally between the topics it covers.
- **Priority:** 100 × (0.4 × weightage + 0.4 × urgency + 0.2 × weakness). The weights can be changed on the Study next screen (UC-08 special requirement 4).
  - Weakness counts only when the average of the covering graded assessments is below 60%.
  - When two topics score the same, the nearer exam goes first.
- **Study plan:** 30-minute blocks from 17:00, inside the hours you give, always before the topic's exam.
  - Rebalancing closes the sessions of finished topics and plans the rest again.
- **Points:** quiz 10, assignment 15, exam 30.
  - A completed topic earns 10 + min(10, its weightage).
  - A score earns points only the first time it is logged.
- **Streak:** consecutive days with at least one completed topic, ending today or yesterday.
- **Forecast:** assumes the remaining assessments go as well as the average so far. It is always labelled an estimate.
- **Pace:** the topics due are those of the weeks that are fully over. A course is behind when more than 10% of its due topics are unfinished.

## Where the code differs from the diagram

These differences are deliberate. Each one is explained in [`docs/DIAGRAM_CONFORMANCE.md`](docs/DIAGRAM_CONFORMANCE.md).

- **Storage:** `SqlitePersistentStorage` stands in for `OraclePersistentStorage`. It uses the same `PersistentStorage` interface, so it can be swapped back.
- **Additions for use-case extensions:**
  - `importSyllabus(..., replaceExisting)` for UC-02 6a.
  - `undoCompletion` for UC-05 *a.
  - `updateSettings` for UC-08 special requirement 4.
  - `addFriend` for UC-12 6a.
- **Sign-in:** the app needs a sign-in, which is not one of the twelve use cases.
- **Passwords:** stored as salted PBKDF2 hashes.
