var events = require('events');
var net = require('net');
var util = require('util');
var varint = require('varint');

function MirroringClient(socket) {
    events.EventEmitter.call(this);
    var self = this;
    this.currentLength = null;
    this.currentMessage = null;
    this.currentOffset = null;
    this.name = socket.name;
    this.socket = socket;
    socket.on('data', function (data) {
        self.handleMessage(data);
    });
    socket.on('error', function (error) {
        console.log(`${self.toString} had an error: ${error}`);
    });
    socket.on('close', function (had_error) {
        console.log(`${self.toString()} disconnected`);
        self.emit('close');
    });
}

util.inherits(MirroringClient, events.EventEmitter);

MirroringClient.prototype.toString = function toString() {
    return this.name || 'unknown socket';
};

MirroringClient.prototype.handleMessage = function handleMessage(data) {
    if (this.currentLength == null) {
        if (this.currentMessage == null) {
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
            this.currentMessage = null;
            this.handleMessage(data);
        }
    }
    else {
        if (this.currentMessage == null) {
            this.currentMessage = Buffer.alloc(this.currentLength);
            this.currentOffset = 0;
        }

        var dataOffset = data.copy(this.currentMessage, this.currentOffset);
        this.currentOffset += dataOffset;

        if (this.currentOffset == this.currentLength) {
            this.emit('message', this.currentMessage);
            this.currentLength = null;
            this.currentMessage = null;
            this.currentOffset = null;
            if (dataOffset < data.length) {
                data = data.slice(dataOffset);
                this.handleMessage(data);
            }
        }
    }
};

MirroringClient.prototype.send = function send(message) {
    this.socket.write(Buffer.from(varint.encode(message.length)));
    this.socket.write(message);
};

MirroringClient.prototype.destroy = function destroy() {
    this.currentLength = null;
    this.currentOffset = null;
    this.currentMessage = null;
    if (this.socket != null) {
        if (!this.socket.destroyed)
            this.socket.destroy();
        this.socket = null;
    }
};

function MirroringSession(id) {
    events.EventEmitter.call(this);
    var self = this;
    this.destroyed = false;
    this.id = id;
    this.closeCallback = function () {
        self.destroy();
    };
    this.sender = null;
    this.receiver = null;
}

util.inherits(MirroringSession, events.EventEmitter);

MirroringSession.prototype.isFull = function isFull() {
    return this.sender != null && this.receiver != null;  
}

MirroringSession.prototype.addClient = function addClient(client) {
    var self = this;
    client.on('close', this.closeCallback);
    if (this.receiver == null) {
        this.receiver = client;
    }
    else if (this.sender == null) {
        this.sender = client;
        this.sender.on('message', function (message) {
            console.log(`sending ${message.length} bytes to ${self.receiver.toString()}`);
            self.receiver.send(message);
        });
    }
}

MirroringSession.prototype.destroy = function destroy() {
    if (this.destroyed)
        return;
    this.destroyed = true;
    if (this.receiver != null) {
        this.receiver.removeListener('close', this.closeCallback);
        this.receiver.destroy();
        this.receiver = null;
    }
    if (this.sender != null) {
        this.sender.removeListener('close', this.closeCallback);
        this.sender.destroy();
        this.sender = null;
    }

    this.emit('close');
}

function MirroringServer() {
    var self = this;
    this.incremental = 0;
    this.port = 27015;
    this.sessions = [];
    this.server = net.createServer(function (socket) {
        socket.name = `${socket.remoteAddress}:${socket.remotePort}`;
        console.log(`${socket.name} connected`);
        self.addClient(socket);
    });
}

MirroringServer.prototype.addClient = function addClient(socket) {
    var self = this;
    var client = new MirroringClient(socket);
    client.on('message', function (message) {
        console.log('received message at size ' + message.length);
    });
    if (this.sessions.length == 0 || this.sessions[this.sessions.length - 1].isFull()) {
        console.log(`creating session #${this.incremental}`);
        var newSession = new MirroringSession(this.incremental++);
        newSession.on('close', function () {
            console.log(`removing session #${newSession.id}`);
            self.sessions = self.sessions.filter((e) => e.id != newSession.id);
        });
        this.sessions.push(newSession);
    }
    this.sessions[this.sessions.length - 1].addClient(client);
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
