package Venyo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;

import java.util.Date;
import java.util.LinkedList;
import java.util.Scanner;

public class Booking {
    private int bookingID;
    private Venue venue;
    private Amenity amenity;
    private Date date;
    private String timeSlot;
    private String paymentStatus;
    private String bookingStatus;
    private String purpose;
    public static LinkedList<Booking> bookingList = new LinkedList<>();

    public Booking(int bookingID, Venue venue, Amenity amenity, Date date, String timeSlot,
                   String paymentStatus, String bookingStatus, String purpose) {
        this.bookingID = bookingID;
        this.venue = venue;
        this.amenity = amenity;
        this.date = date;
        this.timeSlot = timeSlot;
        this.paymentStatus = paymentStatus;
        this.bookingStatus = bookingStatus;
        this.purpose = purpose;
    }

    // ✅ View all bookings from MongoDB
    public static void viewAllBookings() {
        MongoDatabase db = DatabaseConnection.getDatabase("booking");
        MongoCollection<Document> collection = db.getCollection("bookings");

        System.out.println("\n===== ALL BOOKINGS =====");

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            if (!cursor.hasNext()) {
                System.out.println("No bookings found in the database.");
                return;
            }

            while (cursor.hasNext()) {
                Document doc = cursor.next();
                int bookingId = doc.getInteger("bookingID", 0);
                String venueName = doc.getString("venue");
                String amenityName = doc.getString("amenity");
                String date = doc.getString("date");
                String timeSlot = doc.getString("timeSlot");
                String paymentStatus = doc.getString("paymentStatus");
                String bookingStatus = doc.getString("bookingStatus");
                String purpose = doc.getString("purpose");

                System.out.println("-----------------------------------");
                System.out.println("Booking ID   : " + bookingId);
                System.out.println("Venue        : " + venueName);
                System.out.println("Amenity      : " + amenityName);
                System.out.println("Purpose      : " + purpose);
                System.out.println("Date         : " + date);
                System.out.println("Time Slot    : " + timeSlot);
                System.out.println("Status       : " + bookingStatus);
                System.out.println("Payment      : " + paymentStatus);
            }
            System.out.println("-----------------------------------");
        }
    }

    // ✅ Create a new booking
    public static void createBooking(Scanner sc) {
        Venue.displayVenues();
        System.out.print("Choose a venue: ");
        int venueIndex = sc.nextInt() - 1;
        sc.nextLine(); // consume newline

        Venue selectedVenue = Venue.venueList.get(venueIndex);
        if (!selectedVenue.isAvailable()) {
            System.out.println("⚠️ Venue is already booked!");
            return;
        }

        System.out.print("Enter purpose: ");
        String purpose = sc.nextLine();

        Amenity.displayAmenities();
        System.out.print("Choose an amenity: ");
        int amenityIndex = sc.nextInt() - 1;
        sc.nextLine();

        Amenity selectedAmenity = Amenity.amenityList.get(amenityIndex);

        System.out.println("\n--- Available Time Slots ---");
        System.out.println("1. 9:00 AM - 12:00 PM");
        System.out.println("2. 1:00 PM - 4:00 PM");
        System.out.println("3. 5:00 PM - 8:00 PM");
        System.out.print("Select a time slot: ");
        int timeChoice = sc.nextInt();
        sc.nextLine();

        String timeSlot;
        switch (timeChoice) {
            case 1 -> timeSlot = "9:00 AM - 12:00 PM";
            case 2 -> timeSlot = "1:00 PM - 4:00 PM";
            case 3 -> timeSlot = "5:00 PM - 8:00 PM";
            default -> timeSlot = "9:00 AM - 12:00 PM";
        }

        int newBookingID = getNextBookingID();

        Booking booking = new Booking(
                newBookingID,
                selectedVenue,
                selectedAmenity,
                new Date(),
                timeSlot,
                "Pending",
                "Reserved",
                purpose
        );

        bookingList.add(booking);

        // ✅ Mark the selected venue unavailable (both locally & in MongoDB)
        selectedVenue.setAvailability(false);
        System.out.println("🏠 Venue \"" + selectedVenue.getName() + "\" marked as unavailable.");

        // Save booking to MongoDB
        saveToMongo(booking);
        System.out.println("✅ Booking created successfully with ID: " + newBookingID);
    }

    // ✅ Save booking to MongoDB
    public static void saveToMongo(Booking b) {
        MongoDatabase db = DatabaseConnection.getDatabase("booking");
        MongoCollection<Document> collection = db.getCollection("bookings");

        Document doc = new Document("bookingID", b.bookingID)
                .append("venue", b.venue.getName())
                .append("amenity", b.amenity.getName())
                .append("date", b.date.toString())
                .append("timeSlot", b.timeSlot)
                .append("paymentStatus", b.paymentStatus)
                .append("bookingStatus", b.bookingStatus)
                .append("purpose", b.purpose);

        try {
            collection.insertOne(doc);
            System.out.println("💾 Booking " + b.bookingID + " saved to MongoDB!");
        } catch (Exception e) {
            System.out.println("⚠️ Booking ID " + b.bookingID + " already exists, skipping...");
        }
    }

    // ✅ Save all bookings
    public static void saveAllToMongo() {
        if (bookingList.isEmpty()) {
            System.out.println("⚠️ No bookings to save.");
            return;
        }

        for (Booking b : bookingList) {
            saveToMongo(b);
        }
        System.out.println("✅ All bookings saved/updated successfully.");
    }

    // ✅ Auto-increment booking ID
    private static int getNextBookingID() {
        MongoDatabase db = DatabaseConnection.getDatabase("booking");
        MongoCollection<Document> counters = db.getCollection("bookings_counters");

        Document filter = new Document("_id", "bookingID");
        Document update = new Document("$inc", new Document("seq", 1));
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                .upsert(true)
                .returnDocument(ReturnDocument.AFTER);

        Document result = counters.findOneAndUpdate(filter, update, options);
        return result.getInteger("seq", 1);
    }

    // ✅ Load all bookings
    public static void loadBookings() {
        bookingList.clear();
        MongoDatabase db = DatabaseConnection.getDatabase("booking");
        MongoCollection<Document> collection = db.getCollection("bookings");

        System.out.println("\n===== ALL BOOKINGS =====");

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();

                int id = doc.getInteger("bookingID", 0);
                String venueName = doc.getString("venue");
                String amenityName = doc.getString("amenity");
                String dateStr = doc.getString("date");
                String paymentStatus = doc.getString("paymentStatus");
                String bookingStatus = doc.getString("bookingStatus");
                String purpose = doc.getString("purpose");

                Venue venue = Venue.findByName(venueName);
                Amenity amenity = Amenity.findByName(amenityName);

                System.out.println("-----------------------------------------");
                System.out.println("Booking ID : " + id);
                System.out.println("Venue      : " + venueName);
                System.out.println("Amenity    : " + (amenity != null ? amenity.getName() : "None"));
                System.out.println("Purpose    : " + purpose);
                System.out.println("Date       : " + dateStr);
                System.out.println("Status     : " + bookingStatus);
                System.out.println("Payment    : " + paymentStatus);
            }
            System.out.println("-----------------------------------------");
        }
    }

    // ✅ Cancel booking
    public static void cancelBooking(int bookingID) {
        for (Booking b : bookingList) {
            if (b.bookingID == bookingID) {
                b.bookingStatus = "Cancelled";
                b.venue.setAvailability(true); // make venue available again

                MongoDatabase db = DatabaseConnection.getDatabase("booking");
                MongoCollection<Document> collection = db.getCollection("bookings");
                collection.updateOne(
                        new Document("bookingID", bookingID),
                        new Document("$set", new Document("bookingStatus", "Cancelled"))
                );

                System.out.println("❌ Booking " + bookingID + " has been cancelled. Venue is now available.");
                return;
            }
        }
        System.out.println("⚠️ Booking ID not found.");
    }

    // ✅ Complete booking
    public static void completeBooking(int bookingID) {
        for (Booking b : bookingList) {
            if (b.bookingID == bookingID) {
                b.bookingStatus = "Completed";
                b.venue.setAvailability(true); // make venue available again

                MongoDatabase db = DatabaseConnection.getDatabase("booking");
                MongoCollection<Document> collection = db.getCollection("bookings");
                collection.updateOne(
                        new Document("bookingID", bookingID),
                        new Document("$set", new Document("bookingStatus", "Completed"))
                );

                System.out.println("✅ Booking " + bookingID + " has been marked as completed. Venue is now available.");
                return;
            }
        }
        System.out.println("⚠️ Booking ID not found.");
    }
}
