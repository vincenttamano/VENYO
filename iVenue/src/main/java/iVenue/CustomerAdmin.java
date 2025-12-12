package iVenue;

import org.bson.Document;

public class CustomerAdmin extends User implements AdminManagement<Customer> {

    public CustomerAdmin(String username, String password, int userID) {
        super(username, password, userID);
    }

    @Override
    public void update(java.util.Scanner input) {
        System.out.print("Enter Customer User ID to update: ");
        int id;
        try {
            id = Integer.parseInt(input.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid User ID.");
            return;
        }

        Document doc = MongoDb.getDatabase().getCollection("users").find(new Document("userId", id)).first();
        if (doc == null) {
            System.out.println("User not found.");
            return;
        }
        String type = doc.getString("userType");
        if (type == null || !type.equalsIgnoreCase("customer")) {
            System.out.println("Specified user is not a customer.");
            return;
        }

        Customer customer = new Customer(
                doc.getString("username"),
                doc.getString("password"),
                doc.getInteger("userId"),
                doc.getString("firstName"),
                doc.getString("lastName"),
                doc.getString("contactNumber"),
                doc.getString("email"),
                "customer");

        Customer.updateProfile(input, customer);
    }

    @Override
    public void create(java.util.Scanner input) {
        System.out.println("Create customer not implemented.");
    }

    @Override
    public void delete(java.util.Scanner sc) {
        if (sc == null) {
            System.out.println("Scanner is required to select a user to delete.");
            return;
        }
        System.out.print("Enter user ID to delete: ");
        String raw = sc.nextLine().trim();
        int id;
        try {
            id = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            System.out.println("Invalid user ID.");
            return;
        }

        if (id == this.getUserId()) {
            System.out.println("Cannot delete yourself.");
            return;
        }

        org.bson.Document doc = MongoDb.getDatabase().getCollection("users")
                .find(new org.bson.Document("userId", id)).first();
        if (doc == null) {
            System.out.println("User not found.");
            return;
        }
        String t = doc.getString("userType");
        if ("admin".equalsIgnoreCase(t)) {
            System.out.println("Cannot delete admin users.");
            return;
        }
        MongoDb.getDatabase().getCollection("users").deleteOne(new org.bson.Document("userId", id));
        System.out.println("User deleted (id=" + id + ").");
    }

    @Override
    public void displayAll() {
        System.out.println("----ALL USERS----");
        for (User u : UserStore.getAll()) {
            org.bson.Document doc = MongoDb.getDatabase().getCollection("users")
                    .find(new org.bson.Document("userId", u.getUserId())).first();
            String username = u.getUsername();
            String type = doc != null && doc.getString("userType") != null ? doc.getString("userType")
                    : (u instanceof Customer ? "customer" : "user");
            String first = doc != null ? doc.getString("firstName") : null;
            String last = doc != null ? doc.getString("lastName") : null;
            String contact = doc != null ? doc.getString("contactNumber") : null;
            String email = doc != null ? doc.getString("email") : null;
            System.out.println("UserID: " + u.getUserId());
            System.out.println("  username: " + username);
            System.out.println("  type: " + type);
            if (first != null || last != null)
                System.out.println("  name: " + (first == null ? "" : first) + (last == null ? "" : " " + last));
            if (contact != null)
                System.out.println("  contact: " + contact);
            if (email != null)
                System.out.println("  email: " + email);
            System.out.println("---------------------------");
        }
    }

}
