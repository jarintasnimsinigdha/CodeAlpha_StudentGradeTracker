import java.util.*;
public class StudentGradeTracker {

    // ─── Inner model 
    static class Student {
        private final String id;
        private final String name;
        private final List<Double> grades = new ArrayList<>();

        Student(String id, String name) {
            this.id   = id;
            this.name = name;
        }

        void addGrade(double grade) {
            if (grade < 0 || grade > 100)
                throw new IllegalArgumentException("Grade must be 0–100.");
            grades.add(grade);
        }

        boolean removeGrade(int index) {
            if (index < 0 || index >= grades.size()) return false;
            grades.remove(index);
            return true;
        }

        double average() {
            return grades.isEmpty() ? 0
                    : grades.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        }

        double highest() {
            return grades.isEmpty() ? 0
                    : grades.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        }

        double lowest() {
            return grades.isEmpty() ? 0
                    : grades.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        }

        String letterGrade() {
            double avg = average();
            if (avg >= 90) return "A";
            if (avg >= 80) return "B";
            if (avg >= 70) return "C";
            if (avg >= 60) return "D";
            return "F";
        }

        String getStatus() { return average() >= 60 ? "PASS" : "FAIL"; }

        String getId()   { return id;   }
        String getName() { return name; }
        List<Double> getGrades() { return Collections.unmodifiableList(grades); }

        @Override
        public String toString() { return String.format("[%s] %s", id, name); }
    }

    // ─── Application state 
    private final Map<String, Student> students = new LinkedHashMap<>();
    private final Scanner scanner = new Scanner(System.in);
    private int idCounter = 1;

    // ─── Entry point 
    public static void main(String[] args) {
        new StudentGradeTracker().run();
    }

    private void run() {
        printBanner();
        preloadSampleData();   

        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Enter choice").trim();
            System.out.println();
            switch (choice) {
                case "1"  -> addStudent();
                case "2"  -> addGrade();
                case "3"  -> viewStudent();
                case "4"  -> printReport();
                case "5"  -> removeGrade();
                case "6"  -> removeStudent();
                case "7"  -> searchStudent();
                case "8"  -> topBottomStudents();
                case "0"  -> { running = false; System.out.println("Goodbye! 👋"); }
                default   -> System.out.println("  ⚠  Invalid option – please try again.");
            }
        }
    }

    // ─── Menu & banner 
    private void printBanner() {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║       STUDENT GRADE TRACKER  v1.0        ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();
    }

    private void printMenu() {
        System.out.println("┌──────────────── MENU ─────────────────┐");
        System.out.println("│  1. Add student                        │");
        System.out.println("│  2. Add grade to student               │");
        System.out.println("│  3. View student details               │");
        System.out.println("│  4. Print full summary report          │");
        System.out.println("│  5. Remove a grade from student        │");
        System.out.println("│  6. Remove student                     │");
        System.out.println("│  7. Search student by name             │");
        System.out.println("│  8. Top / bottom performers            │");
        System.out.println("│  0. Exit                               │");
        System.out.println("└────────────────────────────────────────┘");
    }
    //Feature
    //Add student 
    private void addStudent() {
        String name = prompt("Student name").trim();
        if (name.isEmpty()) { System.out.println("  ⚠  Name cannot be empty."); return; }
        String id = String.format("STU%03d", idCounter++);
        students.put(id, new Student(id, name));
        System.out.printf("  ✔  Student added: [%s] %s%n%n", id, name);
    }

    //Add grade 
    private void addGrade() {
        Student s = selectStudent();
        if (s == null) return;
        try {
            double grade = Double.parseDouble(prompt("Grade (0-100)").trim());
            s.addGrade(grade);
            System.out.printf("  ✔  Grade %.1f added for %s%n%n", grade, s.getName());
        } catch (NumberFormatException e) {
            System.out.println("  ⚠  Invalid number.");
        } catch (IllegalArgumentException e) {
            System.out.println("  ⚠  " + e.getMessage());
        }
    }

    //View student details
    private void viewStudent() {
        Student s = selectStudent();
        if (s == null) return;
        printStudentDetail(s);
    }

    private void printStudentDetail(Student s) {
        System.out.println("┌────────────────────────────────────────────┐");
        System.out.printf( "│  Student : %-31s│%n", s.getName());
        System.out.printf( "│  ID      : %-31s│%n", s.getId());
        System.out.println("├────────────────────────────────────────────┤");
        List<Double> grades = s.getGrades();
        if (grades.isEmpty()) {
            System.out.println("│  No grades recorded yet.                   │");
        } else {
            for (int i = 0; i < grades.size(); i++) {
                System.out.printf("│  Grade %-3d : %6.2f                          │%n", i + 1, grades.get(i));
            }
            System.out.println("├────────────────────────────────────────────┤");
            System.out.printf( "│  Average : %6.2f   Highest: %6.2f          │%n", s.average(), s.highest());
            System.out.printf( "│  Lowest  : %6.2f   Letter : %-3s   Status: %-4s│%n",
                    s.lowest(), s.letterGrade(), s.getStatus());
        }
        System.out.println("└────────────────────────────────────────────┘");
        System.out.println();
    }

    // Feature: Full report
    private void printReport() {
        if (students.isEmpty()) { System.out.println("  No students registered yet.\n"); return; }

        System.out.println("\n╔═══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                  STUDENT GRADE SUMMARY REPORT                        ║");
        System.out.println("╠══════════╦══════════════════════╦═════════╦══════════╦════════╦══════╣");
        System.out.println("║  ID      ║  Name                ║ Average ║  Highest ║ Lowest ║Grade ║");
        System.out.println("╠══════════╬══════════════════════╬═════════╬══════════╬════════╬══════╣");

        DoubleSummaryStatistics classStats = students.values().stream()
                .filter(s -> !s.getGrades().isEmpty())
                .mapToDouble(Student::average)
                .summaryStatistics();

        for (Student s : students.values()) {
            if (s.getGrades().isEmpty()) {
                System.out.printf("║ %-8s ║ %-20s ║  %-6s ║   %-5s  ║  %-5s ║  %-3s ║%n",
                        s.getId(), truncate(s.getName(), 20), "N/A", "N/A", "N/A", "N/A");
            } else {
                System.out.printf("║ %-8s ║ %-20s ║  %6.2f ║   %6.2f ║ %6.2f ║  %-2s  ║%n",
                        s.getId(), truncate(s.getName(), 20),
                        s.average(), s.highest(), s.lowest(), s.letterGrade() + "/" + s.getStatus().charAt(0));
            }
        }

        System.out.println("╠══════════╩══════════════════════╩═════════╩══════════╩════════╩══════╣");
        System.out.printf( "║  Total students: %-4d                                                 ║%n",
                students.size());
        if (classStats.getCount() > 0) {
            System.out.printf("║  Class average : %6.2f   Highest avg: %6.2f   Lowest avg: %6.2f      ║%n",
                    classStats.getAverage(), classStats.getMax(), classStats.getMin());
        }
        System.out.println("╚═══════════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    //Remove grade 
    private void removeGrade() {
        Student s = selectStudent();
        if (s == null) return;
        if (s.getGrades().isEmpty()) { System.out.println("  ⚠  No grades to remove.\n"); return; }
        printStudentDetail(s);
        try {
            int idx = Integer.parseInt(prompt("Grade number to remove").trim()) - 1;
            if (s.removeGrade(idx)) System.out.printf("  ✔  Grade removed from %s%n%n", s.getName());
            else                     System.out.println("    Invalid grade number.\n");
        } catch (NumberFormatException e) {
            System.out.println("   Invalid input.\n");
        }
    }

    // Remove student 
    private void removeStudent() {
        Student s = selectStudent();
        if (s == null) return;
        students.remove(s.getId());
        System.out.printf("  ✔  Removed student: %s%n%n", s);
    }

    // Search
    private void searchStudent() {
        String query = prompt("Search by name").trim().toLowerCase();
        List<Student> results = students.values().stream()
                .filter(s -> s.getName().toLowerCase().contains(query))
                .toList();
        if (results.isEmpty()) { System.out.println("    No matches found.\n"); return; }
        System.out.printf("  Found %d match(es):%n", results.size());
        results.forEach(this::printStudentDetail);
    }

    // Top / bottom performers
    private void topBottomStudents() {
        List<Student> ranked = students.values().stream()
                .filter(s -> !s.getGrades().isEmpty())
                .sorted(Comparator.comparingDouble(Student::average).reversed())
                .toList();
        if (ranked.isEmpty()) { System.out.println("   No graded students.\n"); return; }
        System.out.println("  🏆  TOP PERFORMER:");
        printStudentDetail(ranked.get(0));
        if (ranked.size() > 1) {
            System.out.println("  📉  LOWEST PERFORMER:");
            printStudentDetail(ranked.get(ranked.size() - 1));
        }
    }

    //  Helpers
    private Student selectStudent() {
        if (students.isEmpty()) { System.out.println("    No students registered.\n"); return null; }
        System.out.println("  Available students:");
        students.values().forEach(s -> System.out.printf("    %-8s %s%n", s.getId(), s.getName()));
        String input = prompt("Enter student ID").trim().toUpperCase();
        Student s = students.get(input);
        if (s == null) System.out.println("  ⚠  Student not found.\n");
        return s;
    }

    private String prompt(String label) {
        System.out.print("  ➤ " + label + ": ");
        return scanner.hasNextLine() ? scanner.nextLine() : "";
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    
    private void preloadSampleData() {
        String[][] sampleStudents = {
            {"Steve Harrington"}, {"Anya Forger"}, {"Harry Potter"}, {"Kim Shin"}
        };
        double[][] sampleGrades = {
            {92, 88, 95, 91},
            {74, 68, 72, 80},
            {55, 60, 58, 62},
            {85, 90, 87, 93}
        };
        for (int i = 0; i < sampleStudents.length; i++) {
            String id   = String.format("STU%03d", idCounter++);
            Student stu = new Student(id, sampleStudents[i][0]);
            for (double g : sampleGrades[i]) stu.addGrade(g);
            students.put(id, stu);
        }
        System.out.println("  ℹ  Sample data loaded (4 students). Use menu option 4 to see report.\n");
    }
}
