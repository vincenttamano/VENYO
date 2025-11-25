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

}
