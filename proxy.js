const express = require('express');
const httpProxy = require('http-proxy');
const path = require('path');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3000;

// Create a proxy server
const proxy = httpProxy.createProxyServer({
  changeOrigin: true
});

// Handle proxy errors
proxy.on('error', (err, req, res) => {
  console.error('Proxy error:', err);
  if (res.writeHead) {
    res.writeHead(500);
    res.end('Proxy error: ' + err.message);
  }
});

// Add CORS headers to proxied responses
proxy.on('proxyRes', (proxyRes) => {
  proxyRes.headers['Access-Control-Allow-Origin'] = '*';
});

// Enable CORS for all routes
app.use(cors());

// Serve static files
app.use(express.static(path.join(__dirname, './')));

// Proxy API requests to the backend
app.use('/api', (req, res) => {
  // Remove /api from the path
  req.url = req.url.replace(/^\/api/, '');
  proxy.web(req, res, {
    target: 'https://specific-backend.onrender.com'
  });
});

// WebSocket proxy
app.use('/ws-game', (req, res) => {
  proxy.web(req, res, {
    target: 'https://specific-backend.onrender.com',
    ws: true
  });
});

// Upgrade WebSocket connections
app.on('upgrade', (req, socket, head) => {
  if (req.url.startsWith('/ws-game')) {
    proxy.ws(req, socket, head, {
      target: 'https://specific-backend.onrender.com',
      ws: true
    });
  }
});

// Serve the HTML file for any other route
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'game-room-test.html'));
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error('Server error:', err);
  res.status(500).send('Something broke!');
});

// Start the server
app.listen(PORT, () => {
  console.log(`Proxy server running on port ${PORT}`);
  console.log(`Open http://localhost:${PORT} in your browser for testing`);
}).on('error', (err) => {
  console.error('Failed to start server:', err);
}); 