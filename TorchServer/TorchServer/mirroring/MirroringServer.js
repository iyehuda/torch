let MirroringClient = require('./MirroringClient');
let MirroringSession = require('./MirroringSession');
let net = require("net");

function MirroringServer() {
    let self = this;
    this.incremental = 0;
    this.port = 27015;
    this.sessions = [];
    this.server = net.createServer(function (socket) {
        socket.name = `${socket.remoteAddress}:${socket.remotePort}`;
        console.log(`${socket.name} connected`);
        self.addClient(socket);
    });
}

/**
 * Adds a client to an available/new session
 * @param {net.Socket} socket
 */
MirroringServer.prototype.addClient = function addClient(socket) {
    let self = this;
    if(this.sessions.length === 0 || this.sessions[this.sessions.length - 1].isFull()) {
        console.log(`creating session #${this.incremental}`);
        let newSession = new MirroringSession(this.incremental++);
        newSession.on('close', function () {
            console.log(`removing session #${newSession.id}`);
            self.sessions = self.sessions.filter((s)=>s.id !== newSession.id);
        });
        this.sessions.push(newSession);
    }
    this.sessions[this.sessions.length - 1].addClient(new MirroringClient(socket));
};

MirroringServer.prototype.listen = function listen() {
    let self = this;
    this.server.listen(this.port, function () {
        console.log(`listening on port ${self.port}`);
    });
};

module.exports = MirroringServer;
