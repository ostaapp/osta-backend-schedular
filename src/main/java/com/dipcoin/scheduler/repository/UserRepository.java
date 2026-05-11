package com.dipcoin.scheduler.repository;

import com.dipcoin.scheduler.model.User;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {

  List<User> findByIdIn(Collection<Integer> ids);
}
