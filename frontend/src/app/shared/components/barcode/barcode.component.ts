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
      try {
        JsBarcode(el, code, {
          format: 'CODE128',
          lineColor: '#000',
          width: 2,
          height: 50,
          displayValue: false,
          fontSize: 14,
          margin: 0,
        });
      } catch (_) {}
    }
  }
}
