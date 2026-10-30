/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package author;

public enum Role {
    STUDENT("/student/dashboard"),
    TEACHER("/teacher/dashboard"),
    ADMIN("/admin/dashboard");

    private final String homePath;

    Role(String homePath) {
        this.homePath = homePath;
    }

    public String getHomePath() {
        return homePath;
    }
}
