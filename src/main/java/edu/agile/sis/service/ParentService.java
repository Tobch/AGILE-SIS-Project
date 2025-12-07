package edu.agile.sis.service;

import edu.agile.sis.dao.SubmissionDAO;
import edu.agile.sis.dao.UserDAO;
import edu.agile.sis.model.Grade;

import org.bson.Document;

import java.lang.reflect.Method;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.bson.types.ObjectId;


public class ParentService {

    private final UserDAO userDAO = new UserDAO();
    private final SubmissionDAO submissionDAO = new SubmissionDAO();
    private final CourseService courseService = new CourseService();
    private final AssignmentService assignmentService = new AssignmentService();

    public List<Document> getLinkedStudents(String parentId) {
        return userDAO.getStudentsForParent(parentId);
    }

    
public List<Grade> getGradesForStudentFromSubmissions(String studentId) {
    List<Document> subs = submissionDAO.listByStudent(studentId);
    if (subs == null) subs = new ArrayList<>();

   
    Map<String, List<Document>> groups = new LinkedHashMap<>();

    for (Document s : subs) {
     
        String subjectId = firstNonBlank(
                asString(s.get("subjectId")),
                asString(s.get("subjectCode")),
                asString(s.get("courseCode")),
                asString(s.get("course")),
                asString(s.get("subject"))
        );

        String subjectName = firstNonBlank(
                asString(s.get("subjectName")),
                asString(s.get("courseName")),
                asString(s.get("title"))
        );

        String assignmentId = asString(s.get("assignmentId"));

        
        if ((subjectId == null || subjectId.isBlank()) && assignmentId != null && !assignmentId.isBlank()) {
            String resolved = tryResolveAssignmentToCourseCode(assignmentId);
            if (resolved != null && !resolved.isBlank()) subjectId = resolved;
        }

       
        String key;
        if (subjectId != null && !subjectId.isBlank()) key = subjectId.trim();
        else if (subjectName != null && !subjectName.isBlank()) key = subjectName.trim();
        else if (assignmentId != null && !assignmentId.isBlank()) key = "assignment::" + assignmentId.trim();
        else key = "unknown::" + UUID.randomUUID();

       
        key = key.replaceAll("\\s+", " ").trim();

        groups.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
    }

    final double BUCKET_FINAL_MAX = 60.0;
    final double BUCKET_QUIZ_MAX  = 20.0;
    final double BUCKET_ASSIGN_MAX= 20.0;

    List<Grade> result = new ArrayList<>();

    for (Map.Entry<String, List<Document>> e : groups.entrySet()) {
        String key = e.getKey();
        List<Document> docs = e.getValue();

        String subjectId = null;
        String subjectName = null;
        Double credits = null;

     
        for (Document d : docs) {
            subjectId = firstNonBlank(subjectId,
                    asString(d.get("subjectId")),
                    asString(d.get("subjectCode")),
                    asString(d.get("courseCode")),
                    asString(d.get("course")));
            subjectName = firstNonBlank(subjectName,
                    asString(d.get("subjectName")),
                    asString(d.get("courseName")),
                    asString(d.get("title")));
            if (credits == null) credits = extractCredits(d);
            if (subjectId != null && subjectName != null && credits != null) break;
        }

        // if we have subjectId but no name, try CourseService
        if ((subjectName == null || subjectName.isBlank()) && subjectId != null && !subjectId.isBlank()) {
            try {
                Object found = courseService.findByCode(subjectId);
                if (found instanceof Document) {
                    Document cd = (Document) found;
                    subjectName = firstNonBlank(asString(cd.get("title")), asString(cd.get("name")));
                } else if (found != null) subjectName = found.toString();
            } catch (Throwable ignored) {}
        }

        if (subjectName == null || subjectName.isBlank()) {
            
            subjectName = key;
        }

        
        double finalSum = 0.0;
        double quizSum  = 0.0;
        double assignSum= 0.0;

        for (Document d : docs) {
           
            String atype = firstNonBlank(asString(d.get("type")), asString(d.get("itemType")), asString(d.get("category")), asString(d.get("assessmentType")));
            if (atype == null || atype.isBlank()) {
     
                Object aid = d.get("assignmentId");
                if (aid != null) {
                    try {
                        Document a = assignmentService.getById(asString(aid));
                        if (a != null) atype = firstNonBlank(asString(a.get("type")), asString(a.get("category")), asString(a.get("title")));
                    } catch (Throwable ignored) {}
                }
            }
            String lower = atype == null ? "" : atype.toLowerCase(Locale.ROOT);

            String bucket;
            if (lower.contains("final") || lower.contains("exam") || lower.contains("midterm") || lower.contains("overall")) bucket = "final";
            else if (lower.contains("quiz") || lower.contains("test")) bucket = "quiz";
            else if (lower.contains("assign") || lower.contains("homework") || lower.contains("project")) bucket = "assign";
            else {
  
                bucket = docs.size() == 1 ? "final" : "assign";
            }

          
            Double sc = asNumber(d.get("score")) == null ? null : asNumber(d.get("score")).doubleValue();
            Double mx = asNumber(d.get("maxScore")) == null ? null : asNumber(d.get("maxScore")).doubleValue();
      
            if (sc == null) {
                Number n = asNumber(d.get("points"));
                if (n != null) sc = n.doubleValue();
            }
            if (mx == null) {
                Number n = asNumber(d.get("pointsPossible"));
                if (n != null) mx = n.doubleValue();
            }

            Double contrib = 0.0;
            double bucketMax = bucket.equals("final") ? BUCKET_FINAL_MAX : (bucket.equals("quiz") ? BUCKET_QUIZ_MAX : BUCKET_ASSIGN_MAX);

            if (sc != null && mx != null && mx > 0.0) {
                contrib = (sc / mx) * bucketMax;
            } else {
                
                Double numeric = null;
                Number gn = asNumber(d.get("grade"));
                if (gn != null) numeric = gn.doubleValue();
                else {
                    Number pn = asNumber(d.get("percentage"));
                    if (pn == null) pn = asNumber(d.get("percent"));
                    if (pn != null) numeric = pn.doubleValue();
                    else {
                        Object gval = d.get("grade");
                        if (gval instanceof String) {
                            try {
                                String s = gval.toString().trim();
                                if (s.endsWith("%")) s = s.substring(0, s.length()-1).trim();
                                numeric = Double.parseDouble(s);
                            } catch (Throwable ignored) {}
                        }
                    }
                }

                if (numeric != null) {
                    if (numeric <= bucketMax) contrib = numeric;                    
                    else if (numeric <= 100.0) contrib = (numeric / 100.0) * bucketMax; 
                    else contrib = Math.min(numeric, bucketMax);
                } else {
                   
                    Object nested = d.get("scoreDetail");
                    if (nested instanceof Document) {
                        Number s2 = asNumber(((Document) nested).get("score"));
                        Number m2 = asNumber(((Document) nested).get("maxScore"));
                        if (s2 != null && m2 != null && m2.doubleValue() > 0.0) contrib = (s2.doubleValue() / m2.doubleValue()) * bucketMax;
                    }
                }
            }

            if ("final".equals(bucket)) finalSum += contrib;
            else if ("quiz".equals(bucket)) quizSum += contrib;
            else assignSum += contrib;
        } 

        // clamp buckets
        double fClamped = Math.min(finalSum, BUCKET_FINAL_MAX);
        double qClamped = Math.min(quizSum,  BUCKET_QUIZ_MAX);
        double aClamped = Math.min(assignSum, BUCKET_ASSIGN_MAX);

        double subjectNumeric = fClamped + qClamped + aClamped; 

     
        String gradeValueStr = String.format(Locale.ROOT, "%.2f", subjectNumeric);
        
        String outSubjectId = subjectId != null && !subjectId.isBlank() ? subjectId : key;
        if (credits == null) credits = 1.0;

        result.add(new Grade(outSubjectId, subjectName, gradeValueStr, credits));
    }

    
    result.sort(Comparator.comparing(Grade::getSubjectName, Comparator.nullsLast(String::compareToIgnoreCase)));
    return result;
}


   
public List<Grade> getAggregatedGradesForStudent(String studentId) {
    if (studentId == null || studentId.isBlank()) return Collections.emptyList();

    SubmissionService submissionServiceLocal = new SubmissionService();
    AssignmentService assignmentServiceLocal = new AssignmentService();
    QuizService quizServiceLocal = new QuizService(); 
    List<Document> subs = submissionServiceLocal.listByStudent(studentId);
    if (subs == null) subs = Collections.emptyList();

   
    final double BUCKET_FINAL_MAX  = 60.0;
    final double BUCKET_QUIZ_MAX   = 20.0;
    final double BUCKET_ASSIGN_MAX = 20.0;

    class BucketSums {
        double rawSum = 0.0;      
        String subjectName = null;
        Double credits = null;
    }


    Map<String, Map<String, BucketSums>> data = new LinkedHashMap<>();
    
    
    
    
    
    
    
    
    
    
    
    

class Contrib {
    String subjectKey;
    String subjectName;
    Double credits;
    String bucket; 
    double amount;

    Contrib(String subjectKey, String subjectName, Double credits, String bucket, double amount) {
        this.subjectKey = subjectKey;
        this.subjectName = subjectName;
        this.credits = credits;
        this.bucket = bucket;
        this.amount = amount;
    }
}


java.util.function.BiConsumer<Contrib, String> addContrib = (contrib, fallbackSubjectKey) -> {
    String subjectKey = contrib.subjectKey;
    if (subjectKey == null || subjectKey.isBlank())
        subjectKey = (fallbackSubjectKey == null ? "unknown" : fallbackSubjectKey);

    Map<String, BucketSums> subjMap =
            data.computeIfAbsent(subjectKey, k -> new HashMap<>());

    BucketSums bs = subjMap.computeIfAbsent(contrib.bucket, k -> new BucketSums());

    if (bs.subjectName == null && contrib.subjectName != null)
        bs.subjectName = contrib.subjectName;

    if (bs.credits == null && contrib.credits != null)
        bs.credits = contrib.credits;

    bs.rawSum += contrib.amount;
};


    
    for (Document s : subs) {

        String subjectKey = firstNonBlank(asString(s.get("courseCode")),
                                          asString(s.get("subjectId")),
                                          asString(s.get("subjectCode")),
                                          asString(s.get("course")));
        String subjectName = firstNonBlank(asString(s.get("subjectName")), asString(s.get("courseName")), asString(s.get("title")));
        Double credits = extractCredits(s);

        
        String atype = firstNonBlank(asString(s.get("type")),
                                     asString(s.get("itemType")),
                                     asString(s.get("category")),
                                     asString(s.get("title")));
        if ((atype == null || atype.isBlank()) && s.containsKey("assignmentId")) {
            try {
                Document a = assignmentServiceLocal.getById(asString(s.get("assignmentId")));
                if (a != null) atype = firstNonBlank(asString(a.get("type")), asString(a.get("category")), asString(a.get("title")));
                if ((subjectKey == null || subjectKey.isBlank()) && a != null) {
                    String resolved = firstNonBlank(asString(a.get("courseCode")), asString(a.get("subjectId")));
                    if (resolved != null && !resolved.isBlank()) subjectKey = resolved;
                }
            } catch (Throwable ignored) {}
        }

        String lower = atype == null ? "" : atype.toLowerCase(Locale.ROOT);
        String bucket;
        if (lower.contains("final") || lower.contains("exam") || lower.contains("midterm") || lower.contains("overall")) bucket = "final";
        else if (lower.contains("quiz") || lower.contains("test")) bucket = "quiz";
        else if (lower.contains("assign") || lower.contains("homework") || lower.contains("project")) bucket = "assign";
        else bucket = s.containsKey("assignmentId") ? "assign" : "assign";

       
        Double numericValue = null;
        Number sc = asNumber(s.get("score"));
        if (sc == null) sc = asNumber(s.get("points"));
        if (sc != null) numericValue = sc.doubleValue();

        if (numericValue == null) {
            Number gnum = asNumber(s.get("grade"));
            if (gnum != null) numericValue = gnum.doubleValue();
        }
        if (numericValue == null) {
            Number perc = asNumber(s.get("percentage"));
            if (perc == null) perc = asNumber(s.get("percent"));
            if (perc != null) numericValue = perc.doubleValue();
        }
      
        if (numericValue == null) {
            Object gv = s.get("grade");
            if (gv instanceof Document) {
                Number maybe = asNumber(((Document) gv).get("score"));
                if (maybe == null) maybe = asNumber(((Document) gv).get("value"));
                if (maybe == null) maybe = asNumber(((Document) gv).get("points"));
                if (maybe != null) numericValue = maybe.doubleValue();
            }
        }
        if (numericValue == null) numericValue = 0.0;

        
        double bucketMax = "final".equals(bucket) ? BUCKET_FINAL_MAX : ("quiz".equals(bucket) ? BUCKET_QUIZ_MAX : BUCKET_ASSIGN_MAX);

        double contribAmount = Math.min(numericValue, bucketMax);

      
        if ((subjectKey == null || subjectKey.isBlank()) && s.containsKey("assignmentId")) {
            String resolved = tryResolveAssignmentToCourseCode(asString(s.get("assignmentId")));
            if (resolved != null) subjectKey = resolved;
        }

        Contrib c = new Contrib(subjectKey, subjectName, credits, bucket, contribAmount);
        addContrib.accept(c, "unknown");
    }


    try {
        List<Document> attempts = quizServiceLocal.listAttemptsForStudent(studentId);
        if (attempts != null) {
            for (Document at : attempts) {
                
                Number nScore = asNumber(at.get("score"));
                if (nScore == null) {
            
                    continue;
                }

                double attemptValue = nScore.doubleValue();
                
                double amount = Math.min(attemptValue, BUCKET_QUIZ_MAX);

               
                String subjectKey = firstNonBlank(asString(at.get("courseCode")), asString(at.get("course")), asString(at.get("subjectId")));
                if ((subjectKey == null || subjectKey.isBlank())) {
                    
                    Object qidObj = at.get("quizId");
                    String qidStr = null;
                    if (qidObj instanceof ObjectId) qidStr = ((ObjectId) qidObj).toHexString();
                    else if (qidObj != null) qidStr = qidObj.toString();
                    if (qidStr != null) {
                        try {
                            Document quizDoc = quizServiceLocal.getQuizById(qidStr);
                            if (quizDoc != null) {
                                String resolved = firstNonBlank(asString(quizDoc.get("courseCode")), asString(quizDoc.get("subjectId")));
                                if (resolved != null && !resolved.isBlank()) subjectKey = resolved;
                            }
                        } catch (Throwable ignored) {}
                    }
                }

                String subjectName = firstNonBlank(asString(at.get("courseName")), asString(at.get("title")));
                Double credits = extractCredits(at);

                Contrib c = new Contrib(subjectKey, subjectName, credits, "quiz", amount);
                addContrib.accept(c, "unknown");
            }
        }
    } catch (Throwable t) {
       
        System.err.println("[ParentService] quiz attempts inclusion failed: " + t.getMessage());
    }


    List<Grade> out = new ArrayList<>();
    for (Map.Entry<String, Map<String, BucketSums>> ent : data.entrySet()) {
        String subjectKey = ent.getKey();
        Map<String, BucketSums> buckets = ent.getValue();

        double finalBucket = 0.0;
        double assignBucket = 0.0;
        double quizBucket = 0.0;

        BucketSums bFinal = buckets.get("final");
        BucketSums bQuiz  = buckets.get("quiz");
        BucketSums bAssign= buckets.get("assign");

        if (bFinal != null)  finalBucket  = Math.min(bFinal.rawSum,  BUCKET_FINAL_MAX);
        if (bQuiz != null)   quizBucket   = Math.min(bQuiz.rawSum,   BUCKET_QUIZ_MAX);
        if (bAssign != null) assignBucket = Math.min(bAssign.rawSum, BUCKET_ASSIGN_MAX);

        double finalNumeric = finalBucket + quizBucket + assignBucket; // 0..100

       
        String subjectName = null;
        Double credits = 1.0;
        if (bFinal != null && bFinal.subjectName != null) { subjectName = bFinal.subjectName; credits = bFinal.credits == null ? credits : bFinal.credits; }
        if (subjectName == null && bAssign != null && bAssign.subjectName != null) { subjectName = bAssign.subjectName; credits = bAssign.credits == null ? credits : bAssign.credits; }
        if (subjectName == null && bQuiz != null && bQuiz.subjectName != null) { subjectName = bQuiz.subjectName; credits = bQuiz.credits == null ? credits : bQuiz.credits; }
        if (subjectName == null) subjectName = subjectKey;

        String gradeValStr = String.format(Locale.ROOT, "%.2f", finalNumeric);
        out.add(new Grade(subjectKey, subjectName, gradeValStr, credits));
    }

    out.sort(Comparator.comparing(Grade::getSubjectName, Comparator.nullsLast(String::compareToIgnoreCase)));
    return out;
}



private static void addIfNotBlank(List<String> list, String s) {
    if (s != null && !s.isBlank()) list.add(s);
}



  


    private String tryResolveAssignmentToCourseCode(String assignmentId) {
        if (assignmentId == null) return null;
        try {
            Document a = assignmentService.getById(assignmentId);
            if (a != null) {
                String courseCode = firstNonBlank(asString(a.get("courseCode")), asString(a.get("subjectId")), asString(a.get("course")));
                if (courseCode != null && !courseCode.isBlank()) return courseCode;
            }
        } catch (Throwable t) {
         
        }
        return null;
    }

    private static String firstNonBlank(String... v) {
        if (v == null) return null;
        for (String s : v) if (s != null && !s.isBlank()) return s;
        return null;
    }

    private static Number asNumber(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return (Number) o;
        try {
            String s = o.toString().trim();
            if (s.isEmpty()) return null;
            if (s.contains(".")) return Double.parseDouble(s);
            return Long.parseLong(s);
        } catch (Throwable ex) {
            return null;
        }
    }

    private static Double extractCredits(Document d) {
        if (d == null) return null;
        Object c = d.get("credits");
        Number n = asNumber(c);
        return n == null ? null : n.doubleValue();
    }

    private static Date safeDate(Document d, String field) {
        if (d == null) return new Date(0);
        Object o = d.get(field);
        if (o == null) {

            o = d.get("submittedAt");
            if (o == null) return new Date(0);
        }
        if (o instanceof Date) return (Date) o;
        if (o instanceof Number) return new Date(((Number) o).longValue());
        if (o instanceof CharSequence) {
            String s = o.toString().trim();
            if (s.isEmpty()) return new Date(0);
            try { Instant inst = Instant.parse(s); return Date.from(inst); } catch (DateTimeParseException ignored) {}
            try {
                LocalDateTime dt = LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return Date.from(dt.atZone(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException ignored) {}
            try {
                OffsetDateTime odt = OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                return Date.from(odt.toInstant());
            } catch (DateTimeParseException ignored) {}
            try { long ms = Long.parseLong(s); return new Date(ms);} catch (NumberFormatException ignored) {}
            try {
                LocalDate ld = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
                return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException ignored) {}
        }
        return new Date(0);
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }


    private static String mapPercentToLetter(double pct) {
        if (pct >= 90.0) return "A";
        if (pct >= 80.0) return "B";
        if (pct >= 70.0) return "C";
        if (pct >= 60.0) return "D";
        return "F";
    }


    public double computeGPA(List<Grade> grades) {
        if (grades == null || grades.isEmpty()) return 0.0;

        double totalPointsTimesCredits = 0.0;
        double totalCredits = 0.0;

        for (Grade g : grades) {
            if (g == null) continue;
            Object raw = null;
            try {
                Method m = g.getClass().getMethod("getGradeValue");
                raw = m.invoke(g);
            } catch (NoSuchMethodException ns) {
           
                try {
                    Method m2 = g.getClass().getMethod("getGrade");
                    raw = m2.invoke(g);
                } catch (Throwable ignored) {}
            } catch (Throwable ignored) {}

            double gp = convertGradeToPoint(raw);

            double c = 1.0;
            try {
                Method mc = g.getClass().getMethod("getCredits");
                Object cred = mc.invoke(g);
                if (cred instanceof Number) c = ((Number) cred).doubleValue();
                else if (cred != null) c = Double.parseDouble(cred.toString());
                if (Double.isNaN(c) || c <= 0.0) c = 1.0;
            } catch (Throwable ignored) {}

            totalPointsTimesCredits += gp * c;
            totalCredits += c;
        }

        if (totalCredits == 0.0) return 0.0;
        double raw = totalPointsTimesCredits / totalCredits;
        return Math.round(raw * 100.0) / 100.0;
    }

    private static int letterToPoints(String letter) {
        if (letter == null) return 0;
        switch (letter.toUpperCase(Locale.ROOT)) {
            case "A": return 4;
            case "B": return 3;
            case "C": return 2;
            case "D": return 1;
            default: return 0;
        }
    }

    private static double convertGradeToPoint(Object gradeVal) {
        if (gradeVal == null) return 0.0;
        if (gradeVal instanceof Number) {
            double v = ((Number) gradeVal).doubleValue();
            return letterToPoints(mapPercentToLetter(v));
        }
        String s = gradeVal.toString().trim();
        if (s.isEmpty()) return 0.0;
        String up = s.toUpperCase(Locale.ROOT);
        if (up.matches("^[A-F][+-]?$")) {
            return letterToPoints(up.substring(0, 1));
        }
        try {
            double v = Double.parseDouble(s);
            return letterToPoints(mapPercentToLetter(v));
        } catch (NumberFormatException ignored) {}
        return 0.0;
    }
}
