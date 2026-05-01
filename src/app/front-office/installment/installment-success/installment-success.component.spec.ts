import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InstallmentSuccessComponent } from './installment-success.component';

describe('InstallmentSuccessComponent', () => {
  let component: InstallmentSuccessComponent;
  let fixture: ComponentFixture<InstallmentSuccessComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [InstallmentSuccessComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InstallmentSuccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
