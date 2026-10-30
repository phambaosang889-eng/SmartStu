import author.*;
import com.smartstudy.service.*;
import com.smartstudy.persistence.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Exports only the clean built-in fixture, never data from the user's database. */
public final class SeedSqlExporter {
    private static final StringBuilder SQL=new StringBuilder("-- Optional local fixture. No plaintext passwords; INSERT IGNORE never overwrites existing rows.\n-- Account hashes correspond to SMARTSTU_SEED_PASSWORD used when exporting.\n-- On another machine, prefer SeedDatabase with your own external password.\nUSE smartstu;\nSTART TRANSACTION;\n");
    private static void table(String name,Class<?> owner,Object instance,String field)throws Exception{
        var f=owner.getDeclaredField(field);f.setAccessible(true);Map<?,?> records=(Map<?,?>)f.get(instance);
        for(var entry:records.entrySet()){
            String hex=HexFormat.of().formatHex(RecordCodec.encode(entry.getValue()).getBytes(StandardCharsets.UTF_8));
            SQL.append("INSERT IGNORE INTO ").append(name).append("(row_key,payload) VALUES('").append(entry.getKey().toString().replace("'","''")).append("',CONVERT(0x").append(hex).append(" USING utf8mb4));\n");
        }
    }
    public static void main(String[] args)throws Exception{
        if(!Database.memory()||Database.setting("SMARTSTU_SEED_PASSWORD","").length()<12)throw new IllegalArgumentException("Export requires explicit memory fixture and a private seed password of at least 12 characters");
        UserStore.list();var catalog=CatalogService.getInstance();var courses=CourseService.getInstance();var quizzes=QuizService.getInstance();var essays=EssayWorkflow.getInstance();
        table("users",UserStore.class,null,"ACCOUNTS");table("user_credentials",UserStore.class,null,"SECRETS");
        table("catalog_entries",CatalogService.class,catalog,"entries");table("chapters",CourseService.class,courses,"chapters");
        table("content_placements",CourseService.class,courses,"placements");table("enrollments",CourseService.class,courses,"enrollments");
        table("quizzes",QuizService.class,quizzes,"quizzes");table("essay_assignments",EssayWorkflow.class,essays,"assignments");
        SQL.append("COMMIT;\n");Files.writeString(Path.of(args.length==0?"database/seed-local.sql":args[0]),SQL,StandardCharsets.UTF_8);
        System.out.println("Seed SQL exported with password hashes only; existing application data was not read.");
    }
}
