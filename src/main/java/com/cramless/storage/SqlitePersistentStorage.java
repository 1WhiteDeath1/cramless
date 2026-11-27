package com.cramless.storage;

import com.cramless.model.Assessment;
import com.cramless.model.Course;
import com.cramless.model.Friendship;
import com.cramless.model.PointTransaction;
import com.cramless.model.Student;
import com.cramless.model.StudyAvailability;
import com.cramless.model.StudyPlan;
import com.cramless.model.StudySession;
import com.cramless.model.StudySettings;
import com.cramless.model.Topic;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLite implementation of {@link PersistentStorage} (the class diagram's database class, here backed by a
 * single local file so nothing has to be installed). Saving rewrites the owning student's rows in one transaction.
 */
public class SqlitePersistentStorage implements PersistentStorage, AutoCloseable {
    private final Connection connection;

    public SqlitePersistentStorage(String filePath) {
        try {
            Class.forName("org.sqlite.JDBC"); // make sure the driver is registered
            connection = DriverManager.getConnection("jdbc:sqlite:" + filePath);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            createSchema();
        } catch (SQLException | ClassNotFoundException e) {
            throw new StorageException("Could not open the database " + filePath, e);
        }
    }

    private void createSchema() throws SQLException {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS students (student_id TEXT PRIMARY KEY, full_name TEXT NOT NULL, "
                + "display_name TEXT NOT NULL, email TEXT, password_hash TEXT NOT NULL, sem_name TEXT, sem_start TEXT, "
                + "sem_end TEXT, weightage_factor REAL, urgency_factor REAL, weakness_factor REAL, weak_threshold REAL)",
            "CREATE TABLE IF NOT EXISTS availability (student_id TEXT NOT NULL REFERENCES students(student_id) ON DELETE CASCADE, "
                + "day TEXT NOT NULL, hours REAL NOT NULL, start_time TEXT NOT NULL, PRIMARY KEY (student_id, day))",
            "CREATE TABLE IF NOT EXISTS courses (course_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "student_id TEXT NOT NULL REFERENCES students(student_id) ON DELETE CASCADE, name TEXT NOT NULL, "
                + "course_code TEXT, section TEXT, week1_date TEXT NOT NULL, target_grade TEXT)",
            "CREATE TABLE IF NOT EXISTS topics (topic_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "course_id INTEGER NOT NULL REFERENCES courses(course_id) ON DELETE CASCADE, week_number INTEGER NOT NULL, "
                + "title TEXT NOT NULL, completed_at TEXT, actual_time INTEGER)",
            "CREATE TABLE IF NOT EXISTS assessments (assessment_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "course_id INTEGER NOT NULL REFERENCES courses(course_id) ON DELETE CASCADE, type TEXT NOT NULL, "
                + "name TEXT NOT NULL, date TEXT, weightage REAL NOT NULL, marks REAL, total_marks REAL, score_date TEXT)",
            "CREATE TABLE IF NOT EXISTS point_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "student_id TEXT NOT NULL REFERENCES students(student_id) ON DELETE CASCADE, points INTEGER NOT NULL, "
                + "reason TEXT, at TEXT NOT NULL, source TEXT)",
            "CREATE TABLE IF NOT EXISTS plans (plan_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "student_id TEXT NOT NULL REFERENCES students(student_id) ON DELETE CASCADE, generated_at TEXT NOT NULL, "
                + "status TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "plan_id INTEGER NOT NULL REFERENCES plans(plan_id) ON DELETE CASCADE, course_name TEXT NOT NULL, "
                + "topic_title TEXT NOT NULL, date TEXT NOT NULL, start_time TEXT NOT NULL, duration INTEGER NOT NULL, "
                + "priority REAL, reason TEXT, status TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS friendships (student_id TEXT NOT NULL REFERENCES students(student_id) ON DELETE CASCADE, "
                + "friend_id TEXT NOT NULL, date_added TEXT NOT NULL, PRIMARY KEY (student_id, friend_id))"
        };
        try (Statement st = connection.createStatement()) {
            for (String sql : ddl) st.execute(sql);
        }
    }

    // ------------------------------------------------------------------ saving

    @Override
    public synchronized void save(Object obj) {
        Student owner = ownerOf(obj);
        try {
            connection.setAutoCommit(false);
            try {
                writeStudent(owner);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new StorageException("Could not save your data", e);
        }
    }

    private Student ownerOf(Object obj) {
        if (obj instanceof Student) return (Student) obj;
        if (obj instanceof Course && ((Course) obj).getOwner() != null) return ((Course) obj).getOwner();
        if (obj instanceof StudyPlan && ((StudyPlan) obj).getOwner() != null) return ((StudyPlan) obj).getOwner();
        throw new IllegalArgumentException("Cannot save " + obj);
    }

    private void writeStudent(Student s) throws SQLException {
        StudySettings set = s.getSettings();
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO students (student_id, full_name, display_name, email, password_hash, sem_name, sem_start, "
                + "sem_end, weightage_factor, urgency_factor, weakness_factor, weak_threshold) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) "
                + "ON CONFLICT(student_id) DO UPDATE SET full_name=excluded.full_name, display_name=excluded.display_name, "
                + "email=excluded.email, password_hash=excluded.password_hash, sem_name=excluded.sem_name, "
                + "sem_start=excluded.sem_start, sem_end=excluded.sem_end, weightage_factor=excluded.weightage_factor, "
                + "urgency_factor=excluded.urgency_factor, weakness_factor=excluded.weakness_factor, "
                + "weak_threshold=excluded.weak_threshold")) {
            ps.setString(1, s.getStudentId());
            ps.setString(2, s.getFullName());
            ps.setString(3, s.getDisplayName());
            ps.setString(4, s.getEmail());
            ps.setString(5, s.getPasswordHash());
            ps.setString(6, s.getSemester().getName());
            ps.setString(7, str(s.getSemester().getStartDate()));
            ps.setString(8, str(s.getSemester().getEndDate()));
            ps.setDouble(9, set.getWeightageFactor());
            ps.setDouble(10, set.getUrgencyFactor());
            ps.setDouble(11, set.getWeaknessFactor());
            ps.setDouble(12, set.getWeakScoreThreshold());
            ps.executeUpdate();
        }

        // Child rows are rewritten; cascading deletes remove topics, assessments and sessions with them.
        for (String table : new String[]{"availability", "courses", "point_transactions", "plans", "friendships"}) {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM " + table + " WHERE student_id = ?")) {
                ps.setString(1, s.getStudentId());
                ps.executeUpdate();
            }
        }

        for (StudyAvailability a : s.getAvailability()) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO availability (student_id, day, hours, start_time) VALUES (?,?,?,?)")) {
                ps.setString(1, s.getStudentId());
                ps.setString(2, a.getDayOfWeek());
                ps.setDouble(3, a.getAvailableHours());
                ps.setString(4, a.getStartTime().toString());
                ps.executeUpdate();
            }
        }

        for (Course c : s.getCourses()) writeCourse(s, c);

        for (PointTransaction t : s.getTransactions()) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO point_transactions (student_id, points, reason, at, source) VALUES (?,?,?,?,?)")) {
                ps.setString(1, s.getStudentId());
                ps.setInt(2, t.getPointsChange());
                ps.setString(3, t.getReason());
                ps.setString(4, t.getTransactionDateTime().toString());
                ps.setString(5, t.getSource());
                ps.executeUpdate();
            }
        }

        for (StudyPlan p : s.getPlans()) writePlan(s, p);

        for (Friendship f : s.getFriendships()) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO friendships (student_id, friend_id, date_added) VALUES (?,?,?)")) {
                ps.setString(1, s.getStudentId());
                ps.setString(2, f.getFriend().getStudentId());
                ps.setString(3, f.getDateAdded().toString());
                ps.executeUpdate();
            }
        }
    }

    private void writeCourse(Student s, Course c) throws SQLException {
        long courseId;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO courses (student_id, name, course_code, section, week1_date, target_grade) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, s.getStudentId());
            ps.setString(2, c.getName());
            ps.setString(3, c.getCourseCode());
            ps.setString(4, c.getSection());
            ps.setString(5, c.getWeek1Date().toString());
            ps.setString(6, c.getTargetGrade());
            ps.executeUpdate();
        }
        courseId = lastInsertId();
        for (Topic t : c.getTopics()) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO topics (course_id, week_number, title, completed_at, actual_time) VALUES (?,?,?,?,?)")) {
                ps.setLong(1, courseId);
                ps.setInt(2, t.getWeekNumber());
                ps.setString(3, t.getTitle());
                ps.setString(4, t.isCompleted() ? t.getProgress().getCompletedAt().toString() : null);
                if (t.isCompleted() && t.getProgress().getActualTimeSpent() != null) {
                    ps.setInt(5, t.getProgress().getActualTimeSpent());
                } else {
                    ps.setNull(5, Types.INTEGER);
                }
                ps.executeUpdate();
            }
        }
        for (Assessment a : c.getAssessments()) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO assessments (course_id, type, name, date, weightage, marks, total_marks, score_date) "
                    + "VALUES (?,?,?,?,?,?,?,?)")) {
                ps.setLong(1, courseId);
                ps.setString(2, a.getTypeDetail());
                ps.setString(3, a.getName());
                ps.setString(4, str(a.getDate()));
                ps.setDouble(5, a.getWeightage());
                if (a.isGraded()) {
                    ps.setDouble(6, a.getScore().getMarksObtained());
                    ps.setDouble(7, a.getScore().getTotalMarks());
                    ps.setString(8, a.getScore().getDate().toString());
                } else {
                    ps.setNull(6, Types.REAL);
                    ps.setNull(7, Types.REAL);
                    ps.setNull(8, Types.VARCHAR);
                }
                ps.executeUpdate();
            }
        }
    }

    private void writePlan(Student s, StudyPlan p) throws SQLException {
        long planId;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO plans (student_id, generated_at, status) VALUES (?,?,?)")) {
            ps.setString(1, s.getStudentId());
            ps.setString(2, p.getGeneratedAt().toString());
            ps.setString(3, p.getStatus());
            ps.executeUpdate();
        }
        planId = lastInsertId();
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO sessions (plan_id, course_name, topic_title, date, start_time, duration, priority, reason, status) "
                + "VALUES (?,?,?,?,?,?,?,?,?)")) {
            for (StudySession ss : p.getSessions()) {
                ps.setLong(1, planId);
                ps.setString(2, ss.getTopic().getCourse().getName());
                ps.setString(3, ss.getTopic().getTitle());
                ps.setString(4, ss.getScheduledDate().toString());
                ps.setString(5, ss.getStartTime().toString());
                ps.setInt(6, ss.getPlannedDuration());
                ps.setDouble(7, ss.getPriorityScore());
                ps.setString(8, ss.getReason());
                ps.setString(9, ss.getStatus());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ------------------------------------------------------------------ loading

    @Override
    public synchronized Student loadStudent(String studentId) {
        for (Student s : loadAllStudents()) if (s.getStudentId().equalsIgnoreCase(studentId)) return s;
        return null;
    }

    @Override
    public synchronized List<Student> loadAllStudents() {
        try {
            Map<String, Student> byId = new LinkedHashMap<>();
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM students ORDER BY student_id")) {
                while (rs.next()) {
                    Student s = new Student(rs.getString("full_name"), rs.getString("display_name"),
                            rs.getString("student_id"), rs.getString("email"), rs.getString("password_hash"));
                    if (rs.getString("sem_start") != null) s.getSemester().setStartDate(LocalDate.parse(rs.getString("sem_start")));
                    if (rs.getString("sem_end") != null) s.getSemester().setEndDate(LocalDate.parse(rs.getString("sem_end")));
                    s.getSettings().setFactors(rs.getDouble("weightage_factor"), rs.getDouble("urgency_factor"),
                            rs.getDouble("weakness_factor"));
                    s.getSettings().setWeakScoreThreshold(rs.getDouble("weak_threshold"));
                    byId.put(s.getStudentId(), s);
                }
            }
            for (Student s : byId.values()) loadDetails(s, byId);
            return new ArrayList<>(byId.values());
        } catch (SQLException e) {
            throw new StorageException("Could not load the accounts", e);
        }
    }

    private void loadDetails(Student s, Map<String, Student> byId) throws SQLException {
        String id = s.getStudentId();

        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM availability WHERE student_id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    s.restoreAvailability(rs.getString("day"), rs.getDouble("hours"), LocalTime.parse(rs.getString("start_time")));
                }
            }
        }

        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM courses WHERE student_id = ? ORDER BY course_id")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Course c = s.createCourse(rs.getString("name"), rs.getString("course_code"),
                            LocalDate.parse(rs.getString("week1_date")));
                    c.setSection(rs.getString("section"));
                    c.setTargetGrade(rs.getString("target_grade"));
                    loadCourseParts(c, rs.getLong("course_id"));
                }
            }
        }

        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM point_transactions WHERE student_id = ? ORDER BY id")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    s.restoreTransaction(rs.getInt("points"), rs.getString("reason"),
                            LocalDateTime.parse(rs.getString("at")), rs.getString("source"));
                }
            }
        }

        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM plans WHERE student_id = ? ORDER BY plan_id")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StudyPlan p = new StudyPlan(LocalDateTime.parse(rs.getString("generated_at")));
                    p.setStatus(rs.getString("status"));
                    s.addPlan(p);
                    loadSessions(s, p, rs.getLong("plan_id"));
                }
            }
        }

        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM friendships WHERE student_id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Student friend = byId.get(rs.getString("friend_id"));
                    if (friend != null) s.restoreFriendship(friend, LocalDate.parse(rs.getString("date_added")));
                }
            }
        }
    }

    private void loadCourseParts(Course c, long courseId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM topics WHERE course_id = ? ORDER BY topic_id")) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Topic t = c.addTopic(rs.getInt("week_number"), rs.getString("title"));
                    if (rs.getString("completed_at") != null) {
                        int minutes = rs.getInt("actual_time");
                        t.restoreProgress(LocalDateTime.parse(rs.getString("completed_at")), rs.wasNull() ? null : minutes);
                    }
                }
            }
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM assessments WHERE course_id = ? ORDER BY assessment_id")) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String date = rs.getString("date");
                    Assessment a = c.addAssessment(rs.getString("type"), rs.getString("name"),
                            date == null ? null : LocalDate.parse(date), rs.getDouble("weightage"));
                    if (rs.getString("score_date") != null) {
                        a.restoreScore(rs.getDouble("marks"), rs.getDouble("total_marks"), LocalDate.parse(rs.getString("score_date")));
                    }
                }
            }
        }
    }

    private void loadSessions(Student s, StudyPlan p, long planId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM sessions WHERE plan_id = ? ORDER BY id")) {
            ps.setLong(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Course c = s.getCourse(rs.getString("course_name"));
                    if (c == null) continue;
                    Topic topic = null;
                    for (Topic t : c.getTopics()) if (t.getTitle().equals(rs.getString("topic_title"))) { topic = t; break; }
                    if (topic == null) continue;
                    StudySession ss = p.addSession(topic, LocalDate.parse(rs.getString("date")),
                            LocalTime.parse(rs.getString("start_time")), rs.getInt("duration"),
                            rs.getDouble("priority"), rs.getString("reason"));
                    ss.setStatus(rs.getString("status"));
                }
            }
        }
    }

    private long lastInsertId() throws SQLException {
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static String str(LocalDate d) { return d == null ? null : d.toString(); }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // nothing useful to do
        }
    }
}
