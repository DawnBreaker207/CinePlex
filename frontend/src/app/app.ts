import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NetworkService } from '@core/services/network/network.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private network = inject(NetworkService);
}
