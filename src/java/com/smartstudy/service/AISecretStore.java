package com.smartstudy.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;

/** Secrets are kept beside the external server configuration, never in public assets or SQL. */
public final class AISecretStore {
    private final Path file;

    public AISecretStore(Path file) {
        this.file = file;
    }

    public synchronized String get(String provider, String fallback) {
        return load().getProperty(provider, fallback);
    }

    private Properties load() {
        Properties values = new Properties();
        if (file != null && Files.isRegularFile(file)) {
            try (var input = Files.newInputStream(file)) {
                values.load(input);
            } catch (IOException e) {
                throw new IllegalArgumentException("Không đọc được kho khóa AI riêng trên server.");
            }
        }
        return values;
    }

    public synchronized void save(String provider, String key) {
        if (!java.util.Set.of("gemini", "gateway").contains(provider))
            throw new IllegalArgumentException("Nhà cung cấp không hợp lệ.");
        if (key == null || key.isBlank()) return;
        if (key.length() < 20 || key.length() > 512 || !key.matches("[A-Za-z0-9._~+/=-]+"))
            throw new IllegalArgumentException(
                    "API key không hợp lệ. Dán nguyên key, không kèm khoảng trắng hoặc dấu ngoặc.");
        if (file == null)
            throw new IllegalArgumentException(
                    "Server cần SMARTSTU_CONFIG_FILE trỏ tới cấu hình riêng để lưu API key.");
        Properties values = load();
        values.setProperty(provider, key);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(file.getParent(), "ai-secret-", ".local.properties");
            try (var output = Files.newOutputStream(temporary)) {
                values.store(output, "Private AI credentials - do not share");
            }
            Files.move(
                    temporary,
                    file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Không lưu được API key. Kiểm tra quyền ghi cấu hình riêng của server.");
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                }
            }
        }
    }
}
