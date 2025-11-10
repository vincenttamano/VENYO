package Venyo_ver2;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        int choice;
        do {
            System.out.println("\n===== VENYO BOOKING SYSTEM =====");
            System.out.println("1. Create Booking");
            System.out.println("2. View Venues");
            System.out.println("3. View Amenities");
            System.out.println("4. View All Bookings");
            System.out.print("Enter choice: ");
            choice = input.nextInt();

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

                default:
                    System.out.println(" Invalid option, please try again!");
            }

        } while (choice != 5);

    }
}