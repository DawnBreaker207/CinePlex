import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AppSwitchComponent } from './app-switch.component';
import { testProviders } from '@shared/testing/test-providers';

describe('AppSwitchComponent', () => {
  let fixture: ComponentFixture<AppSwitchComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppSwitchComponent],
      providers: [testProviders],
    }).compileComponents();
    fixture = TestBed.createComponent(AppSwitchComponent);
  });

  it('create an instance', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('emits the toggled value', () => {
    spyOn(fixture.componentInstance.checkedChange, 'emit');
    fixture.componentRef.setInput('checked', false);
    fixture.componentInstance.toggle();
    expect(fixture.componentInstance.checkedChange.emit).toHaveBeenCalledWith(true);
  });

  it('emits false when checked', () => {
    spyOn(fixture.componentInstance.checkedChange, 'emit');
    fixture.componentRef.setInput('checked', true);
    fixture.componentInstance.toggle();
    expect(fixture.componentInstance.checkedChange.emit).toHaveBeenCalledWith(false);
  });
});
