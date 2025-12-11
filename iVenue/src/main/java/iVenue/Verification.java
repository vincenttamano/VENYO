package iVenue;

import java.util.Scanner;
import org.bson.Document;

public class Verification extends User {

    public Verification(String username, String password, int userID) {
        super(username, password, userID);
    }

    public void login() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Username: ");
        String u = sc.nextLine().trim();
        System.out.print("Password: ");
        String p = sc.nextLine().trim();
        User found = UserStore.findByCredentials(u, p);
        if (found == null) {
            System.out.println("Login failed: invalid username or password.");
            return;
        }
        System.out.println("Login successful. Welcome, " + found.getUsername());
        // determine type from users collection so we no longer rely on AdminUser runtime class
        Document doc = MongoDb.getDatabase().getCollection("users").find(new Document("userId", found.getUserId())).first();
        String type = doc != null ? doc.getString("userType") : null;
        if ("admin".equalsIgnoreCase(type)) {
            Admin.launchInteractive();
        } else if (found instanceof Customer) {
            ((Customer) found).userMenu();
        } else {
            System.out.println("Logged in as a basic user. No menu available.");
        }
    }
}
