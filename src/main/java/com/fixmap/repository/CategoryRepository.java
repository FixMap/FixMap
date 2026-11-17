package com.fixmap.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fixmap.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
