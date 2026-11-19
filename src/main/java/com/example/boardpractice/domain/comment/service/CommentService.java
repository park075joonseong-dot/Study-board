package com.example.boardpractice.domain.comment.service;

import com.example.boardpractice.domain.comment.dto.CommentResponse;
import com.example.boardpractice.domain.comment.entity.Comment;
import com.example.boardpractice.domain.comment.repository.CommentRepository;
import com.example.boardpractice.domain.post.entity.Post;
import com.example.boardpractice.domain.post.repository.PostRepository;
import com.example.boardpractice.global.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public CommentResponse createComment(Long postId, String content) {
        Post post = findPost(postId);
        Comment comment = new Comment(post, content);
        return new CommentResponse(commentRepository.save(comment));
    }

    public List<CommentResponse> getComments(Long postId) {
        findPost(postId);
        List<Comment> comments = commentRepository.findAllByPost_PostIdOrderByCommentIdAsc(postId);
        List<CommentResponse> responses = new ArrayList<>();

        for (Comment comment : comments) {
            responses.add(new CommentResponse(comment));
        }

        return responses;
    }

    public CommentResponse getComment(Long postId, Long commentId) {
        return new CommentResponse(findComment(postId, commentId));
    }

    @Transactional
    public CommentResponse updateComment(Long postId, Long commentId, String content) {
        Comment comment = findComment(postId, commentId);
        comment.update(content);
        return new CommentResponse(comment);
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId) {
        Comment comment = findComment(postId, commentId);
        commentRepository.delete(comment);
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
    }

    private Comment findComment(Long postId, Long commentId) {
        return commentRepository.findByCommentIdAndPost_PostId(commentId, postId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 게시글의 댓글을 찾을 수 없습니다."));
    }
}
