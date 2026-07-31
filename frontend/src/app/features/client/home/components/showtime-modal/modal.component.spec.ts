import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ShowtimeModalComponent } from './modal.component';

import { testProviders } from '@shared/testing/test-providers';

describe('ShowtimeModalComponent', () => {
  let component: ShowtimeModalComponent;
  let fixture: ComponentFixture<ShowtimeModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShowtimeModalComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(ShowtimeModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
