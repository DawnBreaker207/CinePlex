import { DateModel, IsDeleted } from '@core/models/common.model';

export interface Movie extends DateModel, IsDeleted {
  id: number;
  title: string;
  originalTitle: string;
  poster: string;
  backdrop: string;
  overview: string;
  duration: number;
  genres: string[];
  releaseDate: Date;
  language: string;
  country: string;
  imdbId: string;
  filmId: string;
  rating?: number;
}

export interface MovieRequest {
  title: string;
  originalTitle: string;
  poster: string;
  backdrop: string;
  overview: string;
  duration: number;
  genres: string[];
  releaseDate: Date;
  language: string;
  country: string;
  imdbId: string;
  filmId: string;
}

export interface ApiMovie {
  id: number;
  Title: string;
  Year: string;
  Genre: string;
  imdbId?: string;
  Poster?: string;
}
