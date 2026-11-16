package co.uniquindio.crackscolombia.cracks_colombia_api.moderation.dto.response;

import java.util.List;

public record ModerationQueuePage(
        List<ModerationVideoResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
