package co.uniquindio.crackscolombia.cracks_colombia_api.search.dto.response;

import java.util.List;

public record AthleteSearchPage(
        List<AthleteSearchResult> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String message
) {
}
