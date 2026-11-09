package com.fixmap.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fixmap.domain.Place;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findByFloorIdOrderByIdAsc(Long floorId);
}
