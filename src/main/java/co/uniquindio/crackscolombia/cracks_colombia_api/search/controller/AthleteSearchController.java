package co.uniquindio.crackscolombia.cracks_colombia_api.search.controller;

import co.uniquindio.crackscolombia.cracks_colombia_api.search.dto.response.AthleteSearchPage;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.service.AthleteSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AthleteSearchController {

    private final AthleteSearchService athleteSearchService;

    public AthleteSearchController(AthleteSearchService athleteSearchService) {
        this.athleteSearchService = athleteSearchService;
    }

    @GetMapping("/athletes/search")
    public AthleteSearchPage search(
            @RequestParam(required = false) String sport,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return athleteSearchService.search(sport, position, minAge, maxAge, region, page, size);
    }
}
