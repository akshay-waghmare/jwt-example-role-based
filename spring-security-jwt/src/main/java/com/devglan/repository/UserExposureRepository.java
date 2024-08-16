package com.devglan.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devglan.model.LiveMatch;
import com.devglan.model.User;
import com.devglan.model.UserExposure;

public interface UserExposureRepository extends JpaRepository<UserExposure, Long> {
    Optional<UserExposure> findByUserAndMatch(User user, LiveMatch match);
    Optional<List<UserExposure>> findByUser(User user);
    Optional<List<UserExposure>> findByUserAndSoftDeletedFalse(User user);
    Optional<UserExposure> findByUserAndMatchAndSoftDeletedFalse(User user, LiveMatch match);
	List<UserExposure> findByMatchAndSoftDeletedFalse(LiveMatch match);
	List<UserExposure> findByMatch(LiveMatch match);


}