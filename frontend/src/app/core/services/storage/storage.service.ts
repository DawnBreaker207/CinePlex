import { Injectable } from '@angular/core';
import Dexie, { Table } from 'dexie';
import { Movie } from '@domain/movie/models/movie.model';

@Injectable({
  providedIn: 'root',
})
export class StorageService extends Dexie {
  private readonly SELECTED_THEATER_KEY = 'selectedTheaterId';
  movies!: Table<Movie, number>;

  constructor() {
    super('CinemaDB');
    this.version(1).stores({ movies: 'id, title' });
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
    } catch {
      console.error(`Failed to set selected theater `, theaterId);
    }
  }

  getSelectedTheaterId() {
    try {
      const id = localStorage.getItem(this.SELECTED_THEATER_KEY);
      return id ? Number(id) : null;
    } catch {
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
