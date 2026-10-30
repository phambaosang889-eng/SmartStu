import author.*;
import com.smartstudy.service.CourseService;
/** Restricted local UI test identity; lock it after browser QA. */
public final class BrowserFixture {
    public static void main(String[] args){
        User admin=UserStore.active("admin@smartstudy.vn");String email="browser.qa@smartstu.test";
        UserStore.Account old=UserStore.account(email);boolean lock=args.length>0&&args[0].equals("lock");
        UserStore.save(admin,email,"Bạn học SmartStu",Role.STUDENT,lock,old==null?0:old.version(),"LocalQA-Physics10-2026!");
        if(!lock)CourseService.getInstance().enroll(UserStore.active(email),"course-mechanics",true);
        System.out.println(lock?"Local browser fixture locked":"Local browser fixture ready");
    }
}
