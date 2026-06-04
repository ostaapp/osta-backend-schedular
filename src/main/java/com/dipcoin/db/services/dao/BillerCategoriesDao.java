package com.dipcoin.db.services.dao;

import com.dipcoin.db.services.model.BillerCategories;
import java.util.List;
import java.util.Optional;

public interface BillerCategoriesDao extends GenericDao<BillerCategories> {
    
    public void save(BillerCategories category);
    public List<BillerCategories> findAll();
    public Optional<BillerCategories> findByCategoryId(String id);
    public Optional<BillerCategories> findByName(String name);
}
