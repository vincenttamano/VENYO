package Venyo;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Load initial data
        Venue.loadVenue();
        Amenity.loadAmenities();
        Booking.loadBookings();

        int choice;
        do {
            System.out.println("\n===== VENYO BOOKING SYSTEM =====");
            System.out.println("1. Create Booking");
            System.out.println("2. View Venues");
            System.out.println("3. View Amenities");
            System.out.println("4. View All Bookings");
            System.out.println("5. Cancel Booking");
            System.out.println("6. Complete Booking");
            System.out.println("7. Reload Data from MongoDB");
            System.out.println("8. Exit");
            System.out.print("Enter choice: ");
            choice = sc.nextInt();

            switch (choice) {
                case 1:
                    Booking.createBooking(sc);
                    break;

                case 2:
                    Venue.displayVenues();
                    break;

                case 3:
                    Amenity.displayAmenities();
                    break;

                case 4:
                    Booking.viewAllBookings();
                    break;

                case 5:
                    System.out.print("Enter Booking ID to cancel: ");
                    int cancelId = sc.nextInt();
                    Booking.cancelBooking(cancelId);
                    break;

                case 6:
                    System.out.print("Enter Booking ID to complete: ");
                    int completeId = sc.nextInt();
                    Booking.completeBooking(completeId);
                    break;

                case 7:
                    Booking.saveAllToMongo();
                    break;

                case 8:
                    System.out.println(" Exiting... Goodbye!");
                    break;

                default:
                    System.out.println(" Invalid option, please try again!");
            }

        } while (choice != 8);

        sc.close();
    }


}
