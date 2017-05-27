var net = require('net');

function MirroringClient(socket) {
    var self = this;
    this.socket = socket;
    socket.on('data', function (data) {
        console.log(`${self.toString()} sent ${data.length} bytes`);
    });
    socket.on('error', function (error) {
        console.log(`${self.toString} had an error: ${error}`);
    });
    socket.on('close', function (had_error) {
        console.log(`${self.toString()} disconnected`);
    });
}

MirroringClient.prototype.toString = function toString() {
    return this.socket.name;
};

function MirroringServer() {
    var self = this;
    this.port = 27015;
    this.server = net.createServer(function (socket) {
        socket.name = `${socket.remoteAddress}:${socket.remotePort}`;
        console.log(`${socket.name} connected`);
        self.addClient(socket);
    });
}

MirroringServer.prototype.addClient = function addClient(socket) {
    var client = new MirroringClient(socket);
};

MirroringServer.prototype.listen = function listen() {
    var self = this;
    this.server.listen(this.port, function () {
        console.log(`istening on port ${self.port}`);
    });
}

var createServer = function createServer() {
    return new MirroringServer();
}

module.exports = {
    MirroringServer,
    createServer
};
