export const environment = {
  // PRODUCTION: replaced at deploy time (CI env) with the real API origin.
  // Never point this at localhost/127.0.0.1 in a shipped build.
  apiUrl: 'https://api.cineplex.example.com/api/v1',
  tmbd: {
    apiUrl: 'https://api.themoviedb.org/3',
    imageUrl: 'https://image.tmdb.org/t/p/original',
    // PRODUCTION: inject via CI secrets, never commit the real token.
    token: 'TMDB_READ_TOKEN',
  },
};
