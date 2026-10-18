package co.uniquindio.crackscolombia.cracks_colombia_api.search.service;

import co.uniquindio.crackscolombia.cracks_colombia_api.search.dto.response.AthleteSearchPage;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.dto.response.AthleteSearchResult;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.entity.AthleteProfileStatus;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.repository.AthleteProfileRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception.ApiException;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class AthleteSearchService {

    private static final int MAX_AGE = 120;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String EMPTY_RESULTS_MESSAGE = "No se encontraron deportistas con los filtros aplicados.";

    private final AthleteProfileRepository athleteProfileRepository;
    private final Clock clock;

    public AthleteSearchService(AthleteProfileRepository athleteProfileRepository, Clock clock) {
        this.athleteProfileRepository = athleteProfileRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AthleteSearchPage search(String sport, String position, Integer minAge, Integer maxAge,
                                    String region, int page, int size) {
        validateFilters(minAge, maxAge, page, size);

        LocalDate today = LocalDate.now(clock);
        LocalDate earliestBirthDate = maxAge == null
                ? null
                : today.minusYears(maxAge + 1L).plusDays(1);
        LocalDate latestBirthDate = minAge == null ? null : today.minusYears(minAge);

        Page<AthleteSearchResult> results = athleteProfileRepository.searchApprovedProfiles(
                        AthleteProfileStatus.APROBADO,
                        UserRole.DEPORTISTA,
                        normalize(sport),
                        normalize(position),
                        normalize(region),
                        earliestBirthDate,
                        latestBirthDate,
                        PageRequest.of(page, size))
                .map(candidate -> new AthleteSearchResult(
                        candidate.athleteId(),
                        candidate.photoStorageKey() == null ? null
                                : "/athletes/" + candidate.athleteId() + "/photo",
                        candidate.fullName(),
                        candidate.sport(),
                        candidate.position(),
                        java.time.Period.between(candidate.birthDate(), today).getYears(),
                        candidate.region()));

        return new AthleteSearchPage(
                results.getContent(),
                results.getNumber(),
                results.getSize(),
                results.getTotalElements(),
                results.getTotalPages(),
                results.isFirst(),
                results.isLast(),
                results.getTotalElements() == 0 ? EMPTY_RESULTS_MESSAGE : null);
    }

    private void validateFilters(Integer minAge, Integer maxAge, int page, int size) {
        if (minAge != null && (minAge < 0 || minAge > MAX_AGE)) {
            throw ApiException.invalid("La edad mínima debe estar entre 0 y " + MAX_AGE + " años.");
        }
        if (maxAge != null && (maxAge < 0 || maxAge > MAX_AGE)) {
            throw ApiException.invalid("La edad máxima debe estar entre 0 y " + MAX_AGE + " años.");
        }
        if (minAge != null && maxAge != null && minAge > maxAge) {
            throw ApiException.invalid("La edad mínima no puede superar la edad máxima.");
        }
        if (page < 0) {
            throw ApiException.invalid("La página debe ser mayor o igual a cero.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw ApiException.invalid("El tamaño de página debe estar entre 1 y " + MAX_PAGE_SIZE + ".");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
