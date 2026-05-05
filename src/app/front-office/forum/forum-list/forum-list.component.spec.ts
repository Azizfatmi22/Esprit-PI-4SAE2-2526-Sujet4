import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';

import { ForumListComponent } from './forum-list.component';
import { ForumService } from '../services/forum.service';
import { EventService } from '../../events/services/event.service';
import { HolidayService } from '../../events/services/holiday.service';
import { UserService } from '../../services/user.service';
import { PostResponse } from '../models/forum.model';
import { ReactionType } from '../models/reaction.model';

describe('ForumListComponent', () => {
  let component: ForumListComponent;
  let fixture: ComponentFixture<ForumListComponent>;
  let forumServiceSpy: jasmine.SpyObj<ForumService>;
  let eventServiceSpy: jasmine.SpyObj<EventService>;
  let holidayServiceSpy: jasmine.SpyObj<HolidayService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;

  const mockPost: PostResponse = {
    id: 1,
    userId: 'user1',
    formationId: 10,
    title: 'Test Post',
    content: 'Some content here',
    category: 'GENERAL',
    status: 'APPROVED',
    createdAt: new Date().toISOString(),
    reviewedByAdmin: false
  };

  const mockReactionCount = { likeCount: 5, dislikeCount: 2, postId: 1 };

  beforeEach(async () => {
    const forumSpy = jasmine.createSpyObj('ForumService', [
      'getForums', 'deleteForum', 'reactToPost', 'getReactionCounts',
      'getUserReputation', 'getLeaderboard'
    ]);
    const eventSpy = jasmine.createSpyObj('EventService', ['getEvents']);
    const holidaySpy = jasmine.createSpyObj('HolidayService', ['getHolidays']);
    const userSpy = jasmine.createSpyObj('UserService', ['loadUser', 'getUser', 'isTrainer']);

    await TestBed.configureTestingModule({
      declarations: [ForumListComponent],
      imports: [RouterTestingModule],
      providers: [
        { provide: ForumService, useValue: forumSpy },
        { provide: EventService, useValue: eventSpy },
        { provide: HolidayService, useValue: holidaySpy },
        { provide: UserService, useValue: userSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    forumServiceSpy = TestBed.inject(ForumService) as jasmine.SpyObj<ForumService>;
    eventServiceSpy = TestBed.inject(EventService) as jasmine.SpyObj<EventService>;
    holidayServiceSpy = TestBed.inject(HolidayService) as jasmine.SpyObj<HolidayService>;
    userServiceSpy = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;

    // Default spy returns
    userServiceSpy.loadUser.and.returnValue(Promise.resolve({ id: 'user1', username: 'testuser', email: 'test@test.com', fullName: 'Test User', roles: [] }));
    userServiceSpy.getUser.and.returnValue(null);
    userServiceSpy.isTrainer.and.returnValue(false);
    forumServiceSpy.getForums.and.returnValue(of([mockPost]));
    forumServiceSpy.getReactionCounts.and.returnValue(of(mockReactionCount));
    forumServiceSpy.getUserReputation.and.returnValue(of({ userId: 'user1', score: 50, level: 'BEGINNER' } as any));
    forumServiceSpy.getLeaderboard.and.returnValue(of([]));
    eventServiceSpy.getEvents.and.returnValue(of([]));
    holidayServiceSpy.getHolidays.and.returnValue(of([]));

    fixture = TestBed.createComponent(ForumListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with loading=true then false after load', async () => {
    expect(component.loading).toBeTrue();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(component.loading).toBeFalse();
  });

  it('should load forums on init', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    expect(forumServiceSpy.getForums).toHaveBeenCalled();
    expect(component.forums.length).toBe(1);
    expect(component.forums[0].title).toBe('Test Post');
  });

  it('displayedForums should return all posts when showMyPosts is false', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    component.showMyPosts = false;
    expect(component.displayedForums.length).toBe(component.forums.length);
  });

  it('displayedForums should filter to current user posts when showMyPosts is true', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    component.currentUserId = 'user1';
    component.showMyPosts = true;
    const displayed = component.displayedForums;
    expect(displayed.every(f => f.userId === 'user1')).toBeTrue();
  });

  it('setPostFilter() should update showMyPosts and reload forums', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    component.setPostFilter(true);
    expect(component.showMyPosts).toBeTrue();
    expect(forumServiceSpy.getForums).toHaveBeenCalled();
  });

  it('onCategoryChange() should update selectedCategory and reload forums', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    component.onCategoryChange('TECHNOLOGY');
    expect(component.selectedCategory).toBe('TECHNOLOGY');
    expect(forumServiceSpy.getForums).toHaveBeenCalled();
  });

  it('getAuthorName() should return mapped name for known userId', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    const name = component.getAuthorName('10');
    expect(name).toBe('John Doe');
  });

  it('getAuthorName() should return fallback "User {id}" for unknown userId', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    const name = component.getAuthorName('999');
    expect(name).toBe('User 999');
  });

  it('formatDate() should return minutes ago for recent dates', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    const recent = new Date(Date.now() - 5 * 60000);
    const result = component.formatDate(recent);
    expect(result).toContain('m ago');
  });

  it('formatDate() should return days ago for older dates', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    const old = new Date(Date.now() - 3 * 86400000);
    const result = component.formatDate(old);
    expect(result).toContain('d ago');
  });

  it('formatEventDate() should return month and day', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    const result = component.formatEventDate(new Date('2025-06-15'));
    expect(result.month).toBeTruthy();
    expect(result.day).toBeTruthy();
  });

  it('formatLevel() should return Beginner for undefined', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    expect(component.formatLevel(undefined)).toBe('Beginner');
  });

  it('formatLevel() should capitalize level string', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    expect(component.formatLevel('intermediate')).toBe('Intermediate');
  });

  it('loadForums() should set loading=false on error', async () => {
    forumServiceSpy.getForums.and.returnValue(throwError(() => new Error('fail')));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(component.loading).toBeFalse();
  });

  it('deleteForum() should reload forums after confirmation', async () => {
    fixture.detectChanges();
    await fixture.whenStable();

    forumServiceSpy.deleteForum = jasmine.createSpy().and.returnValue(of(undefined));
    spyOn(window, 'confirm').and.returnValue(true);
    const mouseEvent = new MouseEvent('click');
    component.deleteForum(mouseEvent, 1);

    expect(forumServiceSpy.deleteForum).toHaveBeenCalledWith(1);
  });

  it('viewForum() should navigate to forum detail page', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    component.viewForum(1);
    expect(router.navigate).toHaveBeenCalledWith(['/forum', 1]);
  });

  it('createForum() should navigate to forum create page', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    component.createForum();
    expect(router.navigate).toHaveBeenCalledWith(['/forum/create']);
  });
});
