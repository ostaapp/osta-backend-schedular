package com.dipcoin.db.services;

import java.util.List;
import java.util.Optional;

import com.dipcoin.api.model.BillerCategoryItem;
import com.dipcoin.db.services.model.BillerCategories;

public abstract class BillerCategoriesDBService {

	public abstract List<BillerCategories> getAllBillerCategories();

	public abstract Optional<BillerCategories> findByCategoryId(String id);

	public abstract void update(BillerCategories oldCategory);

	public abstract void save(BillerCategories externalCategory);
}
