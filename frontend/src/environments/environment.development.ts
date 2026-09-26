export const environment = {
  // 127.0.0.1, not localhost: localhost resolves to ::1 on some stacks
  // while dev servers listen IPv4-only, breaking API calls intermittently.
  apiUrl: 'http://127.0.0.1:8888/api/v1',
  tmbd: {
    apiUrl: 'https://api.themoviedb.org/3',
    imageUrl: 'https://image.tmdb.org/t/p/original',
    token:
      'eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI3ZjgxYTVkNmVhYTVmZWViZjEzNWM5MTJjNzQ1YmI0MSIsIm5iZiI6MTc1MzcwMTQ4OC4zNzksInN1YiI6IjY4ODc1YzcwZTA4OGQ1NzhjNzhhNzRhYiIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.zCqZxPPYPUxf-b_MJPIyb4tgaN6F_TM_n3Jn9nK8pM8',
  },
};
