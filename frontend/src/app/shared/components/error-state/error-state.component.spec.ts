import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ErrorStateComponent } from './error-state.component';
import { testProviders } from '@shared/testing/test-providers';

describe('ErrorStateComponent', () => {
  let fixture: ComponentFixture<ErrorStateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ErrorStateComponent],
      providers: [testProviders],
    }).compileComponents();
    fixture = TestBed.createComponent(ErrorStateComponent);
  });

  it('create an instance', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('emits retry when the retry button is clicked', () => {
    spyOn(fixture.componentInstance.retry, 'emit');
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('button');
    expect(btn).toBeTruthy();
    btn.click();
    expect(fixture.componentInstance.retry.emit).toHaveBeenCalledTimes(1);
  });

  it('hides the retry button when showRetry is false', () => {
    fixture.componentRef.setInput('showRetry', false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button')).toBeNull();
  });
});
