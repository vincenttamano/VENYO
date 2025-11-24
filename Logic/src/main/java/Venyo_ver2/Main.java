package Venyo_ver2;

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

    public static void userMenu(Scanner input) {
        int choice;
        do {
            System.out.println("\n===== USER MENU =====");
            System.out.println("1. Create Booking");
            System.out.println("2. View Venues");
            System.out.println("3. View Amenities");
            System.out.println("4. View All Bookings");
            System.out.println("5. Back");
            System.out.print("Enter choice: ");
            choice = Integer.parseInt(input.nextLine());

            switch (choice) {
                case 1:
                    Booking.createBooking(input);
                    break;
                case 2:
                    Venue.displayAllVenues();
                    break;
                case 3:
                    Amenity.displayAmenities();
                    break;
                case 4:
                    Booking.displayBookings();
                    break;
                case 5:
                    System.out.println("Returning to main menu.");
                    break;
                default:
                    System.out.println("Invalid option, please try again!");
            }
        } while (choice != 5);
    }

    // Admin menu entry point when lopgged in as admin
    public static void adminMenu(Scanner input) {
        Admin admin = new Admin(input);
        admin.adminMenu();
    }

    // registration helper that collects customer details and stores in MongoDB
    private static void registerCustomer(Scanner input) {
        System.out.println("--- Customer Registration ---");
        System.out.print("Username: ");
        String username = input.nextLine().trim();
        System.out.print("Password: ");
        String password = input.nextLine().trim();
        System.out.print("First name: ");
        String first = input.nextLine().trim();
        System.out.print("Last name: ");
        String last = input.nextLine().trim();
        System.out.print("Contact number: ");
        String contact = input.nextLine().trim();
        System.out.print("Email: ");
        String email = input.nextLine().trim();

        // create and store
        Customer c = UserStore.registerCustomer(username, password, first, last, contact, email);
        if (c != null) System.out.println("Registration successful for: " + c.getUsername());
        else System.out.println("Registration failed. You may try again from the login menu.");
    }
}
