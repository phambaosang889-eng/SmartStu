package com.smartstudy.persistence;

/** Explicit local fixture initializer. Never executed by Tomcat startup. */
public final class SeedDatabase {
    public static void main(String[] args) throws Exception {
        if (Database.memory()) throw new IllegalArgumentException("Seed requires MySQL");
        String password = Database.setting("SMARTSTU_SEED_PASSWORD", "");
        if (password.length() < 12 || password.length() > 200)
            throw new IllegalArgumentException(
                    "Set SMARTSTU_SEED_PASSWORD to 12–200 characters in external configuration");
        System.setProperty("SMARTSTU_SEED", "true");
        Database.begin();
        try {
            author.UserStore.list();
            com.smartstudy.service.CatalogService.getInstance();
            com.smartstudy.service.CourseService.getInstance();
            com.smartstudy.service.QuizService.getInstance();
            com.smartstudy.service.EssayWorkflow.getInstance();
            Database.end(true);
        } catch (Throwable e) {
            Database.end(false);
            throw e;
        }
        System.out.println(
                "Initial accounts and learning content initialized; existing aggregates were"
                    + " preserved.");
    }
}
