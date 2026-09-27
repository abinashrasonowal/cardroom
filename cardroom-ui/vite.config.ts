import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import {defineConfig} from 'vite';

// `npm run dev` serves on :3000 and forwards the API and socket to cardroom-server on :8080,
// so the browser sees one origin and the identity cookie just works.
const SERVER = process.env.CARDROOM_SERVER ?? 'http://localhost:8080';

export default defineConfig(() => {
  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        '@': path.resolve(import.meta.dirname, 'src'),
      },
    },
    server: {
      proxy: {
        '/api': SERVER,
        '/ws': {target: SERVER, ws: true},
      },
    },
  };
});
