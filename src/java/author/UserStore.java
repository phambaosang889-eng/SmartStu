package author;

public final class UserStore {
    public record Account(
            User user,
            boolean locked,
            java.time.Instant created,
            java.time.Instant lastLogin,
            int version) {}

    private record Secret(byte[] salt, byte[] hash) {}

    private static final java.util.Map<String, Account> ACCOUNTS =
            com.smartstudy.persistence.Stores.map("users", Account.class);
    private static final java.util.Map<String, Secret> SECRETS =
            com.smartstudy.persistence.Stores.map("user_credentials", Secret.class);

    static {
        if (com.smartstudy.persistence.Stores.seed() && ACCOUNTS.isEmpty()) {
            seed("student@smartstudy.vn", "Minh", Role.STUDENT);
            seed("teacher@smartstudy.vn", "Cô Hương", Role.TEACHER);
            seed("admin@smartstudy.vn", "Quản trị viên", Role.ADMIN);
        }
    }

    private UserStore() {}

    private static void seed(String email, String name, Role role) {
        String initialPassword =
                com.smartstudy.persistence.Database.setting(
                        "SMARTSTU_SEED_PASSWORD",
                        com.smartstudy.persistence.Database.memory() ? "123456" : "");
        if (!com.smartstudy.persistence.Database.memory()
                && (initialPassword.length() < 12 || initialPassword.length() > 200))
            throw new IllegalStateException("Cần mật khẩu khởi tạo riêng từ 12–200 ký tự.");
        ACCOUNTS.put(
                email,
                new Account(new User(email, name, role), false, java.time.Instant.now(), null, 1));
        SECRETS.put(email, secret(initialPassword));
    }

    private static byte[] hash(String password, byte[] salt) {
        try {
            var spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 120000, 256);
            try {
                return javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                        .generateSecret(spec)
                        .getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Không thể xử lý mật khẩu.", e);
        }
    }

    private static Secret secret(String password) {
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        return new Secret(salt, hash(password, salt));
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static void admin(User actor) {
        if (actor == null || actor.getRole() != Role.ADMIN)
            throw new IllegalArgumentException("Chỉ quản trị viên được quản lý tài khoản.");
    }

    public static synchronized User registerStudent(
            String name, String email, String password, String confirmation) {
        email = normalize(email);
        if (name == null || name.isBlank() || name.length() > 100)
            throw new IllegalArgumentException("Họ tên cần từ 1–100 ký tự.");
        if (email.length() > 150
                || !email.matches(
                        "[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?\\.[A-Za-z]{2,}"))
            throw new IllegalArgumentException("Nhập địa chỉ email hợp lệ, tối đa 150 ký tự.");
        if (password == null
                || password.isBlank()
                || password.length() < 8
                || password.length() > 200)
            throw new IllegalArgumentException(
                    "Mật khẩu cần từ 8–200 ký tự và không chỉ gồm khoảng trắng.");
        if (!java.util.Objects.equals(password, confirmation))
            throw new IllegalArgumentException("Mật khẩu xác nhận chưa khớp.");
        if (ACCOUNTS.containsKey(email))
            throw new IllegalArgumentException(
                    "Email đã được đăng ký. Hãy đăng nhập hoặc sử dụng email khác.");
        Secret credential = secret(password);
        User user = new User(email, name.trim(), Role.STUDENT);
        ACCOUNTS.put(email, new Account(user, false, java.time.Instant.now(), null, 1));
        SECRETS.put(email, credential);
        com.smartstudy.service.AuditService.record(
                email, "Đăng ký tài khoản", "Học sinh tự đăng ký");
        return user;
    }

    public static synchronized User authenticate(String email, String password, Role selectedRole) {
        Account a = ACCOUNTS.get(normalize(email));
        if (a == null
                || a.locked()
                || selectedRole != a.user().getRole()
                || password == null
                || password.length() > 200) return null;
        Secret s = SECRETS.get(a.user().getEmail());
        if (!java.security.MessageDigest.isEqual(s.hash(), hash(password, s.salt()))) return null;
        ACCOUNTS.put(
                a.user().getEmail(),
                new Account(a.user(), false, a.created(), java.time.Instant.now(), a.version()));
        com.smartstudy.service.AuditService.record(
                a.user().getEmail(), "Đăng nhập", selectedRole.name());
        return a.user();
    }

    public static synchronized User active(String email) {
        Account a = ACCOUNTS.get(normalize(email));
        return a == null || a.locked() ? null : a.user();
    }

    public static synchronized java.util.List<Account> list() {
        return java.util.List.copyOf(ACCOUNTS.values());
    }

    public static synchronized Account account(String email) {
        return ACCOUNTS.get(normalize(email));
    }

    public static synchronized void save(
            User actor,
            String email,
            String name,
            Role role,
            boolean locked,
            int version,
            String password) {
        admin(actor);
        email = normalize(email);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                || email.length() > 150
                || name == null
                || name.isBlank()
                || name.length() > 100
                || role == null)
            throw new IllegalArgumentException("Email, tên hoặc vai trò không hợp lệ.");
        Account old = ACCOUNTS.get(email);
        if (old != null && old.version() != version)
            throw new IllegalArgumentException("Tài khoản đã thay đổi ở tab khác. Tải lại trang.");
        if (old != null
                && old.user().getRole() == Role.ADMIN
                && (locked || role != Role.ADMIN)
                && ACCOUNTS.values().stream()
                                .filter(a -> a.user().getRole() == Role.ADMIN && !a.locked())
                                .count()
                        <= 1)
            throw new IllegalArgumentException("Cần giữ ít nhất một quản trị viên đang hoạt động.");
        if (email.equals(actor.getEmail()) && (locked || role != Role.ADMIN))
            throw new IllegalArgumentException(
                    "Bạn không thể khóa hoặc hạ quyền tài khoản đang sử dụng.");
        if (old == null) {
            if (password == null || password.length() < 8 || password.length() > 200)
                throw new IllegalArgumentException("Tài khoản mới cần mật khẩu từ 8–200 ký tự.");
        }
        ACCOUNTS.put(
                email,
                new Account(
                        new User(email, name.trim(), role),
                        locked,
                        old == null ? java.time.Instant.now() : old.created(),
                        old == null ? null : old.lastLogin(),
                        old == null ? 1 : old.version() + 1));
        if (old == null) SECRETS.put(email, secret(password));
        com.smartstudy.service.AuditService.record(
                actor.getEmail(),
                old == null ? "Tạo tài khoản" : "Cập nhật tài khoản",
                email + " · " + role + (locked ? " · Khóa" : " · Hoạt động"));
    }

    public static synchronized User profile(User actor, String name) {
        if (name == null || name.isBlank() || name.length() > 100)
            throw new IllegalArgumentException("Tên cần từ 1–100 ký tự.");
        Account a = ACCOUNTS.get(actor.getEmail());
        User u = new User(a.user().getEmail(), name.trim(), a.user().getRole());
        ACCOUNTS.put(
                u.getEmail(),
                new Account(u, a.locked(), a.created(), a.lastLogin(), a.version() + 1));
        return u;
    }

    public static synchronized void password(User actor, String current, String next) {
        Secret s = SECRETS.get(actor.getEmail());
        if (current == null
                || current.length() > 200
                || !java.security.MessageDigest.isEqual(s.hash(), hash(current, s.salt())))
            throw new IllegalArgumentException("Mật khẩu hiện tại chưa đúng.");
        if (next == null || next.length() < 8 || next.length() > 200)
            throw new IllegalArgumentException("Mật khẩu mới cần từ 8–200 ký tự.");
        SECRETS.put(actor.getEmail(), secret(next));
        com.smartstudy.service.AuditService.record(
                actor.getEmail(), "Đổi mật khẩu", "Tự đổi bằng mật khẩu hiện tại");
    }
}
