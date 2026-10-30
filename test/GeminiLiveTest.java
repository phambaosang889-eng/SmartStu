import author.*;
import com.smartstudy.model.*;
import com.smartstudy.service.*;
import java.time.*;
import java.util.*;

/** Opt-in real calls with synthetic educational content only. No grades are published. */
public class GeminiLiveTest {
    public static void main(String[] args){
        var ai=AIService.getInstance();
        if(!ai.configured())throw new IllegalStateException("Gemini key/model not configured. No live test performed.");
        var student=new User("student@smartstudy.vn","Kiểm thử",Role.STUDENT);
        var teacher=new User("teacher@smartstudy.vn","Kiểm thử",Role.TEACHER);
        int passed=0;
        try{
            String answer=ai.tutor(student,List.of(Map.of("role","user","content","Vật khối lượng 2 kg chịu hợp lực 10 N. Giải thích cách tính gia tốc và hướng gia tốc.")));
            if(answer.isBlank())throw new AssertionError("Empty tutor answer");
            System.out.println("PASS LIVE: tutor returned validated Vietnamese text");passed++;
        }catch(IllegalArgumentException e){System.out.println("FAIL LIVE tutor: "+e.getMessage());}
        try{
            var questions=ai.generate(teacher,new AIQuestionRequest("Định luật Newton",2,"Vận dụng",1));
            if(questions.isEmpty())throw new AssertionError("No questions");
            System.out.println("PASS LIVE: quiz generation validated "+questions.size()+" questions; not saved/published");passed++;
        }catch(IllegalArgumentException e){System.out.println("FAIL LIVE questions: "+e.getMessage());}
        try{
            var assignment=new EssayWorkflow.Assignment("synthetic-ai-test","teacher@smartstudy.vn","Định luật II Newton",
                    "Một vật 2 kg chịu hợp lực 10 N. Tính và giải thích gia tốc.","Trình bày theo SI.",
                    "a = F/m = 10/2 = 5 m/s², cùng hướng hợp lực.",
                    List.of(new EssayWorkflow.Criterion("Công thức",3),new EssayWorkflow.Criterion("Thay số và kết quả",3),new EssayWorkflow.Criterion("Đơn vị và hướng",4)),null,false,1);
            var submission=new EssayWorkflow.Submission("synthetic-ai-test",assignment,"student@smartstudy.vn",
                    "Theo định luật II Newton, a = F/m. Thay số a = 10/2 = 5 m/s². Gia tốc cùng hướng với hợp lực.",Instant.now(),List.of());
            var result=ai.grade(teacher,submission);
            if(result.total()<0||result.total()>10||result.scores().size()!=3)throw new AssertionError("Invalid rubric result");
            System.out.println("PASS LIVE: essay rubric, scores, confidence and exact evidence validated; not published");passed++;
        }catch(IllegalArgumentException e){System.out.println("FAIL LIVE essay: "+e.getMessage());}
        if(passed!=3)throw new AssertionError("Only "+passed+"/3 live Gemini operations passed");
        System.out.println("PASS 3/3 live Gemini operations");
    }
}
