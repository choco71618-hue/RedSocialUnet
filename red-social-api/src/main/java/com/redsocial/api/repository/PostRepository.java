package com.redsocial.api.repository;

import com.redsocial.api.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByAuthor_IdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    long countByAuthor_Id(Long authorId);

    @Query("""
            select p from Post p
            where p.author.id = :userId
               or p.author.id in (select f.following.id from Follow f where f.follower.id = :userId)
            order by p.createdAt desc
            """)
    Page<Post> findFeedForUser(@Param("userId") Long userId, Pageable pageable);
}
