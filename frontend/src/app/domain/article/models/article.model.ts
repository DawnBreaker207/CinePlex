export enum ArticleStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
}

export enum ArticleType {
  NEWS = 'NEWS',
  PROMOTION = 'PROMOTION',
  UNKNOWN = 'UNKNOWN',
}

export interface Article {
  id: number;
  title: string;
  summary: string;
  thumbnail: string;
  content: string;
  author: string;
  status: ArticleStatus;
  type: ArticleType;
  category: string;
  views: number;
  createdAt: string;
}

export interface ArticleRequest {
  title: string;
  thumbnail: string;
  summary: string;
  content: string;
  type: ArticleType;
  status: ArticleStatus;
}
