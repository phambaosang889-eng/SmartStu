import author.*;
import com.smartstudy.persistence.*;
import com.smartstudy.dao.*;
import com.smartstudy.service.*;
import java.util.*;

/** Runs against the explicitly configured local MySQL fixture, never a production database. */
public final class DatabaseIntegrationTest {
    private static int checks;
    private static void check(boolean yes,String label){checks++;if(!yes)throw new AssertionError(label);}
    private static void denied(Runnable operation){checks++;try{operation.run();throw new AssertionError("Ownership check missing");}catch(IllegalArgumentException expected){}}
    private static String state(User student){return RecordCodec.encode(List.of(
        UserStore.account(student.getEmail()),LearningService.getInstance().completed(student.getEmail()),
        LearningService.getInstance().known(student.getEmail()),QuizService.getInstance().history(student),
        EssayWorkflow.getInstance().submissions(student),LearningService.getInstance().activities(student.getEmail())));}
    public static void main(String[] args)throws Exception{
        if(Database.memory())throw new AssertionError("Real MySQL required");
        User student=UserStore.active("student@smartstudy.vn"),teacher=UserStore.active("teacher@smartstudy.vn");
        Map<String,String> settings=Stores.map("system_settings",String.class);
        if(args.length>0&&args[0].equals("verify")){
            String before=settings.get("qa.restart.proof"),after=state(student);
            check(Objects.equals(Json.parseStored(before),Json.parseStored(after)),"Learning and assessment data changed after restart");
            check(UserStore.authenticate(student.getEmail(),Database.setting("SMARTSTU_SEED_PASSWORD",""),Role.STUDENT)!=null,"Password hash survives restart");
            var dao=new ConversationDao();String id=settings.get("qa.restart.conversation");
            check(dao.messages(student,id).size()==2,"Conversation survives restart");
            denied(()->dao.messages(teacher,id));
            System.out.println("PASS "+checks+" restart persistence checks");return;
        }
        check(UserStore.authenticate(student.getEmail(),Database.setting("SMARTSTU_SEED_PASSWORD",""),Role.STUDENT)!=null,"Database authentication");
        check(UserStore.authenticate(student.getEmail(),"wrong",Role.STUDENT)==null,"Wrong password");
        String key="qa."+UUID.randomUUID();
        settings.put(key,"Tiếng Việt ' OR 1=1; -- <script>alert(1)</script>");
        check(settings.get(key).startsWith("Tiếng Việt"),"INSERT and SELECT UTF-8 / parameter binding");
        settings.put(key,"Cập nhật");check(settings.get(key).equals("Cập nhật"),"UPDATE");
        settings.remove(key);check(settings.get(key)==null,"DELETE");
        Database.begin();settings.put(key,"rollback");Database.end(false);check(settings.get(key)==null,"Transaction rollback");
        var dao=new ConversationDao();String id=dao.create(student);String token=UUID.randomUUID().toString();String message="Giải thích định luật II Newton và đơn vị gia tốc.";
        denied(()->dao.messages(teacher,id));denied(()->dao.claim(teacher,id,token,message));
        check(dao.claim(student,id,token,message),"First claim");check(!dao.claim(student,id,token,message),"Repeated POST not duplicated");
        dao.finish(student,id,token,"Chưa có kết nối AI",false);
        check(dao.messages(student,id).size()==2,"Exactly one request pair");
        check(dao.claim(student,id,token,message),"Retry failed request");dao.finish(student,id,token,"Chưa có kết nối AI",false);
        check(dao.messages(student,id).size()==2,"Retry is idempotent");
        denied(()->dao.claim(student,id,token,"Nội dung khác"));
        new TutorService().send(student,id,token,message);
        check(dao.messages(student,id).get(1).status().equals("failed"),"Unconfigured AI reported honestly");
        denied(()->dao.hide(teacher,id));
        String hidden=dao.create(student);dao.hide(student,hidden);denied(()->dao.messages(student,hidden));
        settings.put("qa.restart.conversation",id);settings.put("qa.restart.proof",state(student));
        System.out.println("PASS "+checks+" MySQL CRUD, transaction, ownership and retry checks; restart snapshot saved");
    }
}
