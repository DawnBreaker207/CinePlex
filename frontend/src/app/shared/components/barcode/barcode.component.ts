import {
  Component,
  ElementRef,
  input,
  OnInit,
  viewChild,
  ChangeDetectionStrategy,
} from '@angular/core';
import JsBarcode from 'jsbarcode';

@Component({
  selector: 'app-barcode',
  imports: [],
  templateUrl: './barcode.component.html',
  styleUrl: './barcode.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BarcodeComponent implements OnInit {
  value = input.required<string>();

  barcodeEl = viewChild.required<ElementRef<SVGElement>>('barcodeRef');

  ngOnInit() {
    const code = this.value();
    const el = this.barcodeEl()?.nativeElement;
    if (code && el) {
      // Ink follows the theme: pure black vanishes on dark ticket cards.
      const dark =
        typeof document !== 'undefined' &&
        document.documentElement.classList.contains('dark');
      try {
        JsBarcode(el, code, {
          format: 'CODE128',
          lineColor: dark ? '#edf2f7' : '#1a1a1a',
          width: 2,
          height: 50,
          displayValue: false,
          fontSize: 14,
          margin: 0,
        });
      } catch {
        // ignore invalid barcode input
      }
    }
  }
}
