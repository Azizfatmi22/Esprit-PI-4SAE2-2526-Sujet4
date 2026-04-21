package tn.esprit.microservice.reclamation.config;


import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class SlaConfig {

    // Délais en MINUTES (modifiables ici)
    private final Map<Integer, Integer> slaMinutes = Map.of(
            1, 10,  // Priorité HAUTE  → 10 minutes
            2, 12,  // Priorité MOYENNE → 12 minutes
            3, 12   // Priorité BASSE  → 12 minutes
    );

    public int getSlaMinutes(int priority) {
        return slaMinutes.getOrDefault(priority, 12);
    }

    public String getPriorityLabel(int priority) {
        return switch (priority) {
            case 1 -> "HAUTE";
            case 2 -> "MOYENNE";
            case 3 -> "BASSE";
            default -> "INCONNUE";
        };
    }
}