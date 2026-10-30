import author.*;
import com.smartstudy.service.*;
import com.smartstudy.persistence.*;
import java.util.*;
public class LessonVideoTest {
    static int checks;
    static void check(boolean ok){checks++;if(!ok)throw new AssertionError("video check "+checks);}
    static void rejected(Runnable work){try{work.run();throw new AssertionError("Expected rejection");}catch(IllegalArgumentException e){checks++;}}
    public static void main(String[] args)throws Exception {
        for(String url:List.of("https://youtu.be/gFJgWqZV-aQ","https://www.youtube.com/watch?v=gFJgWqZV-aQ&t=20","https://www.youtube-nocookie.com/embed/gFJgWqZV-aQ"))check(LessonVideoService.youtubeId(url).equals("gFJgWqZV-aQ"));
        for(String url:List.of("javascript:alert(1)","https://youtube.com.evil.test/watch?v=gFJgWqZV-aQ","https://user@youtube.com/watch?v=gFJgWqZV-aQ","http://youtube.com/watch?v=gFJgWqZV-aQ","https://youtube.com/watch?v=bad","https://youtube.com:444/watch?v=gFJgWqZV-aQ"))rejected(()->LessonVideoService.youtubeId(url));
        check(LessonVideoService.detect(new byte[]{0,0,0,24,'f','t','y','p','m','p','4','2'},"lesson.MP4").equals("video/mp4"));
        check(LessonVideoService.detect(new byte[]{0x1a,0x45,(byte)0xdf,(byte)0xa3},"lesson.webm").equals("video/webm"));
        rejected(()->LessonVideoService.detect("<script>evil()</script>".getBytes(),"fake.mp4"));
        rejected(()->LessonVideoService.detect(new byte[]{0,0,0,24,'f','t','y','p','m','p','4','2'},"lesson.jsp"));
        var service=new LessonVideoService();var owner=new User("teacher@smartstudy.vn","Teacher",Role.TEACHER);
        var outsider=new User("other@smartstu.test","Other",Role.TEACHER);var student=new User("student@smartstudy.vn","Student",Role.STUDENT);
        check(service.mayRead(student,"lesson-newton"));check(!service.mayRead(outsider,"lesson-newton"));
        check(!service.mayRead(new User("not-enrolled@smartstu.test","Other student",Role.STUDENT),"lesson-newton"));
        rejected(()->service.requireOwner(outsider,"lesson-newton"));rejected(()->service.requireOwner(student,"lesson-newton"));
        Database.begin();try{
            var video=service.link(owner,"lesson-newton","Video kiểm thử","https://youtu.be/abcdefghijk","Nguồn kiểm thử");
            check(service.list(student,"lesson-newton").stream().anyMatch(v->v.id().equals(video.id())));
            rejected(()->service.link(owner,"lesson-newton","Trùng","https://www.youtube.com/watch?v=abcdefghijk","Nguồn"));
            rejected(()->service.archive(outsider,video.id()));
            service.archive(owner,video.id());check(service.readable(student,video.id())==null);
            var lesson=CatalogService.getInstance().get("lesson-newton");
            CatalogService.getInstance().save(owner,"lesson",lesson.id(),lesson.title(),lesson.body(),lesson.topic(),"draft",lesson.version());
            check(!service.mayRead(student,"lesson-newton"));check(service.mayRead(owner,"lesson-newton"));
        }finally{Database.end(false);}
        System.out.println("PASS "+checks+" video validation, ownership and SQL rollback checks");
    }
}
