import { inject, Injectable } from '@angular/core';
import { environment } from '@env/environment';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class UploadService {
  URL = `${environment.apiUrl}/cloudinary`;
  private http = inject(HttpClient);

  uploadAssets(file: File) {
    const formData = new FormData();
    formData.append('image', file);
    return this.http
      .post<{ secure_url: string }>(`${this.URL}/upload`, formData)
      .pipe(map((res) => res.secure_url));
  }
}
