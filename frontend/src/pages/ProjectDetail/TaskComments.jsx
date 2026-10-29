import { useCallback, useEffect, useState } from 'react';
import { Check, Pencil, Plus, Trash2, X } from 'lucide-react';
import { commentService } from '../../services/commentService';
import { useToast } from '../../context/ToastContext';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { EmptyState } from '../../components/ui/EmptyState';
import { ErrorState } from '../../components/ui/ErrorState';
import { Skeleton } from '../../components/ui/Skeleton';
import { formatDateTime } from '../../utils/date';
import { parseApiError } from '../../utils/apiError';
import './TaskComments.css';

export default function TaskComments({
  taskId,
  workspaceMembers = [],
  currentUserId,
  workspaceRole,
  isArchived = false,
  focusCommentId
}) {
  const { success, error: showErrorToast } = useToast();

  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [newContent, setNewContent] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [editingContent, setEditingContent] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const reload = useCallback(() => {
    setRefreshKey((value) => value + 1);
  }, []);

  useEffect(() => {
    let active = true;

    async function loadComments() {
      setLoading(true);
      setLoadError(null);

      try {
        const result = await commentService.getByTask(taskId);
        if (active) {
          setComments(Array.isArray(result) ? result : []);
        }
      } catch (err) {
        if (active) {
          setLoadError(parseApiError(err));
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadComments();

    return () => {
      active = false;
    };
  }, [taskId, refreshKey]);

  useEffect(() => {
    if (!focusCommentId || loading || loadError) return;

    const timer = window.setTimeout(() => {
      document
        .getElementById(`task-comment-${focusCommentId}`)
        ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, 50);

    return () => window.clearTimeout(timer);
  }, [focusCommentId, loading, loadError, comments]);

  const getAuthorName = (authorId) => {
    const member = workspaceMembers.find(
      (item) => Number(item.userId) === Number(authorId)
    );

    if (member?.name) return member.name;
    if (Number(authorId) === Number(currentUserId)) return 'You';
    return `User ${authorId}`;
  };

  const canDeleteComment = (comment) => {
    const isAuthor = Number(comment.authorId) === Number(currentUserId);
    const isAdminOrOwner = workspaceRole === 'OWNER' || workspaceRole === 'ADMIN';
    return !isArchived && (isAuthor || isAdminOrOwner);
  };

  const handleCreate = async (event) => {
    event.preventDefault();
    const content = newContent.trim();

    if (!content || isArchived) return;

    setSubmitting(true);
    try {
      await commentService.create(taskId, content);
      setNewContent('');
      success('Comment added');
      reload();
    } catch (err) {
      const parsed = parseApiError(err);
      showErrorToast(parsed.message || 'Failed to add comment');
    } finally {
      setSubmitting(false);
    }
  };

  const beginEdit = (comment) => {
    setEditingId(comment.id);
    setEditingContent(comment.content);
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditingContent('');
  };

  const handleUpdate = async (commentId) => {
    const content = editingContent.trim();

    if (!content || isArchived) return;

    setSubmitting(true);
    try {
      await commentService.update(commentId, content);
      setEditingId(null);
      setEditingContent('');
      success('Comment updated');
      reload();
    } catch (err) {
      const parsed = parseApiError(err);
      showErrorToast(parsed.message || 'Failed to update comment');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;

    setDeleting(true);
    try {
      await commentService.delete(deleteTarget.id);
      setDeleteTarget(null);
      success('Comment deleted');
      reload();
    } catch (err) {
      const parsed = parseApiError(err);
      showErrorToast(parsed.message || 'Failed to delete comment');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <section className="task-comments" aria-label="Task comments">
      {loadError && !loading && (
        <ErrorState
          title="Failed to load comments"
          message={loadError.message || 'Please try again.'}
          onRetry={reload}
        />
      )}

      {loading && (
        <div className="task-comments__loading">
          <Skeleton height="72px" />
          <Skeleton height="72px" />
        </div>
      )}

      {!loading && !loadError && comments.length === 0 && (
        <EmptyState
          icon={Plus}
          title="No comments yet"
          description="Start the conversation by adding a comment."
        />
      )}

      {!loading && !loadError && comments.length > 0 && (
        <div className="task-comments__list">
          {comments.map((comment) => {
            const isAuthor =
              Number(comment.authorId) === Number(currentUserId);
            const canEditComment = isAuthor && !isArchived;
            const canDelete = canDeleteComment(comment);
            const isEditing = editingId === comment.id;
            const isFocused =
              Number(comment.id) === Number(focusCommentId);

            return (
              <article
                id={`task-comment-${comment.id}`}
                className={`task-comment${isFocused ? ' task-comment--focused' : ''}`}
                key={comment.id}
                tabIndex={-1}
              >
                <div className="task-comment__header">
                  <div>
                    <strong className="task-comment__author">
                      {getAuthorName(comment.authorId)}
                    </strong>
                    <time
                      className="task-comment__date"
                      dateTime={comment.updatedAt || comment.createdAt}
                    >
                      {formatDateTime(comment.updatedAt || comment.createdAt)}
                      {comment.updatedAt !== comment.createdAt ? ' · edited' : ''}
                    </time>
                  </div>

                  {!isEditing && (canEditComment || canDelete) && (
                    <div className="task-comment__actions">
                      {canEditComment && (
                        <button
                          type="button"
                          className="icon-button"
                          aria-label="Edit comment"
                          onClick={() => beginEdit(comment)}
                        >
                          <Pencil size={15} />
                        </button>
                      )}
                      {canDelete && (
                        <button
                          type="button"
                          className="icon-button task-comment__delete"
                          aria-label="Delete comment"
                          onClick={() => setDeleteTarget(comment)}
                        >
                          <Trash2 size={15} />
                        </button>
                      )}
                    </div>
                  )}
                </div>

                {isEditing ? (
                  <div className="task-comment__edit">
                    <textarea
                      value={editingContent}
                      onChange={(event) => setEditingContent(event.target.value)}
                      maxLength={2000}
                      rows={3}
                      aria-label="Edit comment content"
                      disabled={submitting}
                    />
                    <div className="task-comment__edit-actions">
                      <span>{editingContent.length}/2000</span>
                      <button
                        type="button"
                        className="button-secondary"
                        onClick={cancelEdit}
                        disabled={submitting}
                      >
                        <X size={15} />
                        Cancel
                      </button>
                      <button
                        type="button"
                        className="button-primary"
                        onClick={() => handleUpdate(comment.id)}
                        disabled={submitting || !editingContent.trim()}
                      >
                        <Check size={15} />
                        Save
                      </button>
                    </div>
                  </div>
                ) : (
                  <p className="task-comment__content">{comment.content}</p>
                )}
              </article>
            );
          })}
        </div>
      )}

      <form className="task-comments__composer" onSubmit={handleCreate}>
        {isArchived && (
          <p className="task-comments__archived-note">
            Comments can’t be added or changed while this project is archived.
          </p>
        )}

        <label htmlFor="new-task-comment">Add a comment</label>
        <textarea
          id="new-task-comment"
          value={newContent}
          onChange={(event) => setNewContent(event.target.value)}
          maxLength={2000}
          rows={3}
          placeholder="Write a comment..."
          disabled={isArchived || submitting}
        />

        <div className="task-comments__composer-footer">
          <span>{newContent.length}/2000</span>
          <button
            type="submit"
            className="button-primary"
            disabled={isArchived || submitting || !newContent.trim()}
          >
            <Plus size={16} />
            {submitting ? 'Saving…' : 'Add comment'}
          </button>
        </div>
      </form>

      <ConfirmDialog
        isOpen={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete comment"
        message="Delete this comment permanently?"
        confirmText="Delete"
        isDestructive
        isLoading={deleting}
      />
    </section>
  );
}