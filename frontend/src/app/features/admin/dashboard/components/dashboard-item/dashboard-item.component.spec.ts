import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DashboardItemComponent } from './dashboard-item.component';

import { testProviders } from '@shared/testing/test-providers';

describe('DashboardItemComponent', () => {
  let component: DashboardItemComponent;
  let fixture: ComponentFixture<DashboardItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardItemComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardItemComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('icon', 'trending-up');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
