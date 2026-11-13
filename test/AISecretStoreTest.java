import com.smartstudy.service.AISecretStore;
import java.nio.file.*;

public class AISecretStoreTest {
    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("smartstu-secret-test-");
        Path file = folder.resolve("ai-secrets.local.properties");
        String fixture = "synthetic-test-key-not-a-real-key-12345";
        try {
            AISecretStore store = new AISecretStore(file);
            if (!store.get("gemini", "fallback").equals("fallback")) throw new AssertionError();
            store.save("gemini", fixture);
            if (!new AISecretStore(file).get("gemini", "").equals(fixture))
                throw new AssertionError();
            store.save("gemini", "");
            if (!store.get("gemini", "").equals(fixture)) throw new AssertionError();
            try {
                store.save("gemini", "invalid\nkey");
                throw new AssertionError();
            } catch (IllegalArgumentException expected) {
            }
            try {
                store.save("invalid-provider", fixture);
                throw new AssertionError();
            } catch (IllegalArgumentException expected) {
            }
            store.save("gateway", "synthetic-gateway-test-key-12345");
            if (!store.get("gemini", "").equals(fixture)) throw new AssertionError();
            System.out.println("PASS 6 private secret-store checks; synthetic keys only");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(folder);
        }
    }
}
