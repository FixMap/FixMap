package com.fixmap.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fixmap.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderBySortOrderAsc();
}
