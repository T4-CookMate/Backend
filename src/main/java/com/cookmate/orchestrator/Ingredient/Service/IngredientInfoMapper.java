package com.cookmate.orchestrator.Ingredient.Service;

import com.cookmate.orchestrator.Ingredient.Dto.IngredientInfoDto;
import com.cookmate.orchestrator.Ingredient.Dto.IngredientInfoRequest;
import com.cookmate.orchestrator.Ingredient.Entity.IngredientStatus;
import com.cookmate.orchestrator.Ingredient.Repository.IngredientStatusRepository;
import com.cookmate.orchestrator.Recipe.Repository.LocationRepository;
import com.cookmate.orchestrator.Recipe.Entity.Location;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngredientInfoMapper {

    private final IngredientStatusRepository ingredientStatusRepository;
    private final LocationRepository locationRepository;

    public IngredientInfoDto toDto(IngredientInfoRequest req) {

        IngredientStatus status = ingredientStatusRepository.findByCode(req.status())
                .orElseThrow(() -> new IllegalArgumentException("Unknown status: " + req.status()));

        Location location = null;
        if (req.location() != null) {
            location = locationRepository.findByName(req.location())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown location: " + req.location()));
        }

        return new IngredientInfoDto(status, location);
    }

    public Map<String, IngredientInfoDto> toDtoMap(Map<String, IngredientInfoRequest> requestMap) {
        return requestMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> toDto(e.getValue())
                ));
    }
}