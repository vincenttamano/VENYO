package iVenue;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class Booking {

    private LinkedList<Amenity> amenities;
    private static LinkedList<Booking> bookings = new LinkedList<>(); // can be used within Booking class
    private PaymentStatus paymentStatus;
    private BookingStatus bookingStatus;
    private int bookingId;
    private Venue venue;
    private Date date;
    private String purpose;
    private String username;

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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        MongoCollection<Document> bookingCollection = MongoDb.getDatabase().getCollection("bookings");
        MongoCollection<Document> venueCollection = MongoDb.getDatabase().getCollection("venues");

        System.out.println("----CREATE NEW BOOKING----");
        Venue.displayAvailableVenues();
        System.out.print("Enter Venue ID: ");
        int venueId;
        try {
            venueId = Integer.parseInt(sc.nextLine().trim());
        } catch (Exception e) {
            System.out.println("Invalid venue id.");
            return;
        }

        Venue chosenVenue = Venue.getVenue(venueId, true);
        if (chosenVenue == null) {
            System.out.println("Invalid Venue ID or venue not available. Booking cancelled.");
            return;
        }

        // --- Build set of booked dates (next 30-day availability uses bookings that
        // are PAID or DOWNPAID) ---
        Set<String> bookedDates = new HashSet<>();
        for (Document b : bookingCollection.find(
                new Document("venueId", venueId)
                        .append("bookingStatus", BookingStatus.BOOKED.name())
                        .append("paymentStatus", new Document("$in",
                                Arrays.asList(PaymentStatus.PAID.name(), PaymentStatus.DOWNPAID.name()))))) {
            Date d = b.getDate("date");
            if (d != null)
                bookedDates.add(sdf.format(d));
        }

        // --- Generate available dates (next 30 days) ---
        Calendar cal = Calendar.getInstance();
        List<String> availableDates = new ArrayList<>();
        System.out.println("\nAvailable Dates for this Venue (next 30 days):");
        for (int i = 0; i < 30; i++) {
            String dateStr = sdf.format(cal.getTime());
            if (!bookedDates.contains(dateStr)) {
                System.out.println(" - " + dateStr);
                availableDates.add(dateStr);
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (availableDates.isEmpty()) {
            System.out.println("\n❌ This venue is fully booked for the next 30 days.");
            System.out.println("Booking cancelled.");
            return;
        }

        // --- Select Booking Date ---
        Date bookingDate = null;
        while (bookingDate == null) {
            System.out.print("Enter booking date (yyyy-MM-dd): ");
            String input = sc.nextLine().trim();
            if (!availableDates.contains(input)) {
                System.out.println("Invalid or already booked date. Please choose from the available dates above.");
                continue;
            }
            try {
                bookingDate = sdf.parse(input);
            } catch (ParseException e) {
                System.out.println("Invalid date format. Please use yyyy-MM-dd.");
            }
        }

        // --- Select Amenities ---
        Amenity.displayAmenities();
        LinkedList<Document> selectedAmenityDocs = new LinkedList<>();
        while (true) {
            System.out.print("Enter Amenity ID to add (0 to stop): ");
            int aid;
            try {
                aid = Integer.parseInt(sc.nextLine().trim());
            } catch (Exception e) {
                System.out.println("Invalid input.");
                continue;
            }
            if (aid == 0)
                break;

            Amenity am = Amenity.getAmenity(aid);
            if (am != null) {
                int qty;
                while (true) {
                    System.out.print("Enter quantity for " + am.getName() + " (max " + am.getQuantity() + "): ");
                    try {
                        qty = Integer.parseInt(sc.nextLine().trim());
                    } catch (Exception e) {
                        System.out.println("Invalid input.");
                        continue;
                    }
                    if (qty >= 1 && qty <= am.getQuantity())
                        break;
                    System.out.println("Invalid quantity. Must be between 1 and " + am.getQuantity());
                }
                selectedAmenityDocs.add(new Document("amenityId", aid)
                        .append("quantity", qty)
                        .append("price", am.getPrice() * qty));
                System.out.println("Added " + qty + " x " + am.getName());
            } else {
                System.out.println("Amenity not found.");
            }
        }

        // --- Enter Purpose ---
        System.out.print("Enter Purpose: ");
        String purpose = sc.nextLine().trim();

        // --- Payment Option ---
        System.out.println("\nPAYMENT OPTION:");
        System.out.println("[1] Pay Now");
        System.out.println("[2] Do Not Pay");
        System.out.print("Choose option: ");
        int choice;
        try {
            choice = Integer.parseInt(sc.nextLine().trim());
        } catch (Exception e) {
            choice = 2;
        }

        PaymentStatus paymentStatus = PaymentStatus.UNPAID;
        BookingStatus bookingStatus = (choice == 1) ? BookingStatus.BOOKED : BookingStatus.PENDING;

        // --- Generate Booking ID ---
        int maxId = 0;
        Document lastBooking = bookingCollection.find().sort(new Document("bookingId", -1)).first();
        if (lastBooking != null)
            maxId = lastBooking.getInteger("bookingId");

        int userId = customer.getUserId();
        Document userDoc = MongoDb.getDatabase().getCollection("users").find(new Document("userId", userId)).first();

        Document bookedBy = new Document("userId", userId)
                .append("username", customer.getUsername())
                .append("firstName", userDoc != null ? userDoc.getString("firstName") : customer.getFirstName())
                .append("lastName", userDoc != null ? userDoc.getString("lastName") : customer.getLastName())
                .append("contactNumber",
                        userDoc != null ? userDoc.getString("contactNumber") : customer.getContactNumber())
                .append("email", userDoc != null ? userDoc.getString("email") : customer.getEmail());

        Document bookingDoc = new Document("bookingId", maxId + 1)
                .append("venueId", venueId)
                .append("venueName", chosenVenue.getName())
                .append("userId", userId)
                .append("bookedBy", bookedBy)
                .append("date", bookingDate)
                .append("paymentStatus", paymentStatus.name())
                .append("bookingStatus", bookingStatus.name())
                .append("purpose", purpose)
                .append("amenities", selectedAmenityDocs)
                .append("price", chosenVenue.getPrice())
                .append("isFree", chosenVenue.isFree());

        bookingCollection.insertOne(bookingDoc);

        // --- AFTER INSERT: recompute venue availability ---
        Set<String> paidBookedDates = new HashSet<>();
        for (Document b : bookingCollection.find(
                new Document("venueId", venueId)
                        .append("bookingStatus", BookingStatus.BOOKED.name())
                        .append("paymentStatus", new Document("$in",
                                Arrays.asList(PaymentStatus.PAID.name(), PaymentStatus.DOWNPAID.name()))))) {
            Date d = b.getDate("date");
            if (d != null)
                paidBookedDates.add(sdf.format(d));
        }

        Calendar checkCal = Calendar.getInstance();
        int freeCount = 0;
        for (int i = 0; i < 30; i++) {
            String ds = sdf.format(checkCal.getTime());
            if (!paidBookedDates.contains(ds))
                freeCount++;
            checkCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        boolean fullyBooked = (freeCount == 0);
        venueCollection.updateOne(new Document("venueId", venueId),
                new Document("$set", new Document("availability", !fullyBooked)));

        if (fullyBooked) {
            System.out.println("\n⚠️ NOTICE: This venue is now fully booked for the next 30 days.");
        }

        System.out.println("\nBooking successfully created!");
        System.out.println("Booking ID: " + (maxId + 1));
        System.out.println("Date: " + sdf.format(bookingDate));
        System.out.println("Venue: " + chosenVenue.getName());
        System.out.println("Purpose: " + purpose);
        System.out.println("Booking Status: " + bookingStatus);
        System.out.println("Payment Status: " + paymentStatus);
    }

    // CANCEL BOOKING
    public static void cancelBooking(Customer customer) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Booking ID to cancel: ");
        int id = Integer.parseInt(sc.nextLine().trim());

        MongoCollection<Document> bookingCol = MongoDb.getDatabase().getCollection("bookings");
        MongoCollection<Document> venueCol = MongoDb.getDatabase().getCollection("venues");

        Document booking = bookingCol.find(
                new Document("bookingId", id).append("userId", customer.getUserId())).first();

        if (booking == null) {
            System.out.println("Booking not found or not your booking.");
            return;
        }

        // ----------------------------
        // 🚫 1. Prevent cancelling finished bookings
        // ----------------------------
        Date bookingDate = booking.getDate("date");
        Date today = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        if (today.after(bookingDate)) {
            System.out.println("❌ You cannot cancel a booking that has already finished ("
                    + sdf.format(bookingDate) + ").");
            return;
        }

        // ----------------------------
        // 2. CANCEL BOOKING
        // ----------------------------
        bookingCol.updateOne(
                new Document("bookingId", id),
                new Document("$set",
                        new Document("bookingStatus", BookingStatus.CANCELLED.name())
                                .append("paymentStatus", PaymentStatus.CANCELLED.name())));

        int venueId = booking.getInteger("venueId");

        // ----------------------------
        // 3. RECOMPUTE VENUE AVAILABILITY (Option B logic)
        // ----------------------------
        Set<String> booked = new HashSet<>();
        Calendar cal = Calendar.getInstance();

        for (Document b : bookingCol.find(new Document("venueId", venueId)
                .append("bookingStatus", new Document("$ne", BookingStatus.CANCELLED.name())))) {

            Date d = b.getDate("date");
            if (d != null)
                booked.add(sdf.format(d));
        }

        int freeCount = 0;

        Calendar check = Calendar.getInstance();
        for (int i = 0; i < 30; i++) {
            String ds = sdf.format(check.getTime());
            if (!booked.contains(ds))
                freeCount++;
            check.add(Calendar.DAY_OF_MONTH, 1);
        }

        boolean fullyBooked = (freeCount == 0);

        venueCol.updateOne(
                new Document("venueId", venueId),
                new Document("$set", new Document("availability", !fullyBooked)));

        // ----------------------------
        // 4. Confirmation
        // ----------------------------
        System.out.println("\nBooking cancelled successfully.");
        System.out.println("Venue availability recalculated:");
        System.out.println(" → Availability: " + (!fullyBooked));
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

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        System.out.println("Booking ID: " + doc.getInteger("bookingId"));
        System.out.println("Venue: " + doc.getString("venueName"));
        Date d = doc.getDate("date");
        System.out.println("Date: " + (d != null ? sdf.format(d) : "N/A"));
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
    MongoCollection<Document> venueCollection = MongoDb.getDatabase().getCollection("venues");

    // Find booking
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
        System.out.println("You will be allowed to pay the remaining balance soon.");
        return;
    }

    // Compute total
    double total = 0;
    Integer venueId = doc.getInteger("venueId");
    Venue venue = Venue.getVenue(venueId, false);

    if (venue != null)
        total += venue.getPrice();

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

    System.out.println("\nVenue Price: ₱" + venue.getPrice());
    System.out.println("Total Bill: ₱" + total);

    // Confirm payment
    System.out.println("\nDo you want to proceed with payment?");
    System.out.println("[1] Pay");
    System.out.println("[2] Do Not Pay");
    System.out.print("Choose option: ");
    int choice = Integer.parseInt(sc.nextLine());

    if (choice == 2) {
        System.out.println("Payment cancelled. Booking remains UNPAID.");
        return;
    }

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

    // Simulate payment
    System.out.print("Enter any input to simulate payment: ");
    sc.nextLine();

    // Update final payment in bookings
    collection.updateOne(
        new Document("bookingId", id),
        new Document("$set",
            new Document("paymentStatus", finalStatus.name())
                .append("amountPaid", amountPaid)
                .append("total", total)
                .append("bookingStatus", BookingStatus.BOOKED.name()))
    );

    // =====================================================
    // 🚨 CANCEL ALL OTHER BOOKINGS FOR THIS DATE & VENUE
    // =====================================================
    Date bookingDate = doc.getDate("date");

    Document conflictFilter = new Document("venueId", venueId)
            .append("date", bookingDate)
            .append("bookingId", new Document("$ne", id))
            .append("bookingStatus", new Document("$ne", BookingStatus.CANCELLED.name()));

    List<Document> conflicts = collection.find(conflictFilter).into(new ArrayList<>());

    for (Document c : conflicts) {
        // Move to history
        PaymentStatus cStatus = PaymentStatus.CANCELLED;
        if (c.getString("paymentStatus") != null &&
            c.getString("paymentStatus").equalsIgnoreCase(PaymentStatus.DOWNPAID.name())) {
            cStatus = PaymentStatus.DOWNPAID;
        }

        Booking cancelled = new Booking(
                c.getInteger("bookingId"),
                null,
                c.getDate("date"),
                cStatus,
                BookingStatus.CANCELLED,
                c.getString("purpose"),
                c.get("bookedBy", Document.class).getString("username")
        );
        BookingHistory.addDeleted(cancelled);
    }

    // Remove cancelled bookings from bookings collection
    collection.deleteMany(conflictFilter);

    System.out.println(conflicts.size() + " conflicting booking(s) were cancelled, moved to history, and removed from bookings.");

    // =====================================================
    // ✔ Move this successful payment to FINISHED HISTORY
    // =====================================================
    Booking finishedSnapshot = new Booking(
            id,
            null,
            doc.getDate("date"),
            finalStatus,
            BookingStatus.BOOKED,
            doc.getString("purpose"),
            customer.getUsername()
    );
    BookingHistory.addFinished(finishedSnapshot);

    System.out.println("\nPayment successful!");
    System.out.println("You paid: ₱" + amountPaid);
    System.out.println("Remaining balance: ₱" + (total - amountPaid));
    System.out.println("Payment Status: " + finalStatus);
    System.out.println("Booking confirmed & added to history.");

    // =====================================================
    // UPDATE VENUE AVAILABILITY
    // =====================================================
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Set<String> paidBookedDates = new HashSet<>();

    for (Document b : collection.find(
            new Document("venueId", venueId)
                    .append("bookingStatus", BookingStatus.BOOKED.name())
                    .append("paymentStatus", new Document("$in",
                            Arrays.asList(PaymentStatus.PAID.name(), PaymentStatus.DOWNPAID.name()))))) {

        Date d = b.getDate("date");
        if (d != null)
            paidBookedDates.add(sdf.format(d));
    }

    Calendar checkCal = Calendar.getInstance();
    int freeCount = 0;
    for (int i = 0; i < 30; i++) {
        String ds = sdf.format(checkCal.getTime());
        if (!paidBookedDates.contains(ds))
            freeCount++;
        checkCal.add(Calendar.DAY_OF_MONTH, 1);
    }

    boolean fullyBooked = (freeCount == 0);

    venueCollection.updateOne(
            new Document("venueId", venueId),
            new Document("$set", new Document("availability", !fullyBooked)));

    if (fullyBooked) {
        System.out.println("\n⚠️ NOTICE: This venue is now fully booked for the next 30 days.");
    }
}


}