package iVenue;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Date;
import java.util.LinkedList;
import java.util.Scanner;

public class BookingAdmin implements AdminManagement<Booking> {

    private final MongoCollection<Document> collection;

    public BookingAdmin() {
        MongoDatabase database = MongoDb.getDatabase();
        this.collection = database.getCollection("bookings");
    }


    public void create(Scanner input) {
        System.out.println("----CREATE NEW BOOKING----");

        Venue.displayAvailableVenues();
        System.out.print("Enter Venue ID: ");
        int venueId = Integer.parseInt(input.nextLine());
        Venue selectedVenue = Venue.getVenue(venueId);
        if (selectedVenue == null) {
            System.out.println("Invalid Venue ID. Booking cancelled.");
            return;
        }

        // Mark venue as unavailable
        selectedVenue.setAvailability(false);
        MongoDb.getDatabase().getCollection("venues")
                .updateOne(new Document("venueId", venueId),
                        new Document("$set", new Document("availability", false)));

        // Select amenities
        Amenity.displayAmenities();
        LinkedList<Amenity> selectedAmenities = new LinkedList<>();
        while (true) {
            System.out.print("Enter Amenity ID (0 to stop): ");
            int amenityId = Integer.parseInt(input.nextLine());
            if (amenityId == 0) break;
            Amenity amenity = Amenity.getAmenity(amenityId);
            if (amenity != null) {
                selectedAmenities.add(amenity);
                System.out.println("Added: " + amenity.getName());
            } else {
                System.out.println("Amenity not found.");
            }
        }

        System.out.print("Enter Purpose: ");
        String purpose = input.nextLine();

        // Generate Booking ID
        int maxId = 0;
        Document lastBooking = collection.find().sort(new Document("bookingId", -1)).first();
        if (lastBooking != null) maxId = lastBooking.getInteger("bookingId");

        // Create booking
        Date bookingDate = new Date();
        String paymentStatus = "Pending";
        String bookingStatus = "Booked";

        Booking newBooking = new Booking(maxId + 1, selectedVenue, bookingDate, paymentStatus, bookingStatus, purpose);
        newBooking.getAmenities().addAll(selectedAmenities);

        LinkedList<String> amenityNames = new LinkedList<>();
        for (Amenity a : selectedAmenities) amenityNames.add(a.getName());

        // Include price and FREE/PAID status
        Document doc = new Document("bookingId", newBooking.getBookingId())
                .append("venueName", selectedVenue.getName())
                .append("venueId", selectedVenue.getVenueId())
                .append("date", bookingDate.toString())
                .append("paymentStatus", paymentStatus)
                .append("bookingStatus", bookingStatus)
                .append("purpose", purpose)
                .append("amenities", amenityNames)
                .append("price", selectedVenue.getPrice())
                .append("isFree", selectedVenue.isFree()); // <-- uses public isFree() method

        collection.insertOne(doc);

        System.out.println("Booking successfully created. Booking ID: " + newBooking.getBookingId());
        System.out.println("Venue Price: " + selectedVenue.getPriceLabel());
    }


    // --- UPDATE BOOKING (user input only) ---
    @Override
    public void update(Scanner input) {
        System.out.println("----UPDATE BOOKING STATUS----");
        System.out.print("Enter Booking ID: ");
        int id = Integer.parseInt(input.nextLine());

        Document doc = collection.find(new Document("bookingId", id)).first();
        if (doc == null) {
            System.out.println("Booking not found.");
            return;
        }

        System.out.print("Enter new Booking Status: ");
        String newStatus = input.nextLine();

        collection.updateOne(
                new Document("bookingId", id),
                new Document("$set", new Document("bookingStatus", newStatus))
        );

        System.out.println("Booking updated successfully.");
    }

    @Override
    public void delete(Scanner input) {
        System.out.println("----DELETE BOOKING-----");
        System.out.print("Enter Booking ID: ");
        int id = Integer.parseInt(input.nextLine());

        Document doc = collection.find(new Document("bookingId", id)).first();
        if (doc == null) {
            System.out.println("Booking not found.");
            return;
        }

        String venueName = doc.getString("venueName");
        MongoDb.getDatabase().getCollection("venues")
                .updateOne(new Document("name", venueName),
                        new Document("$set", new Document("availability", true)));

        collection.deleteOne(new Document("bookingId", id));
        System.out.println("Booking deleted successfully. Venue is now available.");
    }

    @Override
        public void displayAll() {
            System.out.println("----ALL BOOKINGS----");
            for (Document doc : collection.find()) {
                System.out.println("Booking ID: " + doc.getInteger("bookingId"));
                System.out.println("Venue: " + doc.getString("venueName"));
                // show who booked
                if (doc.containsKey("bookedBy")) {
                    Document by = (Document) doc.get("bookedBy");
                    System.out.println("Booked By: " + by.getString("username") + " (UserID: " + by.getInteger("userId") + ")");
                    if (by.containsKey("firstName") || by.containsKey("lastName")) {
                        System.out.println("  Name: " + (by.getString("firstName") == null ? "" : by.getString("firstName")) + (by.getString("lastName") == null ? "" : " " + by.getString("lastName")));
                    }
                    if (by.containsKey("contactNumber")) System.out.println("  Contact: " + by.getString("contactNumber"));
                    if (by.containsKey("email")) System.out.println("  Email: " + by.getString("email"));
                }
                System.out.println("Purpose: " + doc.getString("purpose"));
                System.out.println("Status: " + doc.getString("bookingStatus"));
                System.out.println("Price: ₱" + doc.getDouble("price"));
                System.out.println("Free?: " + ((doc.getBoolean("isFree")) ? "YES" : "NO"));
                System.out.print("Amenities: ");
                if (doc.containsKey("amenities")) {
                    for (Object a : doc.getList("amenities", Object.class)) System.out.print(a + " ");
                } else {
                    System.out.print("None");
                }
                System.out.println("\n-----------------------------");
            }
        }
}
