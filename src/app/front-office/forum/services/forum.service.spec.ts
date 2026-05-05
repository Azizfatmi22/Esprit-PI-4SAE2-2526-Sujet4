import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ForumService } from './forum.service';
import { PostResponse, PostRequest, ContentStatus } from '../models/forum.model';
import { CommentRequest, CommentResponse } from '../models/comment.model';
import { ReactionRequest, ReactionType } from '../models/reaction.model';
import { environment } from '../../environment/envirement';

describe('ForumService', () => {
  let service: ForumService;
  let httpMock: HttpTestingController;

  const baseUrl = `${environment.apiGatewayUrl}/api`;

  const mockPost: PostResponse = {
    id: 1,
    userId: 'user1',
    formationId: 10,
    title: 'Test Post',
    content: 'Test content',
    category: 'GENERAL',
    status: 'APPROVED',
    createdAt: '2025-01-01T00:00:00Z',
    reviewedByAdmin: false
  };

  const mockComment: CommentResponse = {
    id: 10,
    userId: 'user1',
    content: 'Great post!',
    status: 'APPROVED',
    isBestAnswer: false,
    reviewedByAdmin: false,
    createdAt: '2025-01-01T00:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ForumService]
    });
    service = TestBed.inject(ForumService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // --- Posts ---

  it('getForums() should GET all posts without filters', () => {
    service.getForums().subscribe(posts => {
      expect(posts.length).toBe(1);
      expect(posts[0].title).toBe('Test Post');
    });
    const req = httpMock.expectOne(`${baseUrl}/posts`);
    expect(req.request.method).toBe('GET');
    req.flush([mockPost]);
  });

  it('getForums() should include userId param when provided', () => {
    service.getForums('user1').subscribe();
    const req = httpMock.expectOne(r => r.url === `${baseUrl}/posts` && r.params.get('userId') === 'user1');
    expect(req.request.method).toBe('GET');
    req.flush([mockPost]);
  });

  it('getForums() should include category param when provided', () => {
    service.getForums(undefined, 'TECHNOLOGY').subscribe();
    const req = httpMock.expectOne(r => r.url === `${baseUrl}/posts` && r.params.get('category') === 'TECHNOLOGY');
    expect(req.request.method).toBe('GET');
    req.flush([mockPost]);
  });

  it('getForumById() should GET a single post by id', () => {
    service.getForumById(1).subscribe(post => {
      expect(post.id).toBe(1);
      expect(post.title).toBe('Test Post');
    });
    const req = httpMock.expectOne(`${baseUrl}/posts/1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockPost);
  });

  it('createForum() should POST a new post', () => {
    const newPost: PostRequest = {
      userId: 'user1',
      formationId: 10,
      title: 'New Post',
      content: 'New content',
      category: 'GENERAL'
    };
    service.createForum(newPost).subscribe(post => {
      expect(post.title).toBe('Test Post');
    });
    const req = httpMock.expectOne(`${baseUrl}/posts`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newPost);
    req.flush(mockPost);
  });

  it('updateForum() should PUT updated post data', () => {
    const update: PostRequest = { userId: 'user1', formationId: 10, title: 'Updated', content: 'Updated content' };
    service.updateForum(1, update).subscribe(post => {
      expect(post.id).toBe(1);
    });
    const req = httpMock.expectOne(`${baseUrl}/posts/1`);
    expect(req.request.method).toBe('PUT');
    req.flush(mockPost);
  });

  it('deleteForum() should DELETE a post', () => {
    service.deleteForum(1).subscribe();
    const req = httpMock.expectOne(`${baseUrl}/posts/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  // --- Moderation ---

  it('getPendingPosts() should GET pending posts', () => {
    service.getPendingPosts().subscribe(posts => {
      expect(posts.length).toBe(1);
    });
    const req = httpMock.expectOne(`${baseUrl}/moderation/posts/pending`);
    expect(req.request.method).toBe('GET');
    req.flush([mockPost]);
  });

  it('updatePostStatus() should PUT status with query param', () => {
    const status: ContentStatus = 'APPROVED';
    service.updatePostStatus(1, status).subscribe();
    const req = httpMock.expectOne(r =>
      r.url === `${baseUrl}/moderation/posts/1/status` && r.params.get('status') === 'APPROVED'
    );
    expect(req.request.method).toBe('PUT');
    req.flush(mockPost);
  });

  // --- Comments ---

  it('getComments() should GET comments for a post', () => {
    service.getComments(1).subscribe(comments => {
      expect(comments.length).toBe(1);
      expect(comments[0].content).toBe('Great post!');
    });
    const req = httpMock.expectOne(`${baseUrl}/posts/1/comments`);
    expect(req.request.method).toBe('GET');
    req.flush([mockComment]);
  });

  it('addComment() should POST a new comment', () => {
    const commentReq: CommentRequest = { userId: 'user1', content: 'Great post!' };
    service.addComment(1, commentReq).subscribe(c => {
      expect(c.id).toBe(10);
    });
    const req = httpMock.expectOne(`${baseUrl}/posts/1/comments`);
    expect(req.request.method).toBe('POST');
    req.flush(mockComment);
  });

  it('markBestAnswer() should PUT to mark a comment as best answer', () => {
    service.markBestAnswer(1, 10).subscribe();
    const req = httpMock.expectOne(`${baseUrl}/posts/1/comments/10/best-answer`);
    expect(req.request.method).toBe('PUT');
    req.flush(mockComment);
  });

  // --- Reactions ---

  it('reactToPost() should POST a reaction', () => {
    const reaction: ReactionRequest = { userId: 'user1', type: ReactionType.LIKE };
    service.reactToPost(1, reaction).subscribe();
    const req = httpMock.expectOne(`${baseUrl}/posts/1/reactions`);
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('getReactionCounts() should GET reaction counts for a post', () => {
    service.getReactionCounts(1).subscribe(counts => {
      expect(counts.likeCount).toBe(5);
    });
    const req = httpMock.expectOne(`${baseUrl}/posts/1/reactions/count`);
    expect(req.request.method).toBe('GET');
    req.flush({ likeCount: 5, dislikeCount: 1 });
  });

  // --- Reputation ---

  it('getUserReputation() should GET user reputation profile', () => {
    service.getUserReputation('user1').subscribe(profile => {
      expect(profile).toBeTruthy();
    });
    const req = httpMock.expectOne(`${baseUrl}/reputation/users/user1`);
    expect(req.request.method).toBe('GET');
    req.flush({ userId: 'user1', score: 100, level: 'INTERMEDIATE' });
  });

  it('getLeaderboard() should GET leaderboard with limit param', () => {
    service.getLeaderboard(5).subscribe(entries => {
      expect(entries.length).toBe(1);
    });
    const req = httpMock.expectOne(r =>
      r.url === `${baseUrl}/reputation/leaderboard` && r.params.get('limit') === '5'
    );
    expect(req.request.method).toBe('GET');
    req.flush([{ userId: 'user1', score: 100, rank: 1 }]);
  });
});
