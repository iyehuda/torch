var events = require('events');
var net = require('net');
var util = require('util');
var varint = require('varint');

function MirroringClient(socket) {
    events.EventEmitter.call(this);
    var self = this;
    this.currentLength = undefined;
    this.currentMessage = undefined;
    this.currentOffset = undefined;
    this.socket = socket;
    socket.on('data', function (data) {
        self.handleMessage(data);
    });
    socket.on('error', function (error) {
        console.log(`${self.toString} had an error: ${error}`);
    });
    socket.on('close', function (had_error) {
        console.log(`${self.toString()} disconnected`);
    });
}

util.inherits(MirroringClient, events.EventEmitter);

MirroringClient.prototype.toString = function toString() {
    return this.socket.name;
};

MirroringClient.prototype.handleMessage = function handleMessage(data) {
    if (this.currentLength == undefined) {
        if (this.currentMessage == undefined) {
            try {
                this.currentLength = varint.decode(data);
                this.currentOffset = 0;
                data = data.slice(varint.decode.bytes);
                this.handleMessage(data);
            }
            catch (error) {
                console.error(error);
                this.currentMessage = data;
            }
        }
        else {
            data = Buffer.concat(this.currentMessage, data);
            this.currentMessage = undefined;
            this.handleMessage(data);
        }
    }
    else {
        if (this.currentMessage == undefined) {
            this.currentMessage = Buffer.alloc(this.currentLength);
            this.currentOffset = 0;
        }

        var dataOffset = data.copy(this.currentMessage, this.currentOffset);
        this.currentOffset += dataOffset;

        if (this.currentOffset == this.currentLength) {
            this.emit('message', this.currentMessage);
            this.currentLength = undefined;
            this.currentMessage = undefined;
            this.currentOffset = undefined;
            if (dataOffset < data.length) {
                data = Buffer.slice(data, dataOffset);
                this.handleMessage(data);
            }
        }
    }
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
    client.on('message', function (message) {
        console.log('received message at size ' + message.length);
    });
};

MirroringServer.prototype.listen = function listen() {
    var self = this;
    this.server.listen(this.port, function () {
        console.log(`listening on port ${self.port}`);
    });
}

var createServer = function createServer() {
    return new MirroringServer();
}

module.exports = {
    MirroringServer,
    createServer
};
