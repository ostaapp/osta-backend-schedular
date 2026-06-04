package com.dipcoin.db.services;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.dipcoin.db.services.model.BillerCategories;

public interface BillerCategoriesRepository extends JpaRepository<BillerCategories, Long> {

	    @Query("SELECT bc.categoryId FROM BillerCategories bc WHERE bc.isActive = 1 AND (bc.name = 'Mobile Prepaid' OR bc.name LIKE '%Mobile Prepaid%')")
	    List<String> findMobilePrepaidCategoryIds();
	}

