package com.teamcanvas.comment;

import com.teamcanvas.comment.dto.CommentResponse;
import com.teamcanvas.comment.dto.CreateCommentRequest;
import com.teamcanvas.comment.dto.UpdateCommentRequest;
import com.teamcanvas.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TaskCommentController {

    private final TaskCommentService commentService;

    public TaskCommentController(TaskCommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/tasks/{taskId}/comments")
    public List<CommentResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long taskId) {
        return commentService.list(taskId, principal.getId());
    }

    @PostMapping("/tasks/{taskId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long taskId,
            @Valid @RequestBody CreateCommentRequest request) {
        return commentService.create(taskId, principal.getId(), request);
    }

    @PatchMapping("/comments/{commentId}")
    public CommentResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        return commentService.update(commentId, principal.getId(), request);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId) {
        commentService.delete(commentId, principal.getId());
    }
}