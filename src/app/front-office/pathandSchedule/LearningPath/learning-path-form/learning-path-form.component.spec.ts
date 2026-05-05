import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LearningPathFormComponent } from './learning-path-form.component';

describe('LearningPathFormComponent', () => {
  let component: LearningPathFormComponent;
  let fixture: ComponentFixture<LearningPathFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [LearningPathFormComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LearningPathFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
