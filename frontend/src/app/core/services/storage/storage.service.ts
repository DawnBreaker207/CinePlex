import { Injectable } from '@angular/core';
import { Jwt } from '@core/models/jwt.model';
import Dexie, { Table } from 'dexie';
import { Movie } from '@domain/movie/models/movie.model';

@Injectable({
  providedIn: 'root',
})
export class StorageService extends Dexie {
  private readonly JWT_KEY = 'jwt';
  private readonly ACCESS_TOKEN_KEY = 'accessToken';
  private readonly SELECTED_THEATER_KEY = 'selectedTheaterId';
  movies!: Table<Movie, number>;

  constructor() {
    super('CinemaDB');
    this.version(1).stores({ movies: 'id, title' });
  }

  setJWT(jwt: Jwt) {
    if (!jwt || !jwt.accessToken) {
      console.error('setJWT called with invalid jwt:', jwt);
      return;
    }
    try {
      localStorage.setItem(this.JWT_KEY, JSON.stringify(jwt));
      localStorage.setItem(this.ACCESS_TOKEN_KEY, jwt.accessToken);
    } catch (error) {
      console.error('Failed to set JWT', error);
    }
  }

  getJwt() {
    try {
      const item = localStorage.getItem(this.JWT_KEY);
      return item ? JSON.parse(item) : null;
    } catch (error) {
      console.error('Failed to parse Jwt', error);
      return null;
    }
  }

  clearAuth() {
    localStorage.removeItem(this.JWT_KEY);
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
  }

  setItem<T>(key: string, value: T) {
    try {
      if (typeof value === 'string') {
        localStorage.setItem(key, value);
      } else {
        localStorage.setItem(key, JSON.stringify(value));
      }
    } catch (error) {
      console.error(`Failed to store ${key}`, error);
    }
  }

  getItem<T>(key: string): T | null {
    try {
      const data = localStorage.getItem(key);
      if (!data) return null;
      try {
        return JSON.parse(data);
      } catch {
        return data as T;
      }
    } catch (error) {
      console.error(`Failed to parse ${key}`, error);
      return null;
    }
  }

  setSelectedTheaterId(theaterId: number) {
    try {
      localStorage.setItem(this.SELECTED_THEATER_KEY, theaterId.toString());
    } catch (error) {
      console.error(`Failed to set selected theater `, theaterId);
    }
  }

  getSelectedTheaterId() {
    try {
      const id = localStorage.getItem(this.SELECTED_THEATER_KEY);
      return id ? Number(id) : null;
    } catch (error) {
      console.error(`Failed to get selected theater `);
      return null;
    }
  }

  clearSelectedTheaterId() {
    localStorage.removeItem(this.SELECTED_THEATER_KEY);
  }

  removeItem(key: string) {
    localStorage.removeItem(key);
  }
  async getMovies(page: number, size: number) {
    const offset = (page - 1) * size;

    const [content, total] = await Promise.all([
      this.movies.offset(offset).limit(size).toArray(),
      this.movies.count(),
    ]);
    return {
      content,
      pagination: {
        pageNumber: page,
        pageSize: size,
        totalElements: total,
      },
    };
  }

  async getMoviesCount() {
    return await this.movies.count();
  }

  async cacheMovie(movies: Movie[]) {
    await this.transaction('rw', this.movies, async () => {
      await this.movies.bulkPut(movies);
    });
  }

  async saveMovieCache(movie: Movie) {
    await this.movies.put(movie);
  }

  async deleteMovieCache(id: number) {
    await this.movies.delete(id);
  }

  async clearAllMovies() {
    await this.movies.clear();
  }
}
