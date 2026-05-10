import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';
import proxy from 'express-http-proxy';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 8080;
const BACKEND_URL = process.env.BACKEND_URL || 'https://backend-pgu-tub-2026.azurewebsites.net';

// Proxy requests starting with /api to the Java backend
app.use('/api', proxy(BACKEND_URL, {
  proxyReqPathResolver: (req) => {
    return '/api' + req.url;
  },
  // Remove Origin/Referer headers — o pedido vem do servidor, não do browser
  proxyReqOptDecorator: (proxyReqOpts) => {
    delete proxyReqOpts.headers['origin'];
    delete proxyReqOpts.headers['referer'];
    return proxyReqOpts;
  }
}));

// Serve static files from the 'dist' directory
app.use(express.static(path.join(__dirname, 'dist')));

// SPA Fallback: Redirect all requests to index.html
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'dist', 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Frontend server running on port ${PORT}`);
  console.log(`SPA fallback enabled for all routes.`);
});
