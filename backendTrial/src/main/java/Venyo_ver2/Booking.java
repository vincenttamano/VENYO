package Venyo_ver2;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Date;
import java.util.LinkedList;
import java.util.Scanner;

public class Booking {
    private static int counter = 1;
    private int bookingId;
    private Venue venue;
    private Date date;
    private static timeSlot timeSlot;
    private String paymentStatus;
    private String bookingStatus;
    private String purpose;
    private LinkedList<Amenity> amenities;
    private static LinkedList<Booking> bookings = new LinkedList<>();

    public Booking(Venue venue, Date date, timeSlot timeSlot, String paymentStatus, String bookingStatus, String purpose) {
        this.bookingId = counter++; // auto-increment ID
        this.venue = venue;
        this.date = date;
        this.timeSlot = timeSlot;
        this.paymentStatus = paymentStatus;
        this.bookingStatus = bookingStatus;
        this.purpose = purpose;
        this.amenities = new LinkedList<>();
    }

    public int getBookingId() { return bookingId; }
    public Venue getVenue() { return venue; }
    public void setVenue(Venue venue) { this.venue = venue; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public timeSlot getTimeSlot() { return timeSlot; }
    public void setTimeSlot(timeSlot timeSlot) { this.timeSlot = timeSlot; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public LinkedList<Amenity> getAmenities() { return amenities; }

    public static void createBooking(Scanner input) {
        MongoDatabase database = MongoDb.getDatabase();
        MongoCollection<Document> collection = database.getCollection("bookings");

        System.out.println("Creating Booking");

        Venue.displayAvailableVenues();
        System.out.println("Select Venue ID:");
        int venueId = input.nextInt();
        Venue selectedVenue = Venue.getVenue(venueId);
        if (selectedVenue == null) {
            System.out.println("Invalid Venue ID. Booking cancelled.");
            return;
        }
        selectedVenue.setAvailability(false);

        collection.updateOne(
                new Document("venueId", selectedVenue.getVenueId()), // filter
                new Document("$set", new Document("availability", false)) // update
        );

        Amenity.displayAmenities();
        LinkedList<Amenity> selectedAmenities = new LinkedList<>();
        boolean choice = true;
        while (choice) {
            System.out.println("Select Amenity ID:");
            int amenityId = input.nextInt();
            Amenity amenity = Amenity.getAmenity(amenityId);
            if (amenity != null) {
                selectedAmenities.add(amenity);
                System.out.println("Added: " + amenity.getName());
            } else {
                System.out.println("Amenity not found. Skipping.");
            }

            System.out.println("Enter Y to add another amenity, N to finish:");
            char answer = input.next().toLowerCase().charAt(0);
            input.nextLine();
            if (answer == 'n') choice = false;
        }

        System.out.println("Purpose:");
        String purpose = input.nextLine();

        timeSlot slot = null;
        while (slot == null) {
            System.out.println("Select Time Slot:");
            System.out.println("1. AM");
            System.out.println("2. PM");
            int slotChoice = Integer.parseInt(input.nextLine());
            if (slotChoice == 1) slot = timeSlot.AM;
            else if (slotChoice == 2) slot = timeSlot.PM;
            else System.out.println("Invalid choice. Please enter 1 for AM or 2 for PM.");
        }

        Date bookingDate = new Date();
        String paymentStatus = "Pending";
        String bookingStatus = "Booked";

        Booking newBooking = new Booking(selectedVenue, bookingDate, slot, paymentStatus, bookingStatus, purpose);
        newBooking.getAmenities().addAll(selectedAmenities);

        LinkedList<String> amenityNames = new LinkedList<>();
        for (Amenity a : selectedAmenities) {
            amenityNames.add(a.getName());
        }

        Document bookingDoc = new Document("bookingId", newBooking.getBookingId())
                .append("venueName", selectedVenue.getName())
                .append("date", bookingDate.toString())
                .append("timeSlot", slot.toString())
                .append("paymentStatus", paymentStatus)
                .append("bookingStatus", bookingStatus)
                .append("purpose", purpose)
                .append("amenities", amenityNames);

        collection.insertOne(bookingDoc);

        bookings.add(newBooking);

        System.out.println("Booking created successfully and saved to database!");
        System.out.println("Booking ID: " + newBooking.getBookingId());
        System.out.println("Venue: " + selectedVenue.getName());
        System.out.print("Amenities: ");
        selectedAmenities.forEach(a -> System.out.print(a.getName() + " "));
        System.out.println("\nPurpose: " + purpose);
    }

    public static void displayBookings() {
        MongoDatabase database = MongoDb.getDatabase();
        MongoCollection<Document> collection = database.getCollection("bookings");

        if (collection.countDocuments() == 0) {
            System.out.println("No bookings found!");
        } else {
            for (Document doc : collection.find()) {
                System.out.println("Booking ID: " + doc.getInteger("bookingId"));
                System.out.println("Venue: " + doc.getString("venueName"));

                if (doc.containsKey("amenities")) {
                    System.out.print("Amenities: ");
                    for (Object a : doc.getList("amenities", Object.class)) {
                        System.out.print(a + " ");
                    }
                    System.out.println();
                } else {
                    System.out.println("Amenities: None");
                }

                System.out.println("Date: " + doc.getString("date"));
                System.out.println("Time Slot: " + doc.getString("timeSlot"));
                System.out.println("Payment Status: " + doc.getString("paymentStatus"));
                System.out.println("Booking Status: " + doc.getString("bookingStatus"));
                System.out.println("Purpose: " + doc.getString("purpose"));
                System.out.println("---------------------------");
            }
        }
    }
}
