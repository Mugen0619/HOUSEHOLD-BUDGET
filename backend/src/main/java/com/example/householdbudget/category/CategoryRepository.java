package com.example.householdbudget.category;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByType(CategoryType type);

    boolean existsByNameAndType(String name, CategoryType type);

    boolean existsByNameAndTypeAndIdNot(String name, CategoryType type, Long id);
}
