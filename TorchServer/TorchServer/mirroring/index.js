let MirroringClient = require('./MirroringClient');
let MirroringSession = require('./MirroringSession');
let MirroringServer = require('./MirroringServer');

let createServer = function createServer() {
    return new MirroringServer();
};

module.exports = {
	MirroringClient,
	MirroringSession,
    MirroringServer,
    createServer
};
