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

        do {
            System.out.println("\n===== VENYO BOOKING SYSTEM =====");
            System.out.println("1. User");
            System.out.println("2. Admin");
            System.out.println("3. Exit");
            System.out.print("Enter choice: ");
            choice = Integer.parseInt(input.nextLine());

            switch (choice) {
                case 1:
                    userMenu(input);
                    break;
                case 2:
                    adminMenu(input);  // call a method for admin
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


    public static void adminMenu(Scanner input) {
        Admin admin = new Admin(input);
        admin.adminMenu();
    }
}
