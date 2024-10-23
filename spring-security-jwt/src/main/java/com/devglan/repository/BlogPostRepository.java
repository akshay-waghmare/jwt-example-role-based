package com.devglan.repository; // Adjust package according to your structure

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.devglan.model.BlogPost;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    Optional<BlogPost> findByLink(String link); // Custom method to find blog post by link
}
