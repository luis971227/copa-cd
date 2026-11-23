var exec = require('cordova/exec');

var CdpCompartir = {
  compartir: function(nombre, contenido, texto, success, error) {
    exec(success, error, 'CdpCompartir', 'compartir', [nombre, contenido || '', texto || '']);
  },
  abrir: function(nombre, success, error) {
    exec(success, error, 'CdpCompartir', 'abrir', [nombre || 'descarga.apk']);
  },
  anexar: function(nombre, b64, success, error) {
    exec(success, error, 'CdpCompartir', 'anexar', [nombre, b64]);
  },
  abrirDescargas: function(success, error) {
    exec(success, error, 'CdpCompartir', 'descargas', []);
  },
  instalar: function(nombre, texto, success, error) {
    exec(success, error, 'CdpCompartir', 'instalar', [nombre, texto || '']);
  },
  notificar: function(titulo, texto, success, error) {
    exec(success, error, 'CdpCompartir', 'notificar', [titulo || 'Copa CD', texto || '']);
  },
  http: function(url, metodo, cuerpo, ct, success, error) {
    exec(success, error, 'CdpCompartir', 'http', [url || '', metodo || 'GET', cuerpo || '', ct || '']);
  },
  bajar: function(url, nombre, progreso, success, error) {
    exec(function(x) {
      if (x && typeof x === 'object' && x.pct !== undefined) {
        if (progreso) progreso(x);
        return;
      }
      success(x);
    }, error, 'CdpCompartir', 'bajar', [url || '', nombre || 'descarga.apk']);
  }
};

module.exports = CdpCompartir;
