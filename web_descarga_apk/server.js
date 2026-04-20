const express = require('express');
const path = require('path');
const app = express();
const PORT = process.env.PORT || 3000;

// Servir archivos estáticos (CSS, Imágenes, JS)
app.use(express.static(path.join(__dirname, 'public')));

// Ruta Principal (Landing Page)
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Ruta de Login
app.get('/login', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'login.html'));
});

// Ruta de Descarga (Después del Login)
app.get('/download-success', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'download.html'));
});

app.listen(PORT, () => {
    console.log(`Servidor elegante corriendo en http://localhost:${PORT}`);
});
