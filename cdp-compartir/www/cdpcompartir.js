var exec = require('cordova/exec');

var CdpCompartir = {
  compartir: function(nombre, contenido, texto, success, error) {
    exec(success, error, 'CdpCompartir', 'compartir', [nombre, contenido || '', texto || '']);
  }
};

module.exports = CdpCompartir;
