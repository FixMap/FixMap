package com.fixmap.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fixmap.domain.Building;

public interface BuildingRepository extends JpaRepository<Building, Long> {

    List<Building> findAllByOrderBySortOrderAsc();
}
