package com.Soo_Shinsa.image.repository;

import com.Soo_Shinsa.image.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
}
