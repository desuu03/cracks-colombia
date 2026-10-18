package co.uniquindio.crackscolombia.cracks_colombia_api.user.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Closed lists for sport, position and region. They match the options of the
 * frontend (front/src/constants/catalog.js) so search filters find what athletes
 * entered.
 */
public final class SportsCatalog {

    public static final Map<String, List<String>> POSITIONS = Map.of(
            "Fútbol", List.of("Portero", "Defensa central", "Lateral", "Volante", "Extremo", "Delantero"),
            "Baloncesto", List.of("Base", "Escolta", "Alero", "Ala-pívot", "Pívot"),
            "Voleibol", List.of("Armador", "Opuesto", "Central", "Punta", "Líbero"),
            "Atletismo", List.of("Velocidad 100 m", "Velocidad 200 m", "Medio fondo 800 m", "Fondo 5.000 m",
                    "Salto largo", "Lanzamiento de jabalina"));

    public static final Set<String> REGIONS = Set.of(
            "Amazonas", "Antioquia", "Arauca", "Atlántico", "Bogotá D.C.", "Bolívar", "Boyacá", "Caldas",
            "Caquetá", "Casanare", "Cauca", "Cesar", "Chocó", "Córdoba", "Cundinamarca", "Guainía",
            "Guaviare", "Huila", "La Guajira", "Magdalena", "Meta", "Nariño", "Norte de Santander",
            "Putumayo", "Quindío", "Risaralda", "San Andrés y Providencia", "Santander", "Sucre",
            "Tolima", "Valle del Cauca", "Vaupés", "Vichada");

    private SportsCatalog() {
    }

    public static boolean isSport(String sport) {
        return sport != null && POSITIONS.containsKey(sport);
    }

    public static boolean isPosition(String sport, String position) {
        return isSport(sport) && POSITIONS.get(sport).contains(position);
    }

    public static boolean isRegion(String region) {
        return region != null && REGIONS.contains(region);
    }
}
