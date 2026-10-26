import java.time.*;
import java.util.*;

enum VehicleType { BIKE, CAR, TRUCK }
enum SpotType { SMALL, MEDIUM, LARGE }

class Vehicle {
    final String plate; final VehicleType type;
    Vehicle(String plate, VehicleType type) { this.plate = plate; this.type = type; }
}

class ParkingSpot {
    final String id; final SpotType type; boolean occupied;
    ParkingSpot(String id, SpotType type) { this.id = id; this.type = type; }

    boolean canFit(VehicleType v) {
        switch (v) {
            case BIKE:  return true;
            case CAR:   return type != SpotType.SMALL;
            default:    return type == SpotType.LARGE;   // TRUCK
        }
    }
}

class Ticket {
    final String id; final Vehicle vehicle; final ParkingSpot spot; final Instant entry;
    Ticket(String id, Vehicle vehicle, ParkingSpot spot) {
        this.id = id; this.vehicle = vehicle; this.spot = spot; this.entry = Instant.now();
    }
}

class ParkingLot {
    private final List<ParkingSpot> spots;
    private final Map<String, Ticket> active = new HashMap<>();
    private final Map<VehicleType, Double> ratePerHour =
        Map.of(VehicleType.BIKE, 10.0, VehicleType.CAR, 20.0, VehicleType.TRUCK, 40.0);
    private int nextId = 1;

    ParkingLot(List<ParkingSpot> spots) { this.spots = spots; }

    synchronized Ticket park(Vehicle v) {
        for (ParkingSpot s : spots) {
            if (!s.occupied && s.canFit(v.type)) {
                s.occupied = true;
                Ticket t = new Ticket("T" + nextId++, v, s);
                active.put(t.id, t);
                return t;
            }
        }
        throw new IllegalStateException("No spot available for " + v.type);
    }

    synchronized double unpark(String ticketId) {
        Ticket t = active.remove(ticketId);
        if (t == null) throw new IllegalArgumentException("Invalid ticket: " + ticketId);
        t.spot.occupied = false;
        long minutes = Duration.between(t.entry, Instant.now()).toMinutes();
        long hours = Math.max(1, (long) Math.ceil(minutes / 60.0));
        return hours * ratePerHour.get(t.vehicle.type);
    }
}

public class Main {
    public static void main(String[] args) {
        ParkingLot lot = new ParkingLot(List.of(
            new ParkingSpot("S1", SpotType.SMALL),
            new ParkingSpot("S2", SpotType.MEDIUM),
            new ParkingSpot("S3", SpotType.LARGE)));

        Ticket t = lot.park(new Vehicle("KA01AB1234", VehicleType.CAR));
        System.out.println("Parked in " + t.spot.id);          // S2
        System.out.println("Fee: " + lot.unpark(t.id));          // 20.0
        try { lot.unpark(t.id); } catch (IllegalArgumentException e) {
            System.out.println("Rejected: " + e.getMessage());
        }
    }
}