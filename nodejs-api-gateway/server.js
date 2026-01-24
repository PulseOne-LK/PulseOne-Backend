const express = require('express');
const httpProxy = require('http-proxy');
const cors = require('cors');

const app = express();
const PORT = 8000;

// CORS Configuration (matching Kong config)
const corsOptions = {
  origin: [
    'http://localhost:3000',
    'http://localhost:3001',
    'https://your-frontend-domain.com'
  ],
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS', 'HEAD'],
  allowedHeaders: [
    'Accept',
    'Authorization',
    'Content-Type',
    'X-CSRF-Token',
    'Origin',
    'X-Requested-With',
    'x-user-id',
    'x-user-role'
  ],
  exposedHeaders: ['Link', 'Authorization'],
  credentials: true,
  maxAge: 3600,
  preflightContinue: false
};

app.use(cors(corsOptions));

// Service configurations
const services = {
  auth: {
    target: 'http://localhost:8080',
    pathPrefix: '/auth'
  },
  profile: {
    target: 'http://localhost:8082',
    pathPrefix: '/profile'
  },
  appointments: {
    target: 'http://localhost:8083',
    pathPrefix: '/appointments'
  },
  inventory: {
    target: 'http://localhost:8084',
    pathPrefix: '/inventory'
  },
  prescription: {
    target: 'http://localhost:8085',
    pathPrefix: '/prescription'
  },
  video: {
    target: 'http://localhost:8086',
    pathPrefix: '/video'
  }
};

// Create proxy instances for each service
const proxies = {};
for (const [key, service] of Object.entries(services)) {
  proxies[key] = httpProxy.createProxyServer({
    target: service.target,
    changeOrigin: true,
    pathRewrite: {
      ['^' + service.pathPrefix]: '' // Remove path prefix before forwarding
    },
    onError: (err, req, res) => {
      console.error(`Error proxying to ${key} service:`, err.message);
      res.status(503).json({
        error: `${key} service unavailable`,
        message: err.message
      });
    }
  });

  // Log proxy requests
  proxies[key].on('proxyReq', (proxyReq, req, res) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.originalUrl} -> ${service.target}`);
  });
}

// Auth Service Route
app.use('/auth', (req, res) => {
  proxies.auth.web(req, res);
});

// Profile Service Route
app.use('/profile', (req, res) => {
  proxies.profile.web(req, res);
});

// Appointments Service Route
app.use('/appointments', (req, res) => {
  proxies.appointments.web(req, res);
});

// Inventory Service Route
app.use('/inventory', (req, res) => {
  proxies.inventory.web(req, res);
});

// Prescription Service Route
app.use('/prescription', (req, res) => {
  proxies.prescription.web(req, res);
});

// Video Consultation Service Route
app.use('/video', (req, res) => {
  proxies.video.web(req, res);
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'API Gateway is running' });
});

// Root endpoint
app.get('/', (req, res) => {
  res.status(200).json({
    message: 'PulseOne API Gateway',
    version: '1.0.0',
    availableRoutes: {
      auth: 'http://localhost:8000/auth',
      profile: 'http://localhost:8000/profile',
      appointments: 'http://localhost:8000/appointments',
      inventory: 'http://localhost:8000/inventory',
      prescription: 'http://localhost:8000/prescription',
      video: 'http://localhost:8000/video'
    },
    health: 'http://localhost:8000/health'
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`\n✓ API Gateway is running on http://localhost:${PORT}`);
  console.log(`✓ CORS enabled for: http://localhost:3000`);
  console.log(`\nProxied Services:`);
  console.log(`  - /auth          -> http://localhost:8080`);
  console.log(`  - /profile       -> http://localhost:8082`);
  console.log(`  - /appointments  -> http://localhost:8083`);
  console.log(`  - /inventory     -> http://localhost:8084`);
  console.log(`  - /prescription  -> http://localhost:8085`);
  console.log(`  - /video         -> http://localhost:8086`);
  console.log(`\nHealth Check: http://localhost:${PORT}/health\n`);
});
