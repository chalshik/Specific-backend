const express = require('express');
const path = require('path');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3000;

// Enable CORS for all routes
app.use(cors());

// Serve static files from the current directory
app.use(express.static(path.join(__dirname, './')));

// Serve simple-test.html by default
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'simple-test.html'));
});

// Original game-room-test.html still available at this endpoint
app.get('/original', (req, res) => {
  res.sendFile(path.join(__dirname, 'game-room-test.html'));
});

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
  console.log(`Open http://localhost:${PORT} in your browser for testing`);
}); 