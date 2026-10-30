package com.smartstudy.service;

import author.*;
import com.smartstudy.model.CatalogEntry;
import com.smartstudy.model.view.*;
import java.util.*;

/** Prepares dashboard read models outside the JSP presentation layer. */
public final class DashboardService {
    private DashboardService() {}

    public static List<DashboardMetric> metrics(
            User u,
            List<CatalogEntry> lessons,
            List<QuizService.Result> history,
            List<EssayWorkflow.Submission> essays) {
        boolean student = u.getRole() == Role.STUDENT, admin = u.getRole() == Role.ADMIN;
        CourseService cs = CourseService.getInstance();
        LearningService ls = LearningService.getInstance();
        List<DashboardMetric> metrics = new ArrayList<>();
        if (student) {
            long complete =
                    lessons.stream()
                            .filter(e -> ls.completed(u.getEmail()).contains(e.id()))
                            .count();
            metrics.add(
                    new DashboardMetric(
                            "Bài học hoàn thành",
                            complete + " / " + lessons.size(),
                            "Trong khóa học đang tham gia"));
            metrics.add(
                    new DashboardMetric(
                            "Tiến độ bài học",
                            (lessons.isEmpty() ? 0 : Math.round(complete * 100.0 / lessons.size()))
                                    + "%",
                            "Lộ trình đang tham gia"));
            metrics.add(
                    new DashboardMetric(
                            "Thời gian học",
                            ls.minutes(u.getEmail()) + " phút",
                            "Thời gian đọc bài"));
            metrics.add(
                    new DashboardMetric(
                            "Chuỗi ngày học",
                            ls.streak(u.getEmail()) + " ngày",
                            "Duy trì thói quen mỗi ngày"));
        } else {
            metrics.add(
                    new DashboardMetric(
                            "Khóa học",
                            String.valueOf(cs.courses(u).size()),
                            admin ? "Toàn hệ thống" : "Do bạn phụ trách"));
            metrics.add(
                    new DashboardMetric(
                            "Học sinh",
                            String.valueOf(
                                    admin
                                            ? UserStore.list().stream()
                                                    .filter(a -> a.user().getRole() == Role.STUDENT)
                                                    .count()
                                            : cs.students(u).size()),
                            admin ? "Tài khoản học sinh" : "Đang tham gia khóa học của bạn"));
            metrics.add(
                    new DashboardMetric(
                            "Bài giảng",
                            String.valueOf(lessons.size()),
                            "Học liệu trong phạm vi quản lý"));
            metrics.add(
                    new DashboardMetric(
                            "Tự luận chờ công bố",
                            String.valueOf(
                                    essays.stream().filter(s -> s.visible() == null).count()),
                            "Bao gồm bài chưa chấm và đang rà soát"));
        }
        metrics.add(
                new DashboardMetric(
                        "Điểm quiz trung bình",
                        history.isEmpty()
                                ? "Chưa có"
                                : Math.round(
                                                history.stream()
                                                        .mapToDouble(
                                                                r ->
                                                                        r.score()
                                                                                * 100.0
                                                                                / r.run()
                                                                                        .quiz()
                                                                                        .maxScore())
                                                        .average()
                                                        .orElse(0))
                                        + "%",
                        "Trên các bài kiểm tra đã nộp"));
        metrics.add(
                new DashboardMetric(
                        "Lượt làm quiz",
                        String.valueOf(history.size()),
                        "Các bài kiểm tra đã hoàn thành"));
        if (admin) {
            metrics.add(
                    new DashboardMetric(
                            "Giáo viên / quản trị",
                            UserStore.list().stream()
                                            .filter(a -> a.user().getRole() == Role.TEACHER)
                                            .count()
                                    + " / "
                                    + UserStore.list().stream()
                                            .filter(a -> a.user().getRole() == Role.ADMIN)
                                            .count(),
                            "Phân loại theo vai trò"));
            metrics.add(
                    new DashboardMetric(
                            "Đăng ký / đang hoạt động",
                            cs.enrollments(u).stream()
                                            .filter(CourseService.Enrollment::active)
                                            .count()
                                    + " / "
                                    + SessionActivity.activeUsers(),
                            "Đăng ký hoạt động / truy cập trong 15 phút"));
        } else {
            metrics.add(
                    new DashboardMetric(
                            student ? "Thẻ đã nhớ" : "Quiz đã tạo",
                            String.valueOf(
                                    student
                                            ? cs.contents(u, "flashcards").stream()
                                                    .filter(
                                                            c ->
                                                                    ls.known(u.getEmail())
                                                                            .contains(c.id()))
                                                    .count()
                                            : QuizService.getInstance().list(u).size()),
                            student
                                    ? "Có thể đánh dấu cần ôn lại"
                                    : "Bao gồm bản nháp và đã phát hành"));
            metrics.add(
                    new DashboardMetric(
                            "Bài tự luận",
                            String.valueOf(
                                    student
                                            ? essays.size()
                                            : EssayWorkflow.getInstance().assignmentCount(u)),
                            student ? "Số lần bạn đã nộp" : "Đề và rubric do bạn biên soạn"));
        }
        return List.copyOf(metrics);
    }

    public static List<ChartBar> bars(List<ReportingService.DayRow> rows) {
        long max =
                Math.max(
                        1, rows.stream().mapToLong(ReportingService.DayRow::count).max().orElse(1));
        return rows.stream()
                .map(r -> new ChartBar(r.day(), r.count(), (int) (r.count() * 100 / max)))
                .toList();
    }

    public static List<GrowthChart> growth(User user) {
        if (user.getRole() != Role.ADMIN) return List.of();
        return List.of(
                new GrowthChart("Tài khoản được tạo", bars(ReportingService.growth(user, true))),
                new GrowthChart("Khóa học được tạo", bars(ReportingService.growth(user, false))));
    }

    public static List<CatalogEntry> nextLessons(
            List<CatalogEntry> lessons, Set<String> completed) {
        return lessons.stream()
                .sorted(Comparator.comparing(e -> completed.contains(e.id())))
                .limit(4)
                .toList();
    }
}
