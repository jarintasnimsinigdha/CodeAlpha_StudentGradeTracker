import java.util.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.ChronoUnit;

public class HotelReservationSystem {


    enum RoomCategory {
        STANDARD(80.00,  "Standard",  "Queen bed, TV, Wi-Fi"),
        DELUXE  (150.00, "Deluxe",   "King bed, Mini-bar, City view"),
        SUITE   (300.00, "Suite",    "Living area, Jacuzzi, Panoramic view");

        final double pricePerNight;
        final String displayName;
        final String description;

        RoomCategory(double price, String display, String desc) {
            pricePerNight = price;
            displayName   = display;
            description   = desc;
        }
    }

    enum BookingStatus { CONFIRMED, CANCELLED, CHECKED_IN, CHECKED_OUT }

    enum PaymentMethod { CREDIT_CARD, CASH, ONLINE }

 

    static class Room {
        private final String roomNumber;
        private final RoomCategory category;
        private final int floor;
        private boolean available;

        Room(String roomNumber, RoomCategory category, int floor) {
            this.roomNumber = roomNumber;
            this.category   = category;
            this.floor      = floor;
            this.available  = true;
        }

        String getRoomNumber() { return roomNumber; }
        RoomCategory getCategory() { return category; }
        int getFloor() { return floor; }
        boolean isAvailable() { return available; }
        void setAvailable(boolean a) { available = a; }
        double getPricePerNight() { return category.pricePerNight; }

        @Override
        public String toString() {
            return String.format("Room %-4s | %-8s | Floor %d | $%.2f/night | %s",
                    roomNumber, category.displayName, floor,
                    category.pricePerNight,
                    available ? "✅ Available" : "❌ Occupied");
        }
    }

    static class Guest {
        private final String guestId;
        private final String name;
        private final String phone;
        private final String email;

        Guest(String guestId, String name, String phone, String email) {
            this.guestId = guestId;
            this.name    = name;
            this.phone   = phone;
            this.email   = email;
        }

        String getGuestId() { return guestId; }
        String getName()    { return name;    }
        String getPhone()   { return phone;   }
        String getEmail()   { return email;   }
    }

    static class Payment {
        private final String paymentId;
        private final double amount;
        private final PaymentMethod method;
        private final LocalDateTime paidAt;
        private boolean successful;

        Payment(String paymentId, double amount, PaymentMethod method) {
            this.paymentId  = paymentId;
            this.amount     = amount;
            this.method     = method;
            this.paidAt     = LocalDateTime.now();
            this.successful = false;
        }

        void process() { this.successful = true; }

        String getPaymentId()     { return paymentId;  }
        double getAmount()        { return amount;     }
        PaymentMethod getMethod() { return method;     }
        boolean isSuccessful()    { return successful; }

        @Override
        public String toString() {
            return String.format("Payment %-10s | %-12s | $%8.2f | %s",
                    paymentId, method.name(), amount,
                    successful ? "✔ SUCCESS" : "✘ PENDING");
        }
    }

    static class Booking {
        private final String bookingId;
        private final Guest guest;
        private final Room room;
        private final LocalDate checkIn;
        private final LocalDate checkOut;
        private BookingStatus status;
        private Payment payment;
        private final LocalDateTime createdAt;

        Booking(String bookingId, Guest guest, Room room,
                LocalDate checkIn, LocalDate checkOut) {
            this.bookingId = bookingId;
            this.guest     = guest;
            this.room      = room;
            this.checkIn   = checkIn;
            this.checkOut  = checkOut;
            this.status    = BookingStatus.CONFIRMED;
            this.createdAt = LocalDateTime.now();
        }

        long nights() { return ChronoUnit.DAYS.between(checkIn, checkOut); }
        double totalCost() { return nights() * room.getPricePerNight(); }

        String getBookingId()  { return bookingId; }
        Guest getGuest()       { return guest;     }
        Room getRoom()         { return room;      }
        LocalDate getCheckIn() { return checkIn;   }
        LocalDate getCheckOut(){ return checkOut;  }
        BookingStatus getStatus() { return status; }
        Payment getPayment()   { return payment;   }
        LocalDateTime getCreatedAt() { return createdAt; }

        void setStatus(BookingStatus s) { this.status  = s; }
        void setPayment(Payment p)      { this.payment = p; }

        boolean overlapsWith(LocalDate ci, LocalDate co) {
            return !(co.compareTo(checkIn) <= 0 || ci.compareTo(checkOut) >= 0);
        }
    }

   

    private final Map<String, Room>    rooms    = new LinkedHashMap<>();
    private final Map<String, Booking> bookings = new LinkedHashMap<>();
    private final Map<String, Guest>   guests   = new LinkedHashMap<>();

    private final Scanner scanner = new Scanner(System.in);
    private int bookingCounter = 1;
    private int guestCounter   = 1;
    private int paymentCounter = 1;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String BOOKINGS_FILE = "bookings_data.csv";

    public static void main(String[] args) {
        new HotelReservationSystem().start();
    }

    private void start() {
        initRooms();
        loadBookings();
        printBanner();

        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Enter choice").trim();
            System.out.println();
            switch (choice) {
                case "1"  -> searchAvailableRooms();
                case "2"  -> makeReservation();
                case "3"  -> viewBooking();
                case "4"  -> cancelReservation();
                case "5"  -> checkIn();
                case "6"  -> checkOut();
                case "7"  -> viewAllBookings();
                case "8"  -> viewAllRooms();
                case "9"  -> viewRoomCategories();
                case "0"  -> { saveBookings(); running = false;
                               System.out.println("Data saved. Goodbye! 🏨"); }
                default   -> System.out.println("  ⚠  Invalid option.\n");
            }
        }
    }


    private void initRooms() {
        // Floor 1 – Standard (101–110)
        for (int i = 1; i <= 10; i++) addRoom(String.format("1%02d", i), RoomCategory.STANDARD, 1);
        // Floor 2 – Deluxe   (201–208)
        for (int i = 1; i <=  8; i++) addRoom(String.format("2%02d", i), RoomCategory.DELUXE,   2);
        // Floor 3 – Suite    (301–304)
        for (int i = 1; i <=  4; i++) addRoom(String.format("3%02d", i), RoomCategory.SUITE,    3);
    }

    private void addRoom(String number, RoomCategory cat, int floor) {
        rooms.put(number, new Room(number, cat, floor));
    }

   

    private void printBanner() {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║             GRAND JAVA HOTEL                 ║");
        System.out.println("║         Reservation System  v1.0             ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();
    }

    private void printMenu() {
        System.out.println("┌────────────────── MAIN MENU ──────────────────┐");
        System.out.println("│  1. Search available rooms                    │");
        System.out.println("│  2. Make a reservation                        │");
        System.out.println("│  3. View booking details                      │");
        System.out.println("│  4. Cancel a reservation                      │");
        System.out.println("│  5. Check in                                  │");
        System.out.println("│  6. Check out                                 │");
        System.out.println("│  7. View all bookings                         │");
        System.out.println("│  8. View all rooms                            │");
        System.out.println("│  9. Room category & pricing info              │");
        System.out.println("│  0. Save & exit                               │");
        System.out.println("└───────────────────────────────────────────────┘");
    }

    

    private void searchAvailableRooms() {
        LocalDate ci = promptDate("Check-in date  (yyyy-MM-dd)");
        LocalDate co = promptDate("Check-out date (yyyy-MM-dd)");
        if (ci == null || co == null) return;
        if (!co.isAfter(ci)) { System.out.println("  ⚠  Check-out must be after check-in.\n"); return; }

        System.out.println("  Filter by category [S=Standard / D=Deluxe / U=Suite / A=All]: ");
        String cat = prompt("Category").trim().toUpperCase();

        List<Room> available = getAvailableRooms(ci, co, cat);
        if (available.isEmpty()) {
            System.out.println("  ⚠  No rooms available for the selected dates/category.\n");
            return;
        }
        long nights = ChronoUnit.DAYS.between(ci, co);
        System.out.printf("%n  ✔  Available rooms for %s → %s (%d night%s):%n%n",
                ci, co, nights, nights == 1 ? "" : "s");
        System.out.printf("  %-6s %-10s %-8s %-16s %-10s%n",
                "Room#", "Category", "Floor", "Price/Night", "Total Cost");
        System.out.println("  " + "─".repeat(60));
        for (Room r : available) {
            System.out.printf("  %-6s %-10s %-8d $%-15.2f $%.2f%n",
                    r.getRoomNumber(), r.getCategory().displayName,
                    r.getFloor(), r.getPricePerNight(),
                    r.getPricePerNight() * nights);
        }
        System.out.println();
    }

    private List<Room> getAvailableRooms(LocalDate ci, LocalDate co, String catFilter) {
        List<Room> result = new ArrayList<>();
        for (Room r : rooms.values()) {
            if (!matchesCategory(r, catFilter)) continue;
            if (!isRoomAvailableForDates(r, ci, co)) continue;
            result.add(r);
        }
        return result;
    }

    private boolean matchesCategory(Room r, String f) {
        return switch (f) {
            case "S" -> r.getCategory() == RoomCategory.STANDARD;
            case "D" -> r.getCategory() == RoomCategory.DELUXE;
            case "U" -> r.getCategory() == RoomCategory.SUITE;
            default  -> true;
        };
    }

    private boolean isRoomAvailableForDates(Room r, LocalDate ci, LocalDate co) {
        for (Booking b : bookings.values()) {
            if (b.getRoom().getRoomNumber().equals(r.getRoomNumber())
                    && b.getStatus() != BookingStatus.CANCELLED
                    && b.getStatus() != BookingStatus.CHECKED_OUT
                    && b.overlapsWith(ci, co)) {
                return false;
            }
        }
        return true;
    }

 
    private void makeReservation() {
        // Guest details
        System.out.println("  ── Guest Information ──");
        String name  = prompt("Guest name").trim();
        String phone = prompt("Phone").trim();
        String email = prompt("Email").trim();
        if (name.isEmpty()) { System.out.println("  ⚠  Name is required.\n"); return; }

        // Dates
        LocalDate ci = promptDate("Check-in  (yyyy-MM-dd)");
        LocalDate co = promptDate("Check-out (yyyy-MM-dd)");
        if (ci == null || co == null) return;
        if (!co.isAfter(ci)) { System.out.println("  ⚠  Check-out must be after check-in.\n"); return; }

        // Room
        String roomNo = prompt("Room number (or ENTER to see available)").trim().toUpperCase();
        if (roomNo.isEmpty()) { searchAvailableRooms(); roomNo = prompt("Room number").trim().toUpperCase(); }
        Room room = rooms.get(roomNo);
        if (room == null) { System.out.println("  ⚠  Room not found.\n"); return; }
        if (!isRoomAvailableForDates(room, ci, co)) {
            System.out.println("    Room is not available for selected dates.\n"); return;
        }

        long   nights    = ChronoUnit.DAYS.between(ci, co);
        double totalCost = nights * room.getPricePerNight();

        // Confirmation
        System.out.printf("%n  ── Booking Summary ──%n");
        System.out.printf("  Room     : %s (%s)%n", room.getRoomNumber(), room.getCategory().displayName);
        System.out.printf("  Dates    : %s → %s  (%d night%s)%n", ci, co, nights, nights == 1 ? "" : "s");
        System.out.printf("  Rate     : $%.2f/night%n", room.getPricePerNight());
        System.out.printf("  TOTAL    : $%.2f%n", totalCost);
        if (!confirm("Confirm booking? (y/n)")) return;

        // Payment
        Payment payment = simulatePayment(totalCost);
        if (payment == null || !payment.isSuccessful()) {
            System.out.println("    Payment failed. Reservation not created.\n"); return;
        }

        // Persist
        String guestId   = String.format("G%04d", guestCounter++);
        String bookingId = String.format("BK%05d", bookingCounter++);
        Guest  guest     = new Guest(guestId, name, phone, email);
        guests.put(guestId, guest);

        Booking booking = new Booking(bookingId, guest, room, ci, co);
        booking.setPayment(payment);
        bookings.put(bookingId, booking);

        System.out.printf("%n  ✅  Reservation confirmed!%n");
        System.out.printf("  Booking ID  : %s%n", bookingId);
        System.out.printf("  Guest       : %s%n", name);
        System.out.printf("  %s%n%n", payment);
        saveBookings();
    }



    private Payment simulatePayment(double amount) {
        System.out.println("\n  ── Payment ──");
        System.out.println("  1. Credit Card");
        System.out.println("  2. Cash");
        System.out.println("  3. Online Transfer");
        String ch = prompt("Select payment method").trim();
        PaymentMethod method = switch (ch) {
            case "1" -> PaymentMethod.CREDIT_CARD;
            case "2" -> PaymentMethod.CASH;
            case "3" -> PaymentMethod.ONLINE;
            default  -> null;
        };
        if (method == null) { System.out.println("  ⚠  Invalid payment method.\n"); return null; }

        if (method == PaymentMethod.CREDIT_CARD) {
            prompt("Card number (16 digits – masked for security)");
            prompt("Expiry (MM/YY)");
            prompt("CVV");
        } else if (method == PaymentMethod.ONLINE) {
            prompt("Transaction reference");
        }

        String payId = String.format("PAY%05d", paymentCounter++);
        Payment payment = new Payment(payId, amount, method);

        // Simulate processing
        System.out.println("  ⏳  Processing payment...");
        try { Thread.sleep(600); } catch (InterruptedException ignored) {}
        payment.process();
        System.out.printf("  ✔  Payment of $%.2f processed via %s%n", amount, method.name());
        return payment;
    }

    private void viewBooking() {
        String id = prompt("Booking ID (e.g. BK00001)").trim().toUpperCase();
        Booking b = bookings.get(id);
        if (b == null) { System.out.println("  ⚠  Booking not found.\n"); return; }
        printBookingDetails(b);
    }

    private void printBookingDetails(Booking b) {
        System.out.println("\n  ┌──────────────────────────────────────────────┐");
        System.out.printf( "  │  Booking ID  : %-29s│%n", b.getBookingId());
        System.out.printf( "  │  Status      : %-29s│%n", b.getStatus());
        System.out.printf( "  │  Created     : %-29s│%n",
                b.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        System.out.println("  ├──────────────────────────────────────────────┤");
        System.out.printf( "  │  Guest       : %-29s│%n", b.getGuest().getName());
        System.out.printf( "  │  Phone       : %-29s│%n", b.getGuest().getPhone());
        System.out.printf( "  │  Email       : %-29s│%n", b.getGuest().getEmail());
        System.out.println("  ├──────────────────────────────────────────────┤");
        System.out.printf( "  │  Room        : %-29s│%n",
                b.getRoom().getRoomNumber() + " – " + b.getRoom().getCategory().displayName);
        System.out.printf( "  │  Description : %-29s│%n", b.getRoom().getCategory().description);
        System.out.printf( "  │  Check-in    : %-29s│%n", b.getCheckIn());
        System.out.printf( "  │  Check-out   : %-29s│%n", b.getCheckOut());
        System.out.printf( "  │  Nights      : %-29s│%n", b.nights());
        System.out.printf( "  │  Rate/Night  : $%-28.2f│%n", b.getRoom().getPricePerNight());
        System.out.printf( "  │  TOTAL COST  : $%-28.2f│%n", b.totalCost());
        if (b.getPayment() != null) {
            System.out.println("  ├──────────────────────────────────────────────┤");
            System.out.printf( "  │  Payment ID  : %-29s│%n", b.getPayment().getPaymentId());
            System.out.printf( "  │  Method      : %-29s│%n", b.getPayment().getMethod());
            System.out.printf( "  │  Pay Status  : %-29s│%n",
                    b.getPayment().isSuccessful() ? "✔ Paid" : "✘ Pending");
        }
        System.out.println("  └──────────────────────────────────────────────┘\n");
    }

    
    private void cancelReservation() {
        String id = prompt("Booking ID to cancel").trim().toUpperCase();
        Booking b = bookings.get(id);
        if (b == null) { System.out.println("  ⚠  Booking not found.\n"); return; }
        if (b.getStatus() == BookingStatus.CANCELLED) {
            System.out.println("  ⚠  Already cancelled.\n"); return;
        }
        if (b.getStatus() == BookingStatus.CHECKED_OUT) {
            System.out.println("  ⚠  Cannot cancel a completed stay.\n"); return;
        }
        printBookingDetails(b);
        double refund = b.getCheckIn().isAfter(LocalDate.now().plusDays(2))
                ? b.totalCost() : b.totalCost() * 0.5;
        System.out.printf("  Refund amount: $%.2f%n", refund);
        if (!confirm("Confirm cancellation? (y/n)")) return;

        b.setStatus(BookingStatus.CANCELLED);
        b.getRoom().setAvailable(true);
        System.out.printf("  ✔  Booking %s cancelled. Refund of $%.2f will be processed.%n%n",
                id, refund);
        saveBookings();
    }


    private void checkIn() {
        String id = prompt("Booking ID").trim().toUpperCase();
        Booking b = bookings.get(id);
        if (b == null) { System.out.println("  ⚠  Booking not found.\n"); return; }
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            System.out.printf("  ⚠  Cannot check in – status is %s%n%n", b.getStatus()); return;
        }
        b.setStatus(BookingStatus.CHECKED_IN);
        b.getRoom().setAvailable(false);
        System.out.printf("  ✔  Checked in: %s | Room %s%n%n",
                b.getGuest().getName(), b.getRoom().getRoomNumber());
        saveBookings();
    }

    private void checkOut() {
        String id = prompt("Booking ID").trim().toUpperCase();
        Booking b = bookings.get(id);
        if (b == null) { System.out.println("  ⚠  Booking not found.\n"); return; }
        if (b.getStatus() != BookingStatus.CHECKED_IN) {
            System.out.printf("  ⚠  Cannot check out – status is %s%n%n", b.getStatus()); return;
        }
        b.setStatus(BookingStatus.CHECKED_OUT);
        b.getRoom().setAvailable(true);
        System.out.printf("  ✔  Checked out: %s | Room %s released%n", b.getGuest().getName(),
                b.getRoom().getRoomNumber());
        System.out.printf("  Total charged: $%.2f%n%n", b.totalCost());
        saveBookings();
    }

   

    private void viewAllBookings() {
        if (bookings.isEmpty()) { System.out.println("  ⚠  No bookings on record.\n"); return; }
        System.out.printf("%n  %-10s %-22s %-6s %-12s %-12s %-12s %-10s%n",
                "BookingID","Guest","Room","Check-in","Check-out","Total","Status");
        System.out.println("  " + "─".repeat(90));
        for (Booking b : bookings.values()) {
            System.out.printf("  %-10s %-22s %-6s %-12s %-12s $%-11.2f %-10s%n",
                    b.getBookingId(), truncate(b.getGuest().getName(), 20),
                    b.getRoom().getRoomNumber(), b.getCheckIn(), b.getCheckOut(),
                    b.totalCost(), b.getStatus());
        }

        long confirmed   = bookings.values().stream().filter(x -> x.getStatus() == BookingStatus.CONFIRMED).count();
        long checkedIn   = bookings.values().stream().filter(x -> x.getStatus() == BookingStatus.CHECKED_IN).count();
        long cancelled   = bookings.values().stream().filter(x -> x.getStatus() == BookingStatus.CANCELLED).count();
        double revenue   = bookings.values().stream()
                .filter(x -> x.getStatus() != BookingStatus.CANCELLED)
                .mapToDouble(Booking::totalCost).sum();

        System.out.println("\n  ── Summary ──");
        System.out.printf("  Total Bookings: %d  |  Confirmed: %d  |  Checked-in: %d  |  Cancelled: %d%n",
                bookings.size(), confirmed, checkedIn, cancelled);
        System.out.printf("  Total Revenue : $%.2f%n%n", revenue);
    }

    private void viewAllRooms() {
        System.out.println();
        for (RoomCategory cat : RoomCategory.values()) {
            System.out.printf("  ── %s Rooms ──%n", cat.displayName);
            rooms.values().stream()
                    .filter(r -> r.getCategory() == cat)
                    .forEach(r -> System.out.println("    " + r));
            System.out.println();
        }
    }

    private void viewRoomCategories() {
        System.out.println("\n  ┌─────────────────────────────────────────────────────┐");
        System.out.println("  │               ROOM CATEGORIES & PRICING              │");
        System.out.println("  ├─────────────────────────────────────────────────────┤");
        for (RoomCategory cat : RoomCategory.values()) {
            System.out.printf("  │  %-10s  $%.2f/night                              │%n",
                    cat.displayName, cat.pricePerNight);
            System.out.printf("  │     %s  │%n",
                    truncate(cat.description, 50));
        }
        System.out.println("  └─────────────────────────────────────────────────────┘\n");
    }

    

    private void saveBookings() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(BOOKINGS_FILE))) {
            pw.println("bookingId,guestId,guestName,guestPhone,guestEmail," +
                       "roomNumber,checkIn,checkOut,status,paymentId,paymentMethod,amount,paymentOk");
            for (Booking b : bookings.values()) {
                Payment p = b.getPayment();
                pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%.2f,%s%n",
                        b.getBookingId(), b.getGuest().getGuestId(),
                        b.getGuest().getName(), b.getGuest().getPhone(), b.getGuest().getEmail(),
                        b.getRoom().getRoomNumber(), b.getCheckIn(), b.getCheckOut(), b.getStatus(),
                        p != null ? p.getPaymentId()          : "",
                        p != null ? p.getMethod().name()      : "",
                        p != null ? p.getAmount()             : 0.0,
                        p != null ? p.isSuccessful()          : false);
            }
        } catch (IOException e) {
            System.out.println("  ⚠  Could not save bookings: " + e.getMessage());
        }
    }

    private void loadBookings() {
        File f = new File(BOOKINGS_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] t = line.split(",", -1);
                if (t.length < 13) continue;
                String bookingId = t[0], guestId = t[1], name = t[2],
                       phone = t[3], email = t[4], roomNo = t[5];
                LocalDate ci = LocalDate.parse(t[6], DATE_FMT);
                LocalDate co = LocalDate.parse(t[7], DATE_FMT);
                BookingStatus status = BookingStatus.valueOf(t[8]);
                Room room = rooms.get(roomNo);
                if (room == null) continue;
                Guest guest = guests.computeIfAbsent(guestId,
                        k -> new Guest(guestId, name, phone, email));
                Booking booking = new Booking(bookingId, guest, room, ci, co);
                booking.setStatus(status);
                if (!t[9].isEmpty()) {
                    Payment pay = new Payment(t[9], Double.parseDouble(t[11]),
                            PaymentMethod.valueOf(t[10]));
                    if (Boolean.parseBoolean(t[12])) pay.process();
                    booking.setPayment(pay);
                }
                bookings.put(bookingId, booking);
                // Update counters
                try {
                    int bNum = Integer.parseInt(bookingId.substring(2));
                    if (bNum >= bookingCounter) bookingCounter = bNum + 1;
                } catch (NumberFormatException ignored) {}
            }
            System.out.printf("  ℹ  Loaded %d booking(s) from %s%n%n", bookings.size(), BOOKINGS_FILE);
        } catch (IOException e) {
            System.out.println("  ⚠  Could not load bookings: " + e.getMessage());
        }
    }


    private String prompt(String label) {
        System.out.print("  ➤ " + label + ": ");
        return scanner.hasNextLine() ? scanner.nextLine() : "";
    }

    private LocalDate promptDate(String label) {
        try { return LocalDate.parse(prompt(label).trim(), DATE_FMT); }
        catch (DateTimeParseException e) { System.out.println("  ⚠  Invalid date format.\n"); return null; }
    }

    private boolean confirm(String label) {
        return prompt(label).trim().equalsIgnoreCase("y");
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}