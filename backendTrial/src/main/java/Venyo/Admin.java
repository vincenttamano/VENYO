package Venyo;

public class Admin extends User{
    private int adminId;
    private String adminName;
    private String adminPassword;

    public Admin(int userId, String firstName, String lastName, String email, String password, String role, int adminId, String adminName, String adminPassword) {
        super(userId, firstName, lastName, email, password, role);
        this.adminId = adminId;
        this.adminName = adminName;
        this.adminPassword = adminPassword;
    }

    public void addVenue() {}

    public void deleteVenue() {}
}
