package iVenue;

import com.mongodb.client.MongoDatabase;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        int choice;

        System.out.println("Connecting to MongoDB...");
        MongoDatabase db = MongoDb.getDatabase();
        System.out.println("MongoDB connected successfully.\n");
        // Ensure there is a single admin account present
        UserStore.ensureAdminExists();

        do {
            System.out.println("\n===== VENYO BOOKING SYSTEM =====");
            System.out.println("1. Login");
            System.out.println("2. Register customer");
            System.out.println("3. Exit");
            System.out.print("Enter choice: ");
            choice = Integer.parseInt(input.nextLine());

            switch (choice) {
                case 1:
                    new User("","",0).login();
                    break;
                case 2:
                    registerCustomer(input);
                    break;
                case 3:
                    System.out.println("Exiting system.");
                    break;
                default:
                    System.out.println("Invalid option, please try again!");
            }
        } while (choice != 3);
    }


    // registration helper that collects customer details and stores in MongoDB
    private static void registerCustomer(Scanner input) {
        System.out.println("--- Customer Registration ---");

        String username = nameInput(input, "Username: ");
        String password = passwordInput(input);
        String firstName = nameInput(input, "First Name: ");
        String lastName = nameInput(input, "Last Name: ");
        String contact = contactInput(input);
        String email = emailInput(input);

        Customer c = UserStore.registerCustomer(username, password, firstName, lastName, contact, email);

        if (c != null) {
            System.out.println("Registration successful for: " + c.getUsername());
        } else {
            System.out.println("Registration failed. Please try again.");
        }
    }
    
    //catch statement for Username, First name and Last name
    private static String nameInput(Scanner input, String message) {
        String value;
        do {
            System.out.print(message);
            value = input.nextLine().trim();
            if (value.isEmpty()) {
                System.out.println("This field cannot be empty. Please try again.");
            }
        } while (value.isEmpty());
        return value;
    }

    //catch statement for password
    private static String passwordInput(Scanner input) {
        String pass;
        do {
            System.out.print("Password (minimum of 6 characters): ");
            pass = input.nextLine().trim();

            if (pass.length() < 6) {
                System.out.println("Password must be at least 6 characters long.");
            }
        } while (pass.length() < 6);

        return pass;
    }
    
    //catch statement for contact number
    private static String contactInput(Scanner input) {
        String contact;
        do {
            System.out.print("Contact number: ");
            contact = input.nextLine().trim();

            if (!contact.matches("\\d+")) {
                System.out.println("Contact number must contain digits only.");
                continue;
            }
            if (contact.length() < 7) {
                System.out.println("Contact number must be at least 7 digits.");
                continue;
            }
            if (contact.isEmpty()) {
                System.out.println("This field cannot be empty. Please try again.");
            }
            break;

        } while (true);

        return contact;
    }
    
    //catch statement for email
    private static String emailInput(Scanner input) {
        String email;
        do {
            System.out.print("Email: ");
            email = input.nextLine().trim();

            if (!email.contains("@") || !email.contains(".")) {
                System.out.println("Invalid email format. Please enter a valid email.");
                continue;
            }
            if (email.isEmpty()) {
                System.out.println("This field cannot be empty. Please try again.");
            }
            break;

        } while (true);

        return email;
    }

}
