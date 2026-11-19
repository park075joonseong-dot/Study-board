package com.example.boardpractice.domain.post.service;

import com.example.boardpractice.domain.comment.repository.CommentRepository;
import com.example.boardpractice.domain.post.dto.PostResponse;
import com.example.boardpractice.domain.post.entity.Post;
import com.example.boardpractice.domain.post.repository.PostRepository;
import com.example.boardpractice.global.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public PostService(PostRepository postRepository, CommentRepository commentRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public PostResponse createPost(String title, String content) {
        Post post = new Post(title, content);
        Post savedPost = postRepository.save(post);
        return new PostResponse(savedPost);
    }

    public List<PostResponse> getPosts() {
        List<Post> posts = postRepository.findAll(Sort.by(Sort.Direction.DESC, "postId"));
        List<PostResponse> responses = new ArrayList<>();

        for (Post post : posts) {
            responses.add(new PostResponse(post));
        }

        return responses;
    }

    public PostResponse getPost(Long postId) {
        return new PostResponse(findPost(postId));
    }

    @Transactional
    public PostResponse updatePost(Long postId, String title, String content) {
        Post post = findPost(postId);
        post.update(title, content);
        // 조회한 엔티티의 변경 사항은 트랜잭션이 끝날 때 DB에 반영된다.
        return new PostResponse(post);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = findPost(postId);
        commentRepository.deleteByPost_PostId(postId);
        postRepository.delete(post);
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
    }
}
