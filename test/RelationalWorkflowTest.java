import author.*;
import com.smartstudy.model.*;
import com.smartstudy.service.*;
import com.smartstudy.persistence.*;
import java.util.*;
import java.time.*;

/** Exercises service mutations through JDBC inside a rollback-only transaction. */
public final class RelationalWorkflowTest {
    private static int checks;
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    private static void denied(Runnable action){checks++;try{action.run();throw new AssertionError("Expected rejection");}catch(IllegalArgumentException expected){}}
    public static void main(String[] args)throws Exception{
        Database.begin();
        try{
            User admin=UserStore.active("admin@smartstudy.vn"),teacher=UserStore.active("teacher@smartstudy.vn"),student=UserStore.active("student@smartstudy.vn");
            String unique=UUID.randomUUID().toString();String otherEmail="qa-"+unique+"@smartstu.test";
            UserStore.save(admin,otherEmail,"Giáo viên kiểm thử",Role.TEACHER,false,0,"Local-Jdbc-Teacher-2026!");User other=UserStore.active(otherEmail);
            check(other!=null,"Account INSERT");
            denied(()->UserStore.save(student,"escalation@smartstu.test","X",Role.ADMIN,false,0,"invalidEscalation"));
            var catalog=CatalogService.getInstance();var courses=CourseService.getInstance();
            var course=catalog.save(teacher,"courses",null,"Khóa kiểm thử JDBC","Mục tiêu và nội dung khóa học kiểm thử toàn luồng.","Vật lý 10","pending",0);
            check(courses.courses(student).stream().noneMatch(c->c.id().equals(course.id())),"Pending course hidden");
            denied(()->catalog.moderate(teacher,course.id(),"published","test",course.version()));
            catalog.moderate(admin,course.id(),"published","Nội dung đã được kiểm tra",course.version());
            courses.chapter(teacher,course.id(),null,"Chương kiểm thử",1);String chapter=courses.chapters(course.id()).get(0).id();
            check(courses.chapters(course.id()).size()==1,"Chapter INSERT");
            courses.chapter(teacher,course.id(),chapter,"Chương cập nhật",2);check(courses.chapter(chapter).order()==2,"Chapter UPDATE");
            courses.chapter(teacher,course.id(),null,"Chương trống để xóa",3);String empty=courses.chapters(course.id()).stream().filter(c->c.order()==3).findFirst().orElseThrow().id();courses.removeChapter(teacher,course.id(),empty);check(courses.chapter(empty)==null,"Chapter DELETE");
            var lesson=catalog.save(teacher,"lesson",null,"Bài học kiểm thử","Mục tiêu, lý thuyết, ví dụ và bài luyện tập.","Động lực học","published",0);
            courses.place(teacher,lesson.id(),course.id(),chapter,1,"");
            check(!courses.canLearn(student,lesson.id()),"Unenrolled student denied");
            denied(()->courses.place(other,lesson.id(),course.id(),chapter,1,""));
            courses.enroll(student,course.id(),true);courses.enroll(student,course.id(),true);
            check(courses.enrollments(student).stream().filter(e->e.course().equals(course.id())).count()==1,"Enrollment uniqueness");
            check(courses.canLearn(student,lesson.id()),"Authorized learning");
            LearningService.getInstance().complete(student.getEmail(),lesson.id(),lesson.title());check(courses.progress(student.getEmail(),course.id())==100,"Progress SELECT after UPDATE");
            denied(()->courses.removeChapter(teacher,course.id(),chapter));
            denied(()->catalog.save(other,"lesson",lesson.id(),"X","X","X","published",lesson.version()));
            var bank=QuestionBankService.getInstance();var question=new Question(null,"Một vật 2 kg chịu hợp lực 10 N. Gia tốc là?",List.of("2 m/s²","5 m/s²","10 m/s²","20 m/s²"),1,"a = F/m = 5 m/s².","Newton","Vận dụng",2,"Giáo viên biên soạn");
            check(bank.addSelected(teacher,List.of(question))==1,"Question bank INSERT");check(bank.addSelected(teacher,List.of(question))==0,"Duplicate bank question skipped");
            denied(()->bank.update(other,question,1));bank.archive(question.id(),true);check(bank.active(teacher).stream().noneMatch(q->q.id().equals(question.id())),"Question archive");bank.archive(question.id(),false);
            var quizzes=QuizService.getInstance();var quiz=quizzes.save(teacher,null,"Quiz kiểm thử JDBC","Kiểm tra tính điểm và lịch sử",5,"published",List.of(question.id()),0,null,1,10,course.id());
            denied(()->quizzes.save(other,quiz.id(),quiz.title(),quiz.description(),5,"published",List.of(question.id()),quiz.version(),null,1,10,course.id()));
            var run=quizzes.start(quiz.id(),student);var result=quizzes.submit(run.token(),student,Map.of("answer_"+question.id(),new String[]{"1"}));check(result.score()==10,"Server weighted score");
            check(quizzes.submit(run.token(),student,Map.of()).score()==10,"Quiz resubmit idempotency");denied(()->quizzes.start(quiz.id(),student));
            check(quizzes.history(student).stream().anyMatch(r->r.run().token().equals(run.token())),"Quiz history from MySQL");
            var essays=EssayWorkflow.getInstance();var assignment=essays.save(teacher,null,"Tự luận JDBC","Tính gia tốc vật 2 kg chịu hợp lực 10 N.","Giải thích bằng công thức và đơn vị.","a = 5 m/s², cùng hướng hợp lực.","Công thức | 4\nTính toán và đơn vị | 6","",true,0);courses.assignAssessment(teacher,assignment.id(),course.id());
            essays.draft(student,assignment.id(),"Bản nháp tiếng Việt có dấu");check(essays.draft(student,assignment.id()).contains("tiếng Việt"),"Essay draft persisted");
            String token=UUID.randomUUID().toString();var submission=essays.submit(student,assignment.id(),"Áp dụng a = F/m = 10/2 = 5 m/s², cùng hướng với hợp lực tác dụng.",token);
            check(essays.submit(student,assignment.id(),"Nội dung khác để kiểm tra gửi trùng",token).id().equals(submission.id()),"Essay submit idempotency");
            denied(()->essays.review(other,submission.id(),List.of(4.0,6.0),"Phản hồi",true,0));
            essays.review(teacher,submission.id(),List.of(4.0,5.0),"Công thức đúng, cần kết luận rõ hướng.",false,0);check(essays.submission(submission.id(),student).visible()==null,"Unpublished marks private");
            essays.review(teacher,submission.id(),List.of(4.0,6.0),"Bài làm đúng, đã nêu hướng gia tốc.",true,1);check(essays.submission(submission.id(),student).visible().total()==10,"Published marks visible");
            check(essays.submission(submission.id(),teacher).reviews().size()==2,"Review history retained");
            denied(()->essays.review(teacher,submission.id(),List.of(4.0,6.0),"Tab cũ",true,0));
            check(Database.use(c->{try(var s=c.prepareStatement("SELECT TIMESTAMPDIFF(SECOND,UTC_TIMESTAMP(),CURRENT_TIMESTAMP())");var r=s.executeQuery()){r.next();return r.getInt(1)==0;}}),"JDBC session uses UTC");
            System.out.println("PASS "+checks+" relational course, account, bank, quiz, rubric, ownership and UTC checks (rolled back)");
        }finally{Database.end(false);}
    }
}
