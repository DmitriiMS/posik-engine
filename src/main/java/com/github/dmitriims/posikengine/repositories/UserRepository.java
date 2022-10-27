package com.github.dmitriims.posikengine.repositories;

import com.github.dmitriims.posikengine.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    User findByUsername(String username);
}
