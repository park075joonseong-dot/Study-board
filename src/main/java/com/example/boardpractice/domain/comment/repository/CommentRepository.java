package com.example.boardpractice.domain.comment.repository;

import com.example.boardpractice.domain.comment.entity.Comment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByPost_PostIdOrderByCommentIdAsc(Long postId);

    Optional<Comment> findByCommentIdAndPost_PostId(Long commentId, Long postId);

    void deleteByPost_PostId(Long postId);
}
