package iVenue;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Date;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Comparator;

public class BookingAdmin implements AdminManagement<Booking> {

    private final MongoCollection<Document> collection;

    public BookingAdmin() {
        MongoDatabase database = MongoDb.getDatabase();
        this.collection = database.getCollection("bookings");
    }

    public void create(Scanner input) {
        MongoCollection<Document> collection = MongoDb.getDatabase().getCollection("bookings");

        System.out.println("----CREATE NEW BOOKING----");

        // Display available venues
        Venue.displayAvailableVenues();
        System.out.print("Enter Venue ID: ");
        int venueId = Integer.parseInt(input.nextLine());

        Venue selectedVenue = Venue.getVenue(venueId, true);
        if (selectedVenue == null) {
            System.out.println("Invalid Venue ID. Booking cancelled.");
            return;
        }

        // ❌ DO NOT mark venue unavailable
        // selectedVenue.setAvailability(false);
        // MongoDb.getDatabase().getCollection("venues")
        // .updateOne(new Document("venueId", venueId),
        // new Document("$set", new Document("availability", false)));

        // Select amenities
        Amenity.displayAmenities();
        LinkedList<Amenity> selectedAmenityObjects = new LinkedList<>();
        LinkedList<Document> selectedAmenityDocs = new LinkedList<>();

        while (true) {
            System.out.print("Enter Amenity ID to add (0 to stop): ");
            int aid = Integer.parseInt(input.nextLine().trim());
            if (aid == 0)
                break;

            Amenity am = Amenity.getAmenity(aid);
            if (am != null) {
                int maxQty = am.getQuantity();
                int qty;

                // Validate quantity
                while (true) {
                    System.out.print("Enter quantity for " + am.getName() + " (max " + maxQty + "): ");
                    qty = Integer.parseInt(input.nextLine().trim());

                    if (qty >= 1 && qty <= maxQty)
                        break;
                    System.out.println("Invalid quantity. Must be between 1 and " + maxQty);
                }

                selectedAmenityObjects.add(am);
                selectedAmenityDocs.add(new Document("amenityId", am.getAmenityId())
                        .append("quantity", qty)
                        .append("price", am.getPrice() * qty));

                System.out.println("Added " + qty + " x " + am.getName());
            } else {
                System.out.println("Amenity not found.");
            }
        }

        System.out.print("Enter Purpose: ");
        String purpose = input.nextLine();

        // Generate Booking ID
        int maxId = 0;
        Document lastBooking = collection.find().sort(new Document("bookingId", -1)).first();
        if (lastBooking != null) {
            maxId = lastBooking.getInteger("bookingId");
        }

        // Create booking object
        Date bookingDate = new Date();
        Booking newBooking = new Booking(
                maxId + 1,
                selectedVenue,
                bookingDate,
                PaymentStatus.UNPAID,
                BookingStatus.PENDING,
                purpose,
                "N/A");

        newBooking.getAmenities().addAll(selectedAmenityObjects);

        // Build MongoDB document
        Document doc = new Document("bookingId", newBooking.getBookingId())
                .append("venueName", selectedVenue.getName())
                .append("venueId", selectedVenue.getVenueId())
                .append("date", bookingDate.toString())
                .append("paymentStatus", newBooking.getPaymentStatus().name())
                .append("bookingStatus", newBooking.getBookingStatus().name())
                .append("purpose", purpose)
                .append("amenities", selectedAmenityDocs)
                .append("price", selectedVenue.getPrice())
                .append("isFree", selectedVenue.isFree());

        // Insert into DB
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

        // Display enum options
        System.out.println("Select new Booking Status:");
        BookingStatus[] statuses = BookingStatus.values();
        for (int i = 0; i < statuses.length; i++) {
            System.out.println((i + 1) + ". " + statuses[i]);
        }

        int choice = 0;
        while (true) {
            System.out.print("Enter choice (1-" + statuses.length + "): ");
            try {
                choice = Integer.parseInt(input.nextLine());
                if (choice >= 1 && choice <= statuses.length)
                    break;
            } catch (NumberFormatException e) {
                // ignore invalid input
            }
            System.out.println("Invalid choice. Try again.");
        }

        BookingStatus newStatus = statuses[choice - 1];

        // Update booking status in MongoDB
        collection.updateOne(
                new Document("bookingId", id),
                new Document("$set", new Document("bookingStatus", newStatus.name())));

        System.out.println("Booking updated successfully.");

        // If marking as finished or cancelled
        if (newStatus == BookingStatus.FINISHED || newStatus == BookingStatus.CANCELLED) {
            // Update venue availability
            if (doc.containsKey("venueId")) {
                int venueId = doc.getInteger("venueId");
                MongoCollection<Document> venueCollection = MongoDb.getDatabase().getCollection("venues");
                // Ensure we set availability (used elsewhere) to true so the venue becomes
                // available again
                venueCollection.updateOne(
                        new Document("venueId", venueId),
                        new Document("$set", new Document("availability", true)));
                System.out.println("Venue marked as available.");
            }

            // Extract username
            String username = "N/A";
            if (doc.containsKey("bookedBy")) {
                Document bookedBy = (Document) doc.get("bookedBy");
                username = bookedBy.getString("username") == null ? "N/A" : bookedBy.getString("username");
            }

            // Create snapshot
            Booking snapshot = new Booking(
                    id,
                    null,
                    null,
                    PaymentStatus.valueOf(doc.getString("paymentStatus")),
                    newStatus,
                    doc.getString("purpose"),
                    username);

            // Add to history
            if (newStatus == BookingStatus.FINISHED) {
                BookingHistory.addFinished(snapshot);
                System.out.println("Booking recorded in finished history.");
            } else if (newStatus == BookingStatus.CANCELLED) {
                BookingHistory.addDeleted(snapshot);
                System.out.println("Booking recorded in cancelled history.");
            }

            // Delete from main bookings collection
            collection.deleteOne(new Document("bookingId", id));
            System.out.println("Booking removed from active bookings.");
        }
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

        // Extract username from bookedBy
        String username = "N/A";
        if (doc.containsKey("bookedBy")) {
            Document bookedBy = (Document) doc.get("bookedBy");
            username = bookedBy.getString("username") == null ? "N/A" : bookedBy.getString("username");
        }

        // Add a snapshot of the deleted booking to history before removing from DB
        Booking deletedSnapshot = new Booking(id, null, null, PaymentStatus.valueOf(doc.getString("paymentStatus")),
                BookingStatus.valueOf(doc.getString("bookingStatus")), doc.getString("purpose"), username);
        BookingHistory.addDeleted(deletedSnapshot);

        collection.deleteOne(new Document("bookingId", id));
        System.out.println(
                "Booking deleted successfully. Venue is now available and booking recorded in deleted history.");
    }

    @Override
    public void displayAll() {
        // Load all bookings into a linked list and show unsorted first
        LinkedList<Booking> list = loadAllBookings();

        System.out.println("----ALL BOOKINGS (UNSORTED)----");
        if (list.isEmpty()) {
            System.out.println("No bookings found.");
        } else {
            for (Booking b : list) {
                System.out.println("ID:" + b.getBookingId() + " | Venue:"
                        + (b.getVenue() == null ? "N/A" : b.getVenue().getName()) +
                        " | User:" + (b.getUsername() == null ? "N/A" : b.getUsername()) +
                        " | Status:" + (b.getBookingStatus() == null ? "N/A" : b.getBookingStatus()));
            }
        }

        // Sorting Option (Selection Sort)
        Scanner sc = new Scanner(System.in);
        System.out.println("\nSort bookings by status? (Selection Sort)");
        System.out.println("1) Yes");
        System.out.println("2) No");
        System.out.print("Enter choice: ");

        int sortChoice;
        try {
            sortChoice = Integer.parseInt(sc.nextLine());
        } catch (Exception e) {
            sortChoice = 2;
        }

        if (sortChoice == 1) {
            selectionSortByStatus(list);
            System.out.println("\n----BOOKINGS SORTED ALPHABETICALLY (SELECTION SORT)----");
            for (Booking b : list) {
                System.out.println("ID:" + b.getBookingId() + " | Status:" + b.getBookingStatus());
            }
        }

        // Filter menu
        System.out.println(
                "\nFilter bookings by status?\n1) BOOKED\n2) PENDING\n3) CANCELLED\n4) FINISHED\n5) No / Back");
        System.out.print("Enter choice: ");
        int filterChoice;
        try {
            filterChoice = Integer.parseInt(sc.nextLine());
        } catch (Exception e) {
            filterChoice = 5;
        }

        BookingStatus filterStatus = null;
        switch (filterChoice) {
            case 1:
                filterStatus = BookingStatus.BOOKED;
                break;
            case 2:
                filterStatus = BookingStatus.PENDING;
                break;
            case 3:
                filterStatus = BookingStatus.CANCELLED;
                break;
            case 4:
                filterStatus = BookingStatus.FINISHED;
                break;
            default:
                filterStatus = null;
        }

        if (filterStatus != null) {
            LinkedList<Booking> filtered = new LinkedList<>();
            for (Booking b : list) {
                if (b.getBookingStatus() == filterStatus) {
                    filtered.add(b);
                }
            }
            System.out.println("\n----FILTERED BOOKINGS (" + filterStatus + ")----");
            if (filtered.isEmpty()) {
                System.out.println("No bookings with status " + filterStatus + ".");
            } else {
                for (Booking b : filtered) {
                    System.out.println("Booking ID: " + b.getBookingId());
                    System.out.println("Purpose: " + (b.getPurpose() == null ? "N/A" : b.getPurpose()));
                    System.out.println("User: " + (b.getUsername() == null ? "N/A" : b.getUsername()));
                    System.out.println("Status: " + b.getBookingStatus());
                    System.out.println("Payment: " + b.getPaymentStatus());
                    System.out.println("-----------------------------");
                }
            }
        }
    }

    // --- SELECTION SORT FOR ACTIVE BOOKINGS ---
    private static void selectionSortByStatus(LinkedList<Booking> list) {
        int n = list.size();

        for (int i = 0; i < n - 1; i++) {
            int minIndex = i;

            for (int j = i + 1; j < n; j++) {
                String s1 = list.get(j).getBookingStatus().name();
                String s2 = list.get(minIndex).getBookingStatus().name();
                if (s1.compareTo(s2) < 0) {
                    minIndex = j;
                }
            }

            Booking temp = list.get(minIndex);
            list.set(minIndex, list.get(i));
            list.set(i, temp);
        }
    }

    // Load bookings from DB into a linked list of lightweight Booking objects
    private LinkedList<Booking> loadAllBookings() {
        LinkedList<Booking> list = new LinkedList<>();
        for (Document doc : collection.find()) {
            int id = doc.containsKey("bookingId") ? doc.getInteger("bookingId") : 0;
            String pay = doc.containsKey("paymentStatus") ? doc.getString("paymentStatus") : null;
            String stat = doc.containsKey("bookingStatus") ? doc.getString("bookingStatus") : null;
            String purpose = doc.getString("purpose");
            String uname = null;
            if (doc.containsKey("bookedBy") && doc.get("bookedBy") instanceof Document bb) {
                uname = bb.getString("username");
            }

            PaymentStatus pstat = PaymentStatus.UNPAID;
            try {
                if (pay != null)
                    pstat = PaymentStatus.valueOf(pay.toUpperCase());
            } catch (Exception ignored) {
            }
            BookingStatus bstat = BookingStatus.PENDING;
            try {
                if (stat != null)
                    bstat = BookingStatus.valueOf(stat.toUpperCase());
            } catch (Exception ignored) {
            }

            Booking b = new Booking(id, null, null, pstat, bstat, purpose, uname);
            list.add(b);
        }
        return list;
    }

    // Bubble sort implemented on LinkedList (non-tree-based)
    private static void bubbleSortLinkedList(LinkedList<Booking> list, Comparator<Booking> cmp) {
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (cmp.compare(list.get(j), list.get(j + 1)) > 0) {
                    // swap
                    Booking temp = list.get(j);
                    list.set(j, list.get(j + 1));
                    list.set(j + 1, temp);
                }
            }
        }
    }

    // Helper to create comparator for history sorting
    private static Comparator<Booking> getHistoryComparator(int choice) {
        return (b1, b2) -> {
            boolean b1Finished = b1.getBookingStatus() == BookingStatus.FINISHED;
            boolean b2Finished = b2.getBookingStatus() == BookingStatus.FINISHED;
            if (choice == 1) { // finished first
                if (b1Finished && !b2Finished)
                    return -1;
                if (!b1Finished && b2Finished)
                    return 1;
                return 0;
            } else { // cancelled first
                if (!b1Finished && b2Finished)
                    return -1;
                if (b1Finished && !b2Finished)
                    return 1;
                return 0;
            }
        };
    }

    /**
     * Display booking history: all finished and cancelled bookings together.
     */
    public void displayHistory(Scanner input) {
        System.out.println("\n--- BOOKING HISTORY ---");
        LinkedList<Booking> allHistory = new LinkedList<>();
        allHistory.addAll(BookingHistory.listFinished());
        allHistory.addAll(BookingHistory.listDeleted());

        if (allHistory.isEmpty()) {
            System.out.println("No bookings in history.");
            return;
        }

        System.out.println("----ALL HISTORY (UNSORTED)----");
        int index = 1;
        for (Booking b : allHistory) {
            Document full = MongoDb.getDatabase().getCollection("bookings")
                    .find(new Document("bookingId", b.getBookingId())).first();
            System.out.println("[" + index + "]");
            if (full != null) {
                System.out.println("Booking ID: " + full.getInteger("bookingId"));
                System.out.println("Venue: " + full.getString("venueName"));
                System.out.println("Purpose: " + full.getString("purpose"));
                System.out.println("Status: "
                        + (b.getBookingStatus() == null ? full.getString("bookingStatus") : b.getBookingStatus()));
                System.out.println("Payment Status: "
                        + (b.getPaymentStatus() == null ? full.getString("paymentStatus") : b.getPaymentStatus()));
                System.out.println("User: " + (b.getUsername() == null ? "N/A" : b.getUsername()));
                Object amenities = full.get("amenities");
                System.out.print("Amenities: ");
                if (amenities != null)
                    System.out.println(amenities);
                else
                    System.out.println("None");
                Double price = full.getDouble("price");
                if (price != null)
                    System.out.println("Price: ₱" + price);
            } else {
                System.out.println("Booking ID: " + b.getBookingId());
                System.out.println("  Payment: " + (b.getPaymentStatus() == null ? "N/A" : b.getPaymentStatus()));
                System.out.println("  Status: " + (b.getBookingStatus() == null ? "N/A" : b.getBookingStatus()));
                System.out.println("  Purpose: " + (b.getPurpose() == null ? "N/A" : b.getPurpose()));
                System.out.println("  User: " + (b.getUsername() == null ? "N/A" : b.getUsername()));
            }
            System.out.println("-----------------------------");
            index++;
        }

        // Sorting menu
        System.out.println("\nSort history using Bubble Sort?");
        System.out.println("1) Finished first");
        System.out.println("2) Cancelled first");
        System.out.println("3) No");
        System.out.print("Enter choice: ");

        int sortChoice;
        try {
            sortChoice = Integer.parseInt(input.nextLine());
        } catch (Exception e) {
            sortChoice = 3;
        }

        if (sortChoice == 1 || sortChoice == 2) {
            Comparator<Booking> cmp = getHistoryComparator(sortChoice);
            bubbleSortLinkedList(allHistory, cmp); // 🔥 BUBBLE SORT APPLIED

            System.out.println(
                    "\n----SORTED HISTORY (" + (sortChoice == 1 ? "FINISHED FIRST" : "CANCELLED FIRST") + ")----");
            for (Booking b : allHistory) {
                System.out.println("ID: " + b.getBookingId() + " | Status: " + b.getBookingStatus() + " | User: "
                        + b.getUsername());
            }
        }

        LinkedList<Booking> finalList = new LinkedList<>(allHistory);

        System.out.println("\nDo you want to show only specific status?");
        System.out.println("1) Finished only");
        System.out.println("2) Cancelled only");
        System.out.println("3) No / Show all");
        System.out.print("Enter choice: ");
        int filterChoice;
        try {
            filterChoice = Integer.parseInt(input.nextLine());
        } catch (Exception e) {
            filterChoice = 3;
        }

        LinkedList<Booking> filteredList = new LinkedList<>();
        switch (filterChoice) {
            case 1 -> finalList.stream().filter(b -> b.getBookingStatus() == BookingStatus.FINISHED)
                    .forEach(filteredList::add);
            case 2 -> finalList.stream().filter(b -> b.getBookingStatus() == BookingStatus.CANCELLED)
                    .forEach(filteredList::add);
            default -> filteredList = finalList;
        }

        System.out.println("\n----BOOKING HISTORY DISPLAY----");
        for (Booking b : filteredList) {
            System.out.println(
                    "ID: " + b.getBookingId() + " | Status: " + b.getBookingStatus() + " | User: " + b.getUsername());
        }

        // Delete options
        int delChoice;
        do {
            System.out.println("\n--- History Options ---");
            System.out.println("1. Delete one entry");
            System.out.println("2. Delete all entries");
            System.out.println("3. Back");
            System.out.print("Enter choice: ");
            try {
                delChoice = Integer.parseInt(input.nextLine());
            } catch (Exception e) {
                delChoice = 3;
            }
            switch (delChoice) {
                case 1:
                    System.out.print("Enter Booking ID to remove: ");
                    int remId;
                    try {
                        remId = Integer.parseInt(input.nextLine());
                    } catch (Exception e) {
                        System.out.println("Invalid ID.");
                        break;
                    }
                    if (BookingHistory.removeFinishedById(remId)) {
                        System.out.println("Removed booking " + remId + " from finished history.");
                    } else if (BookingHistory.removeDeletedById(remId)) {
                        System.out.println("Removed booking " + remId + " from deleted history.");
                    } else {
                        System.out.println("Booking ID not found in history.");
                    }
                    break;
                case 2:
                    System.out.print("Are you sure you want to delete ALL history? (y/n): ");
                    String conf = input.nextLine();
                    if (conf.equalsIgnoreCase("y")) {
                        BookingHistory.clearFinished();
                        BookingHistory.clearDeleted();
                        System.out.println("All history cleared.");
                    } else {
                        System.out.println("Cancelled.");
                    }
                    break;
                case 3:
                    break;
                default:
                    System.out.println("Invalid option, please try again!");
            }
        } while (delChoice != 3);
    }
}