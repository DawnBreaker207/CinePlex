import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { ShowtimesComponent } from './showtimes.component';
import { testProviders } from '@shared/testing/test-providers';
import { environment } from '@env/environment';

describe('ShowtimesComponent', () => {
  let fixture: ComponentFixture<ShowtimesComponent>;
  let ctrl: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShowtimesComponent],
      providers: [testProviders],
    }).compileComponents();

    fixture = TestBed.createComponent(ShowtimesComponent);
    ctrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => ctrl.verify());

  it('loads showtimes exactly once on init with a preset theater (no duplicate fetch)', () => {
    fixture.componentRef.setInput('embedded', true);
    fixture.componentRef.setInput('presetTheaterId', 1);
    fixture.detectChanges();

    ctrl.expectOne(`${environment.apiUrl}/theater?page=0&size=100`).flush({
      code: 'OK',
      message: 'success',
      data: { content: [{ id: 1, name: 'Theater 1' }], pagination: {} },
    });

    // Must be exactly one — the effect must not double-fire on init.
    const req = ctrl.expectOne(
      (r) => r.url === `${environment.apiUrl}/showtime/theaters/1`,
    );
    req.flush({
      code: 'OK',
      message: 'success',
      data: { content: [], pagination: {} },
    });

    expect(fixture.componentInstance.theaterId()).toBe(1);
  });

  it('reloads showtimes when the preset theater id changes after init', () => {
    fixture.componentRef.setInput('embedded', true);
    fixture.componentRef.setInput('presetTheaterId', 1);
    fixture.detectChanges();

    ctrl.expectOne(`${environment.apiUrl}/theater?page=0&size=100`).flush({
      code: 'OK',
      message: 'success',
      data: {
        content: [
          { id: 1, name: 'Theater 1' },
          { id: 2, name: 'Theater 2' },
        ],
        pagination: {},
      },
    });
    ctrl
      .expectOne((r) => r.url === `${environment.apiUrl}/showtime/theaters/1`)
      .flush({ code: 'OK', message: 'success', data: { content: [], pagination: {} } });

    fixture.componentRef.setInput('presetTheaterId', 2);
    fixture.detectChanges();

    const req = ctrl.expectOne(
      (r) => r.url === `${environment.apiUrl}/showtime/theaters/2`,
    );
    req.flush({ code: 'OK', message: 'success', data: { content: [], pagination: {} } });

    expect(fixture.componentInstance.theaterId()).toBe(2);
  });
});
