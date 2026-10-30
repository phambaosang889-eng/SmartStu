import author.Role;
import author.User;
import author.UserStore;

public class AuthRoleTest {
    public static void main(String[] args) {
        String password=com.smartstudy.persistence.Database.setting("SMARTSTU_SEED_PASSWORD","123456");
        assert UserStore.authenticate("student@smartstudy.vn", password, Role.STUDENT) != null;
        assert UserStore.authenticate("teacher@smartstudy.vn", password, Role.TEACHER) != null;
        assert UserStore.authenticate("admin@smartstudy.vn", password, Role.ADMIN) != null;
        assert UserStore.authenticate("student@smartstudy.vn", password, Role.ADMIN) == null;
        assert UserStore.authenticate("student@smartstudy.vn", "wrong", Role.STUDENT) == null;
        System.out.println("PASS: 5/5 authentication role tests");
    }
}
