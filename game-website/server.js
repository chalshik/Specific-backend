const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 8080;

const MIME_TYPES = {
  '.html': 'text/html',
  '.js': 'text/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
};

const server = http.createServer((req, res) => {
  console.log(`${req.method} ${req.url}`);

  // Handle favicon.ico requests
  if (req.url === '/favicon.ico') {
    res.statusCode = 204;
    res.end();
    return;
  }

  // Normalize URL path
  let filePath = '.' + req.url;
  if (filePath === './') {
    filePath = './index.html';
  }

  // Get the full path
  const fullPath = path.join(__dirname, filePath);
  
  // Get file extension
  const extname = path.extname(fullPath).toLowerCase();
  
  // Get content type based on file extension
  const contentType = MIME_TYPES[extname] || 'application/octet-stream';

  // Read file
  fs.readFile(fullPath, (err, content) => {
    if (err) {
      if (err.code === 'ENOENT') {
        // Page not found
        console.log(`File not found: ${fullPath}`);
        
        // Try to serve index.html for any non-file path (for SPA routing)
        if (!extname) {
          fs.readFile(path.join(__dirname, 'index.html'), (err, content) => {
            if (err) {
              res.writeHead(404);
              res.end('404 Not Found');
              return;
            }
            
            res.writeHead(200, { 'Content-Type': 'text/html' });
            res.end(content, 'utf-8');
          });
          return;
        }
        
        res.writeHead(404);
        res.end('404 Not Found');
      } else {
        // Server error
        console.log(`Server error: ${err.code}`);
        res.writeHead(500);
        res.end(`Server Error: ${err.code}`);
      }
    } else {
      // Success
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(content, 'utf-8');
    }
  });
});

server.listen(PORT, () => {
  console.log(`Server running at http://localhost:${PORT}/`);
  console.log(`Press Ctrl+C to stop the server`);
});
