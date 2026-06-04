package com.dipcoin.db.services.dao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;
import com.dipcoin.db.services.model.BillerCategories;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Component("billerCategoriesDao")
public class BillerCategoriesDaoJpa extends GenericDaoImpl<BillerCategories> implements BillerCategoriesDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void save(BillerCategories category) {
        // Check if category already exists by categoryId
        BillerCategories existing = entityManager.createQuery(
                "SELECT b FROM BillerCategories b WHERE b.categoryId = :id", BillerCategories.class)
                .setParameter("id", category.getCategoryId())
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (existing != null) {
            // Update existing record
            existing.setName(category.getName());
            entityManager.merge(existing);
        } else {
            // Insert new record
            entityManager.persist(category);
        }
    }

    @Override
    public List<BillerCategories> findAll() {
        return entityManager.createQuery("SELECT b FROM BillerCategories b", BillerCategories.class)
                .getResultList();
    }

    @Override
    @Transactional
    public Optional<BillerCategories> findByCategoryId(String id) {
        BillerCategories category = entityManager.createQuery(
                "SELECT b FROM BillerCategories b WHERE b.categoryId = :id", BillerCategories.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .orElse(null);

        return Optional.ofNullable(category);
    }

    @Override
    @Transactional
    public Optional<BillerCategories> findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }

        BillerCategories category = entityManager.createQuery(
                "SELECT b FROM BillerCategories b WHERE lower(trim(b.name)) = :name", BillerCategories.class)
                .setParameter("name", name.trim().toLowerCase())
                .getResultStream()
                .findFirst()
                .orElse(null);

        return Optional.ofNullable(category);
    }

    @Override
    @Transactional
    public BillerCategories update(BillerCategories category) {
        return entityManager.merge(category);
    }
}
