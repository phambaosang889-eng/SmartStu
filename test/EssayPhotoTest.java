import author.*;
import com.smartstudy.service.*;
import com.smartstudy.persistence.Database;
import com.smartstudy.dao.EssayPhotoDao;
import com.smartstudy.model.EssayPhotoReading;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** JDBC changes are rolled back. The synthetic photo is saved only as a QA fixture. */
public final class EssayPhotoTest {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    interface Work{void run()throws Exception;}
    static void rejects(Work work,String label)throws Exception{checks++;try{work.run();throw new AssertionError(label);}catch(IllegalArgumentException expected){}}
    public static void main(String[] args)throws Exception{
        var image=new BufferedImage(1200,900,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
        g.setColor(Color.WHITE);g.fillRect(0,0,1200,900);g.setColor(new Color(20,35,65));g.setFont(new Font("Arial",Font.BOLD,36));g.drawString("BAI LAM VAT LY - KIEM THU",65,90);g.setFont(new Font("Arial",Font.PLAIN,36));
        g.drawString("m = 2 kg; F = 10 N",65,190);g.drawString("Dinh luat II Newton: F = m.a",65,280);g.drawString("Suy ra: a = F x m = 10 x 2 = 20 m/s^2",65,370);g.drawString("Gia toc cung huong hop luc.",65,460);g.dispose();
        var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);byte[] png=out.toByteArray();
        Path fixture=Path.of(".runtime/essay-photo-qa.png");Files.createDirectories(fixture.getParent());Files.write(fixture,png);
        var prepared=EssayPhotoService.prepare(new ByteArrayInputStream(png));check(prepared.bytes()[0]==(byte)0xff&&prepared.bytes()[1]==(byte)0xd8,"normalized JPEG");check(prepared.width()==1200&&prepared.height()==900,"dimensions");
        rejects(()->EssayPhotoService.prepare(new ByteArrayInputStream(new byte[0])),"empty image");
        rejects(()->EssayPhotoService.prepare(new ByteArrayInputStream("<svg onload='alert(1)'/>".getBytes())),"fake image");
        rejects(()->EssayPhotoService.prepare(new ByteArrayInputStream(new byte[EssayPhotoService.MAX_UPLOAD+1])),"oversize");
        var small=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(20,20,BufferedImage.TYPE_INT_RGB),"png",small);
        rejects(()->EssayPhotoService.prepare(new ByteArrayInputStream(small.toByteArray())),"too small");
        var gif=new ByteArrayOutputStream();ImageIO.write(image,"gif",gif);rejects(()->EssayPhotoService.prepare(new ByteArrayInputStream(gif.toByteArray())),"GIF rejected");
        image.flush();Database.begin();Database.requestScope();
        try{
            User student=UserStore.active("student@smartstudy.vn"),teacher=UserStore.active("teacher@smartstudy.vn"),admin=UserStore.active("admin@smartstudy.vn");
            String email="photo-unit-"+UUID.randomUUID()+"@example.test";UserStore.save(admin,email,"Học sinh kiểm thử",Role.STUDENT,false,0,"Photo-test-2026!");User outsider=UserStore.active(email);
            var photos=new EssayPhotoService();
            var assignment=EssayWorkflow.getInstance().save(teacher,null,"QA ảnh rollback","Một vật 2 kg chịu lực 10 N. Tính gia tốc.","Nêu công thức và đơn vị.","a = 5 m/s²","Công thức | 5\nTính toán | 5","",true,0);
            CourseService.getInstance().assignAssessment(teacher,assignment.id(),"course-mechanics");
            var photo=photos.upload(student,assignment.id(),new ByteArrayInputStream(png));check(photo.submission()==null,"draft photo");
            check(photos.upload(student,assignment.id(),new ByteArrayInputStream(png)).id().equals(photo.id()),"duplicate upload idempotent");
            check(photos.readable(student,photo.id())!=null,"student own draft");check(photos.readable(teacher,photo.id())==null,"teacher cannot read unsubmitted work");check(photos.readable(outsider,photo.id())==null,"other student denied");check(photos.readable(admin,photo.id())==null,"admin not granted private work");
            rejects(()->photos.upload(outsider,assignment.id(),new ByteArrayInputStream(png)),"unenrolled upload denied");
            new EssayPhotoDao().reading(photo.id(),new EssayPhotoReading("a = F x m = 20 m/s^2","",.98));
            check(photos.scan(student,photo.id()).transcription().contains("20"),"cached OCR retained, no network call");
            String token=UUID.randomUUID().toString();var submission=EssayWorkflow.getInstance().submit(student,assignment.id(),"",token);
            check(photos.submitted(teacher,submission.id()).id().equals(photo.id()),"teacher can read submitted photo");check(photos.readable(student,photo.id())!=null,"student retains photo");check(photos.readable(outsider,photo.id())==null,"cross-student submission denied");
            check(EssayWorkflow.getInstance().submit(student,assignment.id(),"",token).id().equals(submission.id()),"repeated submission idempotent");check(photos.draft(student,assignment.id())==null,"draft detached after submit");
            rejects(()->photos.remove(student,photo.id()),"submitted image immutable");rejects(()->photos.scan(student,photo.id()),"student cannot rescan submitted image");check(submission.visible()==null,"no automatic grade publication");
            EssayWorkflow.getInstance().review(teacher,submission.id(),java.util.List.of(2.0,0.0),"Phép biến đổi sai: a = F/m, không phải F × m.",false,0);
            check(EssayWorkflow.getInstance().submission(submission.id(),student).visible()==null,"draft grade not visible to student");
        }finally{Database.end(false);Database.closeScope();}
        System.out.println("PASS "+checks+" essay photo checks; JDBC fixture rolled back; synthetic image: "+fixture);
    }
}
