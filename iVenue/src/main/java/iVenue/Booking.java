package iVenue;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Date;
import java.util.LinkedList;
import java.util.Scanner;

public class Booking {
    private int bookingId;
    private Venue venue;
    private Date date;
    private String paymentStatus;
    private String bookingStatus;
    private String purpose;
    private LinkedList<Amenity> amenities;
    private String username;

    private static LinkedList<Booking> bookings = new LinkedList<>();

    public Booking(int bookingId, Venue venue, Date date,
                   String paymentStatus, String bookingStatus, String purpose) {
        this.bookingId = bookingId;
        this.venue = venue;
        this.date = date;
        this.paymentStatus = paymentStatus;
        this.bookingStatus = bookingStatus;
        this.purpose = purpose;
        this.amenities = new LinkedList<>();
        this.username = null;
    }

    public Booking(int bookingId, Venue venue, Date date,
                   String paymentStatus, String bookingStatus, String purpose, String username) {
        this.bookingId = bookingId;
        this.venue = venue;
        this.date = date;
        this.paymentStatus = paymentStatus;
        this.bookingStatus = bookingStatus;
        this.purpose = purpose;
        this.amenities = new LinkedList<>();
        this.username = username;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public Venue getVenue() {
        return venue;
    }

    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public LinkedList<Amenity> getAmenities() {
        return amenities;
    }

    public void setAmenities(LinkedList<Amenity> amenities) {
        this.amenities = amenities;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public static LinkedList<Booking> getBookings() {
        return bookings;
    }

    public static void setBookings(LinkedList<Booking> bookings) {
        Booking.bookings = bookings;
    }

    // Display all bookings
    public static void displayBookings() {
        MongoDatabase database = MongoDb.getDatabase();
        MongoCollection<Document> collection = database.getCollection("bookings");

        if (collection.countDocuments() == 0) {
            System.out.println("No bookings found!");
            return;
        }

        for (Document doc : collection.find()) {
            System.out.println("Booking ID: " + doc.getInteger("bookingId"));
            System.out.println("Venue: " + doc.getString("venueName"));
            double price = doc.getDouble("price");
            String priceLabel = (price == 0) ? "FREE" : "₱" + price;
            System.out.println("Price: " + priceLabel);

            // show who booked (if available)
            if (doc.containsKey("bookedBy")) {
                Document by = (Document) doc.get("bookedBy");
                System.out.println("Booked By: " + by.getString("username") + " (UserID: " + by.getInteger("userId") + ")");
                if (by.containsKey("firstName") || by.containsKey("lastName")) {
                    System.out.println("  Name: " + (by.getString("firstName") == null ? "" : by.getString("firstName")) + (by.getString("lastName") == null ? "" : " " + by.getString("lastName")));
                }
                if (by.containsKey("contactNumber")) System.out.println("  Contact: " + by.getString("contactNumber"));
                if (by.containsKey("email")) System.out.println("  Email: " + by.getString("email"));
            }

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
            System.out.println("Payment Status: " + doc.getString("paymentStatus"));
            System.out.println("Booking Status: " + doc.getString("bookingStatus"));
            System.out.println("Purpose: " + doc.getString("purpose"));
            System.out.println("---------------------------");
        }
    }

    // Customer-facing booking operations moved from Customer class.
    public static void createBooking(Customer customer) {
        Scanner sc = new Scanner(System.in);
        Venue.displayAvailableVenues();
        System.out.print("Choose venue ID: ");
        int vid = Integer.parseInt(sc.nextLine().trim());
        Venue chosen = Venue.getVenue(vid);
        if (chosen == null) { System.out.println("Invalid venue."); return; }

        Amenity.displayAmenities();
        java.util.List<Integer> amenityIds = new java.util.ArrayList<>();
        while (true) {
            System.out.print("Enter Amenity ID to add (0 to stop): ");
            int aid = Integer.parseInt(sc.nextLine().trim());
            if (aid == 0) break;
            Amenity am = Amenity.getAmenity(aid);
            if (am != null) {
                amenityIds.add(aid);
                System.out.println("Added: " + am.getName());
            } else {
                System.out.println("Amenity not found.");
            }
        }

        System.out.print("Purpose: ");
        String purpose = sc.nextLine().trim();

        MongoCollection<Document> collection = MongoDb.getDatabase().getCollection("bookings");
        int maxId = 0;
        Document last = collection.find().sort(new Document("bookingId", -1)).first();
        if (last != null) maxId = last.getInteger("bookingId");

        int userId = customer.getUserId();
        Document userDoc = MongoDb.getDatabase().getCollection("users").find(new Document("userId", userId)).first();

        Document bookedBy = new Document("userId", userId)
            .append("username", customer.getUsername())
            .append("firstName", userDoc != null ? userDoc.getString("firstName") : customer.getFirstName())
            .append("lastName", userDoc != null ? userDoc.getString("lastName") : customer.getLastName())
            .append("contactNumber", userDoc != null ? userDoc.getString("contactNumber") : customer.getContactNumber())
            .append("email", userDoc != null ? userDoc.getString("email") : customer.getEmail());

        Document doc = new Document("bookingId", maxId + 1)
            .append("venueId", chosen.getVenueId())
            .append("venueName", chosen.getName())
            .append("userId", userId)
            .append("bookedBy", bookedBy)
            .append("date", new java.util.Date().toString())
            .append("paymentStatus", "Pending")
            .append("bookingStatus", "Pending")
            .append("purpose", purpose)
            .append("amenities", amenityIds)
            .append("price", chosen.getPrice())
            .append("isFree", chosen.isFree());

        collection.insertOne(doc);

        MongoDb.getDatabase().getCollection("venues").updateOne(new Document("venueId", chosen.getVenueId()), new Document("$set", new Document("availability", false)));

        System.out.println("Booking created with ID: " + (maxId + 1));
    }

    public static void cancelBooking(Customer customer) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Booking ID to cancel: ");
        int id = Integer.parseInt(sc.nextLine().trim());
        MongoCollection<Document> collection = MongoDb.getDatabase().getCollection("bookings");
        Document doc = collection.find(new Document("bookingId", id).append("userId", customer.getUserId())).first();
        if (doc == null) { System.out.println("Booking not found or not your booking."); return; }

        collection.updateOne(new Document("bookingId", id), new Document("$set", new Document("bookingStatus", "cancelled")));
        int venueId = doc.getInteger("venueId");
        MongoDb.getDatabase().getCollection("venues").updateOne(new Document("venueId", venueId), new Document("$set", new Document("availability", true)));
        String username = "N/A";
        if (doc.containsKey("bookedBy")) {
            Document bookedBy = (Document) doc.get("bookedBy");
            username = bookedBy.getString("username") == null ? "N/A" : bookedBy.getString("username");
        }
        Booking deletedSnapshot = new Booking(id, null, null, doc.getString("paymentStatus"), "cancelled", doc.getString("purpose"), username);
        BookingHistory.addDeleted(deletedSnapshot);
        System.out.println("Booking cancelled and recorded in deleted history.");
    }

    public static void viewBookingDetails(Customer customer) {
        MongoCollection<Document> collection = MongoDb.getDatabase().getCollection("bookings");
        boolean found = false;
        for (Document doc : collection.find(new Document("userId", customer.getUserId()))) {
            found = true;
            System.out.println("Booking ID: " + doc.getInteger("bookingId"));
            System.out.println("Venue: " + doc.getString("venueName"));
            System.out.println("Purpose: " + doc.getString("purpose"));
            System.out.println("Status: " + doc.getString("bookingStatus"));
            System.out.println("Price: " + doc.getDouble("price"));
            System.out.println("Amenities: " + doc.get("amenities"));
            System.out.println("---------------------------");
        }
        if (!found) System.out.println("You have no bookings.");
    }

    public static void checkStatus(Customer customer) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Booking ID to check status: ");
        int id = Integer.parseInt(sc.nextLine().trim());
        MongoCollection<Document> collection = MongoDb.getDatabase().getCollection("bookings");
        Document doc = collection.find(new Document("bookingId", id).append("userId", customer.getUserId())).first();
        if (doc == null) { System.out.println("Booking not found or not your booking."); return; }
        System.out.println("Status: " + doc.getString("bookingStatus"));
    }

    public static void payBooking(Customer customer) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Booking ID to pay: ");
        int id = Integer.parseInt(sc.nextLine().trim());
        MongoCollection<Document> collection = MongoDb.getDatabase().getCollection("bookings");
        Document doc = collection.find(new Document("bookingId", id).append("userId", customer.getUserId())).first();
        if (doc == null) { System.out.println("Booking not found or not your booking."); return; }
        if ("booked".equalsIgnoreCase(doc.getString("bookingStatus"))) { System.out.println("Already booked/paid."); return; }

        double total = 0;
        Integer venueId = doc.getInteger("venueId");
        if (venueId != null) {
            Venue v = Venue.getVenue(venueId);
            if (v != null) total += v.getPrice();
        }
        if (doc.containsKey("amenities")) {
            for (Object aidObj : doc.getList("amenities", Object.class)) {
                try {
                    int aid = Integer.parseInt(aidObj.toString());
                    Amenity a = Amenity.getAmenity(aid);
                    if (a != null) total += a.getQuantity();
                } catch (Exception e) { }
            }
        }

        System.out.println("Total amount to pay: " + total);
        System.out.print("Enter any input to simulate payment: ");
        sc.nextLine();

        collection.updateOne(new Document("bookingId", id), new Document("$set", new Document("paymentStatus", "Paid").append("bookingStatus", "Booked").append("total", total)));
        
        // Capture customer username for history
        String username = customer.getUsername();
        Booking finishedSnapshot = new Booking(id, null, null, "Paid", "Booked", doc.getString("purpose"), username);
        BookingHistory.addFinished(finishedSnapshot);
        System.out.println("Payment accepted. Booking confirmed and recorded in finished history.");
    }

}
