package com.fixmap.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fixmap.domain.BuildingFloor;

public interface BuildingFloorRepository extends JpaRepository<BuildingFloor, Long> {

    List<BuildingFloor> findAllByOrderByFloorLevelAsc();
}
