import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LearningPathDetailComponent } from './learning-path-detail.component';

describe('LearningPathDetailComponent', () => {
  let component: LearningPathDetailComponent;
  let fixture: ComponentFixture<LearningPathDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [LearningPathDetailComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LearningPathDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
