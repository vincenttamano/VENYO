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
    private PaymentStatus paymentStatus;
    private BookingStatus bookingStatus;
    private String purpose;
    private LinkedList<Amenity> amenities;
    private String username;

    private static LinkedList<Booking> bookings = new LinkedList<>();

    public Booking(int bookingId, Venue venue, Date date,
            PaymentStatus paymentStatus, BookingStatus bookingStatus,
            String purpose, String username) {
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

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(BookingStatus bookingStatus) {
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // ===============================================
    // CREATE BOOKING (NO VENUE AVAILABILITY CHANGE)
    // ===============================================
    public static void createBooking(Customer customer) {
        Scanner sc = new Scanner(System.in);

        Venue.displayAvailableVenues();
        System.out.print("Choose venue ID: ");
        int venuedId = Integer.parseInt(sc.nextLine().trim());
        Venue chosen = Venue.getVenue(venuedId, true); // <-- fixed
        if (chosen == null) {
            System.out.println("Invalid venue.");
            return;
        }

        Amenity.displayAmenities();
        LinkedList<Document> selectedAmenities = new LinkedList<>();

        while (true) {
            System.out.print("Enter Amenity ID to add (0 to stop): ");
            int aid = Integer.parseInt(sc.nextLine().trim());
            if (aid == 0)
                break;

            Amenity am = Amenity.getAmenity(aid);
            if (am != null) {

                System.out.print("Enter quantity for " + am.getName() +
                        " (max " + am.getQuantity() + "): ");
                int qty = Integer.parseInt(sc.nextLine().trim());

                selectedAmenities.add(
                        new Document("amenityId", aid)
                                .append("quantity", qty)
                                .append("price", am.getPrice() * qty));
                System.out.println("Added " + qty + " x " + am.getName());
            } else {
                System.out.println("Amenity not found.");
            }
        }

        System.out.print("Purpose: ");
        String purpose = sc.nextLine().trim();

        // PAYMENT OPTION (2 choices)
        // =============================
        System.out.println("\nPAYMENT OPTION:");
        System.out.println("[1] Pay");
        System.out.println("[2] Do Not Pay");
        System.out.print("Choose: ");
        int choice = Integer.parseInt(sc.nextLine().trim());

        PaymentStatus paymentStatus;
        BookingStatus bookingStatus;
        boolean venueAvailability;

        if (choice == 1) {
            paymentStatus = PaymentStatus.UNPAID; // still pending until full/downpayment
            bookingStatus = BookingStatus.BOOKED; // reserved after clicking pay
            venueAvailability = false; // lock venue
        } else {
            paymentStatus = PaymentStatus.UNPAID;
            bookingStatus = BookingStatus.PENDING;
            venueAvailability = true; // venue still available
        }

        // Set venue availability based on user decision
        MongoDb.getDatabase().getCollection("venues")
                .updateOne(
                        new Document("venueId", venuedId),
                        new Document("$set", new Document("availability", venueAvailability)));

        // =============================
        // SAVE BOOKING
        // =============================
        MongoCollection<Document> collection = MongoDb.getDatabase().getCollection("bookings");

        int maxId = 0;
        Document last = collection.find().sort(new Document("bookingId", -1)).first();
        if (last != null)
            maxId = last.getInteger("bookingId");

        int userId = customer.getUserId();
        Document userDoc = MongoDb.getDatabase().getCollection("users")
                .find(new Document("userId", userId)).first();

        Document bookedBy = new Document("userId", userId)
                .append("username", customer.getUsername())
                .append("firstName", userDoc != null ? userDoc.getString("firstName") : customer.getFirstName())
                .append("lastName", userDoc != null ? userDoc.getString("lastName") : customer.getLastName())
                .append("contactNumber",
                        userDoc != null ? userDoc.getString("contactNumber") : customer.getContactNumber())
                .append("email", userDoc != null ? userDoc.getString("email") : customer.getEmail());

        collection.insertOne(new Document("bookingId", maxId + 1)
                .append("venueId", chosen.getVenueId())
                .append("venueName", chosen.getName())
                .append("userId", userId)
                .append("bookedBy", bookedBy)
                .append("date", new Date().toString())
                .append("paymentStatus", paymentStatus.name())
                .append("bookingStatus", bookingStatus.name())
                .append("purpose", purpose)
                .append("amenities", selectedAmenities)
                .append("price", chosen.getPrice())
                .append("isFree", chosen.isFree()));

        System.out.println("\nBooking saved!");
        System.out.println("Status: " + bookingStatus);
        System.out.println("Payment: " + paymentStatus);
        System.out.println("Venue availability is now: " + venueAvailability);
    }

    // CANCEL BOOKING
    public static void cancelBooking(Customer customer) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Booking ID to cancel: ");
        int id = Integer.parseInt(sc.nextLine().trim());

        MongoCollection<Document> collection = MongoDb.getDatabase().getCollection("bookings");

        Document doc = collection.find(
                new Document("bookingId", id).append("userId", customer.getUserId())).first();

        if (doc == null) {
            System.out.println("Booking not found or not your booking.");
            return;
        }

        collection.updateOne(
                new Document("bookingId", id),
                new Document("$set",
                        new Document("bookingStatus", BookingStatus.CANCELLED.name())
                                .append("paymentStatus", PaymentStatus.CANCELLED.name())));

        int venueId = doc.getInteger("venueId");

        // Restore availability only if you want — leaving unchanged
        MongoDb.getDatabase().getCollection("venues")
                .updateOne(new Document("venueId", venueId),
                        new Document("$set", new Document("availability", true)));

        System.out.println("Booking cancelled and payment status set to Cancelled.");
    }

    // VIEW A SPECIFIC BOOKING (overload) - prints details for a single booking id
    public static void viewBookingDetails(Customer customer, int bookingId) {
        MongoCollection<Document> collection = MongoDb.getDatabase().getCollection("bookings");
        Document doc = collection.find(new Document("bookingId", bookingId).append("userId", customer.getUserId()))
                .first();
        if (doc == null) {
            System.out.println("Booking not found or not your booking.");
            return;
        }

        System.out.println("Booking ID: " + doc.getInteger("bookingId"));
        System.out.println("Venue: " + doc.getString("venueName"));
        System.out.println("Purpose: " + doc.getString("purpose"));
        System.out.println("Status: " + doc.getString("bookingStatus"));
        System.out.println("Payment Status: " + doc.getString("paymentStatus"));
        System.out.println("Price: " + doc.getDouble("price"));

        if (doc.containsKey("amenities")) {
            System.out.println("Amenities selected:");
            for (Object obj : doc.getList("amenities", Object.class)) {
                if (obj instanceof Document aDoc) {
                    int aid = aDoc.getInteger("amenityId");
                    int qty = aDoc.getInteger("quantity");
                    double price = aDoc.getDouble("price");

                    Amenity a = Amenity.getAmenity(aid);
                    String name = a != null ? a.getName() : "Unknown Amenity";

                    System.out.println(" - " + name + " x" + qty + " (₱" + price + ")");
                }
            }
        } else {
            System.out.println("Amenities: None");
        }
        System.out.println("---------------------------");
    }

    // Show segregated booking lists for this customer (paid / unpaid)
    public static void viewBookings(Customer customer) {
        java.util.Scanner sc = new java.util.Scanner(System.in);
        System.out.println("--- My Bookings (segregated) ---");

        java.util.List<Booking> paid = BookingHistory.listFinishedByUsername(customer.getUsername());
        java.util.List<Booking> unpaid = BookingHistory.listUnpaidFromBookings(customer.getUserId());

        System.out.println("Paid / Finished Bookings:");
        if (paid.isEmpty()) {
            System.out.println(" - None");
        } else {
            for (Booking b : paid) {
                System.out.println(" - ID: " + b.getBookingId() + " | Purpose: " + b.getPurpose() + " | Status: "
                        + b.getBookingStatus());
            }
        }

        System.out.println("Unpaid / Pending Bookings:");
        if (unpaid.isEmpty()) {
            System.out.println(" - None");
        } else {
            for (Booking b : unpaid) {
                System.out.println(" - ID: " + b.getBookingId() + " | Purpose: " + b.getPurpose() + " | Status: "
                        + b.getBookingStatus());
            }
        }

        System.out.print("Enter booking ID to view details (0 to go back): ");
        String in = sc.nextLine().trim();
        try {
            int id = Integer.parseInt(in);
            if (id > 0) {
                Booking.viewBookingDetails(customer, id);
            }
        } catch (NumberFormatException e) {
            // ignore and return
        }
    }

    // CHECK STATUS overload - programmatic check
    public static void checkStatus(Customer customer, int bookingId) {
        MongoCollection<Document> collection = MongoDb.getDatabase().getCollection("bookings");

        Document doc = collection.find(
                new Document("bookingId", bookingId).append("userId", customer.getUserId())).first();

        if (doc == null) {
            System.out.println("Booking not found or not your booking.");
            return;
        }

        System.out
                .println("Status: " + doc.getString("bookingStatus") + " | Payment: " + doc.getString("paymentStatus"));
    }

    // Show reservations (unpaid and down payments) and allow status check
    public static void viewReservations(Customer customer) {
        java.util.Scanner sc = new java.util.Scanner(System.in);
        System.out.println("--- My Reservations (Unpaid / Downpayments) ---");

        java.util.List<Booking> unpaid = BookingHistory.listUnpaidFromBookings(customer.getUserId());
        java.util.List<Booking> down = BookingHistory.listDownPaymentsFromBookings(customer.getUserId());

        System.out.println("Unpaid Bookings:");
        if (unpaid.isEmpty())
            System.out.println(" - None");
        else
            for (Booking b : unpaid)
                System.out.println(" - ID: " + b.getBookingId() + " | Purpose: " + b.getPurpose());

        System.out.println("Bookings with Down Payments / Partial Records:");
        if (down.isEmpty())
            System.out.println(" - None");
        else
            for (Booking b : down)
                System.out.println(" - ID: " + b.getBookingId() + " | Purpose: " + b.getPurpose());

        System.out.print("Enter booking ID to check status (0 to go back): ");
        String in = sc.nextLine().trim();
        try {
            int id = Integer.parseInt(in);
            if (id > 0) {
                Booking.checkStatus(customer, id);
            }
        } catch (NumberFormatException e) {
            // ignore and return
        }
    }

    // PAY BOOKING
    public static void payBooking(Customer customer) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Booking ID to pay: ");
        int id = Integer.parseInt(sc.nextLine().trim());

        MongoCollection<Document> collection = MongoDb.getDatabase().getCollection("bookings");

        Document doc = collection.find(
                new Document("bookingId", id).append("userId", customer.getUserId()))
                .first();

        if (doc == null) {
            System.out.println("Booking not found or not your booking.");
            return;
        }

        String paymentStatus = doc.getString("paymentStatus");

        if (paymentStatus.equalsIgnoreCase(PaymentStatus.PAID.name())) {
            System.out.println("This booking is already fully PAID.");
            return;
        }

        if (paymentStatus.equalsIgnoreCase(PaymentStatus.DOWNPAID.name())) {
            System.out.println("This booking already has a DOWNPAYMENT.");
            System.out.println("You will be allowed ONLY to pay the remaining balance soon.");
            return;
        }

        // Compute total
        double total = 0;
        Integer venueId = doc.getInteger("venueId");
        Venue venue = null;
        if (venueId != null) {
            venue = Venue.getVenue(venueId, false); // fetch any venue, booked or available
            if (venue != null)
                total += venue.getPrice();
        }

        if (doc.containsKey("amenities")) {
            var amenitiesList = doc.getList("amenities", Document.class);
            System.out.println("Amenities selected:");
            for (Document aDoc : amenitiesList) {
                int quantity = aDoc.getInteger("quantity", 0);
                double price = aDoc.getDouble("price");
                total += price;
                System.out.println(" - " + aDoc.getInteger("amenityId") + " x " + quantity + " (₱" + price + ")");
            }
        }

        System.out.println("\nVenue Price: ₱" + (venue != null ? venue.getPrice() : 0));
        System.out.println("Total Bill: ₱" + total);

        // Ask if user wants to pay
        System.out.println("\nDo you want to proceed with payment?");
        System.out.println("[1] Pay");
        System.out.println("[2] Do Not Pay");
        System.out.print("Choose option: ");
        int choice = Integer.parseInt(sc.nextLine());

        if (choice == 2) {
            System.out.println("Payment cancelled. Booking remains UNPAID.");
            return;
        }

        // Set booking to BOOKED + payment pending
        collection.updateOne(
                new Document("bookingId", id),
                new Document("$set",
                        new Document("bookingStatus", BookingStatus.BOOKED.name())
                                .append("paymentStatus", PaymentStatus.UNPAID.name())));

        System.out.println("\nBooking status updated to BOOKED.");
        System.out.println("Payment status set to PENDING.");

        // Payment method
        System.out.println("\nSelect payment method:");
        System.out.println("[1] Fully Pay (₱" + total + ")");
        System.out.println("[2] Downpayment 50% (₱" + (total * 0.5) + ")");
        System.out.print("Choose option: ");
        int payOption = Integer.parseInt(sc.nextLine());

        double amountPaid;
        PaymentStatus finalStatus;

        if (payOption == 1) {
            amountPaid = total;
            finalStatus = PaymentStatus.PAID;
        } else {
            amountPaid = total * 0.5;
            finalStatus = PaymentStatus.DOWNPAID;
        }

        // Simulate actual payment
        System.out.print("Enter any input to simulate payment: ");
        sc.nextLine();

        // Update final payment
        collection.updateOne(
                new Document("bookingId", id),
                new Document("$set",
                        new Document("paymentStatus", finalStatus.name())
                                .append("amountPaid", amountPaid)
                                .append("total", total)));

        // Add to booking history
        Booking finishedSnapshot = new Booking(
                id, null, null, finalStatus,
                BookingStatus.BOOKED,
                doc.getString("purpose"),
                customer.getUsername());
        BookingHistory.addFinished(finishedSnapshot);

        System.out.println("\nPayment successful!");
        System.out.println("You paid: ₱" + amountPaid);
        System.out.println("Remaining balance: ₱" + (total - amountPaid));
        System.out.println("Payment Status: " + finalStatus);
        System.out.println("Booking confirmed & added to history.");
    }

}
