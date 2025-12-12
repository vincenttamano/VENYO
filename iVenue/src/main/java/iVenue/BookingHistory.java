package iVenue;

import java.util.Queue;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class BookingHistory {

    // Singleton instance (backwards-compatible facade)
    private static final BookingHistory INSTANCE = new BookingHistory();

    private final Queue<Booking> finishedQueue = new LinkedList<>();
    private final Queue<Booking> deletedQueue = new LinkedList<>();
    // Maximum number of history entries to keep for each queue (FIFO)
    private int maxHistory = 30;
    private final MongoCollection<Document> collection;

    // Public constructor - initializes from DB
    public BookingHistory() {
        this.collection = MongoDb.getDatabase().getCollection("booking_history");
        loadHistory();
    }

    // Load existing history from DB into queues
    private void loadHistory() {
        synchronized (finishedQueue) {
            for (Document doc : collection.find(new Document("type", "finished"))) {
                // Parse paymentStatus from DB in a case-insensitive manner
                String payStr = doc.getString("paymentStatus");
                PaymentStatus paymentStatus = PaymentStatus.UNPAID;
                if (payStr != null) {
                    boolean matched = false;
                    for (PaymentStatus ps : PaymentStatus.values()) {
                        if (ps.name().equalsIgnoreCase(payStr)) {
                            paymentStatus = ps;
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        // try capitalized form as fallback
                        try {
                            paymentStatus = PaymentStatus
                                    .valueOf(payStr.substring(0, 1).toUpperCase() + payStr.substring(1).toLowerCase());
                        } catch (Exception ignored) {
                        }
                    }
                }

                // Normalize bookingStatus string from DB
                String statusStr = doc.getString("bookingStatus");
                BookingStatus bookingStatus = BookingStatus.PENDING;
                if (statusStr != null) {
                    boolean matched = false;
                    for (BookingStatus bs : BookingStatus.values()) {
                        if (bs.name().equalsIgnoreCase(statusStr)) {
                            bookingStatus = bs;
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        try {
                            bookingStatus = BookingStatus.valueOf(statusStr.toUpperCase());
                        } catch (Exception ignored) {
                        }
                    }
                }

                Booking b = new Booking(
                        doc.getInteger("bookingId"),
                        null,
                        null,
                        paymentStatus,
                        bookingStatus,
                        doc.getString("purpose"),
                        doc.getString("username"));
                finishedQueue.offer(b);
            }
        }

        synchronized (deletedQueue) {
            for (Document doc : collection.find(new Document("type", "cancelled"))) {
                String payStr = doc.getString("paymentStatus");
                PaymentStatus paymentStatus = PaymentStatus.UNPAID;
                if (payStr != null) {
                    boolean matched = false;
                    for (PaymentStatus ps : PaymentStatus.values()) {
                        if (ps.name().equalsIgnoreCase(payStr)) {
                            paymentStatus = ps;
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        try {
                            paymentStatus = PaymentStatus
                                    .valueOf(payStr.substring(0, 1).toUpperCase() + payStr.substring(1).toLowerCase());
                        } catch (Exception ignored) {
                        }
                    }
                }

                String statusStr = doc.getString("bookingStatus");
                BookingStatus bookingStatus = BookingStatus.PENDING;
                if (statusStr != null) {
                    boolean matched = false;
                    for (BookingStatus bs : BookingStatus.values()) {
                        if (bs.name().equalsIgnoreCase(statusStr)) {
                            bookingStatus = bs;
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        try {
                            bookingStatus = BookingStatus.valueOf(statusStr.toUpperCase());
                        } catch (Exception ignored) {
                        }
                    }
                }

                Booking b = new Booking(
                        doc.getInteger("bookingId"),
                        null,
                        null,
                        paymentStatus,
                        bookingStatus,
                        doc.getString("purpose"),
                        doc.getString("username"));
                deletedQueue.offer(b);
            }
        }

        // Enforce max size after loading from DB to ensure invariant (trim oldest FIFO)
        enforceMaxSize();
    }

    private void writeHistory(Booking booking, String type) {
        if (booking == null)
            return;
        Document doc = new Document("bookingId", booking.getBookingId())
                .append("type", type)
                .append("paymentStatus", booking.getPaymentStatus() == null ? null : booking.getPaymentStatus().name())
                .append("bookingStatus", booking.getBookingStatus() == null ? null : booking.getBookingStatus().name())
                .append("purpose", booking.getPurpose())
                .append("username", booking.getUsername())
                .append("timestamp", new java.util.Date());
        collection.insertOne(doc);
    }

    // --- Finished bookings ---

    // Instance API
    public synchronized void addFinishedInternal(Booking booking) {
        if (booking == null)
            return;
        finishedQueue.offer(booking);
        writeHistory(booking, "finished");
        // enforce FIFO max size
        while (finishedQueue.size() > maxHistory) {
            Booking removed = finishedQueue.poll();
            if (removed != null) {
                collection.deleteOne(new Document("bookingId", removed.getBookingId()).append("type", "finished"));
            }
        }
    }

    public synchronized List<Booking> listFinishedInternal() {
        return new ArrayList<>(finishedQueue);
    }

    public synchronized int finishedCountInternal() {
        return finishedQueue.size();
    }

    public synchronized void clearFinishedInternal() {
        finishedQueue.clear();
        collection.deleteMany(new Document("type", "finished"));
    }

    public synchronized boolean removeFinishedByIdInternal(int bookingId) {
        java.util.Iterator<Booking> it = finishedQueue.iterator();
        while (it.hasNext()) {
            Booking b = it.next();
            if (b.getBookingId() == bookingId) {
                it.remove();
                collection.deleteOne(new Document("bookingId", bookingId).append("type", "finished"));
                return true;
            }
        }
        return false;
    }

    // --- Deleted bookings ---

    public synchronized void addDeletedInternal(Booking booking) {
        if (booking == null)
            return;
        deletedQueue.offer(booking);
        writeHistory(booking, "cancelled");
        while (deletedQueue.size() > maxHistory) {
            Booking removed = deletedQueue.poll();
            if (removed != null) {
                collection.deleteOne(new Document("bookingId", removed.getBookingId()).append("type", "cancelled"));
            }
        }
    }

    public synchronized List<Booking> listDeletedInternal() {
        return new ArrayList<>(deletedQueue);
    }

    public synchronized int deletedCountInternal() {
        return deletedQueue.size();
    }

    public synchronized void clearDeletedInternal() {
        deletedQueue.clear();
        collection.deleteMany(new Document("type", "cancelled"));
    }

    public synchronized boolean removeDeletedByIdInternal(int bookingId) {
        java.util.Iterator<Booking> it = deletedQueue.iterator();
        while (it.hasNext()) {
            Booking b = it.next();
            if (b.getBookingId() == bookingId) {
                it.remove();
                collection.deleteOne(new Document("bookingId", bookingId).append("type", "cancelled"));
                return true;
            }
        }
        return false;
    }

    // --- Additional helpers ---
    // List finished (paid) bookings by username
    public synchronized List<Booking> listFinishedByUsernameInternal(String username) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : finishedQueue) {
            if (b.getUsername() != null && b.getUsername().equals(username))
                result.add(b);
        }
        return result;
    }

    // List unpaid bookings (present in bookings collection with
    // PaymentStatus.Pending) for a userId
    public static synchronized List<Booking> listUnpaidFromBookings(int userId) {
        List<Booking> result = new ArrayList<>();
        com.mongodb.client.MongoCollection<Document> coll = MongoDb.getDatabase().getCollection("bookings");
        for (Document doc : coll
                .find(new Document("userId", userId).append("paymentStatus", PaymentStatus.UNPAID.name()))) {
            Integer id = doc.getInteger("bookingId");
            String purpose = doc.getString("purpose");
            String uname = null;
            if (doc.containsKey("bookedBy") && doc.get("bookedBy") instanceof Document bb) {
                uname = bb.getString("username");
            }
            String statusStr = doc.getString("bookingStatus");
            BookingStatus bstat = BookingStatus.PENDING;
            if (statusStr != null) {
                try {
                    bstat = BookingStatus.valueOf(statusStr);
                } catch (Exception e) {
                    // keep default
                }
            }
            Booking b = new Booking(id != null ? id : 0, null, null, PaymentStatus.UNPAID, bstat, purpose, uname);
            result.add(b);
        }
        return result;
    }

    // List bookings that contain any "down payment" indicator for a user
    // (This looks for keys that might indicate partial/down payments if present in
    // DB.)
    public static synchronized List<Booking> listDownPaymentsFromBookings(int userId) {
        List<Booking> result = new ArrayList<>();
        com.mongodb.client.MongoCollection<Document> coll = MongoDb.getDatabase().getCollection("bookings");
        for (Document doc : coll.find(new Document("userId", userId))) {
            if (doc.containsKey("downPayment") || doc.containsKey("downPaymentBy") || doc.containsKey("partialPaid")) {
                Integer id = doc.getInteger("bookingId");
                String purpose = doc.getString("purpose");
                String uname = null;
                if (doc.containsKey("bookedBy") && doc.get("bookedBy") instanceof Document bb) {
                    uname = bb.getString("username");
                }
                String statusStr = doc.getString("bookingStatus");
                BookingStatus bstat = BookingStatus.PENDING;
                if (statusStr != null) {
                    try {
                        bstat = BookingStatus.valueOf(statusStr);
                    } catch (Exception e) {
                        // keep default
                    }
                }
                Booking b = new Booking(id != null ? id : 0, null, null, PaymentStatus.DOWNPAID, bstat, purpose, uname);
                result.add(b);
            }
        }
        return result;
    }

    // Enforce max size on both queues (trim oldest FIFO and delete corresponding DB docs)
    private synchronized void enforceMaxSize() {
        synchronized (finishedQueue) {
            while (finishedQueue.size() > maxHistory) {
                Booking removed = finishedQueue.poll();
                if (removed != null) {
                    collection.deleteOne(new Document("bookingId", removed.getBookingId()).append("type", "finished"));
                }
            }
        }
        synchronized (deletedQueue) {
            while (deletedQueue.size() > maxHistory) {
                Booking removed = deletedQueue.poll();
                if (removed != null) {
                    collection.deleteOne(new Document("bookingId", removed.getBookingId()).append("type", "cancelled"));
                }
            }
        }
    }

    // Public getters/setters for encapsulated properties
    public int getMaxHistory() {
        return maxHistory;
    }

    public synchronized void setMaxHistory(int maxHistory) {
        if (maxHistory < 1) return;
        this.maxHistory = maxHistory;
        enforceMaxSize();
    }

    // Backwards-compatible static facade methods that delegate to the singleton instance
    public static void addFinished(Booking booking) { INSTANCE.addFinishedInternal(booking); }
    public static List<Booking> listFinished() { return INSTANCE.listFinishedInternal(); }
    public static int finishedCount() { return INSTANCE.finishedCountInternal(); }
    public static void clearFinished() { INSTANCE.clearFinishedInternal(); }
    public static boolean removeFinishedById(int bookingId) { return INSTANCE.removeFinishedByIdInternal(bookingId); }

    public static void addDeleted(Booking booking) { INSTANCE.addDeletedInternal(booking); }
    public static List<Booking> listDeleted() { return INSTANCE.listDeletedInternal(); }
    public static int deletedCount() { return INSTANCE.deletedCountInternal(); }
    public static void clearDeleted() { INSTANCE.clearDeletedInternal(); }
    public static boolean removeDeletedById(int bookingId) { return INSTANCE.removeDeletedByIdInternal(bookingId); }

    public static List<Booking> listFinishedByUsername(String username) { return INSTANCE.listFinishedByUsernameInternal(username); }

}