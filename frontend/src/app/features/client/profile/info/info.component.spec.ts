import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InfoComponent } from './info.component';

import { testProviders } from '@shared/testing/test-providers';

describe('InfoComponent', () => {
  let component: InfoComponent;
  let fixture: ComponentFixture<InfoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InfoComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(InfoComponent);
    component = fixture.componentInstance;
        fixture.componentRef.setInput('user', {});
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
