import author.*;
import com.smartstudy.persistence.*;
import java.nio.file.*;
import java.util.*;
/** Verify HTTP-created accounts in a separate JVM, including after webapp reload. */
public final class RegistrationPersistenceTest {
    public static void main(String[] args)throws Exception{
        List<String> emails=Files.readAllLines(Path.of(".runtime/registered-qa-email.txt")).stream().filter(s->!s.isBlank()).toList();
        if(emails.isEmpty())throw new AssertionError("No HTTP-created account to verify");
        String email=emails.get(emails.size()-1);var account=UserStore.account(email);
        if(account==null||account.user().getRole()!=Role.STUDENT)throw new AssertionError("Persisted signup must be Student");
        if(UserStore.authenticate(email,"Local-Registration-QA-2026!",Role.STUDENT)==null)throw new AssertionError("Persisted password hash does not verify");
        Database.use(c->{try(var s=c.prepareStatement("SELECT payload FROM user_credentials WHERE row_key=?")){
            s.setString(1,email);try(var r=s.executeQuery()){
                if(!r.next()||r.getString(1).contains("Local-Registration-QA-2026!"))throw new AssertionError("Password not stored as hash");
                var fields=(Map<?,?>)com.smartstudy.service.Json.parseStored(r.getString(1));
                if(Base64.getDecoder().decode((String)fields.get("salt")).length!=16||Base64.getDecoder().decode((String)fields.get("hash")).length!=32)throw new AssertionError("Unexpected hash format");
            }return null;
        }});
        System.out.println("PASS: registered Student account and PBKDF2 credentials persist across JVM/webapp reload.");
    }
}
