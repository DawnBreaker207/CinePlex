import { Directive, output, ElementRef, inject, Renderer2, DestroyRef } from '@angular/core';

@Directive({
  selector: '[appClickOutside]',
})
export class ClickOutsideDirective {
  private elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
  private renderer = inject(Renderer2);
  private destroyRef = inject(DestroyRef);
  readonly appClickOutside = output<void>();

  constructor() {
    const unlisten = this.renderer.listen('document', 'click', (event: MouseEvent) => {
      if (!this.elementRef.nativeElement.contains(event.target as Node)) {
        this.appClickOutside.emit();
      }
    });
    this.destroyRef.onDestroy(() => unlisten());
  }
}
