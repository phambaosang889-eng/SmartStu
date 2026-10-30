import author.*;
import com.smartstudy.model.*;
import com.smartstudy.service.*;
import com.smartstudy.web.ViewSupport;
import java.util.*;
import java.time.*;

/** Runs in a separate JVM: no modification of the running app's demo data. */
public final class DomainWorkflowTest {
    static int checks;
    static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    static void rejects(Runnable action,String message){checks++;try{action.run();throw new AssertionError(message);}catch(IllegalArgumentException expected){}}
    public static void main(String[] args){
        User admin=UserStore.authenticate("admin@smartstudy.vn","123456",Role.ADMIN),teacher=UserStore.active("teacher@smartstudy.vn"),student=UserStore.active("student@smartstudy.vn");
        check(admin!=null,"demo admin login");check(UserStore.authenticate(student.getEmail(),"123456",Role.TEACHER)==null,"role cannot be chosen to escalate");
        rejects(()->UserStore.save(teacher,"x@test.vn","X",Role.ADMIN,false,0,"testPassword1"),"teacher must not create account");
        rejects(()->UserStore.save(admin,admin.getEmail(),"A",Role.ADMIN,true,1,null),"last admin protected");
        UserStore.save(admin,"teacher2@test.vn","Giáo viên 2",Role.TEACHER,false,0,"testPassword1");User other=UserStore.active("teacher2@test.vn");
        UserStore.save(admin,"student2@test.vn","Học sinh 2",Role.STUDENT,false,0,"testPassword1");User stranger=UserStore.active("student2@test.vn");
        check(UserStore.authenticate(other.getEmail(),"wrong",Role.TEACHER)==null,"wrong password rejected");
        rejects(()->UserStore.password(stranger,"wrong","newTestPassword2"),"password change requires current secret");
        rejects(()->UserStore.password(stranger,"testPassword1","short"),"new password length enforced");
        UserStore.password(stranger,"testPassword1","newTestPassword2");check(UserStore.authenticate(stranger.getEmail(),"testPassword1",Role.STUDENT)==null,"old password no longer accepted");check(UserStore.authenticate(stranger.getEmail(),"newTestPassword2",Role.STUDENT)!=null,"changed password authenticates");
        var catalog=CatalogService.getInstance();var cs=CourseService.getInstance();
        var course=catalog.save(other,"courses",null,"Khóa của giáo viên 2","Mục tiêu và lộ trình Cơ học","Vật lý 10","pending",0);
        check(cs.courses(student).stream().noneMatch(c->c.id().equals(course.id())),"pending course hidden");
        rejects(()->catalog.save(admin,"lesson",null,"Sửa chuyên môn","Nội dung","Vật lý","published",0),"admin cannot author professional content");
        catalog.moderate(admin,course.id(),"published","Đã rà soát nội dung",course.version());cs.chapter(other,course.id(),null,"Chương 1",1);var chapter=cs.chapters(course.id()).get(0);
        rejects(()->cs.chapter(teacher,course.id(),null,"Xâm nhập",1),"teacher chapter ownership");
        var lesson=catalog.save(other,"lesson",null,"Bài học riêng","Công thức và ví dụ","Lực","published",0);cs.place(lesson.id(),cs.validate(other,chapter.id(),1,""));
        check(!cs.canLearn(student,lesson.id()),"student not enrolled cannot learn");cs.enroll(student,course.id(),true);check(cs.canLearn(student,lesson.id()),"enrollment grants access");
        check(!cs.canLearn(stranger,lesson.id()),"other student cannot access course");
        rejects(()->cs.validate(other,chapter.id(),1,"javascript:alert(1)"),"unsafe resource scheme rejected");
        rejects(()->cs.validateCover("javascript:alert(1)"),"unsafe cover rejected");
        rejects(()->new AIQuestionRequest(null,2,"Vận dụng",1),"missing AI topic returns validation error");
        rejects(()->new AIQuestionRequest("Chuyển động",2,"Vận dụng",1,"Bài","Mục tiêu","Khó",0,""),"AI estimated time validated");
        rejects(()->catalog.save(other,"topics",null,"Chủ đề","Nội dung","Môn học","published",0),"teacher cannot change system categories");
        var topic=catalog.save(admin,"topics",null,"Đo lường","Sử dụng đơn vị SI","Chủ đề","published",0);
        rejects(()->catalog.save(admin,"topics",null,"  ĐO   LƯỜNG  ","Nội dung khác","Chủ đề","published",0),"duplicate normalized topic rejected");
        var revised=catalog.save(admin,"topics",topic.id(),"Đo lường","Bổ sung cách biểu diễn sai số","Chủ đề","published",topic.version());
        check(revised.version()==topic.version()+1,"existing topic can be edited without duplicate error");
        rejects(()->catalog.save(other,"lesson",lesson.id(),"Stale","Nội dung","Lực","published",0),"stale lesson edit rejected");
        var bank=QuestionBankService.getInstance();var q1=new Question(null,"2 kg chịu 6 N, a bằng?",List.of("2","3","4","5"),1,"a = 6/2 = 3 m/s².","Newton","Vận dụng",2,"Giáo viên biên soạn");var q2=new Question(null,"Đơn vị lực?",List.of("m","N","J","W"),1,"Đơn vị SI của lực là N.","Newton","Nhận biết",1,"Giáo viên biên soạn");
        check(bank.addSelected(other,List.of(q1,q2))==2,"bank adds selected");check(bank.addSelected(other,List.of(q1))==0,"duplicate not added");check(bank.owned(teacher).isEmpty(),"bank ownership");
        var quiz=QuizService.getInstance();var definition=quiz.save(other,null,"Kiểm tra","Kiểm tra thang điểm",10,"published",List.of(q1.id(),q2.id()),0,null,1,10,course.id());
        check(quiz.questions(teacher).stream().noneMatch(q->q.id().equals(q1.id())),"other teacher cannot reuse private questions");
        rejects(()->quiz.save(admin,null,"X","X",10,"draft",List.of(q1.id()),0),"admin quiz mutation denied");
        var run=quiz.start(definition.id(),student);check(quiz.start(definition.id(),student).token().equals(run.token()),"start resumes running attempt");
        rejects(()->quiz.submit(run.token(),student,Map.of("answer_"+q1.id(),new String[]{"99"})),"invalid answer rejected");
        var result=quiz.submit(run.token(),student,Map.of("answer_"+q1.id(),new String[]{"1"}));check(result.score()==7,"weighted configured score");check(quiz.submit(run.token(),student,Map.of())==result,"submission idempotency");
        check(quiz.history(teacher).isEmpty(),"teacher cannot read another teacher results");check(quiz.run(run.token(),stranger)==null,"student run ownership");rejects(()->quiz.start(definition.id(),student),"attempt limit enforced");
        var ew=EssayWorkflow.getInstance();var assignment=ew.save(other,null,"Tự luận","Tính gia tốc từ F và m","Trình bày bước giải","a = F/m", "Kiến thức | 4\nTính toán | 6","",true,0);cs.assignAssessment(other,assignment.id(),course.id());
        check(ew.assignment(assignment.id(),stranger)==null,"essay requires enrollment");ew.draft(student,assignment.id(),"Bản nháp được lưu trên máy chủ");check(!ew.draft(student,assignment.id()).isBlank(),"draft stored");
        var sub=ew.submit(student,assignment.id(),"Theo định luật II Newton, a = F/m = 6/2 = 3 m/s².",UUID.randomUUID().toString());String token=UUID.randomUUID().toString();var sub2=ew.submit(student,assignment.id(),"Lời giải lần hai có đủ độ dài để nộp bài.",token);check(ew.submit(student,assignment.id(),"Nộp lại",token)==sub2,"essay duplicate token idempotency");
        check(ew.submission(sub.id(),teacher)==null,"teacher essay ownership");check(ew.submission(sub.id(),stranger)==null,"student essay ownership");
        rejects(()->ew.review(other,sub.id(),List.of(5.0,6.0),"Sai thang điểm",true,0),"rubric bounds enforced");ew.review(other,sub.id(),List.of(4.0,5.0),"Đúng công thức; bổ sung hướng của gia tốc.",false,0);check(ew.submission(sub.id(),student).visible()==null,"unpublished grade hidden");
        rejects(()->ew.review(other,sub.id(),List.of(4.0,6.0),"Tab cũ",true,0),"stale grade rejected");ew.review(other,sub.id(),List.of(4.0,6.0),"Bài làm đúng, giải thích hướng gia tốc hợp lực.",true,1);check(ew.submission(sub.id(),student).visible().total()==10,"reviewed publication visible");check(ew.submission(sub.id(),other).reviews().size()==2,"grade history retained");
        cs.enroll(student,course.id(),false);check(!cs.canLearn(student,lesson.id()),"leaving revokes content access");check(ew.submission(sub.id(),student)!=null,"personal submitted history retained");
        var account=UserStore.account(other.getEmail());UserStore.save(admin,other.getEmail(),other.getName(),Role.TEACHER,true,account.version(),null);check(UserStore.active(other.getEmail())==null,"locked account denied");check(UserStore.authenticate(other.getEmail(),"testPassword1",Role.TEACHER)==null,"locked login denied");
        rejects(()->Json.parse("{\"x\":1,\"x\":2}"),"duplicate JSON keys rejected");rejects(()->Json.parse("{\"x\":NaN}"),"NaN rejected");rejects(()->Json.parse("{} extra"),"JSON trailing data rejected");check(Json.parse(Json.write(Map.of("vi","Vật lý \"10\"\n"))) instanceof Map,"UTF8 JSON round trip");check(ViewSupport.h("<script>\"'&").equals("&lt;script&gt;&quot;&#39;&amp;"),"output escaping");
        check(ReportingService.csv(ReportingService.courses(admin,null,null)).startsWith("\uFEFF"),"CSV UTF8 BOM");
        System.out.println("PASS "+checks+" domain checks: ownership, enrollment, grading, idempotency, accounts, JSON and reporting.");
    }
}
