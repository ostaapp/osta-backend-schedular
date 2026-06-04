package com.dipcoin.db.services;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.dipcoin.db.services.dao.BillerCategoriesDao;
import com.dipcoin.db.services.model.BillerCategories;

@Component("billerCategoriesDBService")
public class BillerCategoriesDBServiceImpl extends BillerCategoriesDBService {
    @Autowired
    private BillerCategoriesDao billerCategoriesDao;

	@Override
	@Transactional(readOnly = true)
	public List<BillerCategories> getAllBillerCategories() {
		return billerCategoriesDao.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<BillerCategories> findByCategoryId(String id) {
		return billerCategoriesDao.findByCategoryId(id);
	}

	@Override
	@Transactional
	public void update(BillerCategories oldCategory) {
		billerCategoriesDao.update(oldCategory);
	}

	@Override
	@Transactional
	public void save(BillerCategories externalCategory) {
		billerCategoriesDao.save(externalCategory);
	}
//    @Override
//    @Transactional
//    public void addBillerCategory(BillerCategories category) {
//        billerCategoriesDao.saveBillerCategory(category);
//    }
}
