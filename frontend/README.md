# Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 20.1.4.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.

## Component standard

> Chuẩn duy nhất cho mọi component trong CinePlex frontend. Áp dụng trong mọi refactor:
> component đạt **10/10 checklist** bên dưới mới được coi là done.

### Cấu trúc component

```
┌─────────────────────────────────────────────────────────┐
│  TEMPLATE  (chỉ binding, @if/@for, (click)="handler()") │
│  KHÔNG có: logic, gọi service, điều kiện phức tạp       │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────┐
│  CLASS — thứ tự cố định (không đổi thứ tự)              │
│                                                         │
│  imports  →  @Component (OnPush)  →  selector           │
│                                                         │
│  inputs / outputs / model        ← nhận & phát          │
│  viewChild / contentChild                               │
│  signals / computed              ← state cục bộ         │
│  inject()                        ← DI                   │
│                                                         │
│  ngOnInit()   ≤ 2 dòng (khởi tạo store, đọc params)     │
│  handlers     ≤ 3 dòng MỖI cái, chỉ:                   │
│                  validate → store.method() / output()   │
│                                                         │
│  KHÔNG có: business logic, any, subscribe trần,         │
│           logic trong constructor                      │
└─────────────────────────────────────────────────────────┘
                          │
                 business/state (bắt buộc)
                          ▼
   signalStore  /  service  /  util thuần (pure function)
```

### Skeleton chuẩn

```ts
@Component({
  selector: 'app-movie',
  imports: [...],
  templateUrl: './movie.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,   // ← bắt buộc
})
export class MovieComponent {
  // 1. Inputs / Outputs
  readonly movies = input<Movie[]>([]);
  readonly load = output<{ page: number; size: number }>();

  // 2. Local state
  readonly isSearching = signal(false);
  readonly searchResults = computed(() => this.filterMovies());

  // 3. DI
  private store = inject(MovieStore);
  private modal = inject(ConfirmDialogService);   // helper dùng chung

  ngOnInit() {
    this.store.load({ page: 0, size: 10 });      // ≤ 2 dòng
  }

  // 4. Handlers — 1 dòng, chỉ gọi store / emit
  onQueryChange(p: { pageIndex: number; pageSize: number }) {
    this.store.load({ page: p.pageIndex - 1, size: p.pageSize });
  }
  onDelete(id: number) {
    this.modal.confirmDelete(() => this.store.remove(id));
  }
}
```

### Checklist chấp nhận (10 điểm)

| # | Kiểm tra |
|---|----------|
| 1 | Template không chứa logic/gọi service |
| 2 | `ChangeDetectionStrategy.OnPush` |
| 3 | input/output bằng **signal**, không decorator |
| 4 | Không có `any` |
| 5 | Không có `.subscribe()` trần (chỉ `takeUntilDestroyed`/`rxMethod`) |
| 6 | Business logic = 0 trong component (nằm store/service) |
| 7 | Mỗi handler ≤ 3 dòng |
| 8 | `ngOnInit` ≤ 2 dòng |
| 9 | Component ≤ ~150 dòng TS |
| 10 | Đọc xong là biết "nhận gì → hiển thị gì → phát gì", không cần mở store |

### Quy ước bổ sung

- Domain mapping (`toPaymentMethod`, filter phức tạp) → pure function trong `shared/utils` hoặc domain model, có test.
- Modal/confirm boilerplate → shared helper (`ConfirmDialogService`), không viết lặp trong component.
- State async → `signalStore` (`patchState`/`rxMethod`), không gán plain property trong callback.
- `ngOnInit` chỉ khởi tạo; rẽ nhánh phức tạp chuyển vào store method.
