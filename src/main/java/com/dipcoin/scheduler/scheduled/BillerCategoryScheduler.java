package com.dipcoin.scheduler.scheduled;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dipcoin.api.model.BillerCategoriesResponse;
import com.dipcoin.api.model.BillerCategoryItem;
import com.dipcoin.db.service.client.FinacusHttpClient;
import com.dipcoin.db.services.BillerCategoriesDBService;
import com.dipcoin.db.services.model.BillerCategories;
import lombok.RequiredArgsConstructor;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class BillerCategoryScheduler {
	
	@Autowired
	private FinacusHttpClient finacusHttpClient;
	
	@Autowired
	private BillerCategoriesDBService billerCategoriesDBService;
	
	private static final Logger LOG = LogManager.getLogger(BillerCategoryScheduler.class);
	
//	@Scheduled(cron = "0 0 * * * ?")
	@Transactional
	public void syncBillerCategories() {
		
		try {
			
			LOG.info("Fetching ALL biller categories (no coverage filter)");
			
			BillerCategoriesResponse allCategories = finacusHttpClient.fetchBillerCategories();
			
			LOG.info("Total categories fetched: " + allCategories.getResponse().size());
			
			processBillerCategories(allCategories);
			
			
		} catch (Exception e) {
			LOG.error("Error during Biller Category Sync: {}", e.getMessage());
		}
	}

	private void processBillerCategories(BillerCategoriesResponse externalCategories) {

	    if (externalCategories.getResponse() == null || externalCategories.getResponse().isEmpty()) {
	        LOG.info("No categories received to process");
	        return;
	    }

	    int insertedCount = 0;
	    int updatedCount = 0;

	    for (BillerCategoryItem externalCategory : externalCategories.getResponse()) {

	        // Check if exists in DB
	        Optional<BillerCategories> existing =
	                billerCategoriesDBService.findByCategoryId(externalCategory.getId());

	        if (existing.isPresent()) {
	            // === UPDATE ===
	            BillerCategories oldCategory = existing.get();

	            oldCategory.setName(externalCategory.getName());
	            oldCategory.setCategoryId(externalCategory.getId());

	            billerCategoriesDBService.update(oldCategory);
	            updatedCount++;

	            LOG.info("Updated category: " + externalCategory.getId());
	        } else {
	            // === INSERT ===
	        	BillerCategories oldCategory = new BillerCategories();
	        	oldCategory.setCategoryId(externalCategory.getId());
	        	oldCategory.setName(externalCategory.getName());
	            billerCategoriesDBService.save(oldCategory);
	            insertedCount++;

	            LOG.info("Inserted new category: " + externalCategory.getId());
	        }
	    }

	    LOG.info("BillerCategory Sync Summary - Total Inserted: {} | Total Updated: {}", insertedCount, updatedCount);
	}


}